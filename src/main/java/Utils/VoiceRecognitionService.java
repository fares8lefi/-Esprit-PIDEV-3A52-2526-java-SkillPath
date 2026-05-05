package Utils;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.TargetDataLine;
import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VoiceRecognitionService {

    private static final int PYTHON_SERVER_PORT = 5001;
    private static final String PYTHON_SERVER_URL = "http://localhost:" + PYTHON_SERVER_PORT + "/";
    private static final int SERVER_READY_RETRIES = 48; // 48 * 5s = 4 minutes
    private static final int SERVER_READY_SLEEP_MS = 5000;
    private static final Pattern STATUS_PATTERN = Pattern.compile("\"status\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern TEXT_PATTERN = Pattern.compile("\"text\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"");
    private static final Pattern MESSAGE_PATTERN = Pattern.compile("\"message\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"");

    private static HttpServer server;
    private static Process pythonServerProcess;
    private static boolean shutdownHookRegistered = false;

    public static void startRecognition(String languageCode, Consumer<String> onResult) {
        if ("ar-TN".equals(languageCode)) {
            startPythonLocalRecording(onResult);
        } else {
            startBrowserRecognition(languageCode, onResult);
        }
    }

    private static void startPythonLocalRecording(Consumer<String> onResult) {
        try {
            AudioFormat format = new AudioFormat(16000.0f, 16, 1, true, false);
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

            if (!AudioSystem.isLineSupported(info)) {
                Platform.runLater(() -> {
                    Alert a = new Alert(Alert.AlertType.ERROR, "Microphone is not supported by this system.");
                    a.show();
                });
                return;
            }

            TargetDataLine microphone = (TargetDataLine) AudioSystem.getLine(info);
            microphone.open(format);
            microphone.start();

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Thread recordingThread = new Thread(() -> {
                byte[] data = new byte[microphone.getBufferSize() / 5];
                while (microphone.isOpen()) {
                    int numBytesRead = microphone.read(data, 0, data.length);
                    if (numBytesRead > 0) {
                        out.write(data, 0, numBytesRead);
                    }
                }
            });
            recordingThread.setDaemon(true);
            recordingThread.start();

            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Tunisian Dictation (Local GPU)");
                alert.setHeaderText("Recording in Tunisian dialect...");
                alert.setContentText(
                        "Speak now then click stop.\n" +
                        "First run may take time because model files are downloaded once."
                );

                ButtonType stopBtn = new ButtonType("Stop recording", ButtonBar.ButtonData.OK_DONE);
                alert.getButtonTypes().setAll(stopBtn);
                alert.showAndWait();

                microphone.stop();
                microphone.close();

                try {
                    byte[] audioData = out.toByteArray();
                    ByteArrayInputStream bais = new ByteArrayInputStream(audioData);
                    AudioInputStream ais = new AudioInputStream(bais, format, audioData.length / format.getFrameSize());

                    Path tempWav = createTempWavPath();
                    AudioSystem.write(ais, AudioFileFormat.Type.WAVE, tempWav.toFile());
                    System.out.println("Saved temp audio: " + tempWav.toAbsolutePath());

                    runPythonAI(tempWav.toString(), onResult);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void runPythonAI(String audioFilePath, Consumer<String> onResult) {
        new Thread(() -> {
            try {
                ensurePythonServerRunning();
                String jsonInputString = "{\"audio_path\":\"" + escapeJson(audioFilePath) + "\"}";

                Exception lastConnectError = null;
                for (int retryCount = 1; retryCount <= SERVER_READY_RETRIES; retryCount++) {
                    try {
                        HttpURLConnection conn = (HttpURLConnection) new URL(PYTHON_SERVER_URL).openConnection();
                        conn.setRequestMethod("POST");
                        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                        conn.setConnectTimeout(5000);
                        conn.setReadTimeout(600000); // model inference can be long on first run
                        conn.setDoOutput(true);

                        byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
                        try (OutputStream os = conn.getOutputStream()) {
                            os.write(input, 0, input.length);
                        }

                        int code = conn.getResponseCode();
                        if (code == 200) {
                            processServerResponse(conn, onResult);
                            return;
                        }

                        String body = readConnectionBody(conn);
                        throw new IOException("TuniSpeech server returned HTTP " + code + ": " + body);
                    } catch (java.net.ConnectException e) {
                        lastConnectError = e;
                        System.out.println("Waiting for TuniSpeech server startup (" + retryCount + "/" + SERVER_READY_RETRIES + ")...");
                        Thread.sleep(SERVER_READY_SLEEP_MS);
                    }
                }

                throw new Exception(
                        "TuniSpeech server did not become ready in time. " +
                                "Check Python logs for model download/loading errors.",
                        lastConnectError
                );
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    Alert a = new Alert(Alert.AlertType.ERROR, "TuniSpeech error: " + e.getMessage());
                    a.show();
                });
            }
        }).start();
    }

    private static void processServerResponse(HttpURLConnection conn, Consumer<String> onResult) throws Exception {
        String res = readConnectionBody(conn);

        String status = extractJsonField(STATUS_PATTERN, res);
        if ("success".equals(status)) {
            String text = extractJsonField(TEXT_PATTERN, res);
            Platform.runLater(() -> onResult.accept(text == null ? "" : text.trim()));
            return;
        }

        String message = extractJsonField(MESSAGE_PATTERN, res);
        if (message == null || message.isBlank()) {
            message = "Unknown error: " + res;
        }
        throw new Exception(message);
    }

    private static String readConnectionBody(HttpURLConnection conn) throws IOException {
        InputStream is = conn.getResponseCode() >= 400 ? conn.getErrorStream() : conn.getInputStream();
        if (is == null) {
            return "";
        }

        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder responseChunks = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                responseChunks.append(line);
            }
            return responseChunks.toString();
        }
    }

    private static String extractJsonField(Pattern pattern, String json) {
        if (json == null) {
            return null;
        }
        Matcher matcher = pattern.matcher(json);
        if (!matcher.find()) {
            return null;
        }
        return unescapeJson(matcher.group(1));
    }

    private static String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }

    private static String unescapeJson(String value) {
        if (value == null) {
            return null;
        }
        return value
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
    }

    private static Path createTempWavPath() throws IOException {
        Path tempDir = Path.of(System.getProperty("java.io.tmpdir"));
        return Files.createTempFile(tempDir, "tounsi-", ".wav");
    }

    private static synchronized void ensurePythonServerRunning() throws Exception {
        if (pythonServerProcess != null && pythonServerProcess.isAlive()) {
            return;
        }
        startPythonServer();
    }

    private static void startPythonServer() throws Exception {
        String scriptPath = System.getProperty("user.dir") + File.separator + "tunispeech.py";
        File scriptFile = new File(scriptPath);
        if (!scriptFile.exists()) {
            throw new FileNotFoundException("Missing file: " + scriptPath);
        }

        String[] pythonCommand = resolvePythonCommand();
        List<String> command = new ArrayList<>();
        for (String part : pythonCommand) {
            command.add(part);
        }
        command.add(scriptPath);

        System.out.println("Starting local TuniSpeech server: " + String.join(" ", command));
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(new File(System.getProperty("user.dir")));

        String hfToken = System.getenv("HF_TOKEN");
        if (hfToken != null && !hfToken.isBlank()) {
            pb.environment().put("HF_TOKEN", hfToken);
        }

        pb.inheritIO();
        pythonServerProcess = pb.start();

        if (!shutdownHookRegistered) {
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                if (pythonServerProcess != null) {
                    pythonServerProcess.destroy();
                }
            }));
            shutdownHookRegistered = true;
        }
    }

    private static String[] resolvePythonCommand() throws Exception {
        String envPython = System.getenv("VOICE_PYTHON_EXE");
        if (envPython != null && !envPython.isBlank()) {
            String[] forced = new String[]{envPython};
            if (canRunCommand(forced)) {
                return forced;
            }
            throw new Exception("VOICE_PYTHON_EXE is set but not executable: " + envPython);
        }

        List<String[]> candidates = new ArrayList<>();
        candidates.add(new String[]{"python"});
        candidates.add(new String[]{"py", "-3"});
        candidates.add(new String[]{"py"});

        String userHome = System.getProperty("user.home");
        candidates.add(new String[]{userHome + "\\AppData\\Local\\Programs\\Python\\Python311\\python.exe"});
        candidates.add(new String[]{userHome + "\\AppData\\Local\\Programs\\Python\\Python310\\python.exe"});

        for (String[] candidate : candidates) {
            if (canRunCommand(candidate)) {
                return candidate;
            }
        }

        throw new Exception(
                "No Python interpreter found. Install Python 3.10+ or set VOICE_PYTHON_EXE to python.exe path."
        );
    }

    private static boolean canRunCommand(String[] prefix) {
        try {
            List<String> command = new ArrayList<>();
            for (String part : prefix) {
                command.add(part);
            }
            command.add("--version");

            Process process = new ProcessBuilder(command)
                    .redirectErrorStream(true)
                    .start();
            boolean completed = process.waitFor(5, TimeUnit.SECONDS);
            if (!completed) {
                process.destroyForcibly();
                return false;
            }
            return process.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private static void startBrowserRecognition(String languageCode, Consumer<String> onResult) {
        try {
            if (server != null) {
                server.stop(0);
            }
            server = HttpServer.create(new InetSocketAddress(8888), 0);

            server.createContext("/", new HttpHandler() {
                @Override
                public void handle(HttpExchange exchange) throws IOException {
                    String html = "<!DOCTYPE html><html><head><meta charset='UTF-8'><title>Voice Input</title>"
                            + "<style>body{font-family:sans-serif;text-align:center;padding-top:50px;background:#f4f4f9;}"
                            + "h1{color:#333;}#status{color:#666;font-size:18px;margin-top:20px;}.pulse{animation:pulse 1.5s infinite;}"
                            + "@keyframes pulse{0%{transform:scale(1);}50%{transform:scale(1.1);color:red;}100%{transform:scale(1);}}</style></head>"
                            + "<body><h1 class='pulse'>Recording...</h1>"
                            + "<p id='status'>Please speak now.</p>"
                            + "<script>"
                            + "var recognition = new (window.SpeechRecognition || window.webkitSpeechRecognition)();"
                            + "recognition.lang = '" + languageCode + "';"
                            + "recognition.onresult = function(event){"
                            + "var text = event.results[0][0].transcript;"
                            + "document.getElementById('status').innerText = 'Captured text: ' + text;"
                            + "fetch('/submit', { method: 'POST', body: text }).then(function(){"
                            + "document.getElementById('status').innerText += '\\n\\nYou can close this page.';"
                            + "setTimeout(function(){ window.close(); }, 3000);"
                            + "});"
                            + "};"
                            + "recognition.onerror = function(event){"
                            + "document.getElementById('status').innerText = 'Error: ' + event.error;"
                            + "};"
                            + "recognition.onend = function(){"
                            + "document.querySelector('h1').classList.remove('pulse');"
                            + "document.querySelector('h1').innerText = 'Done.';"
                            + "};"
                            + "recognition.start();"
                            + "</script></body></html>";

                    byte[] response = html.getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
                    exchange.sendResponseHeaders(200, response.length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(response);
                    }
                }
            });

            server.createContext("/submit", new HttpHandler() {
                @Override
                public void handle(HttpExchange exchange) throws IOException {
                    if ("POST".equals(exchange.getRequestMethod())) {
                        InputStream is = exchange.getRequestBody();
                        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                        int nRead;
                        byte[] data = new byte[1024];
                        while ((nRead = is.read(data, 0, data.length)) != -1) {
                            buffer.write(data, 0, nRead);
                        }
                        String text = buffer.toString(StandardCharsets.UTF_8);
                        Platform.runLater(() -> onResult.accept(text));

                        String response = "OK";
                        exchange.sendResponseHeaders(200, response.length());
                        try (OutputStream os = exchange.getResponseBody()) {
                            os.write(response.getBytes(StandardCharsets.UTF_8));
                        }

                        new Thread(() -> {
                            try {
                                Thread.sleep(2000);
                            } catch (Exception ignored) {
                            }
                            server.stop(0);
                        }).start();
                    }
                }
            });

            server.setExecutor(null);
            server.start();

            boolean opened = false;
            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                try {
                    Runtime.getRuntime().exec("cmd /c start chrome http://localhost:8888");
                    opened = true;
                } catch (Exception ignored) {
                }
            }
            if (!opened && Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(new URI("http://localhost:8888"));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
