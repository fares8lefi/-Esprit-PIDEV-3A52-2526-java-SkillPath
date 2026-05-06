package Services.evaluation;

import Models.evaluation.Question;
import io.github.cdimascio.dotenv.Dotenv;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

public class AIGeneratorService {

    private static final String API_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final String GROQ_MODEL = "llama-3.1-8b-instant";

    /**
     * Génère automatiquement une courte phrase de motivation.
     */
    public String generateMotivationalQuote() {
        try {
            Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
            String apiKey = dotenv.get("GROQ_API_KEY");
            if (apiKey == null || apiKey.isEmpty()) {
                return "Testez vos connaissances et évaluez votre progression";
            }
            
            String prompt = "Génère une très courte phrase motivante et inspirante en français (maximum 10 mots) pour encourager un étudiant à passer un quiz. Sois original et ne mets pas de guillemets. Aléa : " + System.currentTimeMillis();

            JSONObject jsonBody = new JSONObject()
                    .put("model", GROQ_MODEL)
                    .put("messages", new JSONArray()
                            .put(new JSONObject()
                                    .put("role", "user")
                                    .put("content", prompt)
                            )
                    );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody.toString()))
                    .build();

            HttpClient client = HttpClient.newHttpClient();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JSONObject jsonResponse = new JSONObject(response.body());
                String quote = jsonResponse.getJSONArray("choices")
                        .getJSONObject(0)
                        .getJSONObject("message")
                        .getString("content").trim();
                return quote.replace("\"", "").replace("\n", " ");
            } else {
                System.err.println("Groq API (Citation) a retourné l'erreur HTTP " + response.statusCode() + " : " + response.body());
            }
        } catch (Exception e) {
            System.err.println("Erreur API Groq (Citation) : " + e.getMessage());
        }
        return "Testez vos connaissances et évaluez votre progression";
    }

    /**
     * Génère automatiquement 5 questions pour un quiz en fonction du nom du cours.
     * @param courseName Le domaine ou nom du cours (ex: "Java Avancé", "DevOps")
     * @param idQuiz L'ID du quiz auquel attacher les questions
     * @return Une liste de 5 questions prêtes à être sauvegardées.
     */
    public List<Question> generateQuestions(String courseName, int idQuiz) {
        List<Question> questionsList = new ArrayList<>();
        
        try {
            // Chargement de l'API key depuis le fichier .env
            Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
            String apiKey = dotenv.get("GROQ_API_KEY");
            
            if (apiKey == null || apiKey.isEmpty()) {
                throw new RuntimeException("Erreur : GROQ_API_KEY introuvable ou vide dans le fichier .env !");
            }

            // Utilisation d'un message système pour cadrer l'IA
            String systemInstructions = "Tu es un générateur de quiz professionnel. " +
                    "Tu dois générer 5 questions QCM en français sur un sujet donné. " +
                    "Chaque question doit avoir un énoncé clair (la question), 4 choix (A, B, C, D), une bonne réponse (la lettre) et des points. " +
                    "Réponds UNIQUEMENT au format JSON.";

            String userPrompt = "Sujet du quiz : " + courseName + ". " +
                    "Graine aléatoire : " + System.currentTimeMillis() + ". " +
                    "Format attendu : { \"questions\": [ { \"enonce\": \"...\", \"choix_a\": \"...\", \"choix_b\": \"...\", \"choix_c\": \"...\", \"choix_d\": \"...\", \"bonne_reponse\": \"A\", \"points\": 2 } ] }";

            // Création du corps de la requête JSON avec messages System et User
            JSONObject requestBody = new JSONObject()
                    .put("model", GROQ_MODEL)
                    .put("response_format", new JSONObject().put("type", "json_object"))
                    .put("messages", new JSONArray()
                            .put(new JSONObject().put("role", "system").put("content", systemInstructions))
                            .put(new JSONObject().put("role", "user").put("content", userPrompt))
                    );

            // Création du client HTTP
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(API_URL))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                    .build();

            // Envoi de la requête
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                // Parsing de la réponse JSON de Groq
                JSONObject responseJson = new JSONObject(response.body());
                String aiTextContent = responseJson.getJSONArray("choices")
                        .getJSONObject(0)
                        .getJSONObject("message")
                        .getString("content");
                
                JSONObject resultObject = new JSONObject(aiTextContent);
                JSONArray questionsJsonArray = resultObject.getJSONArray("questions");
                
                for (int i = 0; i < questionsJsonArray.length(); i++) {
                    JSONObject qObj = questionsJsonArray.getJSONObject(i);
                    
                    Question question = new Question(
                            qObj.getString("enonce"),
                            qObj.getString("choix_a"),
                            qObj.getString("choix_b"),
                            qObj.getString("choix_c"),
                            qObj.getString("choix_d"),
                            qObj.getString("bonne_reponse"),
                            qObj.getInt("points"),
                            idQuiz
                    );
                    questionsList.add(question);
                }
            } else {
                System.err.println("Erreur de l'API Groq : HTTP " + response.statusCode());
                System.err.println(response.body());
            }

        } catch (Exception e) {
            System.err.println("Exception lors de l'appel à l'API Groq : " + e.getMessage());
            e.printStackTrace();
        }
        
        return questionsList;
    }

}
