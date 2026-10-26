package com.hospital.harmonia.controller;

import com.hospital.harmonia.App;
import com.hospital.harmonia.model.Role;
import com.hospital.harmonia.model.User;
import com.hospital.harmonia.service.UserService;
import com.hospital.harmonia.util.AlertUtil;
import com.hospital.harmonia.util.SessionManager;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Screen restricted to the CIAU role (see Role.canManageUsers() and
 * HubController.onOpenUsers, which hides/blocks this screen for every other
 * role): lets the admin change the username and/or password of ANY account,
 * register new accounts, and grant/revoke per-account module access
 * (Refeitório/Agenda Telefônica/Assistência Técnica -- see the
 * comment on Role for why module access moved from the Role enum to a
 * per-user setting).
 */
public class UserManagementController implements Initializable {

    private static final Logger log = LoggerFactory.getLogger(UserManagementController.class);

    @FXML private TableView<User> userTable;
    @FXML private TableColumn<User, String> usernameColumn;
    @FXML private TableColumn<User, String> displayNameColumn;
    @FXML private TableColumn<User, String> roleColumn;
    @FXML private TableColumn<User, String> activeColumn;
    @FXML private Label selectedUserLabel;
    @FXML private TextField newUsernameField;
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label statusLabel;

    // --- Permissões do usuário selecionado ---
    @FXML private CheckBox permissionsCafeteriaCheck;
    @FXML private CheckBox permissionsAgendaCheck;
    @FXML private CheckBox permissionsTechnicalSupportCheck;
    @FXML private Label permissionsStatusLabel;

    // --- Novo usuário ---
    @FXML private TextField registerUsernameField;
    @FXML private TextField registerDisplayNameField;
    @FXML private ComboBox<Role> registerRoleCombo;
    @FXML private CheckBox registerCafeteriaCheck;
    @FXML private CheckBox registerAgendaCheck;
    @FXML private CheckBox registerTechnicalSupportCheck;
    @FXML private PasswordField registerPasswordField;
    @FXML private PasswordField registerConfirmPasswordField;
    @FXML private Label registerStatusLabel;

    private final UserService userService = new UserService();
    private final ObservableList<User> users = FXCollections.observableArrayList();
    private User selectedUser;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Defense in depth: HubController.onOpenUsers is today the ONLY gate
        // keeping a non-CIAU user out of this screen (it hides the card and
        // re-checks the role right before navigating here). That's a single
        // point of failure -- if a future change ever adds another way to
        // reach this FXML (a shortcut, a different "voltar" path, a slip
        // during a refactor) without going through that check, this is the
        // last line of defense before every account's username/role and the
        // password-change form are shown and usable.
        //
        // The redirect is deferred with Platform.runLater: at this point
        // FXMLLoader.load() hasn't returned to App.switchScene() yet, and
        // switchScene() unconditionally calls mainStage.setScene(...) with
        // the scene it is currently building right after load() returns --
        // navigating away synchronously from inside initialize() would just
        // get overwritten back to this same screen a moment later.
        User loggedInUser = SessionManager.getLoggedInUser();
        if (loggedInUser == null || !loggedInUser.getRole().canManageUsers()) {
            log.warn("Blocked access to user management for {}", loggedInUser == null ? "no logged-in user"
                    : loggedInUser.getUsername() + " (" + loggedInUser.getRole() + ")");
            Platform.runLater(() -> {
                AlertUtil.error("Acesso negado", "Você não tem permissão para gerenciar usuários.");
                onBack();
            });
            return;
        }

        usernameColumn.setCellValueFactory(new PropertyValueFactory<>("username"));
        displayNameColumn.setCellValueFactory(new PropertyValueFactory<>("displayName"));
        // Role is an enum, not a String -- built as text directly (same
        // pattern used elsewhere in the app for non-String columns, e.g.
        // CafeteriaController's activeColumn/mealTypeColumn) instead of
        // PropertyValueFactory, which would need a matching generic type.
        roleColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                data.getValue().getRole().toString()));
        activeColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                data.getValue().isActive() ? "SIM" : "NÃO"));
        userTable.setItems(users);

        // Whole row in light red for a deactivated account -- same pattern
        // used for Equipment/Employee/Meal/Contact.
        userTable.setRowFactory(table -> new TableRow<User>() {
            @Override
            protected void updateItem(User user, boolean empty) {
                super.updateItem(user, empty);
                setStyle(!empty && user != null && !user.isActive() ? "-fx-background-color: #f8d7da;" : "");
            }
        });

        userTable.getSelectionModel().selectedItemProperty()
                .addListener((obs, oldSelection, newSelection) -> loadSelection(newSelection));

        registerRoleCombo.setItems(FXCollections.observableArrayList(Role.values()));
        // Convenience only, not a restriction: picking a Role just pre-checks
        // the boxes that role traditionally implied (RH -> Refeitório,
        // FINANCEIRO -> Agenda, CIAU -> all three) -- see the comment on
        // Role. CIAU can still check/uncheck anything before cadastrando.
        registerRoleCombo.valueProperty().addListener((obs, oldRole, newRole) -> {
            if (newRole == null) {
                return;
            }
            registerCafeteriaCheck.setSelected(newRole == Role.RH || newRole == Role.CIAU);
            registerAgendaCheck.setSelected(newRole == Role.FINANCEIRO || newRole == Role.CIAU);
            registerTechnicalSupportCheck.setSelected(newRole == Role.CIAU);
        });

        loadUsers();
    }

    private void loadUsers() {
        users.setAll(userService.findAll());
    }

    /** Fills the side panel with the clicked user's info and clears the credential fields. */
    private void loadSelection(User user) {
        selectedUser = user;
        newUsernameField.clear();
        newPasswordField.clear();
        confirmPasswordField.clear();
        statusLabel.setText("");
        selectedUserLabel.setText(user == null ? ""
                : "Editando: " + user.getUsername() + " (" + user.getRole() + ")");

        // Permissions panel only makes sense with a user selected -- reflects
        // that account's CURRENT access, and is disabled otherwise so
        // "SALVAR PERMISSÕES" can't be clicked with nothing selected.
        boolean hasSelection = user != null;
        permissionsCafeteriaCheck.setSelected(hasSelection && user.isAccessCafeteria());
        permissionsAgendaCheck.setSelected(hasSelection && user.isAccessAgendaTelefonica());
        permissionsTechnicalSupportCheck.setSelected(hasSelection && user.isAccessTechnicalSupport());
        permissionsCafeteriaCheck.setDisable(!hasSelection);
        permissionsAgendaCheck.setDisable(!hasSelection);
        permissionsTechnicalSupportCheck.setDisable(!hasSelection);
        permissionsStatusLabel.setText("");
    }

    @FXML
    private void onUpdateCredentials() {
        if (selectedUser == null) {
            AlertUtil.warning("Seleção necessária", "Selecione um usuário na tabela.");
            return;
        }

        String newUsername = newUsernameField.getText();
        String newPassword = newPasswordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        boolean changingUsername = newUsername != null && !newUsername.isBlank();
        boolean changingPassword = newPassword != null && !newPassword.isBlank();

        if (!changingUsername && !changingPassword) {
            AlertUtil.warning("Nada para atualizar", "Informe um novo nome de usuário e/ou uma nova senha.");
            return;
        }
        if (changingPassword && !newPassword.equals(confirmPassword)) {
            AlertUtil.warning("Senhas não conferem", "A confirmação de senha precisa ser igual à nova senha.");
            return;
        }

        try {
            userService.updateCredentials(selectedUser.getId(), newUsername, newPassword);
            loadUsers();
            statusLabel.setText("Credenciais atualizadas com sucesso.");
            newUsernameField.clear();
            newPasswordField.clear();
            confirmPasswordField.clear();
        } catch (Exception e) {
            log.warn("Failed to update credentials for userId={}: {}", selectedUser.getId(), e.getMessage(), e);
            AlertUtil.error("Erro ao atualizar credenciais", e.getMessage());
        }
    }

    @FXML
    private void onRegisterUser() {
        String username = registerUsernameField.getText();
        String displayName = registerDisplayNameField.getText();
        String password = registerPasswordField.getText();
        String confirmPassword = registerConfirmPasswordField.getText();
        Role role = registerRoleCombo.getValue();

        if (password != null && !password.equals(confirmPassword)) {
            AlertUtil.warning("Senhas não conferem", "A confirmação de senha precisa ser igual à senha.");
            return;
        }

        try {
            userService.register(username, displayName, password, role,
                    registerCafeteriaCheck.isSelected(), registerAgendaCheck.isSelected(),
                    registerTechnicalSupportCheck.isSelected());
            loadUsers();
            registerStatusLabel.setText("Usuário cadastrado com sucesso.");
            registerUsernameField.clear();
            registerDisplayNameField.clear();
            registerPasswordField.clear();
            registerConfirmPasswordField.clear();
            registerRoleCombo.setValue(null);
            registerCafeteriaCheck.setSelected(false);
            registerAgendaCheck.setSelected(false);
            registerTechnicalSupportCheck.setSelected(false);
        } catch (Exception e) {
            log.warn("Failed to register user: {}", e.getMessage(), e);
            AlertUtil.error("Erro ao cadastrar usuário", e.getMessage());
        }
    }

    @FXML
    private void onSavePermissions() {
        if (selectedUser == null) {
            AlertUtil.warning("Seleção necessária", "Selecione um usuário na tabela.");
            return;
        }
        try {
            userService.updateModuleAccess(selectedUser.getId(), permissionsCafeteriaCheck.isSelected(),
                    permissionsAgendaCheck.isSelected(), permissionsTechnicalSupportCheck.isSelected());
            loadUsers();
            permissionsStatusLabel.setText("Permissões atualizadas com sucesso.");
        } catch (Exception e) {
            log.warn("Failed to update module permissions for userId={}: {}", selectedUser.getId(), e.getMessage(), e);
            AlertUtil.error("Erro ao atualizar permissões", e.getMessage());
        }
    }

    @FXML
    private void onDeactivateUser() {
        User selected = userTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertUtil.warning("Seleção necessária", "Selecione um usuário na tabela.");
            return;
        }
        if (!selected.isActive()) {
            AlertUtil.warning("Usuário já inativo", "Este usuário já está inativo.");
            return;
        }
        // Guards against an admin locking themselves out: findByUsername (used
        // at login) only returns accounts with ativo = TRUE, so deactivating
        // your own account would block your own next login with no one else
        // necessarily around to undo it.
        User loggedInUser = SessionManager.getLoggedInUser();
        if (loggedInUser != null && loggedInUser.getId().equals(selected.getId())) {
            AlertUtil.warning("Não é possível inativar a própria conta",
                    "Você não pode inativar o usuário com o qual está logado agora. "
                            + "Peça para outro administrador CIAU fazer isso, se for realmente necessário.");
            return;
        }
        if (AlertUtil.confirm("Inativar usuário",
                "Inativar o usuário " + selected.getUsername()
                        + "? Ele não vai mais conseguir fazer login até ser reativado.")) {
            userService.deactivate(selected.getId());
            loadUsers();
        }
    }

    @FXML
    private void onActivateUser() {
        User selected = userTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertUtil.warning("Seleção necessária", "Selecione um usuário na tabela.");
            return;
        }
        if (selected.isActive()) {
            AlertUtil.warning("Usuário já ativo", "Este usuário já está ativo.");
            return;
        }
        userService.activate(selected.getId());
        loadUsers();
    }

    @FXML
    private void onBack() {
        App.backToHub();
    }
}
