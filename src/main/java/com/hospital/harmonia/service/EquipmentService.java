package com.hospital.harmonia.service;

import com.hospital.harmonia.dao.EquipmentDao;
import com.hospital.harmonia.dao.impl.EquipmentDaoImpl;
import com.hospital.harmonia.model.Equipment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

public class EquipmentService {

    private static final Logger log = LoggerFactory.getLogger(EquipmentService.class);

    // Scalability note: for multiple users/workstations accessing the same
    // system, replace this local storage with shared storage (a network
    // folder, or an S3/MinIO bucket) -- see the scalability section in the guide.
    // Folder name kept from when this module only handled printers -- not
    // worth a migration of existing attached files just to rename it.
    private static final Path ATTACHMENTS_FOLDER = Path.of("anexos_impressoras");
    private final EquipmentDao equipmentDao;

    public EquipmentService() {
        this(new EquipmentDaoImpl());
    }

    /** Package-visible constructor allowing a test double to be injected (see EquipmentServiceTest). */
    EquipmentService(EquipmentDao equipmentDao) {
        this.equipmentDao = equipmentDao;
    }

    public Equipment registerShipment(Equipment equipment) {
        validate(equipment);
        return equipmentDao.save(equipment);
    }

    /** Full update of an existing record -- covers both editing any field and registering the return. */
    public void update(Equipment equipment) {
        validate(equipment);
        equipmentDao.update(equipment);
    }

    public List<Equipment> findAll() {
        return equipmentDao.findAll();
    }

    public Optional<Equipment> findById(int id) {
        return equipmentDao.findById(id);
    }

    /**
     * Permanently deletes the record and, best-effort, its attached note file
     * (if any). Kept for occasional administrative use only -- not called by
     * the screen. The normal flow is deactivate/activate, which preserves the
     * record for the log (see MealService.remove for the same convention).
     */
    public void remove(int id) {
        equipmentDao.findById(id).ifPresent(equipment -> {
            if (equipment.getNotePath() != null && !equipment.getNotePath().isBlank()) {
                try {
                    Files.deleteIfExists(Path.of(equipment.getNotePath()));
                } catch (IOException e) {
                    log.warn("Failed to delete note attachment file for equipmentId={}: {}",
                            id, equipment.getNotePath(), e);
                }
            }
        });
        equipmentDao.remove(id);
    }

    public void deactivate(int id) {
        equipmentDao.deactivate(id);
    }

    public void activate(int id) {
        equipmentDao.activate(id);
    }

    /**
     * Copies the attached note (PDF, issued by the technical support when the
     * equipment is dropped off) into the local storage folder and returns the
     * saved relative path (to store in caminho_anexo_garantia).
     */
    public String saveAttachment(Path sourceFile, int equipmentId) throws IOException {
        Files.createDirectories(ATTACHMENTS_FOLDER);
        String timestamp = java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String newName = "nota_%d_%s_%s".formatted(equipmentId, timestamp, sourceFile.getFileName());
        Path destination = ATTACHMENTS_FOLDER.resolve(newName);
        try {
            Files.copy(sourceFile, destination, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.error("Failed to save note attachment for equipmentId={}", equipmentId, e);
            throw e;
        }
        log.info("Note attachment saved for equipmentId={}: {}", equipmentId, destination);
        return destination.toString();
    }

    private void validate(Equipment equipment) {
        if (equipment.getEquipmentType() == null || equipment.getEquipmentType().isBlank()) {
            throw new IllegalArgumentException("Tipo de equipamento é obrigatório.");
        }
        if (equipment.getModel() == null || equipment.getModel().isBlank()) {
            throw new IllegalArgumentException("Modelo do equipamento é obrigatório.");
        }
        if (equipment.getShippedDate() == null) {
            throw new IllegalArgumentException("Data de envio é obrigatória.");
        }
        if (equipment.getTechnicalSupport() == null || equipment.getTechnicalSupport().isBlank()) {
            throw new IllegalArgumentException("Informe a assistência técnica responsável.");
        }
        if (equipment.getReturnedDate() != null && equipment.getReturnedDate().isBefore(equipment.getShippedDate())) {
            throw new IllegalArgumentException("Data de retorno não pode ser anterior à data de envio.");
        }
        if (equipment.getWarrantyDays() != null && equipment.getWarrantyDays() < 0) {
            throw new IllegalArgumentException("Os dias de garantia não podem ser negativos.");
        }
    }
}
