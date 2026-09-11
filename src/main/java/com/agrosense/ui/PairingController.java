package com.agrosense.ui;

import com.agrosense.AgroSenseApp;
import com.agrosense.model.*;
import com.agrosense.pairing.PairingResult;
import com.agrosense.session.SessionManager;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.sql.SQLException;
import java.util.Collections;

public class PairingController {

    @FXML private TextField codeField;
    @FXML private TextField siteNameField;
    @FXML private ComboBox<UseCaseProfile> profileCombo;
    @FXML private Label feedbackLabel;
    @FXML private Label successLabel;
    @FXML private Button pairButton;
    @FXML private Label serverIpLabel;
    @FXML private Button copyIpButton;

    private String detectedIp = "";

    @FXML
    public void initialize() {
        profileCombo.setItems(FXCollections.observableArrayList(UseCaseProfile.values()));
        profileCombo.setValue(UseCaseProfile.HOME);
        detectedIp = detectLocalIp();
        serverIpLabel.setText(detectedIp);
    }

    /** Walks network interfaces to find the best site-local (LAN) IPv4 address. */
    private String detectLocalIp() {
        try {
            for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (!ni.isUp() || ni.isLoopback() || ni.isVirtual()) continue;
                for (InetAddress addr : Collections.list(ni.getInetAddresses())) {
                    if (addr.isSiteLocalAddress() && !addr.isLoopbackAddress()
                            && addr.getHostAddress().contains(".")) {
                        return addr.getHostAddress();
                    }
                }
            }
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "Unable to detect";
        }
    }

    @FXML
    private void onCopyIp() {
        if (detectedIp.isBlank()) return;
        Clipboard clipboard = Clipboard.getSystemClipboard();
        ClipboardContent content = new ClipboardContent();
        content.putString(detectedIp);
        clipboard.setContent(content);
        copyIpButton.setText("Copied!");
        copyIpButton.setDisable(true);
        new Thread(() -> {
            try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
            javafx.application.Platform.runLater(() -> {
                copyIpButton.setText("Copy");
                copyIpButton.setDisable(false);
            });
        }).start();
    }

    @FXML
    private void onPair() {
        String code     = codeField.getText();
        String siteName = siteNameField.getText().trim();
        UseCaseProfile profile = profileCombo.getValue();

        clearFeedback();

        if (code.isBlank() || siteName.isBlank() || profile == null) {
            showError("Please fill in all fields.");
            return;
        }

        try {
            int customerId = SessionManager.getInstance().getCurrentCustomer().getId();
            PairingResult result = AgroSenseApp.pairingService.pair(customerId, code, siteName, profile);

            if (result.isSuccess()) {
                showSuccess("✓ Device paired successfully! Site '" + siteName + "' is now active.");
                pairButton.setDisable(true);
            } else {
                showError(result.getFailureReason());
            }
        } catch (SQLException e) {
            showError("Database error: " + e.getMessage());
        }
    }

    @FXML
    private void goBack() {
        AgroSenseApp.navigateTo("main_layout");
    }

    private void showError(String msg) {
        feedbackLabel.setText("✗ " + msg);
        feedbackLabel.setVisible(true);
        feedbackLabel.setManaged(true);
    }

    private void showSuccess(String msg) {
        successLabel.setText(msg);
        successLabel.setVisible(true);
        successLabel.setManaged(true);
    }

    private void clearFeedback() {
        feedbackLabel.setVisible(false); feedbackLabel.setManaged(false);
        successLabel.setVisible(false);  successLabel.setManaged(false);
    }
}
