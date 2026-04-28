package Services;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class AIService {
    private final String API_URL = "http://127.0.0.1:5000/api/predict";
    private final HttpClient client;
    private final Gson gson;

    public AIService() {
        this.client = HttpClient.newHttpClient();
        this.gson = new Gson();
    }

    public CompletableFuture<Double> predictSuccess(int certifs, double niveau, int progression, int catMatch) {
        JsonObject payload = new JsonObject();
        JsonArray features = new JsonArray();
        features.add(certifs);
        features.add(niveau);
        features.add(progression);
        features.add(catMatch);
        
        payload.add("features", features);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(payload)))
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() == 200) {
                        try {
                            JsonObject jsonResponse = gson.fromJson(response.body(), JsonObject.class);
                            JsonObject prediction = jsonResponse.getAsJsonObject("prediction");
                            JsonArray probs = prediction.getAsJsonArray("probabilities");
                            // index 1 est la probabilité de succès
                            return probs.get(1).getAsDouble() * 100.0;
                        } catch (Exception e) {
                            System.err.println("Erreur parsing IA: " + e.getMessage());
                        }
                    } else {
                        System.err.println("Erreur IA (Flask): " + response.body());
                    }
                    return -1.0;
                })
                .exceptionally(ex -> {
                    System.err.println("Erreur connexion IA Flask: " + ex.getMessage());
                    return -1.0;
                });
    }
}
