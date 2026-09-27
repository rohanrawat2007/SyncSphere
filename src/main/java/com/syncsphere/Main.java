package com.syncsphere;

import com.syncsphere.gui.LoginFrame;

import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                new LoginFrame().setVisible(true);
            } catch (Exception e) {
                System.err.println("Failed to start SyncSphere: " + e.getMessage());
                e.printStackTrace(System.err);
            }
        });
    }
}
