package com.hospital.harmonia.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Desktop;
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
}
