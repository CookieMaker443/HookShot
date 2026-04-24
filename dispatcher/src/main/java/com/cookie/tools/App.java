package com.cookie.tools;

import java.util.Locale;
import java.util.ResourceBundle;

import com.cookie.tools.managers.SceneManager;

import javafx.application.Application;
import javafx.stage.Stage;


/**
 * JavaFX App
 */
public class App extends Application {

    @Override
    public void start(Stage stage) {
        Locale locale = Locale.forLanguageTag("it-IT");
        ResourceBundle bundle = ResourceBundle.getBundle("com.cookie.tools.i18n.language", locale);

        var javaVersion = SystemInfo.javaVersion();
        var javafxVersion = SystemInfo.javafxVersion();
        /* 
        var label = new Label("Hello, JavaFX " + javafxVersion + ", running on Java " + javaVersion + ".");
        var scene = new Scene(new StackPane(label), 640, 480);
        stage.setScene(scene);
        stage.show();
        */
        final double MIN_WIDTH = 1500;
        final double MIN_HEIGHT = 750;

        // LanguageManager.getInstance();               // Inizializza il LanguageManager
        // Inizializza lo SceneManager
        SceneManager.getInstance()
                .loadScene(stage, SceneManager.SceneKeys.MAIN_MENU_VIEW, 
                    bundle.getString("app.title"), MIN_WIDTH, MIN_HEIGHT);
    }

    public static void main(String[] args) {
        launch();
    }

}