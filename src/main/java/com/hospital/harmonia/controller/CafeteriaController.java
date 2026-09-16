package com.hospital.harmonia.controller;

import com.hospital.harmonia.App;
import com.hospital.harmonia.model.Employee;
import com.hospital.harmonia.model.Meal;
import com.hospital.harmonia.model.MealType;
import com.hospital.harmonia.service.DuplicateMealException;
import com.hospital.harmonia.service.EmployeeService;
import com.hospital.harmonia.service.MealPriceService;
import com.hospital.harmonia.service.MealService;
import com.hospital.harmonia.util.AlertUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import javafx.util.StringConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.math.BigDecimal;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;

public class CafeteriaController implements Initializable {

    private static final Logger log = LoggerFactory.getLogger(CafeteriaController.class);

    // --- Employees tab ---
    @FXML private TextField employeeNameField;
    @FXML private TextField employeeRegistrationField;
    @FXML private TextField employeeCategoryField;
    // Marks a matrícula as intentionally shared by more than one person (see
    // Employee.sharedUsage) -- e.g. a "plantão médico" badge. When checked,
    // the one-meal-per-type-per-day rule doesn't apply to this employee.
    @FXML private CheckBox employeeSharedUsageCheckBox;
    @FXML private Button saveEmployeeButton;
    @FXML private TableView<Employee> employeeTable;
    @FXML private TableColumn<Employee, String> nameColumn;
    @FXML private TableColumn<Employee, String> registrationColumn;
    @FXML private TableColumn<Employee, String> categoryColumn;
    @FXML private TableColumn<Employee, String> sharedUsageColumn;
    @FXML private TableColumn<Employee, String> activeColumn;
    @FXML private TextField employeeFilterNameField;
    @FXML private TextField employeeFilterCategoryField;
    @FXML private TextField employeeFilterRegistrationField;
    @FXML private ComboBox<String> employeeFilterStatusCombo;

    // --- Meal registration tab ---
    @FXML private ComboBox<Employee> employeeCombo;
    @FXML private DatePicker mealDatePicker;
    @FXML private TextField mealTimeField;
    @FXML private Button saveMealButton;
    @FXML private TableView<Meal> mealTable;
    @FXML private TableColumn<Meal, String> mealEmployeeColumn;
    @FXML private TableColumn<Meal, String> mealDateColumn;
    @FXML private TableColumn<Meal, String> mealTimeColumn;
    @FXML private TableColumn<Meal, String> mealTypeColumn;
    @FXML private TableColumn<Meal, BigDecimal> mealPriceColumn;
    @FXML private TableColumn<Meal, String> mealActiveColumn;
    @FXML private TextField mealFilterNameField;
    @FXML private ComboBox<MealType> mealFilterTypeCombo;
    @FXML private TextField mealFilterCategoryField;
    @FXML private ComboBox<String> mealFilterStatusCombo;

    // --- Reports tab ---
    @FXML private DatePicker reportStartDatePicker;
    @FXML private DatePicker reportEndDatePicker;
    @FXML private ComboBox<String> reportCategoryCombo;
    @FXML private ComboBox<MealType> reportTypeCombo;
    @FXML private ComboBox<Employee> reportEmployeeCombo;
    @FXML private Label reportStatusLabel;
    @FXML private TextField breakfastPriceField;
    @FXML private TextField lunchPriceField;
    @FXML private TextField dinnerPriceField;
    @FXML private TextField supperPriceField;
    @FXML private Label priceStatusLabel;

    private final EmployeeService employeeService = new EmployeeService();
    private final MealService mealService = new MealService();
    private final MealPriceService mealPriceService = new MealPriceService();

    private final ObservableList<Employee> employees = FXCollections.observableArrayList();
    private final ObservableList<Meal> meals = FXCollections.observableArrayList();

    // List of active employees used by the search combo in the "Meals" tab,
    // with a filter by typed text (see configureEmployeeCombo).
    private final ObservableList<Employee> activeEmployees = FXCollections.observableArrayList();
    private FilteredList<Employee> filteredActiveEmployees;

    // Wrap the tables' underlying lists so the on-screen filter fields
    // (Nome/Categoria/Matricula/Status for employees; Colaborador/Tipo/
    // Categoria/Status for meals) can narrow what's shown without touching
    // the underlying "employees"/"meals" lists themselves -- those still hold
    // everything, so loadEmployees()/loadMeals() keep working exactly as
    // before, and the FilteredList automatically re-filters whenever they change.
    private FilteredList<Employee> filteredEmployeesForTable;
    private FilteredList<Meal> filteredMealsForTable;

    // Date window used to populate the "Meals" tab TABLE. It's independent of
    // the DatePickers in the "Reports" tab -- previously the reload after
    // registering used reportStartDatePicker/today, which could either hide
    // the just-registered meal from the table (if its date fell outside the
    // report window) or throw an error if reportStartDatePicker was after today.
    private LocalDate registrationWindowStart;
    private LocalDate registrationWindowEnd;

    // Id of the employee being edited (double-click on a table row), or null
    // when the form is in normal registration mode (new record). The original
    // "active" flag is stored separately because the form has no field for it
    // -- it must be preserved on update, otherwise an inactive employee would
    // become active again (or vice-versa) without the user asking for it.
    private Integer editingEmployeeId;
    private boolean editingEmployeeActive;
    // "Job title" no longer has a field on the form (removed by the user), but
    // the column still exists in the database -- keep the original value so it
    // isn't wiped out (set to NULL) every time an existing employee is updated.
    private String editingEmployeeJobTitle;

    // Same idea for editing a meal. editingMealOriginalEmployeeId is used as a
    // fallback when the meal's employee is inactive (doesn't appear in the
    // combo, which only lists active ones) and the user hasn't typed another name.
    private Integer editingMealId;
    private Integer editingMealOriginalEmployeeId;
    private boolean editingMealActive;
    // Shared-usage flag of editingMealOriginalEmployeeId's employee (see
    // Employee.sharedUsage/Meal.employeeSharedUsage), needed for the same
    // fallback case: the combo has no Employee object to read it from when
    // editing a meal of an inactive employee, so it's captured from the
    // meal itself (already denormalized via the query's JOIN) when editing starts.
    private boolean editingMealOriginalSharedUsage;

    // Fixed date format (day/month/year) used in the DatePickers on screen --
    // independent of the machine's locale (see configureDatePickers).
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        configureEmployeeTable();
        configureEmployeeFilters();
        configureMealTable();
        configureMealFilters();
        configureEmployeeCombo();
        configureReportFilters();
        configureDatePickers();
        configureTimeField();

        loadEmployees();
        // By default, show the current week in the registration/report views
        LocalDate today = LocalDate.now();
        mealDatePicker.setValue(today);
        reportStartDatePicker.setValue(today.minusDays(7));
        reportEndDatePicker.setValue(today);

        registrationWindowStart = today.minusDays(30);
        registrationWindowEnd = today;
        loadMeals(registrationWindowStart, registrationWindowEnd);

        loadPrices();
    }

    /**
     * By default the DatePicker displays/parses the date in the MACHINE'S
     * LOCALE format (e.g. "7/27/2026" on a machine set to English/US) --
     * explicitly forces the Brazilian day/month/year format on every date
     * picker on screen (Meals and Reports), regardless of the system's locale.
     */
    private void configureDatePickers() {
        configureDateFormat(mealDatePicker);
        configureDateFormat(reportStartDatePicker);
        configureDateFormat(reportEndDatePicker);
    }

    private static void configureDateFormat(DatePicker datePicker) {
        datePicker.setPromptText("dd/mm/aaaa");
        datePicker.setConverter(new StringConverter<LocalDate>() {
            @Override
            public String toString(LocalDate date) {
                return date == null ? "" : DATE_FORMAT.format(date);
            }

            @Override
            public LocalDate fromString(String text) {
                if (text == null || text.isBlank()) {
                    return null;
                }
                return LocalDate.parse(text.trim(), DATE_FORMAT);
            }
        });
    }

    // Internal-only guard to prevent the field's own reformatting
    // (configureTimeField) from re-entering itself when it calls setText().
    private boolean updatingTimeField;

    /**
     * Speeds up typing the meal time: the user can type just the 4 digits in
     * sequence (e.g. "1230") and the ":" is inserted on its own in the middle
     * -> "12:30". Any character that isn't a digit is ignored (including a
     * ":" the user might try to type), and the total is capped at 4 digits.
     */
    private void configureTimeField() {
        mealTimeField.textProperty().addListener((obs, oldText, newText) -> {
            if (updatingTimeField) {
                return;
            }
            String formatted = formatTypedTime(newText);
            if (!formatted.equals(newText)) {
                updatingTimeField = true;
                mealTimeField.setText(formatted);
                mealTimeField.positionCaret(formatted.length());
                updatingTimeField = false;
            }
        });
    }

    private static String formatTypedTime(String text) {
        if (text == null) {
            return "";
        }
        String digits = text.replaceAll("\\D", "");
        if (digits.length() > 4) {
            digits = digits.substring(0, 4);
        }
        if (digits.length() <= 2) {
            return digits;
        }
        return digits.substring(0, 2) + ":" + digits.substring(2);
    }

    // ================= EMPLOYEES =================

    private void configureEmployeeTable() {
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        registrationColumn.setCellValueFactory(new PropertyValueFactory<>("registrationNumber"));
        categoryColumn.setCellValueFactory(new PropertyValueFactory<>("category"));
        sharedUsageColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                data.getValue().isSharedUsage() ? "SIM" : "NÃO"));
        activeColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                data.getValue().isActive() ? "SIM" : "NÃO"));
        filteredEmployeesForTable = new FilteredList<>(employees, e -> true);
        employeeTable.setItems(filteredEmployeesForTable);

        // Double-clicking a row loads the employee into the form on the left
        // for editing (see loadEmployeeForEditing/onSaveEmployee).
        employeeTable.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                Employee selected = employeeTable.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    loadEmployeeForEditing(selected);
                }
            }
        });

        // Highlights the whole row of inactive employees in light red, so it's
        // visually obvious in the table without needing to open/edit the record.
        employeeTable.setRowFactory(table -> new TableRow<Employee>() {
            @Override
            protected void updateItem(Employee employee, boolean empty) {
                super.updateItem(employee, empty);
                if (empty || employee == null) {
                    setStyle("");
                } else if (!employee.isActive()) {
                    setStyle("-fx-background-color: #f8d7da;");
                } else {
                    setStyle("");
                }
            }
        });
    }

    private void loadEmployees() {
        employees.setAll(employeeService.findAll());
        // Updates the base list that feeds the search combo -- since it's the
        // same instance the combo's FilteredList is observing, the filtered
        // list (and what shows up in the dropdown) updates itself.
        activeEmployees.setAll(employeeService.findActive());
        refreshReportFilterOptions();
    }

    /**
     * Wires the 4 filter fields above the Employees table (Nome/Categoria/
     * Matricula/Status). Any combination can be active at once (AND) -- e.g.
     * typing a name AND choosing "Ativos" narrows to active employees whose
     * name matches. Re-applied on every keystroke/selection change.
     */
    private void configureEmployeeFilters() {
        employeeFilterStatusCombo.setItems(FXCollections.observableArrayList("Todos", "Ativos", "Inativos"));
        employeeFilterStatusCombo.setValue("Todos");

        employeeFilterNameField.textProperty().addListener((obs, o, n) -> applyEmployeeFilter());
        employeeFilterCategoryField.textProperty().addListener((obs, o, n) -> applyEmployeeFilter());
        employeeFilterRegistrationField.textProperty().addListener((obs, o, n) -> applyEmployeeFilter());
        employeeFilterStatusCombo.valueProperty().addListener((obs, o, n) -> applyEmployeeFilter());
    }

    private void applyEmployeeFilter() {
        String name = normalize(employeeFilterNameField.getText());
        String category = normalize(employeeFilterCategoryField.getText());
        String registration = orEmpty(employeeFilterRegistrationField.getText()).trim();
        String status = employeeFilterStatusCombo.getValue();

        filteredEmployeesForTable.setPredicate(e ->
                (name.isEmpty() || normalize(e.getName()).contains(name))
                        && (category.isEmpty() || normalize(orEmpty(e.getCategory())).contains(category))
                        && (registration.isEmpty() || orEmpty(e.getRegistrationNumber()).contains(registration))
                        && ("Todos".equals(status)
                                || ("Ativos".equals(status) && e.isActive())
                                || ("Inativos".equals(status) && !e.isActive())));
    }

    private static String orEmpty(String text) {
        return text == null ? "" : text;
    }

    /**
     * Makes the employee combo (in the "Meals" tab) typeable: the user can
     * click and pick from the list OR type part of the name to filter, which
     * helps a lot when there are many employees registered. The filter
     * ignores accents and upper/lowercase.
     */
    private void configureEmployeeCombo() {
        filteredActiveEmployees = new FilteredList<>(activeEmployees, c -> true);

        employeeCombo.setEditable(true);
        employeeCombo.setItems(filteredActiveEmployees);
        employeeCombo.setConverter(new StringConverter<Employee>() {
            @Override
            public String toString(Employee e) {
                return e == null ? "" : e.getName();
            }

            @Override
            public Employee fromString(String text) {
                if (text == null || text.isBlank()) {
                    return null;
                }
                return filteredActiveEmployees.stream()
                        .filter(e -> e.getName().equalsIgnoreCase(text.trim()))
                        .findFirst()
                        .orElse(null);
            }
        });

        employeeCombo.getEditor().textProperty().addListener((obs, oldText, newText) -> {
            // When the combo itself updates the editor's text upon SELECTING an
            // item (click or Enter), that text already matches the selected
            // item's name -- in that case don't refilter, otherwise the list
            // would shrink to just that item right after it's chosen.
            Employee selected = employeeCombo.getSelectionModel().getSelectedItem();
            if (selected != null && selected.getName().equals(newText)) {
                return;
            }
            String filter = normalize(newText);
            filteredActiveEmployees.setPredicate(e -> filter.isEmpty() || normalize(e.getName()).contains(filter));
            if (!employeeCombo.isShowing() && employeeCombo.isFocused()) {
                employeeCombo.show();
            }
        });
    }

    /** Strips accents and lowercases, so the combo's search is more tolerant. */
    private static String normalize(String text) {
        if (text == null) {
            return "";
        }
        String withoutAccents = Normalizer.normalize(text.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return withoutAccents.toLowerCase();
    }

    /**
     * Registration number always with 8 digits -- pads with leading zeros
     * when the number given (form or .txt file) comes shorter than that.
     * If it already comes with 8 or more, keeps it as-is (doesn't truncate).
     */
    private static String normalizeRegistrationNumber(String registrationNumber) {
        if (registrationNumber == null) {
            return null;
        }
        String text = registrationNumber.trim();
        if (text.length() < 8) {
            text = "0".repeat(8 - text.length()) + text;
        }
        return text;
    }

    /**
     * Name always in "Title Case" (each word with its first letter
     * capitalized): e.g. "MARIA clara oliveira" -> "Maria Clara Oliveira".
     * Only touches casing, doesn't strip accents nor treat prepositions
     * (de/da/do) as an exception.
     */
    private static String toTitleCase(String text) {
        if (text == null) {
            return null;
        }
        String[] words = text.trim().toLowerCase().split("\\s+");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) {
                continue;
            }
            if (result.length() > 0) {
                result.append(' ');
            }
            result.append(Character.toUpperCase(word.charAt(0)));
            if (word.length() > 1) {
                result.append(word.substring(1));
            }
        }
        return result.toString();
    }

    /** Category/company always uppercase: e.g. "alunos" -> "ALUNOS". */
    private static String toUpperCaseText(String text) {
        return text == null ? null : text.trim().toUpperCase();
    }

    /**
     * Fills the form with the selected employee's data and enters editing mode
     * (the SALVAR button becomes ATUALIZAR). LIMPAR cancels the edit and
     * returns to normal registration mode.
     */
    private void loadEmployeeForEditing(Employee e) {
        employeeNameField.setText(e.getName());
        employeeRegistrationField.setText(e.getRegistrationNumber());
        employeeCategoryField.setText(e.getCategory());
        employeeSharedUsageCheckBox.setSelected(e.isSharedUsage());
        editingEmployeeId = e.getId();
        editingEmployeeActive = e.isActive();
        editingEmployeeJobTitle = e.getJobTitle();
        saveEmployeeButton.setText("ATUALIZAR");
    }

    @FXML
    private void onSaveEmployee() {
        try {
            Employee e = new Employee();
            e.setName(toTitleCase(employeeNameField.getText()));
            e.setRegistrationNumber(normalizeRegistrationNumber(employeeRegistrationField.getText()));
            e.setCategory(toUpperCaseText(employeeCategoryField.getText()));
            e.setSharedUsage(employeeSharedUsageCheckBox.isSelected());
            if (editingEmployeeId != null) {
                // Updates the existing record -- preserves the original Active
                // and Job title, since the form no longer has a field for
                // either one (deactivate/activate still have their own
                // buttons; Job title was removed from the screen but the
                // column still exists in the database).
                e.setId(editingEmployeeId);
                e.setActive(editingEmployeeActive);
                e.setJobTitle(editingEmployeeJobTitle);
                employeeService.update(e);
            } else {
                employeeService.register(e);
            }
            loadEmployees();
            onClearEmployeeForm();
        } catch (Exception e) {
            log.warn("Failed to save employee: {}", e.getMessage(), e);
            AlertUtil.error("Erro ao salvar colaborador", e.getMessage());
        }
    }

    @FXML
    private void onClearEmployeeForm() {
        employeeNameField.clear();
        employeeRegistrationField.clear();
        employeeCategoryField.clear();
        employeeSharedUsageCheckBox.setSelected(false);
        editingEmployeeId = null;
        editingEmployeeJobTitle = null;
        saveEmployeeButton.setText("SALVAR");
    }

    @FXML
    private void onDeactivateEmployee() {
        Employee selected = employeeTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertUtil.warning("Seleção necessária", "Selecione um colaborador na tabela.");
            return;
        }
        if (AlertUtil.confirm("Inativar colaborador", "Inativar " + selected.getName() + "?")) {
            employeeService.deactivate(selected.getId());
            loadEmployees();
        }
    }

    @FXML
    private void onActivateEmployee() {
        Employee selected = employeeTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertUtil.warning("Seleção necessária", "Selecione um colaborador na tabela.");
            return;
        }
        if (selected.isActive()) {
            AlertUtil.warning("Colaborador já ativo", selected.getName() + " já está ativo.");
            return;
        }
        employeeService.activate(selected.getId());
        loadEmployees();
    }

    // ================= MEAL REGISTRATION =================

    private void configureMealTable() {
        mealEmployeeColumn.setCellValueFactory(new PropertyValueFactory<>("employeeName"));
        mealDateColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                data.getValue().getDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))));
        mealTimeColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                data.getValue().getTime().format(DateTimeFormatter.ofPattern("HH:mm"))));
        mealTypeColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                data.getValue().getType().getDescription()));
        mealPriceColumn.setCellValueFactory(new PropertyValueFactory<>("price"));
        mealActiveColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                data.getValue().isActive() ? "SIM" : "NÃO"));
        filteredMealsForTable = new FilteredList<>(meals, m -> true);
        mealTable.setItems(filteredMealsForTable);

        // Double-clicking a row loads the meal into the form on the left for
        // editing (see loadMealForEditing/onSaveMeal).
        mealTable.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                Meal selected = mealTable.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    loadMealForEditing(selected);
                }
            }
        });

        // Same logic as the employee table: whole row in light red for a
        // deactivated (canceled) meal, but it stays visible in the table --
        // it's the full log/history, active and inactive.
        mealTable.setRowFactory(table -> new TableRow<Meal>() {
            @Override
            protected void updateItem(Meal meal, boolean empty) {
                super.updateItem(meal, empty);
                if (empty || meal == null) {
                    setStyle("");
                } else if (!meal.isActive()) {
                    setStyle("-fx-background-color: #f8d7da;");
                } else {
                    setStyle("");
                }
            }
        });
    }

    private void loadMeals(LocalDate start, LocalDate end) {
        meals.setAll(mealService.findByPeriod(start, end));
    }

    /**
     * Wires the 4 filter fields above the Meals table (Colaborador/Tipo/
     * Categoria/Status), same AND-combination behavior as the Employees
     * table's filters (see configureEmployeeFilters).
     */
    private void configureMealFilters() {
        configureMealTypeCombo(mealFilterTypeCombo);

        mealFilterStatusCombo.setItems(FXCollections.observableArrayList("Todas", "Ativas", "Inativas"));
        mealFilterStatusCombo.setValue("Todas");

        mealFilterNameField.textProperty().addListener((obs, o, n) -> applyMealFilter());
        mealFilterTypeCombo.valueProperty().addListener((obs, o, n) -> applyMealFilter());
        mealFilterCategoryField.textProperty().addListener((obs, o, n) -> applyMealFilter());
        mealFilterStatusCombo.valueProperty().addListener((obs, o, n) -> applyMealFilter());
    }

    private void applyMealFilter() {
        String name = normalize(mealFilterNameField.getText());
        String category = normalize(mealFilterCategoryField.getText());
        MealType type = mealFilterTypeCombo.getValue();
        String status = mealFilterStatusCombo.getValue();

        filteredMealsForTable.setPredicate(m ->
                (name.isEmpty() || normalize(m.getEmployeeName()).contains(name))
                        && (category.isEmpty() || normalize(orEmpty(m.getEmployeeCategory())).contains(category))
                        && (type == null || type == m.getType())
                        && ("Todas".equals(status)
                                || ("Ativas".equals(status) && m.isActive())
                                || ("Inativas".equals(status) && !m.isActive())));
    }

    /**
     * Configures a ComboBox<MealType> so its first entry (represented by
     * null) reads as "Todos" -- used both by the Meals table's type filter
     * and by the report's type filter (configureReportFilters).
     */
    private static void configureMealTypeCombo(ComboBox<MealType> combo) {
        combo.setConverter(new StringConverter<MealType>() {
            @Override
            public String toString(MealType type) {
                return type == null ? "Todos" : type.getDescription();
            }

            @Override
            public MealType fromString(String text) {
                return null; // not editable -- selection only
            }
        });
        combo.setCellFactory(list -> new ListCell<MealType>() {
            @Override
            protected void updateItem(MealType item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : (item == null ? "Todos" : item.getDescription()));
            }
        });
        ObservableList<MealType> items = FXCollections.observableArrayList();
        items.add(null);
        items.addAll(MealType.values());
        combo.setItems(items);
        combo.setValue(null);
    }

    /**
     * Fills the form with the selected meal's data and enters editing mode
     * (the REGISTRAR button becomes ATUALIZAR). If the meal's employee is
     * inactive, they don't appear in the combo (which only lists active ones)
     * -- in that case only the name text is shown, and the original id is
     * reused when saving as long as the user doesn't type another valid name.
     */
    private void loadMealForEditing(Meal m) {
        Employee employeeInList = activeEmployees.stream()
                .filter(e -> e.getId().equals(m.getEmployeeId()))
                .findFirst()
                .orElse(null);
        employeeCombo.setValue(employeeInList);
        employeeCombo.getEditor().setText(m.getEmployeeName());
        mealDatePicker.setValue(m.getDate());
        mealTimeField.setText(m.getTime().format(DateTimeFormatter.ofPattern("HH:mm")));

        editingMealId = m.getId();
        editingMealOriginalEmployeeId = m.getEmployeeId();
        editingMealActive = m.isActive();
        editingMealOriginalSharedUsage = m.isEmployeeSharedUsage();
        saveMealButton.setText("ATUALIZAR");
    }

    /** Returns the meal form to normal registration mode (cancels the edit). */
    private void clearMealForm() {
        mealTimeField.clear();
        employeeCombo.setValue(null);
        employeeCombo.getEditor().clear();
        editingMealId = null;
        editingMealOriginalEmployeeId = null;
        editingMealOriginalSharedUsage = false;
        saveMealButton.setText("SALVAR");
    }

    /**
     * Type and Price are no longer typed by hand: they're calculated
     * automatically from the given time (see MealType.byTime/getDefaultPrice).
     * The prices used today are PLACEHOLDERS -- adjust in MealType once the
     * official values are defined.
     */
    @FXML
    private void onSaveMeal() {
        try {
            Employee employee = employeeCombo.getValue();
            String typedText = employeeCombo.getEditor().getText();
            // With the combo being typeable, the user can type the name
            // correctly and click REGISTRAR directly without formally
            // choosing the item in the list (no click/Enter) -- in that case
            // getValue() would still be stale (null or from a previous
            // selection). Check whether the typed text matches the selected
            // value and, if not, try to resolve it from the text.
            if (employee == null
                    || typedText == null
                    || !employee.getName().equalsIgnoreCase(typedText.trim())) {
                employee = employeeCombo.getConverter().fromString(typedText);
            }

            Integer employeeId;
            boolean employeeSharedUsage;
            if (employee != null) {
                employeeId = employee.getId();
                employeeSharedUsage = employee.isSharedUsage();
            } else if (editingMealId != null && editingMealOriginalEmployeeId != null) {
                // Editing a meal of an inactive employee (doesn't appear in the
                // combo) -- keeps the original employee, since the user didn't choose another.
                // No Employee object to read the shared-usage flag from here,
                // so it's taken from editingMealOriginalSharedUsage instead
                // (captured from the meal itself when editing started -- see loadMealForEditing).
                employeeId = editingMealOriginalEmployeeId;
                employeeSharedUsage = editingMealOriginalSharedUsage;
            } else {
                AlertUtil.warning("Campo obrigatório", "Selecione um colaborador da lista.");
                return;
            }

            LocalTime time = LocalTime.parse(mealTimeField.getText(), DateTimeFormatter.ofPattern("H:mm"));
            MealType type = MealType.byTime(time);

            Meal m = new Meal();
            m.setEmployeeId(employeeId);
            m.setEmployeeSharedUsage(employeeSharedUsage);
            m.setDate(mealDatePicker.getValue());
            m.setTime(time);
            m.setType(type);
            // Price configurable from the screen (Reports tab > "Valores das
            // refeicoes"), no longer MealType's fixed placeholder.
            m.setPrice(mealPriceService.getPrice(type));

            if (editingMealId != null) {
                // Updates the existing record -- preserves the original Active
                // flag (the form doesn't touch it; deactivate/activate have
                // their own button).
                m.setId(editingMealId);
                m.setActive(editingMealActive);
                mealService.update(m);
            } else {
                mealService.register(m);
            }

            // Ensures the just-registered/updated meal always shows up in the
            // table, even if its date falls outside the current window (e.g.
            // a retroactive/future registration), by expanding the window
            // instead of replacing it.
            if (m.getDate().isBefore(registrationWindowStart)) {
                registrationWindowStart = m.getDate();
            }
            if (m.getDate().isAfter(registrationWindowEnd)) {
                registrationWindowEnd = m.getDate();
            }
            loadMeals(registrationWindowStart, registrationWindowEnd);

            clearMealForm();
        } catch (java.time.format.DateTimeParseException e) {
            log.debug("Invalid meal time typed: {}", mealTimeField.getText());
            AlertUtil.error("Horário inválido", "Use o formato HH:mm, ex: 12:30.");
        } catch (Exception e) {
            log.warn("Failed to register/update meal: {}", e.getMessage(), e);
            AlertUtil.error("Erro ao registrar refeição", e.getMessage());
        }
    }

    @FXML
    private void onDeactivateMeal() {
        Meal selected = mealTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertUtil.warning("Seleção necessária", "Selecione uma refeição na tabela.");
            return;
        }
        if (!selected.isActive()) {
            AlertUtil.warning("Refeição já inativa", "Este registro já está inativo.");
            return;
        }
        if (AlertUtil.confirm("Inativar refeição", "Inativar este registro de refeição?"
                + " Ele continua no histórico, mas sai do relatório e do total.")) {
            mealService.deactivate(selected.getId());
            loadMeals(registrationWindowStart, registrationWindowEnd);
        }
    }

    @FXML
    private void onActivateMeal() {
        Meal selected = mealTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertUtil.warning("Seleção necessária", "Selecione uma refeição na tabela.");
            return;
        }
        if (selected.isActive()) {
            AlertUtil.warning("Refeição já ativa", "Este registro já está ativo.");
            return;
        }
        mealService.activate(selected.getId());
        loadMeals(registrationWindowStart, registrationWindowEnd);
    }

    // ================= REPORT =================

    /**
     * Configures the report's 3 optional filters (Categoria/Tipo/
     * Colaborador), applied on top of the mandatory date range. Their
     * options (categories and employees) are refreshed whenever the
     * employee list reloads -- see refreshReportFilterOptions, called from
     * loadEmployees().
     */
    private void configureReportFilters() {
        configureMealTypeCombo(reportTypeCombo);

        reportCategoryCombo.setConverter(new StringConverter<String>() {
            @Override
            public String toString(String category) {
                return category == null ? "Todas" : category;
            }

            @Override
            public String fromString(String text) {
                return text;
            }
        });
        reportCategoryCombo.setCellFactory(list -> new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : (item == null ? "Todas" : item));
            }
        });

        reportEmployeeCombo.setConverter(new StringConverter<Employee>() {
            @Override
            public String toString(Employee employee) {
                return employee == null ? "Todos" : employee.getName();
            }

            @Override
            public Employee fromString(String text) {
                return null; // not editable -- selection only
            }
        });
        reportEmployeeCombo.setCellFactory(list -> new ListCell<Employee>() {
            @Override
            protected void updateItem(Employee item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : (item == null ? "Todos" : item.getName()));
            }
        });
    }

    /**
     * Rebuilds the Categoria/Colaborador report filter options from the
     * current employee list (categories: distinct, non-blank, sorted).
     * Keeps the user's current selection if it's still a valid option after
     * the refresh, otherwise falls back to "Todas"/"Todos".
     */
    private void refreshReportFilterOptions() {
        List<String> categories = employees.stream()
                .map(Employee::getCategory)
                .filter(c -> c != null && !c.isBlank())
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(java.util.stream.Collectors.toList());
        ObservableList<String> categoryItems = FXCollections.observableArrayList();
        categoryItems.add(null);
        categoryItems.addAll(categories);
        String previousCategory = reportCategoryCombo.getValue();
        reportCategoryCombo.setItems(categoryItems);
        reportCategoryCombo.setValue(categoryItems.contains(previousCategory) ? previousCategory : null);

        ObservableList<Employee> employeeItems = FXCollections.observableArrayList();
        employeeItems.add(null);
        employeeItems.addAll(employees);
        Employee previousEmployee = reportEmployeeCombo.getValue();
        reportEmployeeCombo.setItems(employeeItems);
        reportEmployeeCombo.setValue(employeeItems.contains(previousEmployee) ? previousEmployee : null);
    }

    @FXML
    private void onGenerateReport() {
        LocalDate start = reportStartDatePicker.getValue();
        LocalDate end = reportEndDatePicker.getValue();
        if (start == null || end == null) {
            AlertUtil.warning("Período inválido", "Informe as duas datas do período.");
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Salvar relatório como...");
        chooser.setInitialFileName("relatorio_refeitorio_" + start + "_a_" + end + ".pdf");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
        File destination = chooser.showSaveDialog(App.getMainStage());
        if (destination == null) {
            return;
        }

        try {
            mealService.generatePeriodReport(start, end, reportCategoryCombo.getValue(), reportTypeCombo.getValue(),
                    reportEmployeeCombo.getValue(), destination);
            reportStatusLabel.setText("Relatório gerado com sucesso em: " + destination.getAbsolutePath());
        } catch (Exception e) {
            log.error("Failed to generate cafeteria report for period {} - {}", start, end, e);
            reportStatusLabel.setText("Falha ao gerar relatório.");
            AlertUtil.error("Erro ao gerar relatório", e.getMessage());
        }
    }

    /** Same filters/period as onGenerateReport, but exported as a plain .txt file instead of a PDF. */
    @FXML
    private void onGenerateReportTxt() {
        LocalDate start = reportStartDatePicker.getValue();
        LocalDate end = reportEndDatePicker.getValue();
        if (start == null || end == null) {
            AlertUtil.warning("Período inválido", "Informe as duas datas do período.");
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Salvar relatório como...");
        chooser.setInitialFileName("relatorio_refeitorio_" + start + "_a_" + end + ".txt");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Texto (*.txt)", "*.txt"));
        File destination = chooser.showSaveDialog(App.getMainStage());
        if (destination == null) {
            return;
        }

        try {
            mealService.generatePeriodReportTxt(start, end, reportCategoryCombo.getValue(),
                    reportTypeCombo.getValue(), reportEmployeeCombo.getValue(), destination);
            reportStatusLabel.setText("Relatório (.txt) gerado com sucesso em: " + destination.getAbsolutePath());
        } catch (Exception e) {
            log.error("Failed to generate cafeteria TXT report for period {} - {}", start, end, e);
            reportStatusLabel.setText("Falha ao gerar relatório.");
            AlertUtil.error("Erro ao gerar relatório", e.getMessage());
        }
    }

    /**
     * Same filters/period as onGenerateReport, but exported as a calendar-style
     * .xlsx spreadsheet (one mini-table per day, with quantity per meal type)
     * instead of a PDF.
     */
    @FXML
    private void onGenerateReportXlsx() {
        LocalDate start = reportStartDatePicker.getValue();
        LocalDate end = reportEndDatePicker.getValue();
        if (start == null || end == null) {
            AlertUtil.warning("Período inválido", "Informe as duas datas do período.");
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Salvar relatório como...");
        chooser.setInitialFileName("relatorio_refeitorio_" + start + "_a_" + end + ".xlsx");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel (*.xlsx)", "*.xlsx"));
        File destination = chooser.showSaveDialog(App.getMainStage());
        if (destination == null) {
            return;
        }

        try {
            mealService.generatePeriodReportXlsx(start, end, reportCategoryCombo.getValue(),
                    reportTypeCombo.getValue(), reportEmployeeCombo.getValue(), destination);
            reportStatusLabel.setText("Relatório (.xlsx) gerado com sucesso em: " + destination.getAbsolutePath());
        } catch (Exception e) {
            log.error("Failed to generate cafeteria XLSX report for period {} - {}", start, end, e);
            reportStatusLabel.setText("Falha ao gerar relatório.");
            AlertUtil.error("Erro ao gerar relatório", e.getMessage());
        }
    }

    // ================= MEAL PRICES =================

    /** Fills the price fields with the current values (configured or default). */
    private void loadPrices() {
        Map<MealType, BigDecimal> prices = mealPriceService.listPrices();
        breakfastPriceField.setText(formatPrice(prices.get(MealType.CAFE_DA_MANHA)));
        lunchPriceField.setText(formatPrice(prices.get(MealType.ALMOCO)));
        dinnerPriceField.setText(formatPrice(prices.get(MealType.JANTAR)));
        supperPriceField.setText(formatPrice(prices.get(MealType.CEIA)));
    }

    private static String formatPrice(BigDecimal price) {
        return price.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }

    /**
     * Saves the 4 entered values (Breakfast, Lunch, Dinner and Supper -- Snack
     * doesn't show up on screen because it's no longer assigned automatically,
     * see MealType.byTime). Takes effect for new registrations and imports
     * from now on; meals already registered keep the price recorded at the
     * time of registration (not recalculated retroactively).
     */
    @FXML
    private void onSavePrices() {
        try {
            Map<MealType, BigDecimal> prices = new EnumMap<>(MealType.class);
            prices.put(MealType.CAFE_DA_MANHA, readPrice(breakfastPriceField, "Dejejum"));
            prices.put(MealType.ALMOCO, readPrice(lunchPriceField, "Almoço"));
            prices.put(MealType.JANTAR, readPrice(dinnerPriceField, "Jantar"));
            prices.put(MealType.CEIA, readPrice(supperPriceField, "Ceia"));

            mealPriceService.savePrices(prices);
            loadPrices();
            priceStatusLabel.setText("Valores atualizados com sucesso.");
        } catch (Exception e) {
            log.warn("Failed to save meal prices: {}", e.getMessage(), e);
            priceStatusLabel.setText("Falha ao salvar valores.");
            AlertUtil.error("Erro ao salvar valores", e.getMessage());
        }
    }

    /** Accepts either a period OR a comma as the decimal separator (e.g. "18,50" or "18.50"). */
    private static BigDecimal readPrice(TextField field, String typeName) {
        String text = field.getText();
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Informe o preço de " + typeName + ".");
        }
        try {
            BigDecimal price = new BigDecimal(text.trim().replace(",", "."));
            if (price.signum() < 0) {
                throw new IllegalArgumentException("Preço de " + typeName + " não pode ser negativo.");
            }
            return price;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Preço de " + typeName + " inválido: \"" + text + "\".");
        }
    }

    // ================= BULK IMPORT (.TXT) =================

    /** "!" icon next to the import-employees button -- clicking shows the expected format. */
    @FXML
    private void onShowEmployeeFileFormat() {
        AlertUtil.info("Formato do arquivo de colaboradores",
                "Uma linha por colaborador, nessa ordem:\n\n"
                        + "Matrícula | Nome do Colaborador | Categoria\n\n"
                        + "Exemplo:\n348 Maria Clara Oliveira Alunos\n\n"
                        + "Matrículas já cadastradas anteriormente são ignoradas.");
    }

    /** "!" icon next to the import-meals button -- clicking shows the expected format. */
    @FXML
    private void onShowMealFileFormat() {
        AlertUtil.info("Formato do arquivo de refeições",
                "Uma linha por refeição, nessa ordem:\n\n"
                        + "Código da Catraca | Data | Horário | Matrícula | Código Refeição\n\n"
                        + "Exemplo:\nSAICN 16/02/2026 05:29 01002522 000003");
    }

    /**
     * Expected format of each line: "Matrícula Nome do Colaborador Categoria"
     * (e.g. " 348 Maria Clara Oliveira Alunos "). The first token is the
     * registration number, the last is the category (becomes the employee's
     * "category") and everything in between is the name. An already
     * registered registration number is ignored (doesn't update the existing
     * one) and enters the final summary as "already existed".
     */
    @FXML
    private void onImportEmployees() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Selecionar arquivo de colaboradores (.txt)");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Arquivo de texto (*.txt)", "*.txt"));
        File file = chooser.showOpenDialog(App.getMainStage());
        if (file == null) {
            return;
        }

        int imported = 0;
        int duplicates = 0;
        int errorCount = 0;
        List<String> problems = new ArrayList<>();

        try {
            List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i).trim();
                if (line.isEmpty()) {
                    continue;
                }
                int lineNumber = i + 1;
                String[] tokens = line.split("\\s+");
                if (tokens.length < 3) {
                    errorCount++;
                    problems.add("Linha " + lineNumber + ": formato inválido (esperado: Matrícula Nome Categoria).");
                    continue;
                }
                try {
                    String registrationNumber = normalizeRegistrationNumber(tokens[0]);
                    String category = tokens[tokens.length - 1];
                    StringBuilder name = new StringBuilder();
                    for (int t = 1; t < tokens.length - 1; t++) {
                        if (name.length() > 0) {
                            name.append(" ");
                        }
                        name.append(tokens[t]);
                    }

                    if (employeeService.findByRegistrationNumber(registrationNumber).isPresent()) {
                        duplicates++;
                        problems.add("Linha " + lineNumber + ": matrícula " + registrationNumber + " já cadastrada -- ignorada.");
                        continue;
                    }

                    Employee e = new Employee();
                    e.setName(toTitleCase(name.toString()));
                    e.setRegistrationNumber(registrationNumber);
                    e.setCategory(toUpperCaseText(category));
                    employeeService.register(e);
                    imported++;
                } catch (Exception e) {
                    errorCount++;
                    problems.add("Linha " + lineNumber + ": " + e.getMessage());
                    log.debug("Employee import: line {} skipped: {}", lineNumber, e.getMessage());
                }
            }
        } catch (IOException e) {
            log.error("Failed to read employee import file {}", file.getAbsolutePath(), e);
            AlertUtil.error("Erro ao ler arquivo", e.getMessage());
            return;
        }

        log.info("Employee import finished: {} imported, {} duplicate(s), {} error(s) (file={})",
                imported, duplicates, errorCount, file.getAbsolutePath());
        loadEmployees();
        showImportSummary("Importação de colaboradores", imported, duplicates, errorCount, problems);
    }

    /**
     * Expected format of each line: "Código da Catraca | Data | Horário | Matrícula | Permissão RH"
     * (e.g. "SAICN 16/02/2026 05:29 01002522 000003"). Turnstile code (token
     * 0) and RH permission (token 4) are discarded -- only Date (dd/MM/yyyy),
     * Time (HH:mm) and registration number matter. The meal's type and price
     * are calculated automatically from the time, same as manual registration.
     */
    @FXML
    private void onImportMeals() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Selecionar arquivo de refeições (.txt)");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Arquivo de texto (*.txt)", "*.txt"));
        File file = chooser.showOpenDialog(App.getMainStage());
        if (file == null) {
            return;
        }

        int imported = 0;
        int errorCount = 0;
        List<String> problems = new ArrayList<>();
        // Registration numbers that show up in the file but have no matching
        // employee yet -- tracked separately from "problems" above (instead
        // of mixed in as one line-error among many) and deduplicated with an
        // occurrence count. With a large import (thousands of turnstile
        // records) the same unregistered person can clock in dozens of
        // times, so without this a real bulk import would drown the summary
        // in near-identical line errors instead of a short, actionable list
        // of the actual people who still need to be registered -- see
        // showMissingRegistrationsIfAny, called at the end of this method.
        Map<String, Integer> missingRegistrations = new LinkedHashMap<>();
        // Same idea as missingRegistrations, but for lines rejected by the
        // one-meal-per-type-per-day rule (MealService.checkNoDuplicateMealType)
        // instead of an unregistered matrícula -- see showDuplicateMealsIfAny,
        // called at the end of this method.
        Map<String, Integer> duplicateMeals = new LinkedHashMap<>();
        LocalDate earliestDate = null;
        LocalDate latestDate = null;
        // Fetches the prices once before the loop (instead of one query per
        // line) -- the price table is small and doesn't change mid-import.
        Map<MealType, BigDecimal> prices = mealPriceService.listPrices();

        try {
            List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i).trim();
                if (line.isEmpty()) {
                    continue;
                }
                int lineNumber = i + 1;
                String[] tokens = line.split("\\s+");
                if (tokens.length < 4) {
                    errorCount++;
                    problems.add("Linha " + lineNumber
                            + ": formato inválido (esperado: Catraca Data Horário Matrícula Permissão).");
                    continue;
                }
                // Hoisted out of the try block (instead of declared inline)
                // so the DuplicateMealException catch below can build a
                // readable key from them -- a variable declared inside a
                // try block isn't visible in its own catch clauses.
                String registrationNumber = null;
                Employee employee = null;
                MealType type = null;
                LocalDate date = null;
                try {
                    date = LocalDate.parse(tokens[1], DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                    LocalTime time = LocalTime.parse(tokens[2], DateTimeFormatter.ofPattern("HH:mm"));
                    registrationNumber = normalizeRegistrationNumber(tokens[3]);

                    employee = employeeService.findByRegistrationNumber(registrationNumber).orElse(null);
                    if (employee == null) {
                        errorCount++;
                        missingRegistrations.merge(registrationNumber, 1, Integer::sum);
                        continue;
                    }

                    type = MealType.byTime(time);

                    Meal m = new Meal();
                    m.setEmployeeId(employee.getId());
                    m.setEmployeeSharedUsage(employee.isSharedUsage());
                    m.setDate(date);
                    m.setTime(time);
                    m.setType(type);
                    m.setPrice(prices.get(type));
                    mealService.register(m);
                    imported++;

                    if (earliestDate == null || date.isBefore(earliestDate)) {
                        earliestDate = date;
                    }
                    if (latestDate == null || date.isAfter(latestDate)) {
                        latestDate = date;
                    }
                } catch (DateTimeParseException e) {
                    errorCount++;
                    problems.add("Linha " + lineNumber + ": data ou horário em formato inválido.");
                    log.debug("Meal import: line {} has an invalid date/time", lineNumber);
                } catch (DuplicateMealException e) {
                    // employee/type/date are guaranteed non-null here: this
                    // exception is only thrown by mealService.register(),
                    // reached after all three are already set above.
                    errorCount++;
                    String key = registrationNumber + " - " + employee.getName() + " - "
                            + type.getDescription() + " - " + date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                    duplicateMeals.merge(key, 1, Integer::sum);
                    log.debug("Meal import: line {} skipped (duplicate meal type): {}", lineNumber, e.getMessage());
                } catch (Exception e) {
                    errorCount++;
                    problems.add("Linha " + lineNumber + ": " + e.getMessage());
                    log.debug("Meal import: line {} skipped: {}", lineNumber, e.getMessage());
                }
            }
        } catch (IOException e) {
            log.error("Failed to read meal import file {}", file.getAbsolutePath(), e);
            AlertUtil.error("Erro ao ler arquivo", e.getMessage());
            return;
        }

        // Expands the table's window to make sure the just-imported meals show
        // up, even if their date falls outside the current window (same logic
        // already used in onSaveMeal).
        if (earliestDate != null && earliestDate.isBefore(registrationWindowStart)) {
            registrationWindowStart = earliestDate;
        }
        if (latestDate != null && latestDate.isAfter(registrationWindowEnd)) {
            registrationWindowEnd = latestDate;
        }
        loadMeals(registrationWindowStart, registrationWindowEnd);

        log.info("Meal import finished: {} imported, {} error(s), {} unregistered matrícula(s), "
                        + "{} duplicate meal(s) (file={})",
                imported, errorCount, missingRegistrations.size(), duplicateMeals.size(), file.getAbsolutePath());
        showImportSummary("Importação de refeições", imported, 0, errorCount, problems);
        showMissingRegistrationsIfAny(missingRegistrations);
        showDuplicateMealsIfAny(duplicateMeals);
    }

    /**
     * Shown right after showImportSummary, only when the import found at
     * least one registration number with no matching employee (see
     * missingRegistrations in onImportMeals). Deliberately NOT capped like
     * showImportSummary's generic problem list -- RH/CIAU needs the full
     * list to know exactly who to register, and a large import can easily
     * turn up dozens of distinct people. The list is sorted by registration
     * number and shown in a resizable, scrollable, copy-pasteable text area
     * (a plain Alert's fixed content text doesn't handle a long list well),
     * with the option to save it as a .txt file since that's easier to act
     * on (share with RH, check off) than reading it off a dialog.
     */
    private void showMissingRegistrationsIfAny(Map<String, Integer> missingRegistrations) {
        if (missingRegistrations.isEmpty()) {
            return;
        }

        List<String> lines = missingRegistrations.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> entry.getKey() + "   (" + entry.getValue() + " refeição(ões) não incluída(s))")
                .toList();

        TextArea listArea = new TextArea(String.join("\n", lines));
        listArea.setEditable(false);
        listArea.setWrapText(false);
        listArea.setPrefSize(420, 320);

        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Matrículas não cadastradas");
        alert.setHeaderText(lines.size() + " matrícula(s) sem cadastro apareceram nas refeições importadas.");
        alert.setContentText(
                "Essas refeições NÃO foram registradas porque a matrícula não existe em Colaboradores.\n"
                        + "Cadastre essas pessoas na aba Colaboradores e reimporte o mesmo arquivo depois."
                        + "As refeições já importadas com sucesso não são duplicadas.");
        alert.getDialogPane().setExpandableContent(listArea);
        alert.getDialogPane().setExpanded(true);
        alert.setResizable(true);

        ButtonType saveButtonType = new ButtonType("Salvar lista (.txt)", ButtonBar.ButtonData.OTHER);
        // ButtonType.OK's ButtonData is OK_DONE, not CANCEL_CLOSE -- and a
        // JavaFX Alert/Dialog only lets the window's own "X" button actually
        // close it when at least one button is marked CANCEL_CLOSE (without
        // one, clicking "X" is silently ignored, which is exactly the
        // "the X doesn't work" bug reported). Recreating it here with
        // CANCEL_CLOSE keeps the same "OK" label/behavior (just dismiss,
        // nothing saved) while making "X" behave identically to clicking it.
        ButtonType okButtonType = new ButtonType("OK", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(saveButtonType, okButtonType);

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == saveButtonType) {
            saveMissingRegistrationsToFile(lines);
        }
    }

    /**
     * Shown right after showMissingRegistrationsIfAny, only when the import
     * skipped at least one line because the employee already had a meal of
     * that same type registered that same day (see the DuplicateMealException
     * caught in onImportMeals, and MealService.checkNoDuplicateMealType,
     * which enforces the one-meal-per-type-per-day rule). Same treatment as
     * showMissingRegistrationsIfAny and for the same reason: a large import
     * can easily produce several of these (e.g. a duplicate turnstile read),
     * and mixing them into the generic, capped problem list would bury the
     * one thing RH/CIAU actually needs -- which meals to double check --
     * among everything else.
     */
    private void showDuplicateMealsIfAny(Map<String, Integer> duplicateMeals) {
        if (duplicateMeals.isEmpty()) {
            return;
        }

        List<String> lines = duplicateMeals.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> entry.getKey() + "   (" + entry.getValue() + " ocorrência(s) ignorada(s))")
                .toList();

        TextArea listArea = new TextArea(String.join("\n", lines));
        listArea.setEditable(false);
        listArea.setWrapText(false);
        listArea.setPrefSize(420, 320);

        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Refeições duplicadas");
        alert.setHeaderText(lines.size()
                + " refeição(ões) não foram importadas por já existir uma do mesmo tipo no mesmo dia.");
        alert.setContentText(
                "Cada colaborador só pode ter uma refeição de cada tipo por dia\n"
                        + "As ocorrências abaixo foram ignoradas, confira se são leituras repetidas da"
                        + " catraca ou se algum tipo de refeição foi registrado incorretamente.");
        alert.getDialogPane().setExpandableContent(listArea);
        alert.getDialogPane().setExpanded(true);
        alert.setResizable(true);

        ButtonType saveButtonType = new ButtonType("Salvar lista (.txt)", ButtonBar.ButtonData.OTHER);
        // Same CANCEL_CLOSE fix as showMissingRegistrationsIfAny, so the
        // window's "X" button closes this dialog too.
        ButtonType okButtonType = new ButtonType("OK", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(saveButtonType, okButtonType);

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == saveButtonType) {
            saveDuplicateMealsToFile(lines);
        }
    }

    /** Saves the missing-registration list (see showMissingRegistrationsIfAny) as a plain .txt file. */
    private void saveMissingRegistrationsToFile(List<String> lines) {
        saveListToFile("Salvar lista de matrículas não cadastradas", "matriculas_nao_cadastradas",
                "MATRÍCULAS NÃO CADASTRADAS -- refeições importadas que ficaram de fora", lines);
    }

    /** Saves the duplicate-meal list (see showDuplicateMealsIfAny) as a plain .txt file. */
    private void saveDuplicateMealsToFile(List<String> lines) {
        saveListToFile("Salvar lista de refeições duplicadas", "refeicoes_duplicadas",
                "REFEIÇÕES DUPLICADAS -- refeições importadas que ficaram de fora por já existir"
                        + " uma do mesmo tipo no mesmo dia", lines);
    }

    /**
     * Shared by saveMissingRegistrationsToFile and saveDuplicateMealsToFile
     * (the two import-summary lists long/detailed enough to be worth saving
     * instead of just reading off the dialog): lets the user pick a save
     * location via a FileChooser, then writes a small header followed by
     * one line per entry, UTF-8 with a BOM so accents display correctly in
     * Windows Notepad.
     */
    private void saveListToFile(String dialogTitle, String fileNamePrefix, String header, List<String> lines) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(dialogTitle);
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Arquivo de texto (*.txt)", "*.txt"));
        chooser.setInitialFileName(fileNamePrefix + "_"
                + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + ".txt");
        File file = chooser.showSaveDialog(App.getMainStage());
        if (file == null) {
            return;
        }
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
            writer.write('\uFEFF'); // UTF-8 BOM, so accents show correctly in Notepad
            writer.write(header);
            writer.write("\nGerado em " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) + "\n\n");
            for (String line : lines) {
                writer.write(line);
                writer.write("\n");
            }
            log.info("List saved to {}", file.getAbsolutePath());
            AlertUtil.info("Lista salva", "Lista salva em:\n" + file.getAbsolutePath());
        } catch (IOException e) {
            log.error("Failed to save list to {}", file.getAbsolutePath(), e);
            AlertUtil.error("Erro ao salvar arquivo", e.getMessage());
        }
    }

    /** Builds and shows the summary dialog at the end of a bulk import. */
    private void showImportSummary(String title, int imported, int duplicates, int errorCount,
                                    List<String> problems) {
        StringBuilder message = new StringBuilder();
        message.append(imported).append(" registro(s) importado(s) com sucesso.");
        if (duplicates > 0) {
            message.append("\n").append(duplicates).append(" ignorado(s) por matrícula já cadastrada.");
        }
        if (errorCount > 0) {
            message.append("\n").append(errorCount).append(" linha(s) com erro.");
        }
        if (!problems.isEmpty()) {
            message.append("\n\nDetalhes:\n");
            int limit = Math.min(problems.size(), 25);
            for (int i = 0; i < limit; i++) {
                message.append("- ").append(problems.get(i)).append("\n");
            }
            if (problems.size() > limit) {
                message.append("... e mais ").append(problems.size() - limit).append(" linha(s).");
            }
        }
        AlertUtil.info(title, message.toString());
    }

    @FXML
    private void onBack() {
        try {
            App.switchScene("/fxml/hub.fxml", "Harmonia");
        } catch (IOException e) {
            log.error("Failed to navigate back to the hub", e);
            AlertUtil.error("Erro", "Não foi possível voltar ao hub: " + e.getMessage());
        }
    }
}
