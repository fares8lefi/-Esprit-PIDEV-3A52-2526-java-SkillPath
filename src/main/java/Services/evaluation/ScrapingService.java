package Services.evaluation;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;

import java.io.IOException;

public class ScrapingService {

    /**
     * Recherche un article ou une vidéo liée au sujet du quiz sur Dev.to ou YouTube
     * @param subject Le sujet du quiz (ex: "Java", "DevOps")
     * @return Un lien vers une ressource pertinente
     */
    public String getRecommendation(String subject) {
        String query = subject.replace(" ", "").toLowerCase();
        StringBuilder recommendation = new StringBuilder();
        recommendation.append("Basé sur vos résultats, voici des ressources pour approfondir vos connaissances sur \"").append(subject).append("\" :\n\n");

        boolean hasArticle = false;

        // Tentative 1 : Dev.to API (Articles techniques)
        try {
            String url = "https://dev.to/api/articles?tag=" + query + "&per_page=1";
            String json = Jsoup.connect(url)
                    .ignoreContentType(true)
                    .userAgent("Mozilla/5.0")
                    .execute()
                    .body();

            JSONArray articles = new JSONArray(json);
            if (articles.length() > 0) {
                JSONObject firstArticle = articles.getJSONObject(0);
                String link = firstArticle.getString("url");
                String title = firstArticle.getString("title");
                recommendation.append("📖 Article Technique : ").append(title).append("\nLien : ").append(link).append("\n\n");
                hasArticle = true;
            }
        } catch (Exception e) {
            System.err.println("Erreur API Dev.to : " + e.getMessage());
        }

        // Tentative 2 : Wikipedia (Si pas d'article technique)
        if (!hasArticle) {
            try {
                String wikiUrl = "https://fr.wikipedia.org/api/rest_v1/page/summary/" + subject.trim().replace(" ", "_");
                String jsonWiki = Jsoup.connect(wikiUrl)
                        .ignoreContentType(true)
                        .userAgent("Mozilla/5.0")
                        .execute()
                        .body();

                JSONObject wikiObj = new JSONObject(jsonWiki);
                if (wikiObj.has("content_urls")) {
                    String link = wikiObj.getJSONObject("content_urls").getJSONObject("desktop").getString("page");
                    String title = wikiObj.getString("title");
                    recommendation.append("📖 Article Wikipedia : ").append(title).append("\nLien : ").append(link).append("\n\n");
                }
            } catch (Exception e) {
                System.err.println("Erreur API Wikipedia : " + e.getMessage());
            }
        }

        // Ajout systématique de YouTube
        String youtubeUrl = "https://www.youtube.com/results?search_query=" + subject.replace(" ", "+") + "+tutorial";
        recommendation.append("📺 Tutoriel Vidéo YouTube\nLien : ").append(youtubeUrl);

        return recommendation.toString();
    }
}
