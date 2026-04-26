package com.cookie.tools.managers;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class FileManager {
    private static FileManager instance;

    public static FileManager getInstance() {
        if (instance == null) instance = new FileManager();
        return instance;
    }

    private FileManager() {}

    // --- PACKET ---

    // salva un pacchetto su file
    // ritorna false se il file esiste già
    public boolean savePacket(String name, String method, String url,
                               String rawHeaders, String body) throws IOException {

        String finalName = (name == null || name.isBlank()) 
            ? generatePacketName() 
            : name.trim();

        Path path = Path.of(SettingsManager.PACKETS_DIR, finalName);

        // avverte se esiste già
        if (Files.exists(path)) return false;

        StringBuilder sb = new StringBuilder();
        sb.append("METHOD=").append(method).append("\n");
        sb.append("URL=").append(url).append("\n");

        // ogni header va su una riga separata con il prefisso HEADER=
        if (!rawHeaders.isBlank()) {
            for (String line : rawHeaders.split("\n")) {
                if (!line.isBlank()) {
                    sb.append("HEADER=").append(line.trim()).append("\n");
                }
            }
        }

        // il body può essere multiriga, lo scriviamo tutto dopo BODY=
        // usiamo un separatore di fine body per sicurezza
        if (!body.isBlank()) {
            sb.append("BODY=").append(body);
        }

        Files.writeString(path, sb.toString());
        return true;
    }

    // carica un pacchetto dal file e ritorna un oggetto PacketData
    public PacketData loadPacket(String name) throws IOException {
        Path path = Path.of(SettingsManager.PACKETS_DIR, name);
        List<String> lines = Files.readAllLines(path);

        String method = "";
        String url = "";
        List<String> headers = new ArrayList<>();
        StringBuilder body = new StringBuilder();
        boolean readingBody = false;

        for (String line : lines) {
            if (readingBody) {
                // tutto ciò che viene dopo BODY= è body, anche su più righe
                body.append(line).append("\n");
            } else if (line.startsWith("METHOD=")) {
                method = line.substring("METHOD=".length());
            } else if (line.startsWith("URL=")) {
                url = line.substring("URL=".length());
            } else if (line.startsWith("HEADER=")) {
                headers.add(line.substring("HEADER=".length()));
            } else if (line.startsWith("BODY=")) {
                readingBody = true;
                body.append(line.substring("BODY=".length())).append("\n");
            }
        }

        return new PacketData(method, url, headers, body.toString().trim());
    }

    // lista tutti i pacchetti salvati
    public List<String> listPackets() throws IOException {
        return listFiles(SettingsManager.PACKETS_DIR);
    }

    // genera un nome automatico tipo "packet_01", "packet_02", ecc.
    private String generatePacketName() throws IOException {
        List<String> existing = listPackets();
        int i = 1;
        String name;
        do {
            name = String.format("packet_%02d", i++);
        } while (existing.contains(name));
        return name;
    }

    // --- HEADERS ---

    public boolean saveHeaders(String name, String rawHeaders) throws IOException {
        String finalName = (name == null || name.isBlank()) ? "headers_01" : name.trim();
        Path path = Path.of(SettingsManager.HEADERS_DIR, finalName);
        if (Files.exists(path)) return false;
        Files.writeString(path, rawHeaders.trim());
        return true;
    }

    public String loadHeaders(String name) throws IOException {
        return Files.readString(Path.of(SettingsManager.HEADERS_DIR, name));
    }

    public List<String> listHeaders() throws IOException {
        return listFiles(SettingsManager.HEADERS_DIR);
    }

    // --- URL ---

    public boolean saveUrl(String name, String url) throws IOException {
        String finalName = (name == null || name.isBlank()) ? "url_01" : name.trim();
        Path path = Path.of(SettingsManager.URLS_DIR, finalName);
        if (Files.exists(path)) return false;
        Files.writeString(path, url.trim());
        return true;
    }

    public String loadUrl(String name) throws IOException {
        return Files.readString(Path.of(SettingsManager.URLS_DIR, name));
    }

    public List<String> listUrls() throws IOException {
        return listFiles(SettingsManager.URLS_DIR);
    }

    // --- UTILITY ---

    // lista tutti i file in una directory (solo file, non sottocartelle)
    private List<String> listFiles(String dir) throws IOException {
        try (var stream = Files.list(Path.of(dir))) {
            return stream
                .filter(Files::isRegularFile)
                .map(p -> p.getFileName().toString())
                .sorted()
                .collect(java.util.stream.Collectors.toList());
        }
    }

    // salva il log in un path scelto dall'utente
    public void saveLog(String content, File file) throws IOException {
        Files.writeString(file.toPath(), content);
    }
}