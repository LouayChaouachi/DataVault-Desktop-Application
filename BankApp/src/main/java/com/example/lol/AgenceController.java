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
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.*;

public class AgenceController {
    private Stage stage;
    private Scene scene;
    private Parent root;

    @FXML
    private TableView<Agence> tableAgence;
    @FXML
    private TableColumn<Agence, Integer> idAg;
    @FXML
    private TableColumn<Agence, String> nomAg;
    @FXML
    private TableColumn<Agence, String> adresseAg;
    @FXML
    private TableColumn<Agence, String> emailAg;
    @FXML
    private TableColumn<Agence, Integer> numAgenceAg; // nouvelle colonne
    @FXML
    private TableColumn<Agence, String> responsableAg; // nouvelle colonne
    @FXML
    private TableColumn<Agence, Void> optionAg; // colonne pour les boutons Edit/Delete

    private ObservableList<Agence> agenceList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        loadAgences();

        // Associer les propriétés de l'objet Agence aux colonnes du tableau
        idAg.setCellValueFactory(new PropertyValueFactory<>("id"));
        nomAg.setCellValueFactory(new PropertyValueFactory<>("nom"));
        adresseAg.setCellValueFactory(new PropertyValueFactory<>("adresse"));
        emailAg.setCellValueFactory(new PropertyValueFactory<>("email"));
        numAgenceAg.setCellValueFactory(new PropertyValueFactory<>("numAgence"));
        responsableAg.setCellValueFactory(new PropertyValueFactory<>("responsable"));

        tableAgence.setItems(agenceList);

        // Ajout des boutons Edit/Delete dans chaque ligne du tableau
        addOptionsColumn();
    }

    private void loadAgences() {
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM AgenceBancaires")) {
            agenceList.clear();
            while (rs.next()) {
                agenceList.add(new Agence(
                        rs.getInt("agence_id"),
                        rs.getString("nom_agence"),
                        rs.getString("adresse"),
                        rs.getString("email"),
                        rs.getInt("num_agence"),
                        rs.getString("responsable")
                ));
            }
        } catch (SQLException e) {
            showError("Error loading agencies: " + e.getMessage());
        }
    }

    // Ajout de la colonne contenant les boutons Edit et Delete
    private void addOptionsColumn() {
        optionAg.setCellFactory(param -> new TableCell<>() {
            private final Button edit = new Button("Edit");
            private final Button delete = new Button("Delete");
            {
                edit.setOnAction(e -> {
                    // Récupérer l'agence de la ligne courante
                    Agence agence = getTableView().getItems().get(getIndex());
                    handleEdit(agence);
                });
                delete.setOnAction(e -> {
                    Agence agence = getTableView().getItems().get(getIndex());
                    handleDelete(agence);
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    HBox buttons = new HBox(5, edit, delete);
                    setGraphic(buttons);
                }
            }
        });
    }

    // Méthode d'édition d'une agence
    private void handleEdit(Agence agence) {
        Dialog<Agence> dialog = new Dialog<>();
        dialog.setTitle("Edit Agence");

        // Création des champs de saisie pré-remplis avec les données de l'agence
        TextField nomField = new TextField(agence.getNom());
        TextField adresseField = new TextField(agence.getAdresse());
        TextField emailField = new TextField(agence.getEmail());
        TextField numAgenceField = new TextField(String.valueOf(agence.getNumAgence()));
        TextField responsableField = new TextField(agence.getResponsable());

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.add(new Label("Nom:"), 0, 0);
        grid.add(nomField, 1, 0);
        grid.add(new Label("Adresse:"), 0, 1);
        grid.add(adresseField, 1, 1);
        grid.add(new Label("Email:"), 0, 2);
        grid.add(emailField, 1, 2);
        grid.add(new Label("Numéro Agence:"), 0, 3);
        grid.add(numAgenceField, 1, 3);
        grid.add(new Label("Responsable:"), 0, 4);
        grid.add(responsableField, 1, 4);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                agence.setNom(nomField.getText());
                agence.setAdresse(adresseField.getText());
                agence.setEmail(emailField.getText());
                try {
                    agence.setNumAgence(Integer.parseInt(numAgenceField.getText()));
                } catch (NumberFormatException e) {
                    showError("Invalid number for Numéro Agence");
                    return null;
                }
                agence.setResponsable(responsableField.getText());
                updateAgence(agence);
                return agence;
            }
            return null;
        });
        dialog.showAndWait();
    }

    // Mise à jour d'une agence dans la base de données
    private void updateAgence(Agence agence) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                     "UPDATE AgenceBancaires SET nom_agence=?, adresse=?, email=?, num_agence=?, responsable=? WHERE agence_id=?"
             )) {
            pstmt.setString(1, agence.getNom());
            pstmt.setString(2, agence.getAdresse());
            pstmt.setString(3, agence.getEmail());
            pstmt.setInt(4, agence.getNumAgence());
            pstmt.setString(5, agence.getResponsable());
            pstmt.setInt(6, agence.getId());
            pstmt.executeUpdate();
            tableAgence.refresh();
        } catch (SQLException e) {
            showError("Update failed: " + e.getMessage());
        }
    }

    // Suppression d'une agence
    private void handleDelete(Agence agence) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Agence");
        alert.setHeaderText("Are you sure you want to delete this agence?");
        alert.setContentText("Agence: " + agence.getNom());
        if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(
                         "DELETE FROM AgenceBancaires WHERE agence_id=?"
                 )) {
                pstmt.setInt(1, agence.getId());
                pstmt.executeUpdate();
                agenceList.remove(agence);
            } catch (SQLException e) {
                showError("Delete failed: " + e.getMessage());
            }
        }
    }

    // Gestion de l'ajout d'une nouvelle agence via une boîte de dialogue
    @FXML
    private void handleAddAgence() {
        Dialog<Agence> dialog = new Dialog<>();
        dialog.setTitle("New Agence");

        TextField nomField = new TextField();
        TextField adresseField = new TextField();
        TextField emailField = new TextField();
        TextField numAgenceField = new TextField();
        TextField responsableField = new TextField();

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.add(new Label("Nom:"), 0, 0);
        grid.add(nomField, 1, 0);
        grid.add(new Label("Adresse:"), 0, 1);
        grid.add(adresseField, 1, 1);
        grid.add(new Label("Email:"), 0, 2);
        grid.add(emailField, 1, 2);
        grid.add(new Label("Numéro Agence:"), 0, 3);
        grid.add(numAgenceField, 1, 3);
        grid.add(new Label("Responsable:"), 0, 4);
        grid.add(responsableField, 1, 4);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                try {
                    int numAgence = Integer.parseInt(numAgenceField.getText());
                    return new Agence(
                            0, // L'ID sera généré automatiquement
                            nomField.getText(),
                            adresseField.getText(),
                            emailField.getText(),
                            numAgence,
                            responsableField.getText()
                    );
                } catch (NumberFormatException e) {
                    showError("Invalid input for numerical fields.");
                    return null;
                }
            }
            return null;
        });

        dialog.showAndWait().ifPresent(newAgence -> {
            try (Connection conn = DatabaseConnection.getConnection()) {
                // Étape 1 : Générer un nouvel ID
                String idQuery = "SELECT NVL(MAX(agence_id), 0) + 1 AS new_id FROM AgenceBancaires";
                int newId = 0;
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery(idQuery)) {
                    if (rs.next()) {
                        newId = rs.getInt("new_id");
                    }
                }

                // Étape 2 : Insérer l'agence avec le nouvel ID
                String insertQuery = "INSERT INTO AgenceBancaires (agence_id, nom_agence, adresse, email, num_agence, responsable) VALUES (?, ?, ?, ?, ?, ?)";
                try (PreparedStatement pstmt = conn.prepareStatement(insertQuery)) {
                    pstmt.setInt(1, newId);
                    pstmt.setString(2, newAgence.getNom());
                    pstmt.setString(3, newAgence.getAdresse());
                    pstmt.setString(4, newAgence.getEmail());
                    pstmt.setInt(5, newAgence.getNumAgence());
                    pstmt.setString(6, newAgence.getResponsable());

                    pstmt.executeUpdate();
                    newAgence.setId(newId); // Mettre à jour l'ID dans l'objet Java
                    agenceList.add(newAgence);
                }
            } catch (SQLException e) {
                showError("Insert failed: " + e.getMessage());
            }
        });
    }
    private void showError(String message) {
        new Alert(Alert.AlertType.ERROR, message, ButtonType.OK).showAndWait();
    }

    // Méthodes de navigation entre les vues
    @FXML
    private void switchToClients(ActionEvent event) throws IOException {
        switchScene(event, "Clients.fxml");
    }

    @FXML
    private void switchToAgences(ActionEvent event) throws IOException {
        switchScene(event, "Agences.fxml");
    }

    @FXML
    private void switchToComptes(ActionEvent event) throws IOException {
        switchScene(event, "Comptes.fxml");
    }

    private void switchScene(ActionEvent event, String fxml) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource(fxml));
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root));
        stage.show();
    }
}
