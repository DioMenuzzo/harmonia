package com.hospital.harmonia.controller;

import com.hospital.harmonia.App;
import com.hospital.harmonia.model.Contact;
import com.hospital.harmonia.model.ContactCategory;
import com.hospital.harmonia.service.ContactCategoryService;
import com.hospital.harmonia.service.ContactService;
import com.hospital.harmonia.util.AlertUtil;
import com.hospital.harmonia.util.FormUtil;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.StringConverter;

import java.net.URL;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.TreeSet;

/**
 * Phone Directory (Agenda Telefônica) module: internal sectors, doctors,
 * partner institutions, ambulance/transport services and any other useful
 * phone number -- replaces the standalone "Agenda v2.0" Excel spreadsheet
 * the front desk used to keep this in.
 *
 * The Categoria field is its own registry now (table "categorias_agenda",
 * managed on this screen's own Categorias tab -- create/rename/inactivate,
 * exactly like CafeteriaController's Categorias tab does for employee
 * categories), not free text: see ContactCategory's Javadoc for why this
 * is a SEPARATE registry from employee categories rather than reusing
 * "categorias".
 *
 * The ~5.000 contacts already in the old spreadsheet were deliberately NOT
 * imported into this module: its Categoria column turned out to be mostly
 * free-text notes accumulated over the years (scheduling notes, "não
 * trabalha mais aqui", extension numbers, people's names...), not a real,
 * reusable taxonomy. Registration here starts empty and grows from now on;
 * importing the old records can be revisited later as its own task if still
 * wanted.
 */
public class AgendaTelefonicaController implements Initializable {

    // --- Contatos tab: form ---
    @FXML private TextField nameField;
    @FXML private ComboBox<ContactCategory> categoryCombo;
    @FXML private TextField cellPhoneField;
    @FXML private TextField phoneField;
    @FXML private TextField emailField;
    @FXML private TextArea noteArea;
    @FXML private Button saveContactButton;

    // --- Contatos tab: filters ---
    @FXML private TextField searchField;
    @FXML private ComboBox<String> filterCategoryCombo;
    @FXML private ComboBox<String> filterActiveCombo;

    // --- Contatos tab: table ---
    @FXML private TableView<Contact> contactTable;
    @FXML private TableColumn<Contact, String> nameColumn;
    @FXML private TableColumn<Contact, String> categoryColumn;
    @FXML private TableColumn<Contact, String> cellPhoneColumn;
    @FXML private TableColumn<Contact, String> phoneColumn;
    @FXML private TableColumn<Contact, String> emailColumn;
    @FXML private TableColumn<Contact, String> noteColumn;
    @FXML private TableColumn<Contact, String> activeColumn;

    // --- Categorias tab ---
    @FXML private TableView<ContactCategory> categoryTable;
    @FXML private TableColumn<ContactCategory, String> categoryNameColumn;
    @FXML private TableColumn<ContactCategory, String> categoryContactCountColumn;
    @FXML private TableColumn<ContactCategory, String> categoryActiveColumn;
    @FXML private TextField categoryNameField;
    @FXML private Button saveCategoryButton;

    private final ContactService contactService = new ContactService();
    private final ContactCategoryService contactCategoryService = new ContactCategoryService();
    private final ObservableList<Contact> contactList = FXCollections.observableArrayList();
    private final ObservableList<ContactCategory> categories = FXCollections.observableArrayList();
    private FilteredList<Contact> filteredContacts;

    // Id of the contact being edited (double-click on a table row), or null
    // when the form is in normal registration mode (new record). The
    // original "active" flag is stored separately because the form has no
    // field for it -- deactivate/activate have their own buttons.
    private Integer editingContactId;
    private boolean editingContactActive;
    // Same idea, for the category being renamed on the Categorias tab.
    private Integer editingCategoryId;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        configureTable();
        configureCategoryCombo();
        configureCategoryTable();
        configureFilters();
        onClearForm();
        onClearCategoryForm();
        loadCategories();
        loadContacts();
    }

    // ================= CONTATOS: TABLE =================

    private void configureTable() {
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        categoryColumn.setCellValueFactory(new PropertyValueFactory<>("categoryName"));
        cellPhoneColumn.setCellValueFactory(new PropertyValueFactory<>("cellPhone"));
        phoneColumn.setCellValueFactory(new PropertyValueFactory<>("phone"));
        emailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));
        noteColumn.setCellValueFactory(new PropertyValueFactory<>("note"));
        activeColumn.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().isActive() ? "SIM" : "NÃO"));

        filteredContacts = new FilteredList<>(contactList, c -> true);
        contactTable.setItems(filteredContacts);

        // Double-clicking a row loads the record into the form on the left
        // for editing (SALVAR becomes ATUALIZAR) -- same pattern as
        // TechnicalSupportController/CafeteriaController.
        contactTable.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                Contact selected = contactTable.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    loadContactForEditing(selected);
                }
            }
        });

        // Whole row in light red if the contact has been deactivated, same
        // pattern as Equipment/Employee/Meal.
        contactTable.setRowFactory(table -> new TableRow<Contact>() {
            @Override
            protected void updateItem(Contact contact, boolean empty) {
                super.updateItem(contact, empty);
                if (empty || contact == null) {
                    setStyle("");
                } else if (!contact.isActive()) {
                    setStyle("-fx-background-color: #f8d7da;");
                } else {
                    setStyle("");
                }
            }
        });
    }

    private void loadContacts() {
        contactList.setAll(contactService.findAll());
        refreshCategoryFilterOptions();
    }

    /**
     * Rebuilds the Categoria filter's options from the category NAMES
     * actually used by loaded contacts (not from the full registry) -- same
     * light-touch, denormalized-name filter CafeteriaController uses for
     * employees, kept separate from the registry-backed categoryCombo below.
     */
    private void refreshCategoryFilterOptions() {
        String current = filterCategoryCombo.getValue();
        TreeSet<String> names = new TreeSet<>();
        for (Contact c : contactList) {
            if (c.getCategoryName() != null && !c.getCategoryName().isBlank()) {
                names.add(c.getCategoryName());
            }
        }
        ObservableList<String> items = FXCollections.observableArrayList("Todos");
        items.addAll(names);
        filterCategoryCombo.setItems(items);
        filterCategoryCombo.setValue(items.contains(current) ? current : "Todos");
    }

    // ================= CONTATOS: FILTERS =================

    private void configureFilters() {
        filterCategoryCombo.setItems(FXCollections.observableArrayList("Todos"));
        filterCategoryCombo.setValue("Todos");

        filterActiveCombo.setItems(FXCollections.observableArrayList("Todos", "Ativos", "Inativos"));
        filterActiveCombo.setValue("Todos");

        searchField.textProperty().addListener((obs, o, n) -> applyFilter());
        filterCategoryCombo.valueProperty().addListener((obs, o, n) -> applyFilter());
        filterActiveCombo.valueProperty().addListener((obs, o, n) -> applyFilter());
    }

    private void applyFilter() {
        String search = FormUtil.normalize(searchField.getText());
        String category = filterCategoryCombo.getValue();
        String active = filterActiveCombo.getValue();

        filteredContacts.setPredicate(c ->
                (search.isEmpty() || matchesSearch(c, search))
                        // "Todos".equals(category) alone isn't enough here: when
                        // refreshCategoryFilterOptions() calls setItems() on
                        // filterCategoryCombo (every loadContacts(), e.g. right
                        // after saving/editing a contact), the combo's value can
                        // transiently become null for an instant before the
                        // fallback setValue("Todos") right after it takes effect
                        // -- and that transient null still fires this predicate
                        // (FilteredList re-evaluates it later, during the
                        // TableView's own redraw, not synchronously here). The
                        // old "category.equals(...)" crashed with an NPE at that
                        // moment -- with nothing catching it at that point, it
                        // surfaced as the generic "Erro inesperado" dialog
                        // instead of a normal validation message. Objects.equals
                        // treats a null category the same as "no filter matched",
                        // which just means the predicate is momentarily stricter
                        // for one redraw -- harmless, since the very next
                        // setValue("Todos") immediately re-triggers applyFilter().
                        && ("Todos".equals(category) || Objects.equals(category, c.getCategoryName()))
                        && ("Todos".equals(active)
                                || ("Ativos".equals(active) && c.isActive())
                                || ("Inativos".equals(active) && !c.isActive())));
    }

    /**
     * "Pesquise aqui..." equivalent from the old spreadsheet -- one box that
     * searches every field at once, including Observação: the legacy import
     * from "Agenda v2.0" relocates category text that was too rare/messy to
     * become a real category into this field (see ContactCategoryService's
     * import notes), so it needs to stay searchable here too.
     */
    private static boolean matchesSearch(Contact c, String normalizedSearch) {
        return FormUtil.normalize(FormUtil.orEmpty(c.getName())).contains(normalizedSearch)
                || FormUtil.normalize(FormUtil.orEmpty(c.getCategoryName())).contains(normalizedSearch)
                || FormUtil.orEmpty(c.getCellPhone()).contains(normalizedSearch)
                || FormUtil.orEmpty(c.getPhone()).contains(normalizedSearch)
                || FormUtil.normalize(FormUtil.orEmpty(c.getEmail())).contains(normalizedSearch)
                || FormUtil.normalize(FormUtil.orEmpty(c.getNote())).contains(normalizedSearch);
    }

    // ================= CONTATOS: FORM =================

    /**
     * The category dropdown on the contact form. The null item is a real
     * option ("sem categoria"), spelled out instead of rendering as a blank
     * row -- same pattern as CafeteriaController.configureEmployeeCategoryCombo.
     */
    private void configureCategoryCombo() {
        categoryCombo.setConverter(new StringConverter<ContactCategory>() {
            @Override
            public String toString(ContactCategory category) {
                return category == null ? "(sem categoria)" : category.getName();
            }

            @Override
            public ContactCategory fromString(String text) {
                return null; // not editable -- selection only
            }
        });
        categoryCombo.setCellFactory(list -> new ListCell<ContactCategory>() {
            @Override
            protected void updateItem(ContactCategory item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : (item == null ? "(sem categoria)" : item.getName()));
            }
        });
    }

    private void loadContactForEditing(Contact c) {
        nameField.setText(c.getName());
        // ContactCategory.equals is by id, so this preselects the matching
        // item even though it's a different object instance -- same pattern
        // as CafeteriaController.loadEmployeeForEditing.
        categoryCombo.setValue(c.getCategoryId() == null ? null : new ContactCategory(c.getCategoryId(), c.getCategoryName()));
        cellPhoneField.setText(c.getCellPhone());
        phoneField.setText(c.getPhone());
        emailField.setText(c.getEmail());
        noteArea.setText(c.getNote());

        editingContactId = c.getId();
        editingContactActive = c.isActive();
        saveContactButton.setText("ATUALIZAR");
    }

    @FXML
    private void onClearForm() {
        nameField.clear();
        categoryCombo.setValue(null);
        cellPhoneField.clear();
        phoneField.clear();
        emailField.clear();
        noteArea.clear();

        editingContactId = null;
        saveContactButton.setText("SALVAR");
    }

    @FXML
    private void onSave() {
        try {
            Contact contact = new Contact();
            contact.setName(trimOrNull(nameField.getText()));
            ContactCategory selectedCategory = categoryCombo.getValue();
            contact.setCategoryId(selectedCategory == null ? null : selectedCategory.getId());
            contact.setCellPhone(trimOrNull(cellPhoneField.getText()));
            contact.setPhone(trimOrNull(phoneField.getText()));
            contact.setEmail(trimOrNull(emailField.getText()));
            contact.setNote(trimOrNull(noteArea.getText()));

            if (editingContactId != null) {
                contact.setId(editingContactId);
                // Preserves the original active/inactive flag -- the form has
                // no field for it, INATIVAR/ATIVAR have their own button.
                contact.setActive(editingContactActive);
                contactService.update(contact);
            } else {
                contactService.register(contact);
            }

            loadContacts();
            onClearForm();
        } catch (Exception e) {
            AlertUtil.error("Erro ao salvar contato", e.getMessage());
        }
    }

    private static String trimOrNull(String text) {
        if (text == null) {
            return null;
        }
        String trimmed = text.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    @FXML
    private void onDeactivate() {
        Contact selected = contactTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertUtil.warning("Seleção necessária", "Selecione um contato na tabela.");
            return;
        }
        if (!selected.isActive()) {
            AlertUtil.warning("Registro já inativo", "Este registro já está inativo.");
            return;
        }
        if (AlertUtil.confirm("Inativar contato",
                "Inativar o contato " + selected.getName()
                        + "? Ele continua no histórico, mas sai da lista de ativos.")) {
            contactService.deactivate(selected.getId());
            loadContacts();
        }
    }

    @FXML
    private void onActivate() {
        Contact selected = contactTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertUtil.warning("Seleção necessária", "Selecione um contato na tabela.");
            return;
        }
        if (selected.isActive()) {
            AlertUtil.warning("Registro já ativo", "Este registro já está ativo.");
            return;
        }
        contactService.activate(selected.getId());
        loadContacts();
    }

    // ================= CATEGORIAS =================

    private void configureCategoryTable() {
        categoryNameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        categoryContactCountColumn.setCellValueFactory(data -> new SimpleStringProperty(
                String.valueOf(contactList.stream()
                        .filter(c -> data.getValue().getId() != null
                                && data.getValue().getId().equals(c.getCategoryId()))
                        .count())));
        categoryActiveColumn.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().isActive() ? "SIM" : "NÃO"));
        categoryTable.setItems(categories);

        // Double-click loads the category into the form for renaming, same as
        // CafeteriaController.
        categoryTable.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                ContactCategory selected = categoryTable.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    categoryNameField.setText(selected.getName());
                    editingCategoryId = selected.getId();
                    saveCategoryButton.setText("ATUALIZAR");
                }
            }
        });
    }

    private void loadCategories() {
        categories.setAll(contactCategoryService.findAll());
        refreshCategoryComboOptions();
        if (categoryTable != null) {
            categoryTable.refresh();
        }
    }

    /**
     * Refills the contact form's categoryCombo with the ACTIVE categories,
     * keeping whatever was selected if it's still a valid option. A
     * deactivated category stays out of the list so it can't be assigned to
     * anyone new -- contacts already on it keep it (loadContactForEditing
     * puts it back in the box on its own).
     */
    private void refreshCategoryComboOptions() {
        ContactCategory previous = categoryCombo.getValue();
        ObservableList<ContactCategory> options = FXCollections.observableArrayList();
        options.add(null); // "(sem categoria)"
        categories.stream().filter(ContactCategory::isActive).forEach(options::add);
        categoryCombo.setItems(options);
        categoryCombo.setValue(options.contains(previous) ? previous : null);
    }

    @FXML
    private void onSaveCategory() {
        try {
            ContactCategory category = new ContactCategory();
            category.setName(categoryNameField.getText());
            if (editingCategoryId != null) {
                category.setId(editingCategoryId);
                // Renaming only -- activating/deactivating has its own buttons.
                categories.stream()
                        .filter(existing -> existing.getId().equals(editingCategoryId))
                        .findFirst()
                        .ifPresent(existing -> category.setActive(existing.isActive()));
                contactCategoryService.update(category);
            } else {
                contactCategoryService.register(category);
            }
            loadCategories();
            // A rename changes the name shown on the contact table and filters,
            // both of which read the denormalized name.
            loadContacts();
            onClearCategoryForm();
        } catch (Exception e) {
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
        ContactCategory selected = categoryTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertUtil.warning("Nenhuma categoria selecionada", "Selecione a categoria que deseja inativar.");
            return;
        }
        long inUse = contactCategoryService.countContacts(selected.getId());
        String message = inUse == 0
                ? "Inativar a categoria \"" + selected.getName() + "\"?"
                : "A categoria \"" + selected.getName() + "\" está em uso por " + inUse + " contato(s).\n\n"
                        + "Eles continuam vinculados a ela -- a categoria apenas deixa de aparecer para novos"
                        + " cadastros. Inativar mesmo assim?";
        if (!AlertUtil.confirm("Inativar categoria", message)) {
            return;
        }
        contactCategoryService.deactivate(selected.getId());
        loadCategories();
        onClearCategoryForm();
    }

    @FXML
    private void onActivateCategory() {
        ContactCategory selected = categoryTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertUtil.warning("Nenhuma categoria selecionada", "Selecione a categoria que deseja reativar.");
            return;
        }
        contactCategoryService.activate(selected.getId());
        loadCategories();
        onClearCategoryForm();
    }

    @FXML
    private void onBack() {
        App.backToHub();
    }
}
