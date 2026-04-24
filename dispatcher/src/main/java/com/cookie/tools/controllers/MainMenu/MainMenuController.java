package com.cookie.tools.controllers.MainMenu;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class MainMenuController {

    // --- PULSANTI ---
    @FXML
    private Button addHeaderButton; // Collegalo a un Button nel tuo FXML
    @FXML
    private Button resetButton;
    @FXML
    private Button openFileButton;
    @FXML
    private Button openBodyButton;
    @FXML
    private Button sendRequestButton;

    // --- SELETTORI ---
    @FXML
    private TextField urlField; // Collegalo a un TextField nel tuo FXML
    @FXML
    private ChoiceBox<String> methodChoiceBox; // Collegalo a un ChoiceBox nel tuo FXML

    // --- CONTAINERS ---
    @FXML
    private VBox headersContainer; // Collegalo a un VBox nel tuo FXML

    @FXML
    private void onAddHeaderClick(ActionEvent event) {
        // Crea il contenitore orizzontale per la singola riga
        HBox riga = new HBox(10); // 10px di spazio tra gli elementi
        riga.setAlignment(Pos.CENTER_LEFT);
        riga.setPadding(new Insets(5));

        // Crea i campi di input
        TextField keyField = new TextField();
        keyField.setPromptText("Chiave");
        keyField.setPrefWidth(150);

        TextField valueField = new TextField();
        valueField.setPromptText("Valore");
        HBox.setHgrow(valueField, Priority.ALWAYS); // Il valore occupa tutto lo spazio libero

        // Crea il tasto per rimuovere questa specifica riga
        Button removeBtn = new Button("X");
        removeBtn.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
        
        // Logica di rimozione: quando clicco X, la riga rimuove se stessa dal VBox
        removeBtn.setOnAction(e -> headersContainer.getChildren().remove(riga));

        // Mette tutto insieme
        riga.getChildren().addAll(keyField, valueField, removeBtn);

        // Aggiunge la riga al VBox principale
        headersContainer.getChildren().add(riga);
    }
}
