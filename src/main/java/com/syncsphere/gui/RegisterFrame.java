package com.syncsphere.gui;

import com.syncsphere.exception.UserAlreadyExistsException;
import com.syncsphere.service.AuthenticationService;
import com.syncsphere.util.ThemePreferences;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

/** Figma v2 register surface, retaining the real local-account workflow. */
public class RegisterFrame extends JFrame {
    private final AuthenticationService authenticationService = new AuthenticationService();
    private final JTextField usernameField = new JTextField();
    private final JPasswordField passwordField = new JPasswordField();
    private final JPasswordField confirmPasswordField = new JPasswordField();
    private final JButton registerButton = new DesignSystem.GlassButton("Generate Sphere Identity", DesignSystem.PRIMARY, DesignSystem.TEXT);
    private final JButton backButton = new DesignSystem.GlassButton("Load wallet key", new Color(255, 255, 255, 18), DesignSystem.ACCENT);
    private final JLabel statusLabel = new JLabel(" ", SwingConstants.CENTER);
    private boolean darkTheme = ThemePreferences.isDark();

    public RegisterFrame() {
        setTitle("SyncSphere - Generate Identity");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setMinimumSize(new Dimension(450, 680));
        setSize(570, 760);
        setLocationRelativeTo(null);
        setContentPane(buildContentPane());
        registerButton.addActionListener(e -> performRegistration());
        backButton.addActionListener(e -> { dispose(); new LoginFrame().setVisible(true); });
    }

    private JPanel buildContentPane() {
        DesignSystem.AmbientPanel root = new DesignSystem.AmbientPanel(darkTheme);
        root.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(28, 26, 28, 26);
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1;
        gbc.weighty = 1;
        DesignSystem.GlassPanel card = new DesignSystem.GlassPanel(darkTheme, 22);
        card.setLayout(new BorderLayout());
        card.setBorder(BorderFactory.createEmptyBorder(34, 40, 32, 40));
        card.setPreferredSize(new Dimension(480, 630));
        card.add(buildCardBody(), BorderLayout.CENTER);
        root.add(card, gbc);
        return root;
    }

    private JPanel buildCardBody() {
        DesignSystem.Palette colors = DesignSystem.palette(darkTheme);
        registerButton.setForeground(colors.text());
        backButton.setForeground(darkTheme ? DesignSystem.ACCENT : DesignSystem.LIGHT_TEXT);
        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        JLabel brand = new JLabel("◉  SyncSphere", SwingConstants.CENTER);
        brand.setAlignmentX(Component.CENTER_ALIGNMENT);
        brand.setForeground(colors.text());
        brand.setFont(DesignSystem.font(Font.BOLD, 28));
        JLabel subtitle = label("Generate a new decentralized identity and cryptographic keys", 13, colors.muted());
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        subtitle.setHorizontalAlignment(SwingConstants.CENTER);
        body.add(brand);
        body.add(Box.createVerticalStrut(8));
        body.add(subtitle);
        body.add(Box.createVerticalStrut(28));
        body.add(fieldGroup("DISPLAY ALIAS", usernameField));
        body.add(Box.createVerticalStrut(14));
        body.add(fieldGroup("MASTER DECRYPTION KEY", passwordField));
        body.add(Box.createVerticalStrut(10));
        JLabel entropy = label("KEY ENTROPY STRENGTH", 10, colors.muted());
        entropy.setFont(DesignSystem.font(Font.BOLD, 10));
        JLabel entropyValue = label("Validated by existing account policy", 10, DesignSystem.SUCCESS);
        JPanel entropyRow = new JPanel(new BorderLayout());
        entropyRow.setOpaque(false);
        entropyRow.add(entropy, BorderLayout.WEST);
        entropyRow.add(entropyValue, BorderLayout.EAST);
        entropyRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 18));
        body.add(entropyRow);
        body.add(Box.createVerticalStrut(10));
        body.add(fieldGroup("CONFIRM MASTER KEY", confirmPasswordField));
        body.add(Box.createVerticalStrut(10));
        statusLabel.setForeground(colors.muted());
        statusLabel.setFont(DesignSystem.font(Font.PLAIN, 12));
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        body.add(statusLabel);
        body.add(Box.createVerticalStrut(10));
        body.add(fullWidth(registerButton, 46));
        body.add(Box.createVerticalStrut(14));
        JLabel existing = label("Already initialized?", 12, colors.muted());
        existing.setAlignmentX(Component.CENTER_ALIGNMENT);
        body.add(existing);
        body.add(Box.createVerticalStrut(6));
        body.add(fullWidth(backButton, 38));
        return body;
    }

    private JPanel fieldGroup(String labelText, JTextField field) {
        DesignSystem.Palette colors = DesignSystem.palette(darkTheme);
        JPanel group = new JPanel(new BorderLayout(0, 7));
        group.setOpaque(false);
        JLabel label = label(labelText, 11, colors.muted());
        label.setFont(DesignSystem.font(Font.BOLD, 11));
        DesignSystem.styleField(field, darkTheme);
        field.setPreferredSize(new Dimension(360, 44));
        group.add(label, BorderLayout.NORTH);
        group.add(field, BorderLayout.CENTER);
        group.setMaximumSize(new Dimension(Integer.MAX_VALUE, 69));
        return group;
    }

    private JPanel fullWidth(JButton button, int height) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        button.setPreferredSize(new Dimension(360, height));
        panel.add(button, BorderLayout.CENTER);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
        return panel;
    }

    private JLabel label(String text, int size, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(DesignSystem.font(Font.PLAIN, size));
        label.setForeground(color);
        return label;
    }

    private void performRegistration() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());
        String confirmation = new String(confirmPasswordField.getPassword());
        if (username.isBlank() || password.isBlank() || confirmation.isBlank()) { showStatus("Please complete all identity fields.", DesignSystem.DANGER); return; }
        if (!password.equals(confirmation)) { showStatus("The master keys do not match.", DesignSystem.DANGER); return; }
        registerButton.setEnabled(false);
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() throws Exception { authenticationService.register(username, password); return null; }
            @Override protected void done() {
                registerButton.setEnabled(true);
                try {
                    get();
                    showStatus("Identity generated. Return to sign in.", DesignSystem.SUCCESS);
                    dispose();
                    new LoginFrame().setVisible(true);
                } catch (Exception exception) {
                    Throwable cause = exception.getCause();
                    showStatus(cause instanceof UserAlreadyExistsException || cause instanceof IllegalArgumentException
                            ? cause.getMessage() : "Identity generation could not be completed.", DesignSystem.DANGER);
                }
            }
        }.execute();
    }

    private void showStatus(String message, Color color) { statusLabel.setForeground(color); statusLabel.setText(message); }
}
