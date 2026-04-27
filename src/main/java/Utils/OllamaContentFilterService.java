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
    private static final Pattern PARTIAL_MASKED_TOKEN_PATTERN = Pattern.compile("(?iu)(?<!\\p{L}|\\p{N})([\\p{L}\\p{N}_-]*\\*{2,}[\\p{L}\\p{N}_-]*)(?!\\p{L}|\\p{N})");

    private OllamaContentFilterService() {
    }

    public static String censorBadWords(String inputText) {
        if (inputText == null || inputText.isBlank()) {
            return inputText;
        }

        List<String> aiTerms = detectBadTermsWithOllama(inputText);
        if (!aiTerms.isEmpty()) {
            return maskDetectedTerms(inputText, aiTerms);
        }

        String directCensored = directCensorWithOllama(inputText);
        if (directCensored == null || directCensored.isBlank()) {
            return inputText;
        }
        return normalizeCensoredText(directCensored);
    }

    private static List<String> detectBadTermsWithOllama(String inputText) {
        try {
            String responseText = callOllama(buildDetectionPrompt(inputText));
            if (responseText == null || responseText.isBlank()) {
                return List.of();
            }
            return parseBadTermsFromModelResponse(responseText);
        } catch (Exception e) {
            return List.of();
        }
    }

    private static String directCensorWithOllama(String inputText) {
        try {
            String response = callOllama(buildDirectCensorPrompt(inputText));
            if (response == null || response.isBlank()) {
                return inputText;
            }
            return response;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to censor text with Ollama: " + e.getMessage(), e);
        }
    }

    private static String buildDetectionPrompt(String text) {
        return "Detect only offensive, insulting, abusive, hate, or sexual vulgar terms in this text in ANY language.\n"
                + "Return ONLY valid JSON and nothing else.\n"
                + "JSON format: {\"bad_terms\":[\"exact term from input\", \"another exact term\"]}\n"
                + "Rules:\n"
                + "1) Include only terms that are explicitly offensive in context.\n"
                + "2) Terms must appear exactly in the input.\n"
                + "3) Support French, English, Arabic, Tunisian dialect, and Arabizi (Latin+digits).\n"
                + "4) If none, return {\"bad_terms\":[]}.\n"
                + "Input:\n"
                + text;
    }

    private static String buildDirectCensorPrompt(String text) {
        return "Censor this text.\n"
                + "Replace offensive terms only with exactly *****.\n"
                + "Keep all other characters unchanged.\n"
                + "Keep original language and order.\n"
                + "Support French, English, Arabic, Tunisian dialect, and Arabizi (Latin+digits).\n"
                + "Output only censored text.\n"
                + "Input:\n"
                + text;
    }

    private static List<String> parseBadTermsFromModelResponse(String responseText) {
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
            throw new IllegalStateException("Invalid Ollama filter format: missing bad_terms array.");
        }

        String arrayBody = blockMatcher.group(1);
        Matcher itemMatcher = ARRAY_ITEM_PATTERN.matcher(arrayBody);
        Set<String> terms = new LinkedHashSet<>();
        while (itemMatcher.find()) {
            String term = unescapeJson(itemMatcher.group(1)).trim();
            if (!term.isBlank() && term.length() <= 80) {
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
            String trimmed = term == null ? "" : term.trim();
            if (trimmed.isEmpty()) {
                continue;
            }

            Pattern p = buildTermPattern(trimmed);
            Matcher m = p.matcher(masked);
            masked = m.replaceAll("*****");
        }
        return masked;
    }

    private static Pattern buildTermPattern(String term) {
        boolean hasOnlyLettersDigitsSpaces = term.codePoints()
                .allMatch(cp -> Character.isLetterOrDigit(cp) || Character.isWhitespace(cp));

        if (hasOnlyLettersDigitsSpaces) {
            return Pattern.compile("(?iu)(?<!\\p{L})" + Pattern.quote(term) + "(?!\\p{L})");
        }
        return Pattern.compile(Pattern.quote(term), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    }

    private static String normalizeCensoredText(String rawText) {
        String text = rawText.trim();
        if (text.startsWith("```")) {
            text = text.replace("```", "").trim();
        }
        if (text.startsWith("\"") && text.endsWith("\"") && text.length() >= 2) {
            text = text.substring(1, text.length() - 1);
        }

        Matcher m = PARTIAL_MASKED_TOKEN_PATTERN.matcher(text);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String token = m.group(1);
            boolean hasLetterOrDigit = token.codePoints().anyMatch(cp -> Character.isLetterOrDigit(cp));
            if (hasLetterOrDigit) {
                m.appendReplacement(sb, "*****");
            } else {
                m.appendReplacement(sb, Matcher.quoteReplacement(token));
            }
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private static String normalizeBaseUrl(String value) {
        String url = value == null || value.isBlank() ? DEFAULT_OLLAMA_URL : value.trim();
        while (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        return url;
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
        conn.setConnectTimeout(3000);
        conn.setReadTimeout(20000);
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

        String responseText = extractResponseText(body);
        if (responseText == null) {
            throw new IllegalStateException("Invalid Ollama response: missing response field.");
        }
        return responseText;
    }

    private static String extractResponseText(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        Matcher matcher = RESPONSE_PATTERN.matcher(json);
        if (!matcher.find()) {
            return null;
        }
        return unescapeJson(matcher.group(1));
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
