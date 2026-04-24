```panoramica_flussoonSendRequestClick
    │
    ├── legge URL, metodo, headers, body dalla UI
    ├── valida l'URL
    ├── splitta il testo su "\n\n" → lista di body
    │
    └── sendInBatches(url, method, headers, bodies, maxParallel)
            │
            ├── partiziona bodies in gruppi da maxParallel
            │
            ├── Batch 1 → sendSingleRequest(body1), sendSingleRequest(body2) ──┐
            │            aspetta che entrambi finiscano (allOf)                │
            │                                                                   ▼
            ├── Batch 2 → sendSingleRequest(body3), sendSingleRequest(body4)  log
            │            aspetta che entrambi finiscano (allOf)
            │
            └── Batch 3 → ...
```

Metodo 1 — onSendRequestClick
Punto di ingresso. Legge i dati dalla UI, valida, e avvia la catena.
```java
    // Invia la richiesta al click del pulsante "Invia"
    @FXML
    private void onSendRequestClick(ActionEvent event) {
        String url = getTargetUrl();
        // String method = methodChoiceBox.getValue();
        String method = getMethod();
        String rawHeaders = getFormattedHeaders();
        String body = ""; // Implementa la logica per recuperare il corpo della richiesta se necessario
    
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
```

Metodo 2 — sendInBatches
Partiziona la lista di body in gruppi e li esegue in sequenza.
Ogni gruppo viene eseguito in parallelo, ma si aspetta che finisca prima di passare al successivo.
```java
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
        appendLog("❌ Errore durante i batch: " + ex.getMessage());
        return null;
    });
}
```

Metodo 3 — sendSingleRequest
Costruisce e manda una singola richiesta HTTP, poi scrive la risposta nel log.
Ritorna un CompletableFuture<Void> che si completa quando la risposta arriva.
```java
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
                    logArea.appendText("Body risposta:\n" + response.body() + "\n");
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
```

Esempio pratico
Settings: maxParallelRequests = 2
Body nella bodyArea:
```example
{"nome": "Mario"}

{"nome": "Luigi"}

{"nome": "Peach"}

{"nome": "Toad"}

{"nome": "Yoshi"}
```

Esecuzione:
```example
📦 Batch 1/3 — 2 richieste in partenza...
    → [1/5] e [2/5] partono insieme
    → aspetta che entrambi rispondano

📦 Batch 2/3 — 2 richieste in partenza...
    → [3/5] e [4/5] partono insieme
    → aspetta che entrambi rispondano

📦 Batch 3/3 — 1 richiesta in partenza...
    → [5/5] parte
    → aspetta la risposta

=== [1/5] RISPOSTA ===
Body inviato: {"nome": "Mario"}
Status: 200
```



Nota: dentro ogni batch le risposte possono arrivare in ordine qualsiasi
(dipende dal server), ma il batch successivo parte sempre dopo che tutti
hanno risposto.