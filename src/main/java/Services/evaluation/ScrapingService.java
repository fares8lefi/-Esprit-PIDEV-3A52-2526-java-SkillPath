package Services.evaluation;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ScrapingService {

    /**
     * Recherche un article ou une vidéo liée au sujet du quiz sur Dev.to ou YouTube
     * @param subject Le sujet du quiz (ex: "Java", "DevOps")
     * @return Un lien vers une ressource pertinente
     */
    public String getRecommendation(String subject) {
        String query = subject.replace(" ", "+");
        
        // Tentative 1 : Dev.to (Articles techniques)
        try {
            String url = "https://dev.to/search?q=" + query;
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                    .get();

            // Chercher le premier lien d'article
            Elements articles = doc.select("h3.crayons-story__title a");
            if (!articles.isEmpty()) {
                Element firstArticle = articles.first();
                String link = "https://dev.to" + firstArticle.attr("href");
                String title = firstArticle.text();
                return "Basé sur vos résultats, nous vous suggérons de consulter cet article approfondi pour mieux maîtriser " + subject + " :\n\"" + title + "\"\nLien : " + link;
            }
        } catch (IOException e) {
            System.err.println("Erreur scraping Dev.to : " + e.getMessage());
        }

        // Tentative 2 : YouTube (Si Dev.to échoue ou pour varier)
        String youtubeUrl = "https://www.youtube.com/results?search_query=" + query + "+tutorial";
        return "Pour approfondir vos connaissances en " + subject + ", nous avons sélectionné pour vous ce tutoriel vidéo pertinent sur YouTube.\nLien : " + youtubeUrl;

    }
}
