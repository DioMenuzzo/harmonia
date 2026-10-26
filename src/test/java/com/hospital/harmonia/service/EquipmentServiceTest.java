package com.hospital.harmonia.service;

import com.hospital.harmonia.dao.EquipmentDao;
import com.hospital.harmonia.model.Equipment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises EquipmentService's validation rules and its best-effort cleanup
 * of the attached note file, with an in-memory fake instead of a real
 * database (see FakeEquipmentDao below).
 */
class EquipmentServiceTest {

    private FakeEquipmentDao fakeDao;
    private EquipmentService service;

    @BeforeEach
    void setUp() {
        fakeDao = new FakeEquipmentDao();
        service = new EquipmentService(fakeDao);
    }

    private static Equipment valid() {
        Equipment e = new Equipment();
        e.setEquipmentType("Impressora");
        e.setModel("LaserJet 400");
        e.setShippedDate(LocalDate.of(2026, 1, 10));
        e.setTechnicalSupport("Assistência Central");
        return e;
    }

    @Test
    void registerShipment_rejectsMissingEquipmentType() {
        Equipment e = valid();
        e.setEquipmentType(" ");
        assertThrows(IllegalArgumentException.class, () -> service.registerShipment(e));
        assertTrue(fakeDao.saved.isEmpty());
    }

    @Test
    void registerShipment_rejectsMissingModel() {
        Equipment e = valid();
        e.setModel(null);
        assertThrows(IllegalArgumentException.class, () -> service.registerShipment(e));
    }

    @Test
    void registerShipment_rejectsMissingShippedDate() {
        Equipment e = valid();
        e.setShippedDate(null);
        assertThrows(IllegalArgumentException.class, () -> service.registerShipment(e));
    }

    @Test
    void registerShipment_rejectsMissingTechnicalSupport() {
        Equipment e = valid();
        e.setTechnicalSupport("");
        assertThrows(IllegalArgumentException.class, () -> service.registerShipment(e));
    }

    @Test
    void registerShipment_rejectsAReturnDateBeforeTheShippedDate() {
        Equipment e = valid();
        e.setReturnedDate(e.getShippedDate().minusDays(1));
        assertThrows(IllegalArgumentException.class, () -> service.registerShipment(e));
    }

    @Test
    void registerShipment_acceptsAReturnDateOnTheSameDayAsShipped() {
        Equipment e = valid();
        e.setReturnedDate(e.getShippedDate());
        assertEquals(e, service.registerShipment(e));
    }

    @Test
    void registerShipment_rejectsNegativeWarrantyDays() {
        Equipment e = valid();
        e.setWarrantyDays(-1);
        assertThrows(IllegalArgumentException.class, () -> service.registerShipment(e));
    }

    @Test
    void registerShipment_savesAValidEquipmentAndAssignsAnId() {
        Equipment saved = service.registerShipment(valid());
        assertEquals(1, saved.getId().intValue());
        assertEquals(1, fakeDao.saved.size());
    }

    @Test
    void update_appliesTheSameValidationRulesAsRegistration() {
        Equipment e = valid();
        e.setId(1);
        e.setModel(null);
        assertThrows(IllegalArgumentException.class, () -> service.update(e));
        assertTrue(fakeDao.updated.isEmpty());
    }

    @Test
    void update_delegatesToTheDaoWhenValid() {
        Equipment e = valid();
        e.setId(1);
        service.update(e);
        assertEquals(1, fakeDao.updated.size());
    }

    @Test
    void findAll_findById_deactivate_activate_delegateToTheDao() {
        Equipment saved = service.registerShipment(valid());

        assertEquals(1, service.findAll().size());
        assertTrue(service.findById(saved.getId()).isPresent());
        assertTrue(service.findById(999).isEmpty());

        service.deactivate(saved.getId());
        assertFalse(fakeDao.findById(saved.getId()).orElseThrow().isActive());

        service.activate(saved.getId());
        assertTrue(fakeDao.findById(saved.getId()).orElseThrow().isActive());
    }

    @Test
    void remove_bestEffortDeletesTheAttachedNoteFile(@TempDir Path tempDir) throws IOException {
        Path notePath = tempDir.resolve("nota_garantia.pdf");
        Files.writeString(notePath, "conteudo de teste");
        assertTrue(Files.exists(notePath), "test setup: the file should exist before remove() runs");

        Equipment e = valid();
        e.setNotePath(notePath.toString());
        Equipment saved = fakeDao.save(e);

        service.remove(saved.getId());

        assertFalse(Files.exists(notePath), "remove() should have deleted the attached note file");
        assertTrue(fakeDao.removedIds.contains(saved.getId()));
    }

    @Test
    void remove_toleratesAnEquipmentWithNoAttachment() {
        Equipment saved = fakeDao.save(valid()); // notePath left null
        assertDoesNotThrow(() -> service.remove(saved.getId()));
        assertTrue(fakeDao.removedIds.contains(saved.getId()));
    }

    @Test
    void remove_toleratesAnAttachmentFileThatNoLongerExists() {
        Equipment e = valid();
        e.setNotePath("/this/path/does/not/exist/nota.pdf");
        Equipment saved = fakeDao.save(e);
        assertDoesNotThrow(() -> service.remove(saved.getId()));
    }

    /** Minimal in-memory stand-in for EquipmentDaoImpl -- no database involved. */
    private static class FakeEquipmentDao implements EquipmentDao {
        final Map<Integer, Equipment> saved = new LinkedHashMap<>();
        final List<Equipment> updated = new ArrayList<>();
        final List<Integer> removedIds = new ArrayList<>();
        private int nextId = 1;

        @Override
        public Equipment save(Equipment equipment) {
            equipment.setId(nextId++);
            saved.put(equipment.getId(), equipment);
            return equipment;
        }

        @Override
        public void update(Equipment equipment) {
            updated.add(equipment);
            saved.put(equipment.getId(), equipment);
        }

        @Override
        public List<Equipment> findAll() {
            return List.copyOf(saved.values());
        }

        @Override
        public Optional<Equipment> findById(int id) {
            return Optional.ofNullable(saved.get(id));
        }

        @Override
        public void remove(int id) {
            removedIds.add(id);
            saved.remove(id);
        }

        @Override
        public void deactivate(int id) {
            Optional.ofNullable(saved.get(id)).ifPresent(e -> e.setActive(false));
        }

        @Override
        public void activate(int id) {
            Optional.ofNullable(saved.get(id)).ifPresent(e -> e.setActive(true));
        }
    }
}
