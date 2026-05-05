package Utils;

import javafx.scene.image.Image;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URL;

public class AssetLoader {

    /**
     * Tries to find the project root by searching for 'src' or 'pom.xml' 
     * in the current and parent directories.
     */
    private static File findProjectRoot() {
        // Strategy 1: Use the actual location of the compiled class
        // This is the most reliable way in Maven/IDE environments
        try {
            URL location = AssetLoader.class.getProtectionDomain().getCodeSource().getLocation();
            File classDir = new File(location.toURI());
            
            // If we are in 'target/classes', go up 2 levels
            File current = classDir;
            for (int i = 0; i < 5; i++) {
                if (new File(current, "uploads").exists() || new File(current, "pom.xml").exists()) {
                    return current;
                }
                current = current.getParentFile();
                if (current == null) break;
            }
        } catch (Exception e) {
            System.err.println("AssetLoader: Erreur lors de la détection via ClassLoader");
        }

        // Strategy 2: Check current working directory and children
        File userDir = new File(System.getProperty("user.dir"));
        if (new File(userDir, "uploads").exists()) return userDir;
        
        // Scan for the project folder in subdirectories (if launched from parent)
        File[] subs = userDir.listFiles(File::isDirectory);
        if (subs != null) {
            for (File sub : subs) {
                if (new File(sub, "uploads").exists()) {
                    return sub;
                }
            }
        }

        // Strategy 3: Check parents of user.dir
        File current = userDir;
        for (int i = 0; i < 3; i++) {
            if (new File(current, "uploads").exists()) return current;
            current = current.getParentFile();
            if (current == null) break;
        }

        return userDir;
    }

    /**
     * Standard robust way to get the uploads/modules directory.
     */
    public static File getModulesUploadsDir() {
        File root = findProjectRoot();
        File modulesDir = new File(root, "uploads/modules/");
        if (!modulesDir.exists()) modulesDir.mkdirs();
        return modulesDir;
    }

    /**
     * Loads an image using FileInputStream for maximum reliability.
     */
    public static Image loadCourseImage(String imageName) {
        if (imageName == null || imageName.isEmpty()) return null;

        File root = findProjectRoot();
        
        // Detect .webp which is NOT supported by JavaFX natively
        String finalImageName = imageName;
        if (imageName.toLowerCase().endsWith(".webp")) {
            System.out.println("AssetLoader: .WEBP detecté (" + imageName + "). JavaFX ne supporte pas ce format nativement.");
            finalImageName = imageName.substring(0, imageName.length() - 5) + ".png";
            System.out.println("AssetLoader: Tentative de repli sur " + finalImageName);
        }

        // Potential paths to check
        File[] candidates = {
            new File(root, "uploads/modules/" + imageName), // Try original first (most likely)
            new File(root, "uploads/modules/" + finalImageName),
            new File(root, "uploads/" + imageName),
            new File(root, "uploads/" + finalImageName),
            new File(root, "src/main/resources/uploads/modules/" + imageName)
        };

        for (File file : candidates) {
            if (file.exists()) {
                try {
                    return new Image(new FileInputStream(file));
                } catch (FileNotFoundException e) {
                    System.err.println("AssetLoader: Erreur accès file " + file.getAbsolutePath());
                }
            }
        }

        System.out.println("AssetLoader: Image non trouvée après scan de " + root.getAbsolutePath() + " pour : " + imageName);
        return null;
    }
}
