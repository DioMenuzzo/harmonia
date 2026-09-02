package com.hospital.harmonia.controller;

import com.hospital.harmonia.App;
import com.hospital.harmonia.model.Employee;
import com.hospital.harmonia.model.Meal;
import com.hospital.harmonia.model.MealType;
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
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

public class CafeteriaController implements Initializable {

    private static final Logger log = LoggerFactory.getLogger(CafeteriaController.class);

    // --- Employees tab ---
    @FXML private TextField employeeNameField;
    @FXML private TextField employeeRegistrationField;
    @FXML private TextField employeeCategoryField;
    @FXML private Button saveEmployeeButton;
    @FXML private TableView<Employee> employeeTable;
    @FXML private TableColumn<Employee, Integer> idColumn;
    @FXML private TableColumn<Employee, String> nameColumn;
    @FXML private TableColumn<Employee, String> registrationColumn;
    @FXML private TableColumn<Employee, String> categoryColumn;
    @FXML private TableColumn<Employee, String> activeColumn;

    // --- Meal registration tab ---
    @FXML private ComboBox<Employee> employeeCombo;
    @FXML private DatePicker mealDatePicker;
    @FXML private TextField mealTimeField;
    @FXML private Button saveMealButton;
    @FXML private TableView<Meal> mealTable;
    @FXML private TableColumn<Meal, Integer> mealIdColumn;
    @FXML private TableColumn<Meal, String> mealEmployeeColumn;
    @FXML private TableColumn<Meal, String> mealDateColumn;
    @FXML private TableColumn<Meal, String> mealTimeColumn;
    @FXML private TableColumn<Meal, String> mealTypeColumn;
    @FXML private TableColumn<Meal, BigDecimal> mealPriceColumn;
    @FXML private TableColumn<Meal, String> mealActiveColumn;

    // --- Reports tab ---
    @FXML private DatePicker reportStartDatePicker;
    @FXML private DatePicker reportEndDatePicker;
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

    // Fixed date format (day/month/year) used in the DatePickers on screen --
    // independent of the machine's locale (see configureDatePickers).
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        configureEmployeeTable();
        configureMealTable();
        configureEmployeeCombo();
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
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        registrationColumn.setCellValueFactory(new PropertyValueFactory<>("registrationNumber"));
        categoryColumn.setCellValueFactory(new PropertyValueFactory<>("category"));
        activeColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                data.getValue().isActive() ? "SIM" : "NÃO"));
        employeeTable.setItems(employees);

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
        editingEmployeeId = null;
        editingEmployeeJobTitle = null;
        saveEmployeeButton.setText("SALVAR");
    }

    @FXML
    private void onDeactivateEmployee() {
        Employee selected = employeeTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertUtil.warning("Selecao necessaria", "Selecione um colaborador na tabela.");
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
            AlertUtil.warning("Selecao necessaria", "Selecione um colaborador na tabela.");
            return;
        }
        if (selected.isActive()) {
            AlertUtil.warning("Colaborador ja ativo", selected.getName() + " ja esta ativo.");
            return;
        }
        employeeService.activate(selected.getId());
        loadEmployees();
    }

    // ================= MEAL REGISTRATION =================

    private void configureMealTable() {
        mealIdColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
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
        mealTable.setItems(meals);

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
        saveMealButton.setText("ATUALIZAR");
    }

    /** Returns the meal form to normal registration mode (cancels the edit). */
    private void clearMealForm() {
        mealTimeField.clear();
        employeeCombo.setValue(null);
        employeeCombo.getEditor().clear();
        editingMealId = null;
        editingMealOriginalEmployeeId = null;
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
            if (employee != null) {
                employeeId = employee.getId();
            } else if (editingMealId != null && editingMealOriginalEmployeeId != null) {
                // Editing a meal of an inactive employee (doesn't appear in the
                // combo) -- keeps the original employee, since the user didn't choose another.
                employeeId = editingMealOriginalEmployeeId;
            } else {
                AlertUtil.warning("Campo obrigatorio", "Selecione um colaborador da lista.");
                return;
            }

            LocalTime time = LocalTime.parse(mealTimeField.getText(), DateTimeFormatter.ofPattern("H:mm"));
            MealType type = MealType.byTime(time);

            Meal m = new Meal();
            m.setEmployeeId(employeeId);
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
            AlertUtil.error("Horario invalido", "Use o formato HH:mm, ex: 12:30.");
        } catch (Exception e) {
            log.warn("Failed to register/update meal: {}", e.getMessage(), e);
            AlertUtil.error("Erro ao registrar refeicao", e.getMessage());
        }
    }

    @FXML
    private void onDeactivateMeal() {
        Meal selected = mealTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertUtil.warning("Selecao necessaria", "Selecione uma refeicao na tabela.");
            return;
        }
        if (!selected.isActive()) {
            AlertUtil.warning("Refeicao ja inativa", "Este registro ja esta inativo.");
            return;
        }
        if (AlertUtil.confirm("Inativar refeicao", "Inativar este registro de refeicao?"
                + " Ele continua no historico, mas sai do relatorio e do total.")) {
            mealService.deactivate(selected.getId());
            loadMeals(registrationWindowStart, registrationWindowEnd);
        }
    }

    @FXML
    private void onActivateMeal() {
        Meal selected = mealTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertUtil.warning("Selecao necessaria", "Selecione uma refeicao na tabela.");
            return;
        }
        if (selected.isActive()) {
            AlertUtil.warning("Refeicao ja ativa", "Este registro ja esta ativo.");
            return;
        }
        mealService.activate(selected.getId());
        loadMeals(registrationWindowStart, registrationWindowEnd);
    }

    // ================= REPORT =================

    @FXML
    private void onGenerateReport() {
        LocalDate start = reportStartDatePicker.getValue();
        LocalDate end = reportEndDatePicker.getValue();
        if (start == null || end == null) {
            AlertUtil.warning("Periodo invalido", "Informe as duas datas do periodo.");
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Salvar relatorio como...");
        chooser.setInitialFileName("relatorio_refeitorio_" + start + "_a_" + end + ".pdf");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
        File destination = chooser.showSaveDialog(App.getMainStage());
        if (destination == null) {
            return;
        }

        try {
            mealService.generatePeriodReport(start, end, destination);
            reportStatusLabel.setText("Relatorio gerado com sucesso em: " + destination.getAbsolutePath());
        } catch (Exception e) {
            log.error("Failed to generate cafeteria report for period {} - {}", start, end, e);
            reportStatusLabel.setText("Falha ao gerar relatorio.");
            AlertUtil.error("Erro ao gerar relatorio", e.getMessage());
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
            prices.put(MealType.CAFE_DA_MANHA, readPrice(breakfastPriceField, "Cafe da manha"));
            prices.put(MealType.ALMOCO, readPrice(lunchPriceField, "Almoco"));
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
            throw new IllegalArgumentException("Informe o preco de " + typeName + ".");
        }
        try {
            BigDecimal price = new BigDecimal(text.trim().replace(",", "."));
            if (price.signum() < 0) {
                throw new IllegalArgumentException("Preco de " + typeName + " nao pode ser negativo.");
            }
            return price;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Preco de " + typeName + " invalido: \"" + text + "\".");
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
                        + "Codigo da Catraca | Data | Horário | Matricula | Código Refeição\n\n"
                        + "Exemplo:\nSAICN 16/02/2026 05:29 01002522 000003");
    }

    /**
     * Expected format of each line: "Matricula Nome do Colaborador Categoria"
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
                    problems.add("Linha " + lineNumber + ": formato invalido (esperado: Matricula Nome Categoria).");
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
                        problems.add("Linha " + lineNumber + ": matricula " + registrationNumber + " ja cadastrada -- ignorada.");
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
     * Expected format of each line: "Codigo da Catraca | Data | Horario | Matricula | Permissao RH"
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
                            + ": formato invalido (esperado: Catraca Data Horario Matricula Permissao).");
                    continue;
                }
                try {
                    LocalDate date = LocalDate.parse(tokens[1], DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                    LocalTime time = LocalTime.parse(tokens[2], DateTimeFormatter.ofPattern("HH:mm"));
                    String registrationNumber = normalizeRegistrationNumber(tokens[3]);

                    Employee employee = employeeService.findByRegistrationNumber(registrationNumber).orElse(null);
                    if (employee == null) {
                        errorCount++;
                        problems.add("Linha " + lineNumber + ": matricula " + registrationNumber + " nao encontrada.");
                        continue;
                    }

                    MealType type = MealType.byTime(time);

                    Meal m = new Meal();
                    m.setEmployeeId(employee.getId());
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
                    problems.add("Linha " + lineNumber + ": data ou horario em formato invalido.");
                    log.debug("Meal import: line {} has an invalid date/time", lineNumber);
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

        log.info("Meal import finished: {} imported, {} error(s) (file={})",
                imported, errorCount, file.getAbsolutePath());
        showImportSummary("Importação de refeições", imported, 0, errorCount, problems);
    }

    /** Builds and shows the summary dialog at the end of a bulk import. */
    private void showImportSummary(String title, int imported, int duplicates, int errorCount,
                                    List<String> problems) {
        StringBuilder message = new StringBuilder();
        message.append(imported).append(" registro(s) importado(s) com sucesso.");
        if (duplicates > 0) {
            message.append("\n").append(duplicates).append(" ignorado(s) por matricula ja cadastrada.");
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
            AlertUtil.error("Erro", "Nao foi possivel voltar ao hub: " + e.getMessage());
        }
    }
}
