package com.hospital.harmonia.controller;

import com.hospital.harmonia.App;
import com.hospital.harmonia.model.Category;
import com.hospital.harmonia.model.Employee;
import com.hospital.harmonia.model.Meal;
import com.hospital.harmonia.model.MealType;
import com.hospital.harmonia.service.DuplicateMealException;
import com.hospital.harmonia.service.CategoryService;
import com.hospital.harmonia.service.EmployeeService;
import com.hospital.harmonia.service.MealPriceService;
import com.hospital.harmonia.service.MealService;
import com.hospital.harmonia.service.ReportFilters;
import com.hospital.harmonia.util.AlertUtil;
import com.hospital.harmonia.util.FormUtil;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import javafx.util.StringConverter;
import org.controlsfx.control.CheckComboBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.stream.Collectors;

public class CafeteriaController implements Initializable {

    private static final Logger log = LoggerFactory.getLogger(CafeteriaController.class);

    // --- Employees tab ---
    @FXML private TextField employeeNameField;
    @FXML private TextField employeeRegistrationField;
    @FXML private ComboBox<Category> employeeCategoryCombo;

    // --- Categorias tab ---
    @FXML private TableView<Category> categoryTable;
    @FXML private TableColumn<Category, String> categoryNameColumn;
    @FXML private TableColumn<Category, String> categoryEmployeeCountColumn;
    @FXML private TableColumn<Category, String> categoryActiveColumn;
    @FXML private TextField categoryNameField;
    @FXML private Button saveCategoryButton;
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
    @FXML private CheckComboBox<String> reportCategoryCombo;
    @FXML private CheckBox reportCategoryExcludeCheck;
    @FXML private CheckComboBox<MealType> reportTypeCombo;
    @FXML private CheckBox reportTypeExcludeCheck;
    @FXML private ComboBox<Employee> reportEmployeeCombo;
    @FXML private Label reportStatusLabel;
    @FXML private TextField breakfastPriceField;
    @FXML private TextField lunchPriceField;
    @FXML private TextField dinnerPriceField;
    @FXML private TextField supperPriceField;
    @FXML private Label priceStatusLabel;

    private final EmployeeService employeeService = new EmployeeService();
    private final CategoryService categoryService = new CategoryService();
    private final MealService mealService = new MealService();
    private final MealPriceService mealPriceService = new MealPriceService();

    private final ObservableList<Employee> employees = FXCollections.observableArrayList();
    private final ObservableList<Category> categories = FXCollections.observableArrayList();
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
    private Integer editingCategoryId;
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

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        configureEmployeeTable();
        configureEmployeeFilters();
        configureCategoryTable();
        configureEmployeeCategoryCombo();
        configureMealTable();
        configureMealFilters();
        configureEmployeeCombo();
        configureReportFilters();
        configureDatePickers();
        configureTimeField();

        // Before loadEmployees(): the employee table and the report filters
        // both display category names, and the employee form's dropdown needs
        // its options in place.
        loadCategories();
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
        FormUtil.configureDateFormat(mealDatePicker);
        FormUtil.configureDateFormat(reportStartDatePicker);
        FormUtil.configureDateFormat(reportEndDatePicker);
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
        categoryColumn.setCellValueFactory(new PropertyValueFactory<>("categoryName"));
        sharedUsageColumn.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().isSharedUsage() ? "SIM" : "NÃO"));
        activeColumn.setCellValueFactory(data -> new SimpleStringProperty(
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
        // The "colaboradores" column counts from this very list, so it has to
        // be redrawn whenever the list changes.
        if (categoryTable != null) {
            categoryTable.refresh();
        }
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
        String name = FormUtil.normalize(employeeFilterNameField.getText());
        String category = FormUtil.normalize(employeeFilterCategoryField.getText());
        String registration = FormUtil.orEmpty(employeeFilterRegistrationField.getText()).trim();
        String status = employeeFilterStatusCombo.getValue();

        filteredEmployeesForTable.setPredicate(e ->
                (name.isEmpty() || FormUtil.normalize(e.getName()).contains(name))
                        && (category.isEmpty() || FormUtil.normalize(FormUtil.orEmpty(e.getCategoryName())).contains(category))
                        && (registration.isEmpty() || FormUtil.orEmpty(e.getRegistrationNumber()).contains(registration))
                        && ("Todos".equals(status)
                                || ("Ativos".equals(status) && e.isActive())
                                || ("Inativos".equals(status) && !e.isActive())));
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
            String filter = FormUtil.normalize(newText);
            filteredActiveEmployees.setPredicate(e -> filter.isEmpty() || FormUtil.normalize(e.getName()).contains(filter));
            if (!employeeCombo.isShowing() && employeeCombo.isFocused()) {
                employeeCombo.show();
            }
        });
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
        // Category.equals is by id, so this preselects the matching item even
        // though it's a different instance from the one in the combo's list.
        // An employee whose category was deactivated (and is therefore not in
        // the list of options) still shows it here instead of coming up blank
        // -- see refreshCategoryOptions.
        employeeCategoryCombo.setValue(e.getCategoryId() == null ? null : new Category(e.getCategoryId(), e.getCategoryName()));
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
            Category selectedCategory = employeeCategoryCombo.getValue();
            e.setCategoryId(selectedCategory == null ? null : selectedCategory.getId());
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
        employeeCategoryCombo.setValue(null);
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

    // ================= CATEGORIES =================

    private void configureCategoryTable() {
        categoryNameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        // Counted from the employee list already in memory instead of one
        // query per row -- it's the same list the Colaboradores tab shows, so
        // the number on screen always matches what's on screen.
        categoryEmployeeCountColumn.setCellValueFactory(data -> new SimpleStringProperty(
                String.valueOf(employees.stream()
                        .filter(e -> data.getValue().getId() != null
                                && data.getValue().getId().equals(e.getCategoryId()))
                        .count())));
        categoryActiveColumn.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().isActive() ? "SIM" : "NÃO"));
        categoryTable.setItems(categories);

        // Double-click loads the category into the form for renaming, same as
        // the Colaboradores tab.
        categoryTable.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                Category selected = categoryTable.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    categoryNameField.setText(selected.getName());
                    editingCategoryId = selected.getId();
                    saveCategoryButton.setText("ATUALIZAR");
                }
            }
        });
    }

    /**
     * The category dropdown on the employee form. The null item is a real
     * option ("sem categoria"), spelled out instead of rendering as a blank
     * row -- same treatment the report's "Todos"/"Todas" combos get.
     */
    private void configureEmployeeCategoryCombo() {
        employeeCategoryCombo.setConverter(new StringConverter<Category>() {
            @Override
            public String toString(Category category) {
                return category == null ? "(sem categoria)" : category.getName();
            }

            @Override
            public Category fromString(String text) {
                return null; // not editable -- selection only
            }
        });
        employeeCategoryCombo.setCellFactory(list -> new ListCell<Category>() {
            @Override
            protected void updateItem(Category item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : (item == null ? "(sem categoria)" : item.getName()));
            }
        });
    }

    private void loadCategories() {
        categories.setAll(categoryService.findAll());
        refreshCategoryOptions();
    }

    /**
     * Refills the employee form's dropdown with the ACTIVE categories, keeping
     * whatever was selected if it's still a valid option. A deactivated
     * category stays out of the list so it can't be assigned to anyone new --
     * employees already on it keep it (loadEmployeeForEditing puts it back in
     * the box on its own).
     */
    private void refreshCategoryOptions() {
        Category previous = employeeCategoryCombo.getValue();
        ObservableList<Category> options = FXCollections.observableArrayList();
        options.add(null); // "(sem categoria)"
        categories.stream().filter(Category::isActive).forEach(options::add);
        employeeCategoryCombo.setItems(options);
        employeeCategoryCombo.setValue(options.contains(previous) ? previous : null);
    }

    @FXML
    private void onSaveCategory() {
        try {
            Category category = new Category();
            category.setName(categoryNameField.getText());
            if (editingCategoryId != null) {
                category.setId(editingCategoryId);
                // Renaming only -- activating/deactivating has its own buttons.
                categories.stream()
                        .filter(existing -> existing.getId().equals(editingCategoryId))
                        .findFirst()
                        .ifPresent(existing -> category.setActive(existing.isActive()));
                categoryService.update(category);
            } else {
                categoryService.register(category);
            }
            loadCategories();
            // A rename changes the name shown on the employee table and in the
            // report filters, both of which read the denormalized name.
            loadEmployees();
            onClearCategoryForm();
        } catch (Exception e) {
            log.warn("Failed to save category: {}", e.getMessage(), e);
            AlertUtil.error("Erro ao salvar categoria", e.getMessage());
        }
    }

    @FXML
    private void onClearCategoryForm() {
        categoryNameField.clear();
        editingCategoryId = null;
        saveCategoryButton.setText("SALVAR");
        categoryTable.getSelectionModel().clearSelection();
    }

    @FXML
    private void onDeactivateCategory() {
        Category selected = categoryTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertUtil.warning("Nenhuma categoria selecionada", "Selecione a categoria que deseja inativar.");
            return;
        }
        long inUse = categoryService.countEmployees(selected.getId());
        String message = inUse == 0
                ? "Inativar a categoria \"" + selected.getName() + "\"?"
                : "A categoria \"" + selected.getName() + "\" está em uso por " + inUse + " colaborador(es).\n\n"
                        + "Eles continuam vinculados a ela e os relatórios anteriores não mudam -- a categoria"
                        + " apenas deixa de aparecer para novos cadastros. Inativar mesmo assim?";
        if (!AlertUtil.confirm("Inativar categoria", message)) {
            return;
        }
        categoryService.deactivate(selected.getId());
        loadCategories();
        onClearCategoryForm();
    }

    @FXML
    private void onActivateCategory() {
        Category selected = categoryTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertUtil.warning("Nenhuma categoria selecionada", "Selecione a categoria que deseja reativar.");
            return;
        }
        categoryService.activate(selected.getId());
        loadCategories();
        onClearCategoryForm();
    }

    // ================= MEAL REGISTRATION =================

    private void configureMealTable() {
        mealEmployeeColumn.setCellValueFactory(new PropertyValueFactory<>("employeeName"));
        mealDateColumn.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))));
        mealTimeColumn.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getTime().format(DateTimeFormatter.ofPattern("HH:mm"))));
        mealTypeColumn.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getType().getDescription()));
        mealPriceColumn.setCellValueFactory(new PropertyValueFactory<>("price"));
        mealActiveColumn.setCellValueFactory(data -> new SimpleStringProperty(
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
        String name = FormUtil.normalize(mealFilterNameField.getText());
        String category = FormUtil.normalize(mealFilterCategoryField.getText());
        MealType type = mealFilterTypeCombo.getValue();
        String status = mealFilterStatusCombo.getValue();

        filteredMealsForTable.setPredicate(m ->
                (name.isEmpty() || FormUtil.normalize(m.getEmployeeName()).contains(name))
                        && (category.isEmpty() || FormUtil.normalize(FormUtil.orEmpty(m.getEmployeeCategory())).contains(category))
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
        } catch (DateTimeParseException e) {
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
     * Configures the report's optional filters (Categoria/Tipo/Colaborador),
     * applied on top of the mandatory date range. Categoria and Tipo are
     * multi-select (CheckComboBox, from ControlsFX): any number of values
     * can be checked, and the matching "exceto" checkbox next to each turns
     * the checked values into an exclusion list instead of an inclusion
     * list -- see ReportFilters, built by currentReportFilters() when a
     * report is actually generated. Their item lists (categories and
     * employees) are refreshed whenever the employee list reloads -- see
     * refreshReportFilterOptions, called from loadEmployees().
     */
    private void configureReportFilters() {
        // MealType already orders its values the same way the rest of the
        // screen's meal-type combos do (MealType.values() declaration order);
        // kept consistent here instead of introducing a different order.
        reportTypeCombo.getItems().setAll(MealType.values());

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
     * Builds a ReportFilters from the Reports tab's current filter
     * selections: whatever's checked in the Categoria/Tipo checklists (plus
     * their "exceto" checkboxes) and the Colaborador combo -- passed as-is
     * to MealService.generatePeriodReport/generatePeriodReportXlsx.
     */
    private ReportFilters currentReportFilters() {
        Set<String> categories = new LinkedHashSet<>(reportCategoryCombo.getCheckModel().getCheckedItems());
        Set<MealType> types = new LinkedHashSet<>(reportTypeCombo.getCheckModel().getCheckedItems());
        return new ReportFilters(categories, reportCategoryExcludeCheck.isSelected(),
                types, reportTypeExcludeCheck.isSelected(), reportEmployeeCombo.getValue());
    }

    /**
     * Rebuilds the Categoria/Colaborador report filter options from the
     * current employee list (categories: distinct, non-blank, sorted).
     * Keeps whichever of the user's checked categories are still valid
     * options after the refresh (a category can disappear if the last
     * employee using it was edited/removed); Colaborador falls back to
     * "Todos" the same way it always did.
     */
    private void refreshReportFilterOptions() {
        // From the category registry, not from the distinct values found on
        // the employee list: a category with no employee at the moment still
        // has meals in past periods and must stay filterable. Inactive ones
        // are included for the same reason.
        List<String> categories = this.categories.stream()
                .map(Category::getName)
                .filter(name -> name != null && !name.isBlank())
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(Collectors.toList());
        Set<String> previouslyChecked = new LinkedHashSet<>(reportCategoryCombo.getCheckModel().getCheckedItems());
        reportCategoryCombo.getItems().setAll(categories);
        for (String category : categories) {
            if (previouslyChecked.contains(category)) {
                reportCategoryCombo.getCheckModel().check(category);
            }
        }

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
            mealService.generatePeriodReport(start, end, currentReportFilters(), destination);
            reportStatusLabel.setText("Relatório gerado com sucesso em: " + destination.getAbsolutePath());
        } catch (Exception e) {
            log.error("Failed to generate cafeteria report for period {} - {}", start, end, e);
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
            mealService.generatePeriodReportXlsx(start, end, currentReportFilters(), destination);
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
        return price.setScale(2, RoundingMode.HALF_UP).toPlainString();
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
        // Categories named in the file that aren't registered yet, with how
        // many lines each one cost -- shown in its own dialog afterwards so
        // whoever runs the import knows exactly what to register and re-run,
        // same treatment the missing matrículas already get.
        Map<String, Integer> unknownCategories = new LinkedHashMap<>();

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

                    // The category must already exist in the registry: letting
                    // the import create one on the fly is exactly how "UNICAMP"
                    // and "UNICAM" both ended up as categories, splitting the
                    // same company into two blocks on the billing report.
                    String categoryName = toUpperCaseText(category);
                    Optional<Category> knownCategory = categoryService.findByName(categoryName);
                    if (knownCategory.isEmpty()) {
                        unknownCategories.merge(categoryName, 1, Integer::sum);
                        problems.add("Linha " + lineNumber + ": categoria \"" + categoryName
                                + "\" não está cadastrada -- linha ignorada.");
                        continue;
                    }

                    Employee e = new Employee();
                    e.setName(toTitleCase(name.toString()));
                    e.setRegistrationNumber(registrationNumber);
                    e.setCategoryId(knownCategory.get().getId());
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

        log.info("Employee import finished: {} imported, {} duplicate(s), {} error(s), "
                        + "{} unknown category(ies) (file={})",
                imported, duplicates, errorCount, unknownCategories.size(), file.getAbsolutePath());
        loadEmployees();
        showImportSummary("Importação de colaboradores", imported, duplicates, errorCount, problems);
        showUnknownCategoriesIfAny(unknownCategories);
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
        // Same idea again, but for a registration number that DOES exist,
        // just for an inactive employee -- findByRegistrationNumber matches
        // active and inactive alike (it has to: the manual form's dedupe
        // check and the unique constraint on cpf apply regardless of
        // status), so without this check right here a turnstile record for
        // someone who was deactivated would still be imported as a real
        // meal. See showInactiveEmployeesIfAny, called at the end of this
        // method.
        Map<String, Integer> inactiveEmployees = new LinkedHashMap<>();
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
                    if (!employee.isActive()) {
                        errorCount++;
                        inactiveEmployees.merge(registrationNumber + " - " + employee.getName(), 1, Integer::sum);
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
                        + "{} duplicate meal(s), {} inactive employee(s) (file={})",
                imported, errorCount, missingRegistrations.size(), duplicateMeals.size(),
                inactiveEmployees.size(), file.getAbsolutePath());
        showImportSummary("Importação de refeições", imported, 0, errorCount, problems);
        showMissingRegistrationsIfAny(missingRegistrations);
        showDuplicateMealsIfAny(duplicateMeals);
        showInactiveEmployeesIfAny(inactiveEmployees);
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

        List<String> lines = toSortedLines(missingRegistrations, "refeição(ões) não incluída(s)");

        showSkippedItemsDialog("Matrículas não cadastradas",
                lines.size() + " matrícula(s) sem cadastro apareceram nas refeições importadas.",
                "Essas refeições NÃO foram registradas porque a matrícula não existe em Colaboradores.\n"
                        + "Cadastre essas pessoas na aba Colaboradores e reimporte o mesmo arquivo depois. "
                        + "As refeições já importadas com sucesso não são duplicadas.",
                lines, this::saveMissingRegistrationsToFile);
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

        List<String> lines = toSortedLines(duplicateMeals, "ocorrência(s) ignorada(s)");

        showSkippedItemsDialog("Refeições duplicadas",
                lines.size()
                        + " refeição(ões) não foram importadas por já existir uma do mesmo tipo no mesmo dia.",
                "Cada colaborador só pode ter uma refeição de cada tipo por dia.\n"
                        + "As ocorrências abaixo foram ignoradas, confira se são leituras repetidas da"
                        + " catraca ou se algum tipo de refeição foi registrado incorretamente.",
                lines, this::saveDuplicateMealsToFile);
    }

    /**
     * Shown right after showImportSummary when the employee import found a
     * category that isn't registered yet. Those lines are rejected on purpose
     * rather than creating the category on the fly: a typo in the file
     * ("UNICAM" for "UNICAMP") would otherwise become a brand new category and
     * split one company into two blocks on the report used to bill it.
     */
    private void showUnknownCategoriesIfAny(Map<String, Integer> unknownCategories) {
        if (unknownCategories.isEmpty()) {
            return;
        }

        List<String> lines = toSortedLines(unknownCategories, "colaborador(es) não importado(s)");

        showSkippedItemsDialog("Categorias não cadastradas",
                lines.size() + " categoria(s) do arquivo não existem no cadastro.",
                "Esses colaboradores NÃO foram importados porque a categoria não está cadastrada.\n"
                        + "Cadastre as categorias abaixo na aba Categorias e reimporte o mesmo arquivo depois. "
                        + "Os colaboradores já importados com sucesso não são duplicados.",
                lines, this::saveUnknownCategoriesToFile);
    }

    /**
     * Shown right after showDuplicateMealsIfAny, only when the import found
     * at least one line whose matrícula belongs to an employee that was
     * deactivated (see inactiveEmployees in onImportMeals). Rejected on
     * purpose instead of silently registering the meal: someone who was let
     * go or reassigned shouldn't keep accumulating billable meals just
     * because their old badge still reads at the turnstile.
     */
    private void showInactiveEmployeesIfAny(Map<String, Integer> inactiveEmployees) {
        if (inactiveEmployees.isEmpty()) {
            return;
        }

        List<String> lines = toSortedLines(inactiveEmployees, "refeição(ões) não incluída(s)");

        showSkippedItemsDialog("Colaboradores inativos",
                lines.size() + " matrícula(s) pertencem a colaborador(es) inativo(s).",
                "Essas refeições NÃO foram registradas porque o colaborador está inativo.\n"
                        + "Se a inativação foi um engano, reative a pessoa na aba Colaboradores e reimporte o "
                        + "mesmo arquivo depois. As refeições já importadas com sucesso não são duplicadas.",
                lines, this::saveInactiveEmployeesToFile);
    }

    /**
     * Builds the sorted "key   (count suffix)" lines the four "what the
     * import left out" dialogs below all show -- e.g. counts=(matrícula ->
     * how many meals it caused to be skipped), suffix="refeição(ões) não
     * incluída(s)" renders "00012345   (3 refeição(ões) não incluída(s))".
     */
    private static List<String> toSortedLines(Map<String, Integer> counts, String suffix) {
        return counts.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> entry.getKey() + "   (" + entry.getValue() + " " + suffix + ")")
                .toList();
    }

    /**
     * The dialog the four "what the import left out" lists share
     * (showMissingRegistrationsIfAny, showDuplicateMealsIfAny,
     * showUnknownCategoriesIfAny, showInactiveEmployeesIfAny): a resizable, scrollable, copy-pasteable
     * text area -- a plain Alert's fixed content text doesn't handle a long
     * list well -- plus the option to save it as a .txt, which is easier to
     * act on (share with RH, check off) than reading it off the screen.
     *
     * The OK button is deliberately rebuilt with CANCEL_CLOSE instead of
     * using ButtonType.OK: a JavaFX Alert only lets the window's own "X"
     * actually close it when at least one button carries that ButtonData
     * (without it, clicking "X" is silently ignored -- the "the X doesn't
     * work" bug). The label and behavior stay the same, just dismiss.
     */
    private void showSkippedItemsDialog(String title, String headerText, String contentText,
                                         List<String> lines, java.util.function.Consumer<List<String>> saveAction) {
        TextArea listArea = new TextArea(String.join("\n", lines));
        listArea.setEditable(false);
        listArea.setWrapText(false);
        listArea.setPrefSize(420, 320);

        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(headerText);
        alert.setContentText(contentText);
        alert.getDialogPane().setExpandableContent(listArea);
        alert.getDialogPane().setExpanded(true);
        alert.setResizable(true);

        ButtonType saveButtonType = new ButtonType("Salvar lista (.txt)", ButtonBar.ButtonData.OTHER);
        ButtonType okButtonType = new ButtonType("OK", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(saveButtonType, okButtonType);

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == saveButtonType) {
            saveAction.accept(lines);
        }
    }

    /** Saves the unknown-category list (see showUnknownCategoriesIfAny) as a plain .txt file. */
    private void saveUnknownCategoriesToFile(List<String> lines) {
        saveListToFile("Salvar lista de categorias não cadastradas", "categorias_nao_cadastradas",
                "CATEGORIAS NÃO CADASTRADAS -- colaboradores do arquivo que ficaram de fora", lines);
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

    /** Saves the inactive-employee list (see showInactiveEmployeesIfAny) as a plain .txt file. */
    private void saveInactiveEmployeesToFile(List<String> lines) {
        saveListToFile("Salvar lista de colaboradores inativos", "colaboradores_inativos",
                "COLABORADORES INATIVOS -- refeições importadas que ficaram de fora porque a matrícula"
                        + " pertence a um colaborador inativo", lines);
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
        App.backToHub();
    }
}
