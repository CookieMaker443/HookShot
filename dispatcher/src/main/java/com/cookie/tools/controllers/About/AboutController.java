package com.cookie.tools.controllers.About;

import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.MutableDataSet;
import javafx.fxml.FXML;
import javafx.scene.web.WebView;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import javafx.scene.web.WebView;

public class AboutController {

    @FXML private WebView aboutWebView;

    @FXML
    private void initialize() {
        try {
            // legge il file about.md dalle resources
            InputStream in = getClass()
                .getResourceAsStream("/com/cookie/tools/about.md");
            String markdown = new String(in.readAllBytes(), StandardCharsets.UTF_8);

            // converte markdown in HTML con Flexmark
            MutableDataSet options = new MutableDataSet();
            Parser parser = Parser.builder(options).build();
            HtmlRenderer renderer = HtmlRenderer.builder(options).build();
            Node document = parser.parse(markdown);
            String html = renderer.render(document);

            // carica l'HTML nella WebView con un po' di stile base
            aboutWebView.getEngine().loadContent(
                "<html><body style='" +
                "font-family: sans-serif;" +
                "padding: 20px;" +
                "max-width: 600px;" +
                "'>" + html + "</body></html>"
            );

            // apre i link nel browser di sistema invece che nella WebView
            aboutWebView.getEngine().locationProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null && !newVal.isEmpty() && !newVal.equals("about:blank")) {
                    try {
                        java.awt.Desktop.getDesktop().browse(new java.net.URI(newVal));
                        aboutWebView.getEngine().load("about:blank");
                    } catch (Exception e) {
                        System.out.println("Errore apertura link: " + e.getMessage());
                    }
                }
            });

        } catch (Exception e) {
            aboutWebView.getEngine().loadContent("<p>Errore caricamento about.</p>");
        }
    }
}