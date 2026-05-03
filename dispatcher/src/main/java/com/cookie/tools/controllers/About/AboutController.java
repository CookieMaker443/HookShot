package com.cookie.tools.controllers.About;

import javafx.fxml.FXML;

@SuppressWarnings("unused")
public class AboutController {
    
    @FXML
    private void onGithubClick() {
        openUrlwithThread("https://github.com/CookieMaker443/HookShot");
    }

    @FXML
    private void onCoffeeClick() {
        openUrlwithThread("https://buymeacoffee.com/cookiemaker"); // aggiungi dopo
    }

    private void openUrlnormal(String url) {
        try {
            java.awt.Desktop.getDesktop().browse(new java.net.URI(url));
        } catch (Exception e) {
            System.out.println("Errore apertura link: " + e.getMessage());
        }
    }
    private void openUrlwithThread(String url) {
        new Thread(() -> {
            try {
                java.awt.Desktop.getDesktop().browse(new java.net.URI(url));
            } catch (Exception e) {
                System.out.println("Errore apertura link: " + e.getMessage());
            }
        }).start();
    }
}