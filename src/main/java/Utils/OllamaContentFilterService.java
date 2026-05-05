package Utils;

import io.github.cdimascio.dotenv.Dotenv;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class OllamaContentFilterService {

    private static final Dotenv DOTENV = Dotenv.configure().ignoreIfMissing().load();
    private static final String DEFAULT_OLLAMA_URL = "http://localhost:11434";
    private static final String DEFAULT_OLLAMA_MODEL = "llama3";
    private static final Pattern RESPONSE_PATTERN = Pattern.compile("\"response\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"");
    private static final Pattern BAD_TERMS_BLOCK_PATTERN = Pattern.compile("\"bad_terms\"\\s*:\\s*\\[(.*?)]", Pattern.DOTALL);
    private static final Pattern ARRAY_ITEM_PATTERN = Pattern.compile("\"((?:\\\\.|[^\"\\\\])*)\"");

    private OllamaContentFilterService() {
    }

    public static String censorBadWords(String inputText) {
        if (inputText == null || inputText.isBlank()) {
            return inputText;
        }

        try {
            List<String> badTerms = detectBadTerms(inputText);
            if (badTerms.isEmpty()) {
                return inputText;
            }
            return maskDetectedTerms(inputText, badTerms);
        } catch (Exception e) {
            System.err.println("[AI Filter] Error connecting to Ollama: " + e.getMessage());
            System.err.println("[AI Filter] Skipping AI content filtering. Please ensure Ollama is running at " + readConfig("OLLAMA_URL", DEFAULT_OLLAMA_URL));
            return inputText; // Fallback: return original text without filtering
        }
    }

    private static List<String> detectBadTerms(String inputText) throws Exception {
        String responseText = callOllama(buildDetectionPrompt(inputText));
        if (responseText == null || responseText.isBlank()) {
            return List.of();
        }
        return parseBadTermsFromModelResponse(responseText, inputText);
    }

    private static String buildDetectionPrompt(String text) {
        return "You are a profanity detector for a SkillPath support application.\n"
                + "Task: identify ONLY words or short expressions in the input that are clearly vulgar, insulting, abusive, hateful, sexual profanity, or direct harassment.\n"
                + "Return ONLY valid JSON with this exact format: {\"bad_terms\":[]}\n"
                + "Rules:\n"
                + "1) Do not rewrite, translate, censor, explain, or add text.\n"
                + "2) Each bad_terms item must be copied exactly from the input text.\n"
                + "3) Do not include normal complaint words, technical words, course names, usernames, or neutral negative words.\n"
                + "4) Support French, English, Arabic, and Tunisian dialect/transliteration.\n"
                + "5) If there is no clear profanity, return {\"bad_terms\":[]}.\n\n"
                + "Input:\n"
                + text;
    }

    private static List<String> parseBadTermsFromModelResponse(String responseText, String inputText) {
        String cleaned = responseText.trim();
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.replace("```json", "").replace("```", "").trim();
        }

        int jsonStart = cleaned.indexOf('{');
        int jsonEnd = cleaned.lastIndexOf('}');
        if (jsonStart >= 0 && jsonEnd > jsonStart) {
            cleaned = cleaned.substring(jsonStart, jsonEnd + 1);
        }

        Matcher blockMatcher = BAD_TERMS_BLOCK_PATTERN.matcher(cleaned);
        if (!blockMatcher.find()) {
            return List.of();
        }

        String arrayBody = blockMatcher.group(1);
        Matcher itemMatcher = ARRAY_ITEM_PATTERN.matcher(arrayBody);
        Set<String> terms = new LinkedHashSet<>();
        while (itemMatcher.find()) {
            String term = unescapeJson(itemMatcher.group(1)).trim();
            if (!term.isBlank() && term.length() <= 80 && containsIgnoreCase(inputText, term)) {
                terms.add(term);
            }
        }

        return new ArrayList<>(terms);
    }

    private static String maskDetectedTerms(String text, List<String> badTerms) {
        List<String> terms = new ArrayList<>(badTerms);
        terms.sort(Comparator.comparingInt(String::length).reversed());

        String masked = text;
        for (String term : terms) {
            Pattern pattern = buildTermPattern(term);
            Matcher matcher = pattern.matcher(masked);
            String replacement = repeatStars(term.length());
            masked = matcher.replaceAll(Matcher.quoteReplacement(replacement));
        }
        return masked;
    }

    private static Pattern buildTermPattern(String term) {
        boolean wordLike = term.codePoints()
                .allMatch(cp -> Character.isLetterOrDigit(cp) || Character.isWhitespace(cp) || cp == '_' || cp == '-');

        if (wordLike) {
            return Pattern.compile("(?iu)(?<!\\p{L}|\\p{N})" + Pattern.quote(term) + "(?!\\p{L}|\\p{N})");
        }
        return Pattern.compile(Pattern.quote(term), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    }

    private static String repeatStars(int count) {
        StringBuilder sb = new StringBuilder(Math.max(1, count));
        for (int i = 0; i < Math.max(1, count); i++) {
            sb.append('*');
        }
        return sb.toString();
    }

    private static boolean containsIgnoreCase(String text, String term) {
        return text.toLowerCase().contains(term.toLowerCase());
    }

    private static String callOllama(String prompt) throws Exception {
        String ollamaUrl = readConfig("OLLAMA_URL", DEFAULT_OLLAMA_URL);
        String ollamaModel = readConfig("OLLAMA_MODEL", DEFAULT_OLLAMA_MODEL);
        String endpoint = normalizeBaseUrl(ollamaUrl) + "/api/generate";

        String requestBody = "{"
                + "\"model\":\"" + escapeJson(ollamaModel) + "\","
                + "\"prompt\":\"" + escapeJson(prompt) + "\","
                + "\"stream\":false,"
                + "\"options\":{\"temperature\":0}"
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
        String body = readBody(conn, code >= 400);
        if (code != 200) {
            throw new IllegalStateException("Ollama filter HTTP error " + code + ": " + body);
        }

        Matcher matcher = RESPONSE_PATTERN.matcher(body);
        if (!matcher.find()) {
            return null;
        }
        return unescapeJson(matcher.group(1));
    }

    private static String readConfig(String key, String fallback) {
        String env = System.getenv(key);
        if (env != null && !env.isBlank()) {
            return env.trim();
        }

        String dot = DOTENV.get(key);
        if (dot != null && !dot.isBlank()) {
            return dot.trim();
        }
        return fallback;
    }

    private static String normalizeBaseUrl(String value) {
        String url = value == null || value.isBlank() ? DEFAULT_OLLAMA_URL : value.trim();
        while (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        return url;
    }

    private static String readBody(HttpURLConnection conn, boolean fromError) throws Exception {
        InputStream is = fromError ? conn.getErrorStream() : conn.getInputStream();
        if (is == null) {
            return "";
        }

        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        }
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
        String s = value
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");

        Matcher unicodeMatcher = Pattern.compile("\\\\u([0-9a-fA-F]{4})").matcher(s);
        StringBuffer sb = new StringBuffer();
        while (unicodeMatcher.find()) {
            unicodeMatcher.appendReplacement(sb, String.valueOf((char) Integer.parseInt(unicodeMatcher.group(1), 16)));
        }
        unicodeMatcher.appendTail(sb);
        return sb.toString();
    }
}
