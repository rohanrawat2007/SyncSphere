package com.syncsphere.gui;

import com.syncsphere.exception.InvalidLoginException;
import com.syncsphere.model.User;
import com.syncsphere.service.AuthenticationService;
import com.syncsphere.service.GoogleOAuthService;
import com.syncsphere.service.RemoteApiClient;
import com.syncsphere.util.ThemePreferences;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.IOException;

/** Figma v2 login surface, backed by the existing authentication services. */
public class LoginFrame extends JFrame {
    private final AuthenticationService authenticationService = new AuthenticationService();
    private final GoogleOAuthService googleOAuthService = new GoogleOAuthService();
    private final JTextField usernameField = new JTextField();
    private final JPasswordField passwordField = new JPasswordField();
    private final JButton loginButton = new DesignSystem.GlassButton("Access Sphere", DesignSystem.PRIMARY, DesignSystem.TEXT);
    private final JButton googleButton = new DesignSystem.GlassButton("Continue with Google", new Color(255, 255, 255, 28), DesignSystem.TEXT);
    private final JButton registerButton = new DesignSystem.GlassButton("Generate identity", new Color(255, 255, 255, 18), DesignSystem.ACCENT);
    private final JButton themeButton = new JButton("Theme");
    private final JLabel statusLabel = new JLabel(" ", SwingConstants.CENTER);
    private boolean darkTheme = ThemePreferences.isDark();

    public LoginFrame() {
        setTitle("SyncSphere");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(420, 620));
        setSize(560, 720);
        setLocationRelativeTo(null);
        setContentPane(buildContentPane());
        loginButton.addActionListener(e -> performLogin());
        googleButton.addActionListener(e -> handleGoogleLogin());
        registerButton.addActionListener(e -> { dispose(); new RegisterFrame().setVisible(true); });
        themeButton.addActionListener(e -> toggleTheme());
        usernameField.addActionListener(e -> performLogin());
        passwordField.addActionListener(e -> performLogin());
        SwingUtilities.invokeLater(this::ensureApiConfiguration);
    }

    private JPanel buildContentPane() {
        DesignSystem.AmbientPanel root = new DesignSystem.AmbientPanel(darkTheme);
        root.setLayout(new GridBagLayout());
        GridBagConstraints rootGbc = new GridBagConstraints();
        rootGbc.insets = new Insets(28, 26, 28, 26);
        rootGbc.fill = GridBagConstraints.BOTH;
        rootGbc.weightx = 1;
        rootGbc.weighty = 1;

        DesignSystem.GlassPanel card = new DesignSystem.GlassPanel(darkTheme, 22);
        card.setLayout(new BorderLayout());
        card.setPreferredSize(new Dimension(440, 540));
        card.setBorder(BorderFactory.createEmptyBorder(34, 40, 30, 40));
        card.add(buildCardBody(), BorderLayout.CENTER);
        root.add(card, rootGbc);
        return root;
    }

    private JPanel buildCardBody() {
        DesignSystem.Palette colors = DesignSystem.palette(darkTheme);
        loginButton.setForeground(colors.text());
        googleButton.setForeground(colors.text());
        registerButton.setForeground(darkTheme ? DesignSystem.ACCENT : DesignSystem.LIGHT_TEXT);
        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));

        JPanel heading = new JPanel(new BorderLayout());
        heading.setOpaque(false);
        JLabel brand = new JLabel("◉  SyncSphere", SwingConstants.CENTER);
        brand.setFont(DesignSystem.font(Font.BOLD, 28));
        brand.setForeground(colors.text());
        JLabel themeLabel = new JLabel(darkTheme ? "Light" : "Dark");
        themeLabel.setForeground(colors.muted());
        themeLabel.setFont(DesignSystem.font(Font.PLAIN, 11));
        themeButton.setText(themeLabel.getText());
        DesignSystem.styleButton(themeButton, new Color(255, 255, 255, 0), colors.muted());
        themeButton.setPreferredSize(new Dimension(58, 28));
        heading.add(brand, BorderLayout.CENTER);
        heading.add(themeButton, BorderLayout.EAST);
        heading.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));

        JLabel subtitle = label("Enter your credentials to access your secure sphere", 13, colors.muted());
        subtitle.setHorizontalAlignment(SwingConstants.CENTER);

        body.add(heading);
        body.add(Box.createVerticalStrut(8));
        body.add(subtitle);
        body.add(Box.createVerticalStrut(28));
        body.add(fieldGroup("SECURE EMAIL / USERNAME", usernameField));
        body.add(Box.createVerticalStrut(15));
        body.add(fieldGroup("DECRYPTION KEY / PASSWORD", passwordField));
        body.add(Box.createVerticalStrut(10));
        statusLabel.setFont(DesignSystem.font(Font.PLAIN, 12));
        statusLabel.setForeground(colors.muted());
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        body.add(statusLabel);
        body.add(Box.createVerticalStrut(8));
        body.add(fullWidth(loginButton, 46));
        body.add(Box.createVerticalStrut(10));
        body.add(fullWidth(googleButton, 42));
        body.add(Box.createVerticalStrut(16));
        JLabel newHere = label("New to the sphere?", 12, colors.muted());
        newHere.setHorizontalAlignment(SwingConstants.CENTER);
        body.add(newHere);
        body.add(Box.createVerticalStrut(6));
        body.add(fullWidth(registerButton, 38));
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

    private void toggleTheme() {
        darkTheme = !darkTheme;
        ThemePreferences.save(darkTheme);
        setContentPane(buildContentPane());
        revalidate();
        repaint();
    }

    private void ensureApiConfiguration() {
        if (RemoteApiClient.isConfigured()) return;

        JTextField apiField = new JTextField("https://");
        DesignSystem.styleField(apiField, darkTheme);
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setOpaque(false);
        JLabel message = new JLabel("Enter the deployed SyncSphere API URL. No database password is needed here.");
        message.setForeground(DesignSystem.palette(darkTheme).muted());
        panel.add(message, BorderLayout.NORTH);
        panel.add(apiField, BorderLayout.CENTER);

        Object[] options = {"Save API", "Use local mode"};
        int choice = JOptionPane.showOptionDialog(this, panel, "SyncSphere connection setup",
                JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, options[0]);
        if (choice != 0) return;
        try {
            RemoteApiClient.saveApiUrl(apiField.getText());
            JOptionPane.showMessageDialog(this, "Connection saved. You can now sign in from any computer.", "Ready", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException | IllegalArgumentException exception) {
            JOptionPane.showMessageDialog(this, exception.getMessage(), "Connection setup failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void performLogin() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());
        if (username.isBlank() || password.isBlank()) { showStatus("Please enter both username and password.", DesignSystem.DANGER); return; }
        loginButton.setEnabled(false);
        new SwingWorker<User, Void>() {
            @Override protected User doInBackground() throws Exception { return authenticationService.login(username, password); }
            @Override protected void done() {
                loginButton.setEnabled(true);
                try {
                    User user = get();
                    showStatus("Access granted. Opening your workspace...", DesignSystem.SUCCESS);
                    dispose();
                    new ChatWorkspaceFrame(user).setVisible(true);
                } catch (Exception exception) {
                    Throwable cause = exception.getCause();
                    showStatus(cause instanceof InvalidLoginException ? cause.getMessage() : "Login could not be completed.", DesignSystem.DANGER);
                }
            }
        }.execute();
    }

    private void handleGoogleLogin() {
        if (!googleOAuthService.isConfigured()) { showStatus("Google Login is not configured. Please use username/password login.", DesignSystem.DANGER); return; }
        googleButton.setEnabled(false);
        showStatus("Opening Google sign-in in your browser...", DesignSystem.ACCENT);
        new SwingWorker<User, Void>() {
            @Override protected User doInBackground() throws Exception { return googleOAuthService.authenticateInBrowser(); }
            @Override protected void done() {
                googleButton.setEnabled(true);
                try {
                    User user = authenticationService.loginGoogleUser(get());
                    dispose();
                    new ChatWorkspaceFrame(user).setVisible(true);
                } catch (Exception exception) { showStatus("Google login could not be completed. Please try again.", DesignSystem.DANGER); }
            }
        }.execute();
    }

    private void showStatus(String message, Color color) { statusLabel.setForeground(color); statusLabel.setText(message); }
}
