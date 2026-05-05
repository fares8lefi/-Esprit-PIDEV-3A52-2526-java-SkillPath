package Utils;

import io.github.cdimascio.dotenv.Dotenv;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class OllamaReclamationAssistant {
    private static final Dotenv DOTENV = Dotenv.configure().ignoreIfMissing().load();
    private static final String DEFAULT_OLLAMA_URL = "http://localhost:11434";
    private static final String DEFAULT_OLLAMA_MODEL = "llama3";
    private static final Pattern RESPONSE_PATTERN = Pattern.compile("\"response\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"");
    public static final String WELCOME_MESSAGE = "\uD83D\uDC4B Bienvenue sur SkillPath !\n\n"
            + "Merci pour votre r\u00E9clamation. Notre \u00E9quipe l\u2019a bien re\u00E7ue et vous r\u00E9pondra dans les plus brefs d\u00E9lais.\n\n"
            + "Merci pour votre confiance \uD83D\uDC99\n"
            + "L\u2019\u00E9quipe SkillPath";

    private OllamaReclamationAssistant() {
    }

    /**
     * Genere une reponse automatique via Llama 3 pour une reclamation.
     *
     * @param description La description de la reclamation.
     * @return La reponse generee apres le message d'accueil.
     */
    public static String generateAutoResponse(String description) {
        if (description == null || description.isBlank()) {
            return null;
        }

        System.out.println("[AI] Generation de reponse pour: " + (description.length() > 30 ? description.substring(0, 30) + "..." : description));
        String response = generateAssistantResponse(description, null);
        if (response == null || response.isBlank()) {
            System.err.println("[AI] Ollama a retourne une reponse vide ou null.");
            return null;
        }

        System.out.println("[AI] Reponse generee avec succes (" + response.length() + " caracteres).");
        return response.trim();
    }

    public static String generateAssistantResponse(String question, String reclamationContext) {
        if (question == null || question.isBlank()) {
            return null;
        }

        String prompt = "Tu es l'assistant support de SkillPath, une application de gestion de carri\u00E8re et de formation.\n"
                + "Tu r\u00E9ponds uniquement aux questions li\u00E9es \u00E0 SkillPath: r\u00E9clamations, cours, formations, profil, compte, bugs, navigation, suivi et support.\n"
                + "Si la question n'est pas li\u00E9e \u00E0 SkillPath, dis poliment en une seule phrase que tu peux aider seulement sur SkillPath.\n"
                + "R\u00E9ponds dans la m\u00EAme langue que l'utilisateur.\n"
                + "Sois clair, utile et court: 1 \u00E0 3 phrases maximum.\n";

        if (reclamationContext != null && !reclamationContext.isBlank()) {
            prompt += "\nContexte de la r\u00E9clamation:\n" + reclamationContext + "\n";
        }

        prompt += "\nMessage de l'utilisateur:\n" + question;

        try {
            String response = callOllama(prompt);
            return response == null ? null : response.trim();
        } catch (Exception e) {
            System.err.println("[AI] Erreur assistant SkillPath: " + e.getMessage());
            return null;
        }
    }

    private static String callOllama(String prompt) throws Exception {
        String ollamaUrl = readConfig("OLLAMA_URL", DEFAULT_OLLAMA_URL);
        String ollamaModel = readConfig("OLLAMA_MODEL", DEFAULT_OLLAMA_MODEL);
        String endpoint = normalizeBaseUrl(ollamaUrl) + "/api/generate";

        String requestBody = "{"
                + "\"model\":\"" + escapeJson(ollamaModel) + "\","
                + "\"prompt\":\"" + escapeJson(prompt) + "\","
                + "\"stream\":false,"
                + "\"options\":{\"temperature\":0.7}"
                + "}";

        HttpURLConnection conn = (HttpURLConnection) new URL(endpoint).openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(30000);
        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = requestBody.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        int code = conn.getResponseCode();
        if (code != 200) {
            return null;
        }

        try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }

            Matcher m = RESPONSE_PATTERN.matcher(sb.toString());
            if (m.find()) {
                return unescapeJson(m.group(1));
            }
        }
        return null;
    }

    private static String readConfig(String key, String fallback) {
        String env = System.getenv(key);
        if (env != null && !env.isBlank()) {
            return env.trim();
        }
        String dot = DOTENV.get(key);
        return (dot != null && !dot.isBlank()) ? dot.trim() : fallback;
    }

    private static String normalizeBaseUrl(String url) {
        if (url == null) {
            return DEFAULT_OLLAMA_URL;
        }
        url = url.trim();
        while (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        return url;
    }

    private static String escapeJson(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n")
                .replace("\t", "\\t");
    }

    private static String unescapeJson(String value) {
        return value
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
    }
}
