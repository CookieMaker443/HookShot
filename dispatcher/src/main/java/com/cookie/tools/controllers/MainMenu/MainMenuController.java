package com.cookie.tools.controllers.MainMenu;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextArea;
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

    // --- HTTP CLIENT ---
    private HttpClient client = HttpClient.newHttpClient();

    // --- TEXT AREA ---
    @FXML
    private TextArea logArea; 
    @FXML
    private TextArea bodyArea; 
    @FXML
    private TextArea responseArea;

    
    // aggiunge righe di header dinamicamente al click del pulsante "Aggiungi Header"
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

    // legge il valore delle righe degli header
    private String getFormattedHeaders() {
        StringBuilder sb = new StringBuilder();

        for (Node node : headersContainer.getChildren()) {
            // Verifichiamo che il nodo sia effettivamente una riga HBox
            if (node instanceof HBox riga) {
                // Estraiamo i TextField (sappiamo che sono in posizione 0 e 1)
                TextField keyField = (TextField) riga.getChildren().get(0);
                TextField valueField = (TextField) riga.getChildren().get(1);

                String chiave = keyField.getText().trim();
                String valore = valueField.getText().trim();

                // Aggiungiamo solo se la chiave non è vuota
                if (!chiave.isEmpty()) {
                    sb.append(chiave)
                    .append(" : ")
                    .append(valore)
                    .append("\n");
                }
            }
        }
        //logArea.appendText("Headers formattati:\n" + sb);
        return sb.toString();
    }

    // Recupera l'URL dal campo di testo
    private String getTargetUrl() {
        return urlField.getText().trim();
    }

    // valida l url inserita dall'utente
    private boolean isValidUrl(String url) {
        try {
            URI uri = URI.create(url);
            return uri.getScheme() != null && 
                (uri.getScheme().equals("http") || uri.getScheme().equals("https"));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    // Invia la richiesta al click del pulsante "Invia"
    @FXML
    private void onSendRequestClick(ActionEvent event) {
        String url = getTargetUrl();
        // String method = methodChoiceBox.getValue();
        String method = "POST";
        String rawHeaders = getFormattedHeaders();
        String body = ""; // Implementa la logica per recuperare il corpo della richiesta se necessario
    
        if (url.isEmpty() || !isValidUrl(url)) {
            // mostra errore in UI
            return;
        }

        try {
            
            // 2. Inizia a costruire la richiesta
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(url));

            // 3. Applica gli header dinamicamente
            if (!rawHeaders.isEmpty()) {
                String[] lines = rawHeaders.split("\n");
                for (String line : lines) {
                    // Dividiamo alla prima occorrenza di ":"
                    String[] parts = line.split(" : ", 2);
                    if (parts.length == 2) {
                        // Rimuoviamo le virgolette che hai aggiunto nel metodo getFormattedHeaders
                        String key = parts[0].trim();
                        String value = parts[1].trim();
                        requestBuilder.header(key, value);
                    }
                }
            }

            // 4. Imposta il metodo (GET, POST, ecc.)
            requestBuilder.method(method, HttpRequest.BodyPublishers.noBody());

            // 5. Invia la richiesta
            HttpRequest request = requestBuilder.build();
            client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenAccept(response -> {
                Platform.runLater(() -> {
                    appendLog(response);
                });
            })
            .exceptionally(ex -> {
                Platform.runLater(() -> logArea.appendText("❌ Errore: " + ex.getMessage() + "\n"));
                return null;
            });

        } catch (Exception e) {
            // e.printStackTrace();
        }
    }

    private void appendLog(HttpResponse<String> response) {
        Platform.runLater(() -> {
            logArea.appendText("=== RISPOSTA ===\n");
            logArea.appendText("Status: " + response.statusCode() + "\n");
            logArea.appendText("Body:\n" + response.body() + "\n");
            logArea.appendText("================\n\n");
        });
    }
}
