package com.hospital.harmonia.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Desktop;
import java.io.File;
import java.net.URI;

public final class BrowserUtil {

    private static final Logger log = LoggerFactory.getLogger(BrowserUtil.class);

    private BrowserUtil() {
    }

    public static void open(String url) {
        try {
            Desktop.getDesktop().browse(new URI(url));
        } catch (Exception e) {
            log.warn("Failed to open link in the default browser: {}", url, e);
            AlertUtil.error("Erro", "Não foi possível abrir o link: " + e.getMessage());
        }
    }

    /** Opens a local file (e.g. a PDF attachment) with the system's default viewer. */
    public static void openFile(File file) {
        if (!file.exists()) {
            AlertUtil.error("Arquivo não encontrado", "O arquivo não foi encontrado: " + file.getAbsolutePath());
            return;
        }
        try {
            Desktop.getDesktop().open(file);
        } catch (Exception e) {
            log.warn("Failed to open local file: {}", file.getAbsolutePath(), e);
            AlertUtil.error("Erro", "Não foi possível abrir o arquivo: " + e.getMessage());
        }
    }
}
