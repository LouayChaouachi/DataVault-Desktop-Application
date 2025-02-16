package com.example.lol;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class ClientController {
    private Stage stage;
    private Scene scene;
    private Parent root;

    @FXML private TableView<Client> tableClient;
    @FXML private TableColumn<Client, Integer> idCl;
    @FXML private TableColumn<Client, String> nomCl;
    @FXML private TableColumn<Client, String> prenomCl;
    @FXML private TableColumn<Client, String> adresseCl;
    @FXML private TableColumn<Client, String> emailCl;
    @FXML private TableColumn<Client, String> telCl;
    @FXML private TableColumn<Client, String> agenceCl;
    @FXML private TableColumn<Client, Void> optionCl;

    private ObservableList<Client> clientList = FXCollections.observableArrayList();
    private Map<Integer, String> agences = new HashMap<>();

    @FXML
    public void initialize() {
        loadAgences();
        loadClients();
        setupColumns();
        addOptionsColumn();
    }

    private void loadAgences() {
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT agence_id, nom_agence FROM AgenceBancaires")) {
            agences.clear();
            while (rs.next()) agences.put(rs.getInt(1), rs.getString(2));
        } catch (SQLException e) { showError("Error loading agencies: " + e.getMessage()); }
    }

    private void loadClients() {
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM Clients")) {
            clientList.clear();
            while (rs.next()) clientList.add(new Client(
                    rs.getInt("client_id"),
                    rs.getString("nom"),
                    rs.getString("prenom"),
                    rs.getString("adresse"),
                    rs.getString("email"),
                    rs.getString("numero_telephone"),
                    rs.getInt("agence_id")
            ));
        } catch (SQLException e) { showError("Error loading clients: " + e.getMessage()); }
    }

    private void setupColumns() {
        idCl.setCellValueFactory(new PropertyValueFactory<>("id"));
        nomCl.setCellValueFactory(new PropertyValueFactory<>("nom"));
        prenomCl.setCellValueFactory(new PropertyValueFactory<>("prenom"));
        adresseCl.setCellValueFactory(new PropertyValueFactory<>("adresse"));
        emailCl.setCellValueFactory(new PropertyValueFactory<>("mail"));
        telCl.setCellValueFactory(new PropertyValueFactory<>("tel"));
        agenceCl.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(
                agences.get(cellData.getValue().getAgenceId())
        ));
        tableClient.setItems(clientList);
    }

    private void addOptionsColumn() {
        optionCl.setCellFactory(param -> new TableCell<>() {
            private final Button edit = new Button("Edit");
            private final Button delete = new Button("Delete");
            {
                edit.setOnAction(e -> handleEdit(getTableRow().getItem()));
                delete.setOnAction(e -> handleDelete(getTableRow().getItem()));
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : new javafx.scene.layout.HBox(5, edit, delete));
            }
        });
    }

    private void handleEdit(Client client) {
        Dialog<Client> dialog = new Dialog<>();
        dialog.setTitle("Edit Client");
        ChoiceBox<String> agencyChoice = new ChoiceBox<>(FXCollections.observableArrayList(agences.values()));
        TextField nomField = new TextField(client.getNom());
        TextField prenomField = new TextField(client.getPrenom());
        TextField adresseField = new TextField(client.getAdresse());
        TextField emailField = new TextField(client.getMail());
        TextField telField = new TextField(client.getTel());
        agencyChoice.setValue(agences.get(client.getAgenceId()));

        GridPane grid = new GridPane();
        grid.addRow(0, new Label("Nom:"), nomField);
        grid.addRow(1, new Label("Prenom:"), prenomField);
        grid.addRow(2, new Label("Adresse:"), adresseField);
        grid.addRow(3, new Label("Email:"), emailField);
        grid.addRow(4, new Label("Tel:"), telField);
        grid.addRow(5, new Label("Agence:"), agencyChoice);
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                client.setNom(nomField.getText());
                client.setPrenom(prenomField.getText());
                client.setAdresse(adresseField.getText());
                client.setMail(emailField.getText());
                client.setTel(telField.getText());
                agences.forEach((k,v) -> { if(v.equals(agencyChoice.getValue())) client.setAgenceId(k); });
                updateClient(client);
                return client;
            }
            return null;
        });
        dialog.showAndWait();
    }

    private void updateClient(Client client) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                     "UPDATE Clients SET nom=?, prenom=?, adresse=?, email=?, numero_telephone=?, agence_id=? WHERE client_id=?")) {
            pstmt.setString(1, client.getNom());
            pstmt.setString(2, client.getPrenom());
            pstmt.setString(3, client.getAdresse());
            pstmt.setString(4, client.getMail());
            pstmt.setString(5, client.getTel());
            pstmt.setInt(6, client.getAgenceId());
            pstmt.setInt(7, client.getId());
            pstmt.executeUpdate();
            tableClient.refresh();
        } catch (SQLException e) { showError("Update failed: " + e.getMessage()); }
    }

    private void handleDelete(Client client) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement("DELETE FROM Clients WHERE client_id=?")) {
            pstmt.setInt(1, client.getId());
            pstmt.executeUpdate();
            clientList.remove(client);
        } catch (SQLException e) { showError("Delete failed: " + e.getMessage()); }
    }

    @FXML
    private void handleAddClient() {
        Dialog<Client> dialog = new Dialog<>();
        dialog.setTitle("New Client");

        ChoiceBox<String> agencyChoice = new ChoiceBox<>(FXCollections.observableArrayList(agences.values()));
        TextField nomField = new TextField();
        TextField prenomField = new TextField();
        TextField adresseField = new TextField();
        TextField emailField = new TextField();
        TextField telField = new TextField();

        GridPane grid = new GridPane();
        grid.addRow(0, new Label("Nom:"), nomField);
        grid.addRow(1, new Label("Prenom:"), prenomField);
        grid.addRow(2, new Label("Adresse:"), adresseField);
        grid.addRow(3, new Label("Email:"), emailField);
        grid.addRow(4, new Label("Tel:"), telField);
        grid.addRow(5, new Label("Agence:"), agencyChoice);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                Client newClient = new Client(
                        0, // L'ID sera généré automatiquement
                        nomField.getText(),
                        prenomField.getText(),
                        adresseField.getText(),
                        emailField.getText(),
                        telField.getText(),
                        0
                );
                agences.forEach((k, v) -> {
                    if (v.equals(agencyChoice.getValue())) newClient.setAgenceId(k);
                });
                return newClient;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(newClient -> {
            try (Connection conn = DatabaseConnection.getConnection()) {
                // Étape 1 : Générer un nouvel ID
                String idQuery = "SELECT NVL(MAX(client_id), 0) + 1 AS new_id FROM Clients";
                int newId = 0;
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery(idQuery)) {
                    if (rs.next()) {
                        newId = rs.getInt("new_id");
                    }
                }

                // Étape 2 : Insérer le client avec le nouvel ID
                String insertQuery = "INSERT INTO Clients (client_id, nom, prenom, adresse, email, numero_telephone, agence_id) VALUES (?, ?, ?, ?, ?, ?, ?)";
                try (PreparedStatement pstmt = conn.prepareStatement(insertQuery)) {
                    pstmt.setInt(1, newId);
                    pstmt.setString(2, newClient.getNom());
                    pstmt.setString(3, newClient.getPrenom());
                    pstmt.setString(4, newClient.getAdresse());
                    pstmt.setString(5, newClient.getMail());
                    pstmt.setString(6, newClient.getTel());
                    pstmt.setInt(7, newClient.getAgenceId());

                    pstmt.executeUpdate();
                    newClient.setId(newId); // Mettre à jour l'ID dans l'objet Java
                    clientList.add(newClient);
                }
            } catch (SQLException e) {
                showError("Insert failed: " + e.getMessage());
            }
        });
    }

    private void showError(String message) {
        new Alert(Alert.AlertType.ERROR, message, ButtonType.OK).showAndWait();
    }
    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type, message, ButtonType.OK);
        alert.setHeaderText(title);
        alert.showAndWait();
    }
    @FXML
    public void switchToClients(ActionEvent event) {
        try {
            root = FXMLLoader.load(getClass().getResource("Clients.fxml"));
            stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            scene = new Scene(root);
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void switchToAgences(ActionEvent event) {
        try {
            root = FXMLLoader.load(getClass().getResource("Agences.fxml"));
            stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            scene = new Scene(root);
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    @FXML
    public void switchToComptes(ActionEvent event) {
        try {
            root = FXMLLoader.load(getClass().getResource("Comptes.fxml"));
            stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            scene = new Scene(root);
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}