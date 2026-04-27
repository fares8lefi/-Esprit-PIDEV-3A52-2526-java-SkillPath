package Services;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.github.cdimascio.dotenv.Dotenv;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

public class ChatbotService {
    private static final Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
    private final String API_TOKEN;
    private final String API_URL;
    private final HttpClient client;
    private final Gson gson;

    public ChatbotService() {
        String token = dotenv.get("GROQ_API_KEY");
        // Forçage du token si .env n'est pas lu par l'IDE
        this.API_TOKEN = (token != null && !token.trim().isEmpty()) ? token.trim() : "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";
        
        this.API_URL = "https://api.groq.com/openai/v1/chat/completions";
        this.client = HttpClient.newHttpClient();
        this.gson = new Gson();
        System.out.println("[Chatbot] Initialisé avec Groq.");
        System.out.println("[Chatbot] Token chargé : " + (API_TOKEN.length() > 5 ? API_TOKEN.substring(0, 5) + "..." : "Vide"));
    }

    public CompletableFuture<String> askQuestion(String userMessage) {
        // Message utilisateur
        JsonObject message = new JsonObject();
        message.addProperty("role", "user");
        message.addProperty("content", userMessage);

        JsonArray messages = new JsonArray();
        messages.add(message);

        // Payload
        JsonObject payload = new JsonObject();
        payload.add("messages", messages);
        // llama3-8b-8192 a été supprimé par Groq. On utilise le nouveau modèle valide !
        payload.addProperty("model", "llama-3.1-8b-instant"); 
        payload.addProperty("max_tokens", 500);
        payload.addProperty("temperature", 0.7);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .version(HttpClient.Version.HTTP_1_1)
                .header("Authorization", "Bearer " + API_TOKEN)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(payload)))
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    System.out.println("[Chatbot] Code: " + response.statusCode());
                    System.out.println("[Chatbot] Body: " + response.body());

                    if (response.statusCode() == 200) {
                        try {
                            JsonObject json = gson.fromJson(response.body(), JsonObject.class);
                            return json.getAsJsonArray("choices")
                                       .get(0).getAsJsonObject()
                                       .getAsJsonObject("message")
                                       .get("content").getAsString().trim();
                        } catch (Exception e) {
                            return "Erreur décodage : " + e.getMessage();
                        }
                    } else if (response.statusCode() == 401) {
                        return "❌ Token invalide. Vérifiez GROQ_API_KEY dans votre .env";
                    } else if (response.statusCode() == 429) {
                        return "⏳ Limite atteinte, réessayez dans quelques secondes.";
                    } else {
                        return "Erreur (Code: " + response.statusCode() + ") : " + response.body();
                    }
                })
                .exceptionally(ex -> "Erreur de connexion : " + ex.getMessage());
    }
}