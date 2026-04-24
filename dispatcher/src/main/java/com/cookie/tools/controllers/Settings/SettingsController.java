package com.cookie.tools.controllers.Settings;

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
        languageChoiceBox.getItems().addAll("en", "it");
        languageChoiceBox.setValue(SettingsManager.getInstance().getLanguage());
        maxRequestsSpinner.getValueFactory().setValue(
            SettingsManager.getInstance().getMaxParallelRequests()
        );
    }

    @FXML
    private void onSaveClick() {
        SettingsManager.getInstance().setLanguage(languageChoiceBox.getValue());
        SettingsManager.getInstance().setMaxParallelRequests(maxRequestsSpinner.getValue());

        // chiude il popup
        Stage stage = (Stage) languageChoiceBox.getScene().getWindow();
        stage.close();
    }
}