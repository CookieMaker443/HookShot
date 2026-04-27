package com.cookie.tools.controllers.Settings;

import com.cookie.tools.controllers.MainMenu.MainMenuController.HttpVersion;
import com.cookie.tools.managers.LanguageManager;
import com.cookie.tools.managers.SceneManager;
import com.cookie.tools.managers.SettingsManager;

import javafx.fxml.FXML;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Spinner;
import javafx.stage.Stage;

@SuppressWarnings("unused")
public class SettingsController {

    @FXML private ChoiceBox<String> languageChoiceBox;
    @FXML private Spinner<Integer> maxRequestsSpinner;
    @FXML private ChoiceBox<String> httpVersionChoiceBox;

    @FXML
    private void initialize() {
        languageChoiceBox.getItems().addAll("en", "it", "de", "es", "fr", "pl");
        languageChoiceBox.setValue(SettingsManager.getInstance().getLanguage());
        maxRequestsSpinner.getValueFactory().setValue(
            SettingsManager.getInstance().getMaxParallelRequests()
        );
        for (HttpVersion v : HttpVersion.values()) {
            httpVersionChoiceBox.getItems().add(v.name());
        }
        httpVersionChoiceBox.setValue(SettingsManager.getInstance().getHttpVersion());
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

        // prima salva la versione http da utase
        SettingsManager.getInstance().setHttpVersion(httpVersionChoiceBox.getValue());
        System.out.println("HTTP Version Saved: " + SettingsManager.getInstance().getHttpVersion());

        // poi ricarica la scena principale per applicare le modifiche
        SceneManager.getInstance().reloadCurrentScene(
            primaryStage,
            SceneManager.SceneKeys.MAIN_MENU_VIEW,
            LanguageManager.getInstance().getBundle().getString("app.title"),
            1500, 750
        );
    }
}