package Services.evaluation;

import io.github.cdimascio.dotenv.Dotenv;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.util.Properties;

public class MailService {
    private static final Dotenv dotenv = Dotenv.load();
    private final String username = dotenv.get("MAIL_USERNAME");
    private final String password = dotenv.get("MAIL_PASSWORD");

    public void sendQuizResultEmail(String toEmail, String studentName, String quizTitle, int score, int totalPoints, String feedback) {
        if (username == null || password == null || toEmail == null) {
            System.err.println("SMTP configuration missing or recipient email null.");
            return;
        }

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(username));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject("Résultat de votre Quiz : " + quizTitle);

            String htmlContent = "<div style='font-family: Arial, sans-serif; max-width: 600px; margin: auto; padding: 20px; border: 1px solid #e2e8f0; border-radius: 10px; background-color: #f8fafc;'>" +
                    "<h1 style='color: #8b5cf6; text-align: center;'>Félicitations !</h1>" +
                    "<p>Bonjour <strong>" + studentName + "</strong>,</p>" +
                    "<p>Vous venez de terminer le quiz : <strong>" + quizTitle + "</strong>.</p>" +
                    "<div style='background-color: #ffffff; padding: 20px; border-radius: 8px; text-align: center; margin: 20px 0; border: 1px solid #cbd5e1;'>" +
                    "<span style='font-size: 18px; color: #64748b;'>Votre Score Final</span><br/>" +
                    "<span style='font-size: 48px; font-weight: bold; color: #10b981;'>" + score + " / " + totalPoints + "</span>" +
                    "</div>" +
                    "<p style='font-style: italic; color: #475569; text-align: center;'>\" " + feedback + " \"</p>" +
                    "<hr style='border: 0; border-top: 1px solid #e2e8f0; margin: 20px 0;' />" +
                    "<p style='font-size: 12px; color: #94a3b8; text-align: center;'>Ceci est un message automatique de SkillPath. Merci de ne pas y répondre.</p>" +
                    "</div>";

            message.setContent(htmlContent, "text/html; charset=UTF-8");

            Transport.send(message);
            System.out.println("Email de résultat envoyé avec succès à " + toEmail);

        } catch (MessagingException e) {
            e.printStackTrace();
            System.err.println("Erreur lors de l'envoi de l'email : " + e.getMessage());
        }
    }
}
