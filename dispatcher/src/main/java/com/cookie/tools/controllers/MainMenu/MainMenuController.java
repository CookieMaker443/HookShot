package com.cookie.tools.controllers.MainMenu;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class MainMenuController {
    @FXML
    private VBox headersContainer; // Collegalo a un VBox nel tuo FXML

    @FXML
    private void addHeaderRow() {
        TextField keyField = new TextField();
        keyField.setPromptText("Chiave (es. Authorization)");
        
        TextField valueField = new TextField();
        valueField.setPromptText("Valore");
        
        Button removeBtn = new Button("X");
        
        HBox row = new HBox(10, keyField, valueField, removeBtn);
        
        // Rimuove se stessa quando clicchi il tasto
        removeBtn.setOnAction(e -> headersContainer.getChildren().remove(row));
        
        headersContainer.getChildren().add(row);
    }
}
