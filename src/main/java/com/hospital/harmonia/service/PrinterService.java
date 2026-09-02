package com.hospital.harmonia.service;

import com.hospital.harmonia.dao.PrinterDao;
import com.hospital.harmonia.dao.impl.PrinterDaoImpl;
import com.hospital.harmonia.model.Printer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class PrinterService {

    private static final Logger log = LoggerFactory.getLogger(PrinterService.class);

    private static final Path ATTACHMENTS_FOLDER = Path.of("anexos_garantia");
    private final PrinterDao printerDao = new PrinterDaoImpl();

    public Printer registerShipment(Printer printer) {
        validate(printer);
        return printerDao.save(printer);
    }

    public void registerReturn(Printer printer) {
        printerDao.update(printer);
    }

    public List<Printer> findAll() {
        return printerDao.findAll();
    }

    /**
     * Copies the attached warranty file into the local storage folder and
     * returns the saved relative path (to store in caminho_anexo_garantia).
     *
     * Scalability note: for multiple users/workstations accessing the same
     * system, replace this local storage with shared storage (a network
     * folder, or an S3/MinIO bucket) -- see the scalability section in the guide.
     */
    public String saveWarrantyAttachment(Path sourceFile, int printerId) throws IOException {
        Files.createDirectories(ATTACHMENTS_FOLDER);
        String timestamp = java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String newName = "garantia_%d_%s_%s".formatted(printerId, timestamp, sourceFile.getFileName());
        Path destination = ATTACHMENTS_FOLDER.resolve(newName);
        try {
            Files.copy(sourceFile, destination, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.error("Failed to save warranty attachment for printerId={}", printerId, e);
            throw e;
        }
        log.info("Warranty attachment saved for printerId={}: {}", printerId, destination);
        return destination.toString();
    }

    private void validate(Printer printer) {
        if (printer.getModel() == null || printer.getModel().isBlank()) {
            throw new IllegalArgumentException("Modelo da impressora e obrigatorio.");
        }
        if (printer.getShippedDate() == null) {
            throw new IllegalArgumentException("Data de envio e obrigatoria.");
        }
        if (printer.getTechnicalSupport() == null || printer.getTechnicalSupport().isBlank()) {
            throw new IllegalArgumentException("Informe a assistencia tecnica responsavel.");
        }
    }
}
