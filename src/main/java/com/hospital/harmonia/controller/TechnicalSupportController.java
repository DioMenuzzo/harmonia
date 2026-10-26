package com.hospital.harmonia.controller;

import com.hospital.harmonia.App;
import com.hospital.harmonia.model.Equipment;
import com.hospital.harmonia.service.EquipmentService;
import com.hospital.harmonia.util.AlertUtil;
import com.hospital.harmonia.util.BrowserUtil;
import com.hospital.harmonia.util.FormUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Technical Support module: logs any equipment sent out for repair
 * (printers, notebooks, projectors, monitors, desktops/CPUs, etc.) -- ship
 * date, return date, reported issue, service performed, warranty (in days)
 * and the note (PDF) attached to the record.
 */
public class TechnicalSupportController implements Initializable {

    private static final Logger log = LoggerFactory.getLogger(TechnicalSupportController.class);

    // Marks the "write it in yourself" option in the equipment type/issue/service combos below.
    private static final String OTHER_OPTION = "Outro";

    // Most common equipment types sent to technical support -- see
    // configureEquipmentTypeCombo.
    private static final List<String> EQUIPMENT_TYPES = List.of(
            "Impressora",
            "Notebook",
            "Projetor",
            "Monitor"
    );

    // Lists of the most common issues/solutions PER EQUIPMENT TYPE -- see
    // issuesForType/solutionsForType. Changing the "Equipment type" in the
    // form updates the Reported issue/Service performed combos to the
    // matching list (configureEquipmentTypeCombo).

    private static final List<String> PRINTER_ISSUES = List.of(
            "Atolamento de papel",
            "Impressão borrada ou desbotada",
            "Não liga",
            "Toner/cartucho vazio ou baixo",
            "Não puxa/alimenta o papel",
            "Erro de conexão (rede/USB)",
            "Ruído anormal durante a impressão",
            "Manchas ou riscos na impressão",
            "Painel/display travado ou com erro"
    );

    private static final List<String> PRINTER_SOLUTIONS = List.of(
            "Troca de toner/cartucho",
            "Limpeza interna/unidade de imagem",
            "Substituição de peça (rolete, fusor, etc.)",
            "Atualização de firmware/driver",
            "Reparo do mecanismo de alimentação de papel",
            "Reparo da placa/circuito",
            "Configuração de rede reinstalada",
            "Sem defeito encontrado (teste OK)"
    );

    private static final List<String> NOTEBOOK_ISSUES = List.of(
            "Não liga",
            "Tela quebrada ou com manchas",
            "Bateria não carrega/não segura carga",
            "Superaquecimento/desliga sozinho",
            "Teclado ou touchpad não funciona",
            "Lentidão excessiva",
            "Não conecta wi-fi/rede",
            "Tela azul/travamentos frequentes",
            "Dobradiça/carcaça danificada",
            "Vírus/malware"
    );

    private static final List<String> NOTEBOOK_SOLUTIONS = List.of(
            "Troca de bateria",
            "Troca de tela/display",
            "Limpeza interna e troca de pasta térmica",
            "Formatação/reinstalação do sistema",
            "Troca de teclado",
            "Upgrade de memória/HD/SSD",
            "Reparo da placa-mãe",
            "Remoção de vírus/malware",
            "Sem defeito encontrado (teste OK)"
    );

    private static final List<String> PROJECTOR_ISSUES = List.of(
            "Não liga",
            "Imagem escura ou fraca",
            "Lâmpada queimada",
            "Superaquecimento",
            "Sem sinal de vídeo (HDMI/VGA)",
            "Imagem desfocada",
            "Ruído excessivo da ventoinha",
            "Cores distorcidas",
            "Filtro de ar sujo/entupido"
    );

    private static final List<String> PROJECTOR_SOLUTIONS = List.of(
            "Troca de lâmpada",
            "Limpeza de filtro/ventilação",
            "Troca de placa/fonte",
            "Ajuste de foco/lente",
            "Atualização de firmware",
            "Reparo da entrada de vídeo",
            "Sem defeito encontrado (teste OK)"
    );

    private static final List<String> MONITOR_ISSUES = List.of(
            "Não liga",
            "Sem imagem/tela preta",
            "Imagem distorcida ou com listras",
            "Manchas ou pixels queimados",
            "Cores desbotadas/erradas",
            "Piscando ou instável",
            "Não reconhece o sinal do computador"
    );

    private static final List<String> MONITOR_SOLUTIONS = List.of(
            "Troca de cabo de vídeo",
            "Troca de painel/tela",
            "Reparo da fonte interna",
            "Reparo da placa de vídeo do monitor",
            "Ajuste de configurações",
            "Sem defeito encontrado (teste OK)"
    );

    // Used when the type is "Outro" (equipment not in the list) or while no
    // type has been selected yet -- generic options that fit any equipment.
    private static final List<String> GENERIC_ISSUES = List.of(
            "Não liga",
            "Apresenta defeito intermitente",
            "Dano físico/quebrado",
            "Superaquecimento",
            "Desempenho anormal",
            "Erro de conexão (rede/cabo)",
            "Ruído anormal",
            "Painel/display com problema"
    );

    private static final List<String> GENERIC_SOLUTIONS = List.of(
            "Troca de peça",
            "Limpeza interna",
            "Atualização de firmware/software",
            "Reparo do circuito/placa",
            "Configuração/reinstalação",
            "Sem defeito encontrado (teste OK)"
    );

    private static List<String> issuesForType(String type) {
        if (type == null) {
            return GENERIC_ISSUES;
        }
        switch (type) {
            case "Impressora": return PRINTER_ISSUES;
            case "Notebook": return NOTEBOOK_ISSUES;
            case "Projetor": return PROJECTOR_ISSUES;
            case "Monitor": return MONITOR_ISSUES;
            default: return GENERIC_ISSUES;
        }
    }

    private static List<String> solutionsForType(String type) {
        if (type == null) {
            return GENERIC_SOLUTIONS;
        }
        switch (type) {
            case "Impressora": return PRINTER_SOLUTIONS;
            case "Notebook": return NOTEBOOK_SOLUTIONS;
            case "Projetor": return PROJECTOR_SOLUTIONS;
            case "Monitor": return MONITOR_SOLUTIONS;
            default: return GENERIC_SOLUTIONS;
        }
    }

    // --- Form ---
    @FXML private ComboBox<String> equipmentTypeCombo;
    @FXML private TextField equipmentTypeOtherField;
    @FXML private TextField modelField;
    @FXML private TextField brandField;
    @FXML private TextField assetTagField;
    @FXML private TextField serialNumberField;
    @FXML private TextField originSectorField;
    @FXML private TextField technicalSupportField;
    @FXML private DatePicker shippedDatePicker;
    @FXML private DatePicker returnedDatePicker;
    @FXML private ComboBox<String> reportedIssueCombo;
    @FXML private TextArea reportedIssueOtherArea;
    @FXML private ComboBox<String> serviceDoneCombo;
    @FXML private TextArea serviceDoneOtherArea;
    @FXML private TextField warrantyDaysField;
    @FXML private Label noteFileLabel;
    @FXML private Button saveButton;

    // --- Filters ---
    @FXML private ComboBox<String> equipmentFilterTypeCombo;
    @FXML private TextField filterModelField;
    @FXML private TextField filterAssetTagField;
    @FXML private ComboBox<String> equipmentFilterStatusCombo;
    @FXML private ComboBox<String> equipmentFilterActiveCombo;

    // --- Table ---
    @FXML private TableView<Equipment> equipmentTable;
    @FXML private TableColumn<Equipment, Integer> idColumn;
    @FXML private TableColumn<Equipment, String> typeColumn;
    @FXML private TableColumn<Equipment, String> modelColumn;
    @FXML private TableColumn<Equipment, String> brandColumn;
    @FXML private TableColumn<Equipment, String> assetTagColumn;
    @FXML private TableColumn<Equipment, String> serialNumberColumn;
    @FXML private TableColumn<Equipment, String> originSectorColumn;
    @FXML private TableColumn<Equipment, String> technicalSupportColumn;
    @FXML private TableColumn<Equipment, String> shippedDateColumn;
    @FXML private TableColumn<Equipment, String> returnedDateColumn;
    @FXML private TableColumn<Equipment, String> warrantyColumn;
    @FXML private TableColumn<Equipment, String> noteColumn;
    @FXML private TableColumn<Equipment, String> statusColumn;
    @FXML private TableColumn<Equipment, String> activeColumn;

    private final EquipmentService equipmentService = new EquipmentService();
    private final ObservableList<Equipment> equipmentList = FXCollections.observableArrayList();
    private FilteredList<Equipment> filteredEquipment;

    // Id of the equipment record being edited (double-click on a table row),
    // or null when the form is in normal registration mode (new record). The
    // original "active" flag is stored separately because the form has no
    // field for it -- deactivate/activate have their own buttons.
    private Integer editingEquipmentId;
    private boolean editingEquipmentActive;
    // Path of an already-saved note attachment, kept when editing a record
    // whose note isn't being replaced in this form session.
    private String currentNotePath;
    // A new PDF picked via "Anexar nota", not yet copied into local storage
    // -- only actually saved (and its path recorded) when the form is saved.
    private File selectedAttachmentFile;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        configureTable();
        configureFilters();
        configureDatePickers();
        configureEquipmentTypeCombo();
        configureIssueAndServiceCombos();
        onClearForm();
        loadEquipment();
    }

    /**
     * Configures the "Equipment type" combo with the list of most common
     * types + "Outro" -- same pattern used for Reported issue/Service
     * performed. Changing the type also updates the Reported issue/Service
     * performed combos' options to the most common ones FOR THAT type (see
     * onEquipmentTypeChanged).
     */
    private void configureEquipmentTypeCombo() {
        configureOptionsCombo(equipmentTypeCombo, equipmentTypeOtherField, EQUIPMENT_TYPES);
        equipmentTypeCombo.valueProperty().addListener((obs, o, n) -> onEquipmentTypeChanged());

        equipmentFilterTypeCombo.setItems(FXCollections.observableArrayList("Todos"));
        equipmentFilterTypeCombo.getItems().addAll(EQUIPMENT_TYPES);
        equipmentFilterTypeCombo.setValue("Todos");
    }

    /**
     * Configures the "Reported issue" and "Service performed" combos with
     * the initial generic lists (no type selected yet) + "Outro" -- choosing
     * "Outro" reveals a free-text field to describe what isn't in the list.
     * The options themselves are swapped dynamically by onEquipmentTypeChanged
     * according to the chosen Equipment type.
     */
    private void configureIssueAndServiceCombos() {
        configureOptionsCombo(reportedIssueCombo, reportedIssueOtherArea, GENERIC_ISSUES);
        configureOptionsCombo(serviceDoneCombo, serviceDoneOtherArea, GENERIC_SOLUTIONS);
    }

    /**
     * Updates the Reported issue/Service performed options to the most
     * common ones for the selected equipment type (or the generic ones, if
     * "Outro"/no type) -- clears the current selection of these two combos,
     * since an issue/solution from a different equipment type usually
     * doesn't apply. When loading an existing record for editing
     * (loadEquipmentForEditing), the saved value is restored right after,
     * so this clearing has no visible effect in that case.
     */
    private void onEquipmentTypeChanged() {
        String type = equipmentTypeCombo.getValue();
        List<String> issues = issuesForType(type);
        List<String> solutions = solutionsForType(type);
        setComboItems(reportedIssueCombo, issues);
        setComboItems(serviceDoneCombo, solutions);
        setComboValue(reportedIssueCombo, reportedIssueOtherArea, issues, null);
        setComboValue(serviceDoneCombo, serviceDoneOtherArea, solutions, null);
    }

    private static void configureOptionsCombo(ComboBox<String> combo, TextInputControl otherControl, List<String> commonOptions) {
        setComboItems(combo, commonOptions);
        combo.valueProperty().addListener((obs, o, n) -> toggleOtherVisibility(combo, otherControl));
        toggleOtherVisibility(combo, otherControl);
    }

    private static void setComboItems(ComboBox<String> combo, List<String> commonOptions) {
        ObservableList<String> items = FXCollections.observableArrayList(commonOptions);
        items.add(OTHER_OPTION);
        combo.setItems(items);
    }

    private static void toggleOtherVisibility(ComboBox<String> combo, TextInputControl otherControl) {
        boolean showOther = OTHER_OPTION.equals(combo.getValue());
        otherControl.setVisible(showOther);
        otherControl.setManaged(showOther);
    }

    /** Reads the combo's final value (the chosen option, or the free text when "Outro" is selected). */
    private static String resolveComboValue(ComboBox<String> combo, TextInputControl otherControl) {
        String selected = combo.getValue();
        if (selected == null) {
            return null;
        }
        if (OTHER_OPTION.equals(selected)) {
            return trimOrNull(otherControl.getText());
        }
        return selected;
    }

    /**
     * Fills the combo from an already-saved value: if it matches one of the
     * common options, selects it; otherwise (previously saved free text),
     * selects "Outro" and puts the text in the free field.
     */
    private static void setComboValue(ComboBox<String> combo, TextInputControl otherControl, List<String> knownOptions, String value) {
        if (value == null || value.isBlank()) {
            combo.setValue(null);
            otherControl.clear();
        } else if (knownOptions.contains(value)) {
            combo.setValue(value);
            otherControl.clear();
        } else {
            combo.setValue(OTHER_OPTION);
            otherControl.setText(value);
        }
        toggleOtherVisibility(combo, otherControl);
    }

    private void configureDatePickers() {
        FormUtil.configureDateFormat(shippedDatePicker);
        FormUtil.configureDateFormat(returnedDatePicker);
    }

    // ================= TABLE =================

    private void configureTable() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        typeColumn.setCellValueFactory(new PropertyValueFactory<>("equipmentType"));
        modelColumn.setCellValueFactory(new PropertyValueFactory<>("model"));
        brandColumn.setCellValueFactory(new PropertyValueFactory<>("brand"));
        assetTagColumn.setCellValueFactory(new PropertyValueFactory<>("assetTag"));
        serialNumberColumn.setCellValueFactory(new PropertyValueFactory<>("serialNumber"));
        originSectorColumn.setCellValueFactory(new PropertyValueFactory<>("originSector"));
        technicalSupportColumn.setCellValueFactory(new PropertyValueFactory<>("technicalSupport"));
        shippedDateColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                data.getValue().getShippedDate() == null ? "" : FormUtil.DATE_FORMAT.format(data.getValue().getShippedDate())));
        returnedDateColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                data.getValue().getReturnedDate() == null ? "" : FormUtil.DATE_FORMAT.format(data.getValue().getReturnedDate())));
        warrantyColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                formatWarranty(data.getValue())));
        noteColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                FormUtil.orEmpty(data.getValue().getNotePath()).isBlank() ? "-" : "Anexada"));
        statusColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                statusOf(data.getValue())));
        activeColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                data.getValue().isActive() ? "SIM" : "NÃO"));

        filteredEquipment = new FilteredList<>(equipmentList, e -> true);
        equipmentTable.setItems(filteredEquipment);

        // Double-clicking a row loads the record into the form on the left
        // for editing (SALVAR becomes ATUALIZAR) -- useful both for fixing
        // any field and for registering the return (filling in Return date
        // and Service performed, then clicking ATUALIZAR).
        equipmentTable.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                Equipment selected = equipmentTable.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    loadEquipmentForEditing(selected);
                }
            }
        });

        // Whole row in light red if the record has been deactivated (same
        // pattern as employees/meals), otherwise in light yellow while the
        // equipment is still under repair (no return date) -- so it's
        // visible in the table without having to open the record.
        equipmentTable.setRowFactory(table -> new TableRow<Equipment>() {
            @Override
            protected void updateItem(Equipment equipment, boolean empty) {
                super.updateItem(equipment, empty);
                if (empty || equipment == null) {
                    setStyle("");
                } else if (!equipment.isActive()) {
                    setStyle("-fx-background-color: #f8d7da;");
                } else if (equipment.getReturnedDate() == null) {
                    setStyle("-fx-background-color: #fff3cd;");
                } else {
                    setStyle("");
                }
            }
        });
    }

    /**
     * "Em conserto"/"Retornado" text for the table and the status filter --
     * moved here from Equipment.getStatus() (see the audit's "3.6 Camadas"):
     * every other UI-facing mapping in the project (Meal, Employee, ...)
     * happens in the controller, not the model.
     */
    private static String statusOf(Equipment e) {
        return e.getReturnedDate() == null ? "Em conserto" : "Retornado";
    }

    private static String formatWarranty(Equipment e) {
        if (e.getWarrantyDays() == null) {
            return "-";
        }
        LocalDate expiration = e.getWarrantyExpirationDate();
        return expiration == null
                ? e.getWarrantyDays() + " dias"
                : e.getWarrantyDays() + " dias (até " + FormUtil.DATE_FORMAT.format(expiration) + ")";
    }

    private void loadEquipment() {
        equipmentList.setAll(equipmentService.findAll());
    }

    // ================= FILTERS =================

    private void configureFilters() {
        equipmentFilterStatusCombo.setItems(FXCollections.observableArrayList("Todos", "Em conserto", "Retornado"));
        equipmentFilterStatusCombo.setValue("Todos");

        equipmentFilterActiveCombo.setItems(FXCollections.observableArrayList("Todos", "Ativos", "Inativos"));
        equipmentFilterActiveCombo.setValue("Todos");

        equipmentFilterTypeCombo.valueProperty().addListener((obs, o, n) -> applyFilter());
        filterModelField.textProperty().addListener((obs, o, n) -> applyFilter());
        filterAssetTagField.textProperty().addListener((obs, o, n) -> applyFilter());
        equipmentFilterStatusCombo.valueProperty().addListener((obs, o, n) -> applyFilter());
        equipmentFilterActiveCombo.valueProperty().addListener((obs, o, n) -> applyFilter());
    }

    private void applyFilter() {
        String type = equipmentFilterTypeCombo.getValue();
        String model = FormUtil.normalize(filterModelField.getText());
        String assetTag = FormUtil.orEmpty(filterAssetTagField.getText()).trim();
        String status = equipmentFilterStatusCombo.getValue();
        String active = equipmentFilterActiveCombo.getValue();

        filteredEquipment.setPredicate(e ->
                ("Todos".equals(type) || type.equals(e.getEquipmentType()))
                        && (model.isEmpty() || FormUtil.normalize(FormUtil.orEmpty(e.getModel())).contains(model))
                        && (assetTag.isEmpty() || FormUtil.orEmpty(e.getAssetTag()).contains(assetTag))
                        && ("Todos".equals(status) || status.equals(statusOf(e)))
                        && ("Todos".equals(active)
                                || ("Ativos".equals(active) && e.isActive())
                                || ("Inativos".equals(active) && !e.isActive())));
    }

    // ================= FORM =================

    private void loadEquipmentForEditing(Equipment e) {
        setComboValue(equipmentTypeCombo, equipmentTypeOtherField, EQUIPMENT_TYPES, e.getEquipmentType());
        modelField.setText(e.getModel());
        brandField.setText(e.getBrand());
        assetTagField.setText(e.getAssetTag());
        serialNumberField.setText(e.getSerialNumber());
        originSectorField.setText(e.getOriginSector());
        technicalSupportField.setText(e.getTechnicalSupport());
        shippedDatePicker.setValue(e.getShippedDate());
        returnedDatePicker.setValue(e.getReturnedDate());
        // The equipmentTypeCombo listener (triggered by the setComboValue call
        // above) has already swapped the Issue/Service combos to the loaded
        // type's options -- all that's left is filling in the actual saved value.
        setComboValue(reportedIssueCombo, reportedIssueOtherArea, issuesForType(e.getEquipmentType()), e.getReportedIssue());
        setComboValue(serviceDoneCombo, serviceDoneOtherArea, solutionsForType(e.getEquipmentType()), e.getServiceDone());
        warrantyDaysField.setText(e.getWarrantyDays() == null ? "" : String.valueOf(e.getWarrantyDays()));

        currentNotePath = e.getNotePath();
        selectedAttachmentFile = null;
        noteFileLabel.setText(FormUtil.orEmpty(e.getNotePath()).isBlank()
                ? "Nenhuma nota anexada" : "Nota anexada (use ABRIR NOTA para visualizar)");

        editingEquipmentId = e.getId();
        editingEquipmentActive = e.isActive();
        saveButton.setText("ATUALIZAR");
    }

    @FXML
    private void onClearForm() {
        setComboValue(equipmentTypeCombo, equipmentTypeOtherField, EQUIPMENT_TYPES, null);
        modelField.clear();
        brandField.clear();
        assetTagField.clear();
        serialNumberField.clear();
        originSectorField.clear();
        technicalSupportField.clear();
        // Ship date already comes pre-filled with today's date -- in the vast
        // majority of cases that's when the record is being made; the person
        // can change it if they need to log it retroactively. Return date is
        // left blank: it's normal to log the shipment without knowing yet
        // when it'll come back.
        shippedDatePicker.setValue(LocalDate.now());
        returnedDatePicker.setValue(null);
        // The equipmentTypeCombo listener (triggered by the setComboValue call
        // above, which resets the type) has already cleared the Issue/Service
        // combos and reverted them to the generic options.
        warrantyDaysField.clear();

        currentNotePath = null;
        selectedAttachmentFile = null;
        noteFileLabel.setText("Nenhuma nota anexada");

        editingEquipmentId = null;
        saveButton.setText("SALVAR");
    }

    @FXML
    private void onSave() {
        try {
            Equipment equipment = new Equipment();
            equipment.setEquipmentType(resolveComboValue(equipmentTypeCombo, equipmentTypeOtherField));
            equipment.setModel(trimOrNull(modelField.getText()));
            equipment.setBrand(trimOrNull(brandField.getText()));
            equipment.setAssetTag(trimOrNull(assetTagField.getText()));
            equipment.setSerialNumber(trimOrNull(serialNumberField.getText()));
            equipment.setOriginSector(trimOrNull(originSectorField.getText()));
            equipment.setTechnicalSupport(trimOrNull(technicalSupportField.getText()));
            equipment.setShippedDate(shippedDatePicker.getValue());
            equipment.setReturnedDate(returnedDatePicker.getValue());
            equipment.setReportedIssue(resolveComboValue(reportedIssueCombo, reportedIssueOtherArea));
            equipment.setServiceDone(resolveComboValue(serviceDoneCombo, serviceDoneOtherArea));
            equipment.setWarrantyDays(readWarrantyDays());
            // Keeps the previously saved note unless the user picked a new
            // one below (saveAttachment, then a second update call replaces it).
            equipment.setNotePath(currentNotePath);

            if (editingEquipmentId != null) {
                equipment.setId(editingEquipmentId);
                // Preserves the original active/inactive flag -- the form has
                // no field for it, INATIVAR/ATIVAR have their own button.
                equipment.setActive(editingEquipmentActive);
                equipmentService.update(equipment);
            } else {
                equipmentService.registerShipment(equipment);
            }

            if (selectedAttachmentFile != null) {
                String savedPath = equipmentService.saveAttachment(selectedAttachmentFile.toPath(), equipment.getId());
                equipment.setNotePath(savedPath);
                equipmentService.update(equipment);
            }

            loadEquipment();
            onClearForm();
        } catch (IOException e) {
            log.error("Failed to save note attachment", e);
            AlertUtil.error("Erro ao anexar nota", e.getMessage());
        } catch (Exception e) {
            log.warn("Failed to save equipment record: {}", e.getMessage(), e);
            AlertUtil.error("Erro ao salvar registro", e.getMessage());
        }
    }

    private static String trimOrNull(String text) {
        if (text == null) {
            return null;
        }
        String trimmed = text.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private Integer readWarrantyDays() {
        String text = warrantyDaysField.getText();
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            int days = Integer.parseInt(text.trim());
            if (days < 0) {
                throw new IllegalArgumentException("Os dias de garantia não podem ser negativos.");
            }
            return days;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Dias de garantia inválido: \"" + text + "\".");
        }
    }

    @FXML
    private void onAttachNote() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Selecionar nota (PDF)");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
        File file = chooser.showOpenDialog(App.getMainStage());
        if (file == null) {
            return;
        }
        selectedAttachmentFile = file;
        noteFileLabel.setText(file.getName() + " (será salvo ao clicar em SALVAR/ATUALIZAR)");
    }

    @FXML
    private void onOpenNote() {
        Equipment selected = equipmentTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertUtil.warning("Seleção necessária", "Selecione um equipamento na tabela.");
            return;
        }
        if (FormUtil.orEmpty(selected.getNotePath()).isBlank()) {
            AlertUtil.warning("Sem anexo", "Este registro não tem nota anexada.");
            return;
        }
        BrowserUtil.openFile(new File(selected.getNotePath()));
    }

    @FXML
    private void onDeactivate() {
        Equipment selected = equipmentTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertUtil.warning("Seleção necessária", "Selecione um equipamento na tabela.");
            return;
        }
        if (!selected.isActive()) {
            AlertUtil.warning("Registro já inativo", "Este registro já está inativo.");
            return;
        }
        if (AlertUtil.confirm("Inativar registro",
                "Inativar o registro do equipamento " + selected.getModel()
                        + "? Ele continua no histórico, mas sai da lista de ativos.")) {
            equipmentService.deactivate(selected.getId());
            loadEquipment();
        }
    }

    @FXML
    private void onActivate() {
        Equipment selected = equipmentTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertUtil.warning("Seleção necessária", "Selecione um equipamento na tabela.");
            return;
        }
        if (selected.isActive()) {
            AlertUtil.warning("Registro já ativo", "Este registro já está ativo.");
            return;
        }
        equipmentService.activate(selected.getId());
        loadEquipment();
    }

    @FXML
    private void onBack() {
        App.backToHub();
    }
}
