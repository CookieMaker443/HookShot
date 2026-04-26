package com.cookie.tools.controllers.MainMenu;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import com.cookie.tools.managers.LanguageManager;
import com.cookie.tools.managers.SceneManager;
import com.cookie.tools.managers.SettingsManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

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
import javafx.stage.FileChooser;

public class MainMenuController {

    // --- PULSANTI ---
    @FXML
    private Button sendRequestButton;
    @FXML
    private Button settingsButton;
    @FXML
    private Button resetButton;
    @FXML
    private Button addHeaderButton; // Collegalo a un Button nel tuo FXML
    @FXML
    private Button openFileButton;
    @FXML
    private Button clearLogsButton;
    @FXML
    private Button clearBodyButton;

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

    // --- JSON PRETTY PRINT (opzionale, per loggare i body in modo leggibile) ---

    private final ObjectMapper objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    // --- TEXT AREA ---
    @FXML
    private TextArea logArea; 
    @FXML
    private TextArea bodyArea; 
    @FXML
    private TextArea responseArea;

    @FXML
    private void initialize() {
        for (HttpMethod m : HttpMethod.values()) {
            methodChoiceBox.getItems().add(m.name()); // aggiunge "GET", "POST", ecc.
        }
        methodChoiceBox.setValue(HttpMethod.GET.name()); // valore di default
        urlField.setText(SettingsManager.getInstance().getLastUrl());
    }

    private String getMethod() {
        return methodChoiceBox.getValue(); // ritorna la stringa "GET", "POST", ecc.
    }
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

    // Invia la richiesta al click del pulsante "Invia"
    @FXML
    private void onSendRequestClick(ActionEvent event) {
        String url = getTargetUrl();
        // String method = methodChoiceBox.getValue();
        String method = getMethod();
        String rawHeaders = getFormattedHeaders();
        String rawBody = bodyArea.getText(); // Implementa la logica per recuperare il corpo della richiesta se necessario

        if (url.isEmpty() || !isValidUrl(url)) {
            appendLog("URL non valido: " + url);
            return;
        }

        SettingsManager.getInstance().setLastUrl(url);

        
        // --- SPLIT DEI BODY ---
        // il testo nella bodyArea può contenere più body separati da una riga vuota, esempio:
        //
        //   {"nome": "Mario"}
        //
        //   {"nome": "Luigi"}
        //
        // split("\n\n") li separa in una lista di stringhe
        // .map(String::trim)      → rimuove spazi e a capo in eccesso da ciascun body
        // .filter(b -> !b.isEmpty()) → scarta eventuali body vuoti (es. doppio a capo finale)
        List<String> bodies = Arrays.stream(rawBody.split("\n\n"))
            .map(String::trim)
            .filter(b -> !b.isEmpty())
            .collect(Collectors.toList());

        // se non c'è nessun body (es. richiesta GET), aggiungiamo una stringa vuota
        // così il resto del codice funziona sempre allo stesso modo
        if (bodies.isEmpty()) bodies.add("");

        // legge il numero massimo di richieste parallele dalle settings
        int maxParallel = SettingsManager.getInstance().getMaxParallelRequests();

        // avvia il sistema di batch
        sendInBatches(url, method, rawHeaders, bodies, maxParallel);
    }

    private void sendInBatches(String url, String method, String rawHeaders,
                            List<String> bodies, int maxParallel) {

        // --- PARTIZIONAMENTO IN BATCH ---
        // esempio: bodies = [b1, b2, b3, b4, b5], maxParallel = 2
        // risultato: batches = [[b1, b2], [b3, b4], [b5]]
        List<List<String>> batches = new ArrayList<>();
        for (int i = 0; i < bodies.size(); i += maxParallel) {
            // Math.min evita di sforare la fine della lista nell'ultimo batch
            batches.add(bodies.subList(i, Math.min(i + maxParallel, bodies.size())));
        }

        // --- CATENA DI COMPLETABLEFUTURE ---
        // CompletableFuture rappresenta un'operazione asincrona che verrà completata in futuro.
        // Partiamo da un future già completato come "punto zero" della catena.
        CompletableFuture<Void> chain = CompletableFuture.completedFuture(null);

        for (int batchIndex = 0; batchIndex < batches.size(); batchIndex++) {
            List<String> batch = batches.get(batchIndex);
            int batchNum = batchIndex + 1; // numero leggibile per il log (parte da 1)

            // thenCompose = "quando il future precedente è completato, esegui questo blocco"
            // garantisce che i batch vengano eseguiti IN SEQUENZA uno dopo l'altro
            chain = chain.thenCompose(ignored -> {
                // "ignored" è il risultato del future precedente: non ci interessa (è Void)

                appendLog("📦 Batch " + batchNum + "/" + batches.size()
                        + " — " + batch.size() + " richieste in partenza...");

                // --- RICHIESTE PARALLELE DENTRO IL BATCH ---
                // per ogni body del batch creiamo un CompletableFuture indipendente
                // tutti partono contemporaneamente (in parallelo)
                List<CompletableFuture<Void>> futures = new ArrayList<>();
                for (int i = 0; i < batch.size(); i++) {
                    String body = batch.get(i);

                    // bodyIndex = posizione globale del body (es. body 3 di 5 totali)
                    // bodies.indexOf(body) trova la posizione nella lista originale completa
                    int bodyIndex = bodies.indexOf(body) + 1; // +1 per partire da 1

                    futures.add(sendSingleRequest(url, method, rawHeaders, body, bodyIndex, bodies.size()));
                }

                // allOf = crea un future che si completa solo quando TUTTI i future nella lista
                // sono completati. È il meccanismo che fa aspettare la fine del batch corrente
                // prima di passare al thenCompose successivo (batch successivo).
                return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
            });
        }

        // gestione errori sull'intera catena
        chain.exceptionally(ex -> {
            appendLog("Errore durante i batch: " + ex.getMessage());
            return null;
        });
    }

    private CompletableFuture<Void> sendSingleRequest(String url, String method,
                                                   String rawHeaders, String body,
                                                   int bodyIndex, int total) {
        try {
            // --- COSTRUZIONE DELLA REQUEST ---
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(url));

            // applica gli header dinamici se presenti
            if (!rawHeaders.isEmpty()) {
                for (String line : rawHeaders.split("\n")) {
                    String[] parts = line.split(" : ", 2);
                    if (parts.length == 2) {
                        requestBuilder.header(parts[0].trim(), parts[1].trim());
                    }
                }
            }

            // GET e DELETE non possono avere un body per specifica HTTP
            // tutti gli altri metodi (POST, PUT, PATCH) mandano il body come stringa
            HttpRequest.BodyPublisher publisher = (method.equals("GET") || method.equals("DELETE"))
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(body);

            requestBuilder.method(method, publisher);
            HttpRequest request = requestBuilder.build();

            // --- INVIO ASINCRONO ---
            // sendAsync non blocca il thread UI: la richiesta parte in background
            // e thenAccept viene chiamato quando la risposta arriva
            return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    // siamo su un thread separato (non il thread JavaFX)
                    // Platform.runLater è OBBLIGATORIO per toccare i componenti UI
                    // senza di esso JavaFX lancia un'eccezione
                    Platform.runLater(() -> {
                        logArea.appendText("=== [" + bodyIndex + "/" + total + "] RISPOSTA ===\n");
                        logArea.appendText("Body inviato: " + (body.isEmpty() ? "(vuoto)" : body) + "\n");
                        logArea.appendText("Status: " + response.statusCode() + "\n");
                        logArea.appendText("Body risposta:\n" + prettyPrintJson(response.body()) + "\n");
                        logArea.appendText("================\n\n");
                    });
                });

        } catch (Exception e) {
            // se la costruzione della request fallisce (es. URL malformato)
            // logghiamo e ritorniamo un future già completato per non bloccare la catena
            appendLog("❌ Errore costruzione richiesta [" + bodyIndex + "]: " + e.getMessage());
            return CompletableFuture.completedFuture(null);
        }
    }

    private String prettyPrintJson(String raw) {
        try {
            Object json = objectMapper.readValue(raw, Object.class);
            return objectMapper.writeValueAsString(json);
        } catch (Exception e) {
            return raw; // non è JSON, restituisce il body originale
        }
    }

    @FXML
    private void onOpenFileClick(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("File di testo", "*.txt", "*.md")
        );
        File file = fileChooser.showOpenDialog(openFileButton.getScene().getWindow());
        if (file != null) {
            try {
                String content = Files.readString(file.toPath());
                bodyArea.setText(content);
            } catch (IOException e) {
                appendLog("Errore lettura file: " + e.getMessage());
            }
        }
    }

    @FXML
    private void onSettingsClick(ActionEvent event) {
        SceneManager.getInstance().loadPopupScene(
            event,
            SceneManager.SceneKeys.SETTINGS_VIEW,
            LanguageManager.getInstance().getBundle().getString("settings.title"),
            350, 200
        );
    }

    @FXML
    private void onResetClick(ActionEvent event) {
        resetAll();
    }

    @FXML
    private void onClearLogsClick(ActionEvent event) {
        clearLog();
    }

    @FXML
    private void onClearBodyClick(ActionEvent event) {
        clearBody();
    }

    private void appendLog(String message) {
        Platform.runLater(() -> logArea.appendText(message + "\n"));
    }

    private void appendLog(HttpResponse<String> response) {
        Platform.runLater(() -> {
            logArea.appendText("=== RISPOSTA ===\n");
            logArea.appendText("Status: " + response.statusCode() + "\n");
            logArea.appendText("Body:\n" + response.body() + "\n");
            logArea.appendText("================\n\n");
        });
    }

    private void resetAll() {
        clearLog();
        clearBody();
        clearUrl();
        clearHeaders();
        methodChoiceBox.setValue(HttpMethod.GET.name()); // riporta al default
    }

    private void clearLog() {
        logArea.clear();
    }

    private void clearBody() {
        bodyArea.clear();
    }

    private void clearUrl() {
        urlField.clear();
    }

    private void clearHeaders() {
        headersContainer.getChildren().clear();
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
}
