package Utils;

public class TestOllama {
    public static void main(String[] args) {
        System.out.println("Testing Ollama Content Filter...");
        String text = "This is a test message with some bad words like 'merde' and 'salope'.";
        String filtered = OllamaContentFilterService.censorBadWords(text);
        System.out.println("Original: " + text);
        System.out.println("Filtered: " + filtered);

        System.out.println("\nTesting Ollama Reclamation Assistant...");
        String description = "J'ai un probleme avec mon cours de Java, le contenu ne s'affiche pas.";
        String response = OllamaReclamationAssistant.generateAutoResponse(description);
        System.out.println("Description: " + description);
        System.out.println("AI Response: " + response);
    }
}
