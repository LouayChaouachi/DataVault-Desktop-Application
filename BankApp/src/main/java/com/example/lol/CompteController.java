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
import javafx.util.Callback;

import java.io.IOException;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;

public class CompteController {

    @FXML private TableView<Compte> tableCompte;
    @FXML private TableColumn<Compte, Integer> idCo;
    @FXML private TableColumn<Compte, Double> soldeCo;
    @FXML private TableColumn<Compte, String> typeCo;
    @FXML private TableColumn<Compte, Date> dateCo;
    @FXML private TableColumn<Compte, Integer> clientIdCo;
    @FXML private TableColumn<Compte, Void> optionCo;

    private ObservableList<Compte> compteList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        idCo.setCellValueFactory(new PropertyValueFactory<>("id"));
        soldeCo.setCellValueFactory(new PropertyValueFactory<>("solde"));
        typeCo.setCellValueFactory(new PropertyValueFactory<>("type"));
        dateCo.setCellValueFactory(new PropertyValueFactory<>("date_ouverture"));
        clientIdCo.setCellValueFactory(new PropertyValueFactory<>("clientId"));
        loadDataFromDatabase();
        addOptionsColumn();
    }

    private void loadDataFromDatabase() {
        String query = "SELECT * FROM Comptes";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            compteList.clear();
            while (rs.next()) {
                compteList.add(new Compte(
                        rs.getInt("compte_id"),
                        rs.getDouble("solde"),
                        rs.getString("type_compte"),
                        rs.getDate("date_ouverture"),
                        rs.getInt("client_id")
                ));
            }
            tableCompte.setItems(compteList);
        } catch (SQLException e) {
            showAlert("Database Error", "Error loading data: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void addOptionsColumn() {
        Callback<TableColumn<Compte, Void>, TableCell<Compte, Void>> cellFactory = param -> new TableCell<>() {
            private final Button deposit = new Button("Deposit");
            private final Button withdraw = new Button("Withdraw");
            private final Button transfer = new Button("Transfer");
            private final Button delete = new Button("Delete");

            {
                deposit.setOnAction(e -> handleDeposit(getTableRow().getItem()));
                withdraw.setOnAction(e -> handleWithdrawal(getTableRow().getItem()));
                transfer.setOnAction(e -> handleTransfer(getTableRow().getItem()));
                delete.setOnAction(e -> handleDelete(getTableRow().getItem()));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                } else {
                    setGraphic(new HBox(5, deposit, withdraw, transfer, delete));
                }
            }
        };
        optionCo.setCellFactory(cellFactory);
    }

    private void handleDeposit(Compte compte) {
        Dialog<Double> dialog = createAmountDialog("Deposit", "Enter deposit amount:");
        dialog.showAndWait().ifPresent(amount -> {
            compte.setSolde(compte.getSolde() + amount);
            updateSolde(compte);
            tableCompte.refresh();
        });
    }

    private void handleWithdrawal(Compte compte) {
        Dialog<Double> dialog = createAmountDialog("Withdraw", "Enter withdrawal amount:");
        dialog.showAndWait().ifPresent(amount -> {
            if (compte.getSolde() >= amount) {
                compte.setSolde(compte.getSolde() - amount);
                updateSolde(compte);
                tableCompte.refresh();
            } else {
                showAlert("Error", "Insufficient funds", Alert.AlertType.ERROR);
            }
        });
    }

    private void handleTransfer(Compte source) {
        Dialog dialog = new Dialog<>();
        dialog.setTitle("Transfer");
        dialog.setHeaderText("Transfer from Account #" + source.getId());

        TextField recipientField = new TextField();
        TextField amountField = new TextField();

        GridPane grid = new GridPane();
        grid.add(new Label("Recipient ID:"), 0, 0);
        grid.add(recipientField, 1, 0);
        grid.add(new Label("Amount:"), 0, 1);
        grid.add(amountField, 1, 1);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                try {
                    int recipientId = Integer.parseInt(recipientField.getText());
                    double amount = Double.parseDouble(amountField.getText());
                    return new TransferData(recipientId, amount);
                } catch (NumberFormatException e) {
                    showAlert("Invalid Input", "Please enter valid numbers", Alert.AlertType.ERROR);
                }
            }
            return null;
        });

        dialog.showAndWait().ifPresent(data -> {
            if (data instanceof TransferData) { // Explicitly check and cast
                TransferData transferData = (TransferData) data;
                try (Connection conn = DatabaseConnection.getConnection()) {
                    conn.setAutoCommit(false);

                    // Débit du compte source
                    PreparedStatement debit = conn.prepareStatement(
                            "UPDATE Comptes SET solde = solde - ? WHERE compte_id = ?");
                    debit.setDouble(1, transferData.amount);
                    debit.setInt(2, source.getId());
                    debit.executeUpdate();

                    // Crédit du compte destination
                    PreparedStatement credit = conn.prepareStatement(
                            "UPDATE Comptes SET solde = solde + ? WHERE compte_id = ?");
                    credit.setDouble(1, transferData.amount);
                    credit.setInt(2, transferData.recipientId);
                    credit.executeUpdate();

                    conn.commit();

                    source.setSolde(source.getSolde() - transferData.amount);
                    loadDataFromDatabase();
                } catch (SQLException e) {
                    showAlert("Transfer Failed", "Error: " + e.getMessage(), Alert.AlertType.ERROR);
                }
            }
        });
    }

    private void handleDelete(Compte compte) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Are you sure you want to delete this account?", ButtonType.YES, ButtonType.NO);
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                String sql = "DELETE FROM Comptes WHERE compte_id = ?";
                try (Connection conn = DatabaseConnection.getConnection();
                     PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setInt(1, compte.getId());
                    pstmt.executeUpdate();
                    compteList.remove(compte);
                } catch (SQLException e) {
                    showAlert("Database Error", "Delete failed: " + e.getMessage(), Alert.AlertType.ERROR);
                }
            }
        });
    }

    private Dialog<Double> createAmountDialog(String title, String header) {
        Dialog<Double> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.setHeaderText(header);
        TextField amountField = new TextField();
        dialog.getDialogPane().setContent(new GridPane() {{
            add(new Label("Amount:"), 0, 0);
            add(amountField, 1, 0);
        }});
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                try {
                    return Double.parseDouble(amountField.getText());
                } catch (NumberFormatException e) {
                    showAlert("Invalid Input", "Please enter a valid amount", Alert.AlertType.ERROR);
                }
            }
            return null;
        });
        return dialog;
    }

    private void updateSolde(Compte compte) {
        String sql = "UPDATE Comptes SET solde = ? WHERE compte_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDouble(1, compte.getSolde());
            pstmt.setInt(2, compte.getId());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            showAlert("Database Error", "Update failed: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void handleAddCompte() {
        Dialog<Compte> dialog = new Dialog<>();
        dialog.setTitle("New Account");

        TextField soldeField = new TextField();
        TextField typeField = new TextField();
        TextField dateField = new TextField();
        TextField clientIdField = new TextField();

        GridPane grid = new GridPane();
        grid.addRow(0, new Label("Balance:"), soldeField);
        grid.addRow(1, new Label("Type:"), typeField);
        grid.addRow(2, new Label("Date (yyyy-MM-dd):"), dateField);
        grid.addRow(3, new Label("Client ID:"), clientIdField);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                try {
                    double solde = Double.parseDouble(soldeField.getText());
                    String type = typeField.getText();
                    Date date = new SimpleDateFormat("yyyy-MM-dd").parse(dateField.getText());
                    int clientId = Integer.parseInt(clientIdField.getText());

                    return new Compte(
                            0, // L'ID sera généré automatiquement
                            solde,
                            type,
                            date,
                            clientId
                    );
                } catch (Exception e) {
                    showError("Invalid input: " + e.getMessage());
                    return null;
                }
            }
            return null;
        });

        dialog.showAndWait().ifPresent(newAccount -> {
            try (Connection conn = DatabaseConnection.getConnection()) {
                // Étape 1 : Générer un nouvel ID
                String idQuery = "SELECT NVL(MAX(compte_id), 0) + 1 AS new_id FROM Comptes";
                int newId = 0;
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery(idQuery)) {
                    if (rs.next()) {
                        newId = rs.getInt("new_id");
                    }
                }

                // Étape 2 : Insérer le compte avec le nouvel ID
                String insertQuery = "INSERT INTO Comptes (compte_id, solde, type_compte, date_ouverture, client_id) VALUES (?, ?, ?, ?, ?)";
                try (PreparedStatement pstmt = conn.prepareStatement(insertQuery)) {
                    pstmt.setInt(1, newId);
                    pstmt.setDouble(2, newAccount.getSolde());
                    pstmt.setString(3, newAccount.getType());
                    pstmt.setDate(4, new java.sql.Date(newAccount.getDate_ouverture().getTime()));
                    pstmt.setInt(5, newAccount.getClientId());

                    pstmt.executeUpdate();
                    newAccount.setId(newId); // Mettre à jour l'ID dans l'objet Java
                    loadDataFromDatabase();
                }
            } catch (SQLException e) {
                showError("Insert failed: " + e.getMessage());
            }
        });
    }

    @FXML
    private void switchToClients(ActionEvent event) throws IOException {
        switchScene(event, "Clients.fxml");
    }

    @FXML
    private void switchToAgences(ActionEvent event) throws IOException {
        switchScene(event, "Agences.fxml");
    }

    private void switchScene(ActionEvent event, String fxml) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource(fxml));
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root));
        stage.show();
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type, message, ButtonType.OK);
        alert.setHeaderText(title);
        alert.showAndWait();
    }
    private void showError(String message) {
        new Alert(Alert.AlertType.ERROR, message, ButtonType.OK).showAndWait();
    }
    private static class TransferData {
        final int recipientId;
        final double amount;

        TransferData(int recipientId, double amount) {
            this.recipientId = recipientId;
            this.amount = amount;
        }
    }
}