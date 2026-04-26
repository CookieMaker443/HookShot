package com.cookie.tools.controllers.Settings;

import com.cookie.tools.managers.LanguageManager;
import com.cookie.tools.managers.SceneManager;
import com.cookie.tools.managers.SettingsManager;

import javafx.fxml.FXML;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Spinner;
import javafx.stage.Stage;

public class SettingsController {

    @FXML private ChoiceBox<String> languageChoiceBox;
    @FXML private Spinner<Integer> maxRequestsSpinner;

    @FXML
    private void initialize() {
        languageChoiceBox.getItems().addAll("en", "it", "de", "es", "fr", "pl");
        languageChoiceBox.setValue(SettingsManager.getInstance().getLanguage());
        maxRequestsSpinner.getValueFactory().setValue(
            SettingsManager.getInstance().getMaxParallelRequests()
        );
    }

    @FXML
    private void onSaveClick() {
        String selectedLang = languageChoiceBox.getValue();

        // salva nei settings
        SettingsManager.getInstance().setLanguage(selectedLang);
        SettingsManager.getInstance().setMaxParallelRequests(maxRequestsSpinner.getValue());

        // aggiorna il bundle
        LanguageManager.getInstance().load(selectedLang);

        // ricarica la scena principale
        Stage popupStage = (Stage) languageChoiceBox.getScene().getWindow();
        Stage primaryStage = (Stage) popupStage.getOwner();
        popupStage.close();
        int height = (int) primaryStage.getHeight();
        int width = (int) primaryStage.getWidth(); 

        SceneManager.getInstance().reloadCurrentScene(
            primaryStage,
            SceneManager.SceneKeys.MAIN_MENU_VIEW,
            LanguageManager.getInstance().getBundle().getString("settings.title"),
            1500, 750
        );
    }
}