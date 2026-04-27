package com.cookie.tools;

import com.cookie.tools.managers.LanguageManager;
import com.cookie.tools.managers.SceneManager;

import javafx.application.Application;
import javafx.stage.Stage;


/**
 * JavaFX App
 */
public class App extends Application {

    @Override
    public void start(Stage stage) {

        final double MIN_WIDTH = 1500;
        final double MIN_HEIGHT = 750;

        // LanguageManager.getInstance();               // Inizializza il LanguageManager
        // Inizializza lo SceneManager
        SceneManager.getInstance()
                .loadScene(stage, SceneManager.SceneKeys.MAIN_MENU_VIEW, 
                    LanguageManager.getInstance().getBundle().getString("app.title"), MIN_WIDTH, MIN_HEIGHT);
    }

    public static void main(String[] args) {
        launch();
    }

}