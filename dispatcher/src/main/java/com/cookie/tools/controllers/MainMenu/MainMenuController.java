package com.cookie.tools.controllers.MainMenu;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import com.cookie.tools.managers.FileManager;
import com.cookie.tools.managers.LanguageManager;
import com.cookie.tools.managers.PacketData;
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
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
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
    private Button clearBodyButton;
    @FXML
    private Button clearLogsButton;
    @FXML
    private Button saveLogButton;
    @FXML
    private Button saveUrlButton;
    @FXML
    private Button loadUrlButton;
    @FXML
    private Button saveHeadersButton;
    @FXML
    private Button loadHeadersButton;
    @FXML
    private Button savePacketButton;
    @FXML
    private Button loadPacketButton;

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

    // metodo base che crea una riga con chiave e valore già compilati
    // usato sia dal pulsante "Aggiungi Header" che dal caricamento dei file
    private void addHeaderRow(String key, String value) {
        HBox riga = new HBox(10);
        riga.setAlignment(Pos.CENTER_LEFT);
        riga.setPadding(new Insets(5));

        TextField keyField = new TextField(key);
        keyField.setPromptText("Chiave");
        keyField.setPrefWidth(150);

        TextField valueField = new TextField(value);
        valueField.setPromptText("Valore");
        HBox.setHgrow(valueField, Priority.ALWAYS);

        Button removeBtn = new Button("X");
        removeBtn.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
        removeBtn.setOnAction(e -> headersContainer.getChildren().remove(riga));

        riga.getChildren().addAll(keyField, valueField, removeBtn);
        headersContainer.getChildren().add(riga);
    }

    // aggiunge righe di header dinamicamente al click del pulsante "Aggiungi Header"
    // pulsante "Aggiungi Header" — riga vuota
    @FXML
    private void onAddHeaderClick(ActionEvent event) {
        addHeaderRow("", "");
    }

    // pulsante "Carica Headers" — carica da file e popola le righe
    @FXML
    private void onLoadHeadersClick(ActionEvent event) {
        try {
            List<String> headers = FileManager.getInstance().listHeaders();
            if (headers.isEmpty()) {
                appendLog("[INFO] Nessun headers template salvato.");
                return;
            }

            ChoiceDialog<String> dialog = new ChoiceDialog<>(headers.get(0), headers);
            dialog.setTitle("Carica Headers");
            dialog.setHeaderText(null);
            dialog.setContentText("Scegli un template:");

            dialog.showAndWait().ifPresent(name -> {
                try {
                    String rawHeaders = FileManager.getInstance().loadHeaders(name);

                    // pulisce le righe esistenti
                    clearHeaders();

                    // per ogni riga del file crea una riga nella UI
                    for (String line : rawHeaders.split("\n")) {
                        if (line.isBlank()) continue;
                        String[] parts = line.split(" : ", 2);
                        if (parts.length == 2) {
                            addHeaderRow(parts[0].trim(), parts[1].trim());
                        }
                    }

                    appendLog("[OK] Headers caricati: " + name);
                } catch (IOException e) {
                    appendLog("[ERRORE] Caricamento headers fallito: " + e.getMessage());
                }
            });
        } catch (IOException e) {
            appendLog("[ERRORE] Lettura headers fallita: " + e.getMessage());
        }
    }

    // pulsante "Salva Headers"
    @FXML
    private void onSaveHeadersClick(ActionEvent event) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Salva Headers");
        dialog.setHeaderText(null);
        dialog.setContentText("Nome template (vuoto = automatico):");

        dialog.showAndWait().ifPresent(name -> {
            try {
                boolean saved = FileManager.getInstance().saveHeaders(
                    name,
                    getFormattedHeaders()
                );
                if (saved) {
                    appendLog("[OK] Headers salvati: " + (name.isBlank() ? "automatico" : name));
                } else {
                    appendLog("[ERRORE] Esiste già un template con questo nome.");
                }
            } catch (IOException e) {
                appendLog("[ERRORE] Salvataggio headers fallito: " + e.getMessage());
            }
        });
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

    // pulsante "Carica URL"
    @FXML
    private void onLoadUrlClick(ActionEvent event) {
        try {
            List<String> urls = FileManager.getInstance().listUrls();
            if (urls.isEmpty()) {
                appendLog("[INFO] Nessun URL salvato.");
                return;
            }

            ChoiceDialog<String> dialog = new ChoiceDialog<>(urls.get(0), urls);
            dialog.setTitle("Carica URL");
            dialog.setHeaderText(null);
            dialog.setContentText("Scegli un URL:");

            dialog.showAndWait().ifPresent(name -> {
                try {
                    String url = FileManager.getInstance().loadUrl(name);
                    urlField.setText(url);
                    appendLog("[OK] URL caricato: " + name + " → " + url);
                } catch (IOException e) {
                    appendLog("[ERRORE] Caricamento URL fallito: " + e.getMessage());
                }
            });
        } catch (IOException e) {
            appendLog("[ERRORE] Lettura URL fallita: " + e.getMessage());
        }
    }

    // pulsante "Salva URL"
    @FXML
    private void onSaveUrlClick(ActionEvent event) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Salva URL");
        dialog.setHeaderText(null);
        dialog.setContentText("Nome (vuoto = automatico):");

        dialog.showAndWait().ifPresent(name -> {
            try {
                boolean saved = FileManager.getInstance().saveUrl(
                    name,
                    getTargetUrl()
                );
                if (saved) {
                    appendLog("[OK] URL salvato: " + (name.isBlank() ? "automatico" : name));
                } else {
                    appendLog("[ERRORE] Esiste già un URL con questo nome.");
                }
            } catch (IOException e) {
                appendLog("[ERRORE] Salvataggio URL fallito: " + e.getMessage());
            }
        });
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
    private void onSavePacketClick(ActionEvent event) {
        // apre un dialog per chiedere il nome
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Salva pacchetto");
        dialog.setHeaderText(null);
        dialog.setContentText("Nome pacchetto (vuoto = automatico):");

        dialog.showAndWait().ifPresent(name -> {
            try {
                boolean saved = FileManager.getInstance().savePacket(
                    name,
                    getMethod(),
                    getTargetUrl(),
                    getFormattedHeaders(),
                    bodyArea.getText().trim()
                );
                if (saved) {
                    appendLog("[OK] Pacchetto salvato: " + (name.isBlank() ? "automatico" : name));
                } else {
                    appendLog("[ERRORE] Esiste già un pacchetto con questo nome.");
                }
            } catch (IOException e) {
                appendLog("[ERRORE] Salvataggio fallito: " + e.getMessage());
            }
        });
    }

    @FXML
    private void onLoadPacketClick(ActionEvent event) {
        try {
            List<String> packets = FileManager.getInstance().listPackets();
            if (packets.isEmpty()) {
                appendLog("[INFO] Nessun pacchetto salvato.");
                return;
            }

            // mostra una ChoiceDialog con la lista dei pacchetti
            ChoiceDialog<String> dialog = new ChoiceDialog<>(packets.get(0), packets);
            dialog.setTitle("Carica pacchetto");
            dialog.setHeaderText(null);
            dialog.setContentText("Scegli un pacchetto:");

            dialog.showAndWait().ifPresent(name -> {
                try {
                    PacketData packet = FileManager.getInstance().loadPacket(name);

                    urlField.setText(packet.url);
                    methodChoiceBox.setValue(packet.method);

                    // prettifica ogni body separatamente e li riunisce con doppio a capo
                    String prettyBody = Arrays.stream(packet.body.split("\n\n"))
                        .map(String::trim)
                        .filter(b -> !b.isEmpty())
                        .map(this::prettyPrintJson) // applica jackson su ciascuno
                        .collect(Collectors.joining("\n\n"));

                    bodyArea.setText(prettyBody);

                    clearHeaders();
                    for (String header : packet.headers) {
                        String[] parts = header.split(" : ", 2);
                        if (parts.length == 2) addHeaderRow(parts[0], parts[1]);
                    }

                    appendLog("[OK] Pacchetto caricato: " + name);
                } catch (IOException e) {
                    appendLog("[ERRORE] Caricamento fallito: " + e.getMessage());
                }
            });
        } catch (IOException e) {
            appendLog("[ERRORE] Lettura pacchetti fallita: " + e.getMessage());
        }
    }

    @FXML
    private void onSaveLogClick(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Salva Log");
        
        // parte dalla cartella saved_logs come default
        fileChooser.setInitialDirectory(
            new File(SettingsManager.LOGS_DIR)
        );
        
        // nome file di default con timestamp
        String defaultName = "log_" + LocalDateTime.now()
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")) + ".txt";
        fileChooser.setInitialFileName(defaultName);

        // filtri per tipo file
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("File di testo", "*.txt"),
            new FileChooser.ExtensionFilter("Tutti i file", "*.*")
        );

        // apre il file explorer — ritorna null se l'utente annulla
        File file = fileChooser.showSaveDialog(saveLogButton.getScene().getWindow());
        
        if (file != null) {
            try {
                FileManager.getInstance().saveLog(logArea.getText(), file);
                appendLog("[OK] Log salvato in: " + file.getAbsolutePath());
            } catch (IOException e) {
                appendLog("[ERRORE] Salvataggio log fallito: " + e.getMessage());
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
