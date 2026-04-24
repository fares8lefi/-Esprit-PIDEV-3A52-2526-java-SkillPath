package Utils;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ButtonBar;

import javax.sound.sampled.*;
import java.io.*;
import java.net.InetSocketAddress;
import java.awt.Desktop;
import java.net.URI;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VoiceRecognitionService {
    
    private static HttpServer server;
    private static final String WIT_AI_TOKEN = "KYMPZZSBSL7JLA4AO2TDBWK4JCRUFVYS";

    public static void startRecognition(String languageCode, Consumer<String> onResult) {
        if ("ar-TN".equals(languageCode)) {
            // Use Wit.ai API natively for Tounsi
            startWitAiRecording(onResult);
        } else {
            // Use Browser trick for French/English
            startBrowserRecognition(languageCode, onResult);
        }
    }

    private static void startWitAiRecording(Consumer<String> onResult) {
        try {
            // High CD-Quality rate to maximize accurate AI detection
            AudioFormat format = new AudioFormat(44100.0f, 16, 1, true, false);
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

            if (!AudioSystem.isLineSupported(info)) {
                Platform.runLater(() -> {
                    Alert a = new Alert(Alert.AlertType.ERROR, "Le microphone n'est pas supporté par le système Java.");
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
                // Loop correctly waiting for OS to initialize mic and record until user stops
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
                alert.setTitle("Dictée Tunisienne (Wit.ai)");
                alert.setHeaderText("🎤 Enregistrement en Tounsi...");
                alert.setContentText("Parlez maintenant, puis cliquez sur 'Arrêter' pour traduire.");
                
                ButtonType stopBtn = new ButtonType("Arrêter l'enregistrement", ButtonBar.ButtonData.OK_DONE);
                alert.getButtonTypes().setAll(stopBtn);
                
                alert.showAndWait();

                microphone.stop();
                microphone.close();

                // Convert captured raw audio to WAV and send
                try {
                    byte[] audioData = out.toByteArray();
                    ByteArrayInputStream bais = new ByteArrayInputStream(audioData);
                    AudioInputStream ais = new AudioInputStream(bais, format, audioData.length / format.getFrameSize());

                    ByteArrayOutputStream wavOut = new ByteArrayOutputStream();
                    AudioSystem.write(ais, AudioFileFormat.Type.WAVE, wavOut);
                    byte[] wavData = wavOut.toByteArray();

                    sendToWitAi(wavData, onResult);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void sendToWitAi(byte[] wavData, Consumer<String> onResult) {
        new Thread(() -> {
            try {
                URL url = new URL("https://api.wit.ai/speech?v=20240428");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Authorization", "Bearer " + WIT_AI_TOKEN);
                conn.setRequestProperty("Content-Type", "audio/wav");
                conn.setDoOutput(true);

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(wavData);
                }

                int responseCode = conn.getResponseCode();
                if (responseCode == 200) {
                    try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"))) {
                        StringBuilder response = new StringBuilder();
                        String line;
                        while ((line = br.readLine()) != null) {
                            response.append(line);
                        }
                        
                        String resStr = response.toString();

                        // Try multiple ways to parse the text gracefully
                        String text = "";
                        int lastIsFinal = resStr.lastIndexOf("\"is_final\":true");
                        if(lastIsFinal == -1) lastIsFinal = resStr.lastIndexOf("\"is_final\": true");
                        
                        if (lastIsFinal != -1) {
                            String segment = resStr.substring(0, lastIsFinal);
                            int textIdx = segment.lastIndexOf("\"text\":\"");
                            if(textIdx == -1) textIdx = segment.lastIndexOf("\"text\": \"");
                            
                            if (textIdx != -1) {
                                int endIdx = segment.indexOf("\"", textIdx + 9);
                                if(endIdx != -1) {
                                    text = segment.substring(textIdx + 9, endIdx);
                                }
                            }
                        } else {
                            int textIdx = resStr.lastIndexOf("\"text\":\"");
                            if(textIdx == -1) textIdx = resStr.lastIndexOf("\"text\": \"");
                            if (textIdx != -1) {
                                int endIdx = resStr.indexOf("\"", textIdx + 9);
                                if(endIdx != -1) {
                                    text = resStr.substring(textIdx + 9, endIdx);
                                }
                            }
                        }

                        if (!text.isEmpty()) {
                            text = decodeUnicode(text); // Arabic letters decode: \\uXXXX
                            final String finalText = text;
                            Platform.runLater(() -> onResult.accept(finalText));
                        }
                    }
                } else {
                    System.err.println("Wit.ai error code: " + responseCode);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private static String decodeUnicode(String unicode) {
        StringBuffer stringBuffer = new StringBuffer();
        Matcher matcher = Pattern.compile("\\\\u([0-9a-fA-F]{4})").matcher(unicode);
        while (matcher.find()) {
            matcher.appendReplacement(stringBuffer, String.valueOf((char) Integer.parseInt(matcher.group(1), 16)));
        }
        matcher.appendTail(stringBuffer);
        return stringBuffer.toString();
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
                            + "<style>body{font-family:sans-serif; text-align:center; padding-top:50px; background:#f4f4f9;}"
                            + "h1{color:#333;} #status{color:#666; font-size:18px; margin-top:20px;} .pulse{animation: pulse 1.5s infinite;}"
                            + "@keyframes pulse { 0% { transform: scale(1); } 50% { transform: scale(1.1); color: red; } 100% { transform: scale(1); } }</style></head>"
                            + "<body><h1 class='pulse'>🎤 Enregistrement en cours...</h1>"
                            + "<p id='status'>Veuillez parler maintenant.</p>"
                            + "<script>"
                            + "var recognition = new (window.SpeechRecognition || window.webkitSpeechRecognition)();"
                            + "recognition.lang = '" + languageCode + "';"
                            + "recognition.onresult = function(event) {"
                            + "    var text = event.results[0][0].transcript;"
                            + "    document.getElementById('status').innerText = 'Texte capturé: ' + text;"
                            + "    fetch('/submit', { method: 'POST', body: text }).then(() => {"
                            + "        document.getElementById('status').innerText += '\\n\\nVous pouvez fermer cette page.';"
                            + "        setTimeout(() => window.close(), 3000);"
                            + "    });"
                            + "};"
                            + "recognition.onerror = function(event) {"
                            + "    if(event.error === 'network') {"
                            + "        document.getElementById('status').innerHTML = 'Erreur : <b>network</b>.<br>Veuillez vous assurer que vous avez autorisé le microphone dans Chrome.';"
                            + "    } else {"
                            + "        document.getElementById('status').innerText = 'Erreur: ' + event.error + '. Veuillez réessayer.';"
                            + "    }"
                            + "};"
                            + "recognition.onend = function() {"
                            + "    document.querySelector('h1').classList.remove('pulse');"
                            + "    document.querySelector('h1').innerText = 'Terminé.';"
                            + "};"
                            + "recognition.start();"
                            + "</script></body></html>";

                    byte[] response = html.getBytes("UTF-8");
                    exchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
                    exchange.sendResponseHeaders(200, response.length);
                    OutputStream os = exchange.getResponseBody();
                    os.write(response);
                    os.close();
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
                        String text = new String(buffer.toByteArray(), "UTF-8");
                        
                        Platform.runLater(() -> onResult.accept(text));
                        
                        String response = "OK";
                        exchange.sendResponseHeaders(200, response.length());
                        OutputStream os = exchange.getResponseBody();
                        os.write(response.getBytes());
                        os.close();
                        
                        // Stop server shortly after receiving result
                        new Thread(() -> {
                            try { Thread.sleep(2000); } catch (Exception e) {}
                            server.stop(0);
                        }).start();
                    }
                }
            });

            server.setExecutor(null);
            server.start();

            boolean opened = false;
            String osName = System.getProperty("os.name").toLowerCase();
            if (osName.contains("win")) {
                try {
                    Runtime.getRuntime().exec("cmd /c start chrome http://localhost:8888");
                    opened = true;
                } catch (Exception e) {}
            }
            if (!opened && Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(new URI("http://localhost:8888"));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
