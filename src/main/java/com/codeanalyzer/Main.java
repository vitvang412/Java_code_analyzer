package com.codeanalyzer;

import com.codeanalyzer.database.DatabaseConnection;
import com.codeanalyzer.database.DatabaseInitializer;
import com.codeanalyzer.ui.MainFrame;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        // 1. Setup Look & Feel (Nimbus nếu có – gọn + hiện đại hơn system LAF).
        try {
            boolean found = false;
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    found = true;
                    break;
                }
            }
            if (!found) {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            }
        } catch (Exception ignored) {}

        // 2. Init DB
        DatabaseInitializer.initialize();

        // 3. Launch UI
        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame();
            frame.setVisible(true);
        });

        // 4. Cleanup on close
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            DatabaseConnection.getInstance().close();
        }));
    }
}
