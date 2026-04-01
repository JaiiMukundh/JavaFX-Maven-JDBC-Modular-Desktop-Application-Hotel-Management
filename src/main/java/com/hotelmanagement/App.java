package com.hotelmanagement;

import com.hotelmanagement.utils.DatabaseInitializer;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.lang.reflect.Method;

/**
 * JavaFX App - Hotel Management System
 */
public class App extends Application {

    private static Scene scene;

    @Override
    public void start(Stage stage) throws IOException {
        // Initialize database on app startup
        DatabaseInitializer.initializeDatabase();
        
        Parent root = loadFXML("fxml/main");
        scene = new Scene(root, 1200, 750);
        
        // Theme selection: prefer Atlantafx (if on classpath), else BootstrapFX, else custom CSS
        boolean themeApplied = false;
        try {
            // Try Atlantafx via reflection (optional dependency)
            Class<?> forestClass = Class.forName("io.github.mkpaz.atlantafx.base.theme.Forest");
            Object forest = forestClass.getDeclaredConstructor().newInstance();
            Method getSheet = forestClass.getMethod("getUserAgentStylesheet");
            String sheet = (String) getSheet.invoke(forest);
            Application.setUserAgentStylesheet(sheet);
            themeApplied = true;
        } catch (Throwable t) {
            // Atlantafx not available or failed — fall through
        }

        if (!themeApplied) {
            try {
                // Try BootstrapFX via reflection
                Class<?> bf = Class.forName("org.kordamp.bootstrapfx.BootstrapFX");
                java.lang.reflect.Method m = bf.getMethod("bootstrapFXStylesheet");
                String sheet = (String) m.invoke(null);
                if (sheet != null) {
                    scene.getStylesheets().add(sheet);
                    themeApplied = true;
                }
            } catch (Throwable t) {
                // BootstrapFX not available — fall back to custom CSS
            }
        }

        // Always append the application's custom overrides so we can tweak Bootstrap/Atlantafx styles
        String css = this.getClass().getResource("css/styles.css").toExternalForm();
        if (css != null) {
            scene.getStylesheets().add(css);
        }
        
        stage.setTitle("Hotel Management System");
        stage.setScene(scene);
        stage.show();
    }

    static void setRoot(String fxml) throws IOException {
        scene.setRoot(loadFXML(fxml));
    }

    private static Parent loadFXML(String fxml) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource(fxml + ".fxml"));
        return fxmlLoader.load();
    }

    public static void main(String[] args) {
        launch();
    }

}