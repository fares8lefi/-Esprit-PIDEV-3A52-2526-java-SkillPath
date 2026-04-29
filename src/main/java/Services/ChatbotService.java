package Services;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

public class ChatbotService {
    private final String API_URL;
    private final HttpClient client;
    private final Gson gson;

    public ChatbotService() {
        this.API_URL = "http://127.0.0.1:5000/api/chat";
        this.client = HttpClient.newHttpClient();
        this.gson = new Gson();
        System.out.println("[Chatbot] Initialisé (Mode Proxy via Flask)");
    }

    public CompletableFuture<String> askQuestion(String userMessage) {
        JsonObject payload = new JsonObject();
        payload.addProperty("message", userMessage);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(payload)))
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() == 200) {
                        try {
                            JsonObject json = gson.fromJson(response.body(), JsonObject.class);
                            return json.get("reply").getAsString().trim();
                        } catch (Exception e) {
                            return "Erreur décodage : " + e.getMessage();
                        }
                    } else {
                        return "Erreur Serveur IA : " + response.body();
                    }
                })
                .exceptionally(ex -> "Erreur de connexion au serveur IA : " + ex.getMessage());
    }
}