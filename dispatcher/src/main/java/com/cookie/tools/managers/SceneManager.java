package com.cookie.tools.managers;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class SceneManager {
    private static SceneManager istance;
    private Map<String, String> FXML_SCENES = new HashMap<>();
    ResourceBundle sceneBundle;
    FXMLLoader loader;

    public enum SceneKeys {
        MAIN_MENU_VIEW("MainMenuView"),
        SETTINGS_VIEW("SettingsView");

        private final String key;

        SceneKeys(String key) {
            this.key = key;
        }

        public String getKey() {
            return key;
        }
    }

    public static SceneManager getInstance() {
        if (istance == null) {
            istance = new SceneManager();
        }
        return istance;
    }

    private SceneManager() {
        InitializeSceneFile();
    }

    private void InitializeSceneFile() {
        sceneBundle = ResourceBundle.getBundle("com.cookie.tools.scenes.scenes");
        for (String key : sceneBundle.keySet()) {
            FXML_SCENES.put(key, sceneBundle.getString(key));
        }
        FXML_SCENES = Collections.unmodifiableMap(FXML_SCENES);
    }

    public void loadScene(Stage stage, SceneKeys sceneKey, String title, double minWidth, double minHeight) {
        try {
            // prende la scena passarta come parametro e la carica
            loader = new FXMLLoader(getClass().getResource(FXML_SCENES.get(sceneKey.getKey())), LanguageManager.getInstance().getBundle());
            
            // Carica il file FXML
            Parent root = loader.load();
            Scene scene = new Scene(root, minWidth, minHeight);

            // Setta le dimensioni minime
            stage.setMinWidth(minWidth);
            stage.setMinHeight(minHeight);

            stage.setTitle(title);
            stage.setScene(scene);
            
            // mette lo stage al centro dello schermo
            stage.centerOnScreen();

            System.out.println("Caricata scena: " + sceneKey.getKey());
            stage.show();
            
        } catch (IOException | RuntimeException e) {
            // e.printStackTrace();
            showAlert(LanguageManager.getInstance().getBundle().getString("alert.errorTitle"),
                LanguageManager.getInstance().getBundle().getString("alert.errorLoadScene") + " " + sceneKey.getKey(), AlertType.ERROR);
            System.out.println("Errore nel caricamento della scena: " + sceneKey.getKey());
        }
    }

    public void reloadCurrentScene(Stage stage, SceneKeys sceneKey, String title, double minWidth, double minHeight) {
        loadScene(stage, sceneKey, title, minWidth, minHeight);
    }

    public void loadPopupScene(ActionEvent event, SceneKeys sceneKey, String title, double minWidth, double minHeight) {
        try {
            // prende la scena passarta come parametro e la carica
            loader = new FXMLLoader(getClass().getResource(FXML_SCENES.get(sceneKey.getKey())), LanguageManager.getInstance().getBundle());
            
            // Carica il file FXML
            Parent root = loader.load();

            // Crea il poopup stage e trova lo stage principale
            Stage popupStage = new Stage();
            Stage primaryStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            
            // Assegna le proprietà al popup
            popupStage.initModality(Modality.WINDOW_MODAL);
            popupStage.initOwner(primaryStage);

            popupStage.setMinWidth(minWidth);   // Impedisce alla finestra di stringersi troppo
            popupStage.setMinHeight(minHeight); // Impedisce alla finestra di abbassarsi troppo
            
            popupStage.setTitle(title);

            Scene popupScene = new Scene(root, minWidth, minHeight);

            popupStage.setScene(popupScene);

            // Centra la finestra popup rispetto alla finestra di partenza
            double centerX = primaryStage.getX() + primaryStage.getWidth() / 2;
            double centerY = primaryStage.getY() + primaryStage.getHeight() / 2;

            popupStage.setX(centerX - minWidth / 2);
            popupStage.setY(centerY - minHeight / 2);

            System.out.println("Caricata scena popup: " + sceneKey.getKey());
            popupStage.showAndWait();
            
        } catch (IOException | RuntimeException e) {
            // e.printStackTrace();
            showAlert(LanguageManager.getInstance().getBundle().getString("alert.errorTitle"),
            LanguageManager.getInstance().getBundle().getString("alert.errorLoadScene") + " " + sceneKey.getKey(), AlertType.ERROR);
            System.out.println("Errore nel caricamento della scena: " + sceneKey.getKey());
        }
    }

    public  void showAlert(String title, String message, AlertType alertType) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}