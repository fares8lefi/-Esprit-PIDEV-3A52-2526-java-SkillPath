package Services;

import Models.Course;
import Models.User;
import Models.UserCourseProgress;

import java.util.concurrent.CompletableFuture;

public class PredictionService {
    private final CertificateService certificateService = new CertificateService();
    private final ProgressService progressService = new ProgressService();
    private final AIService aiService = new AIService();

    private final RecommendationService recommendationService = new RecommendationService();

    public CompletableFuture<Double> predictSuccess(User user, Course course, int totalModules) {
        // 1. Calculer les certificats (Certifs)
        int certifs = certificateService.countCertificates(user.getId());

        // 2. Calculer le niveau (Priorité: Profil > Clics/Session > Défaut)
        String rawNiveau = user.getNiveau();
        if (rawNiveau == null || rawNiveau.isEmpty() || rawNiveau.equalsIgnoreCase("Débutant")) {
            String preferredLevel = recommendationService.getPreferredLevel();
            if (preferredLevel != null) rawNiveau = preferredLevel;
        }
        if (rawNiveau == null) rawNiveau = "Débutant";
        double niveau = encodeNiveau(rawNiveau);

        // 3. Calculer la progression
        int progression = 0;
        UserCourseProgress progress = progressService.getProgress(user.getId(), course.getId());
        if (progress != null && totalModules > 0) {
            int actualCompleted = progress.getCompletedModules();
            progression = (int) (((double) actualCompleted / totalModules) * 100);
            
            // On ne force 100% que si l'utilisateur a VRAIMENT fini tous les modules actuels
            if (progress.isCompleted() && actualCompleted < totalModules) {
                // Si le cours a évolué (nouveaux modules), il n'est plus à 100%
                progression = (int) (((double) actualCompleted / totalModules) * 100);
            } else if (progress.isCompleted()) {
                progression = 100;
            }
        }

        // 4. Calculer le Category Match (Priorité: Profil > Clics/Session > N/A)
        int catMatch = 0;
        String userDomain = user.getDomaine();
        if (userDomain == null || userDomain.isEmpty()) {
            userDomain = recommendationService.getPreferredCategory();
        }
        if (userDomain == null) userDomain = "N/A";
        
        String courseCat = course.getCategory() != null ? course.getCategory() : "N/A";

        System.out.println("DEBUG IA DYNAMIQUE - User[" + user.getUsername() + "] | Domaine Detecté[" + userDomain + "] | Niveau Detecté[" + rawNiveau + " -> " + niveau + "]");

        if (!userDomain.equals("N/A") && !courseCat.equals("N/A")) {
            String u = normalize(userDomain);
            String c = normalize(courseCat);
            if (u.contains(c) || c.contains(u) || u.equals(c)) {
                catMatch = 1;
                System.out.println("DEBUG IA - Match par Intérêt Détecté !");
            }
        }

        // Appel asynchrone à Flask
        return aiService.predictSuccess(certifs, niveau, progression, catMatch);
    }

    private double encodeNiveau(String niveau) {
        if (niveau == null) return 0.0;
        String l = normalize(niveau);
        if (l.contains("avanc") || l.contains("expert") || l.contains("senior")) return 1.0;
        if (l.contains("inter") || l.contains("moyen") || l.contains("pro")) return 0.5;
        return 0.0; // Débutant
    }

    private String normalize(String input) {
        if (input == null) return "";
        // Supprime les accents, met en minuscule et enlève les espaces
        String normalized = java.text.Normalizer.normalize(input, java.text.Normalizer.Form.NFD);
        return normalized.replaceAll("[^\\p{ASCII}]", "").toLowerCase().trim();
    }
}
