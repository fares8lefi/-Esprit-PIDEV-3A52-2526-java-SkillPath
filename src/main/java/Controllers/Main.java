package Controllers;

import java.io.IOException;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage stage) {
        try {
            Parent root = FXMLLoader.load(
                getClass().getResource("/FrontOffice/home/home.fxml")
            );
            Scene scene = new Scene(root);
            stage.setTitle("SkillPath");
            stage.setScene(scene);
            stage.setResizable(true);
            stage.show();
        } catch (IOException e) {
            System.out.println("Erreur chargement FXML : " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
