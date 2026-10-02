package com.syncsphere.gui;

import com.syncsphere.dao.UserDAO;
import com.syncsphere.model.Message;
import com.syncsphere.model.PrivateMessage;
import com.syncsphere.model.PublicMessage;
import com.syncsphere.model.User;
import com.syncsphere.service.AuthenticationService;
import com.syncsphere.service.ChatService;
import com.syncsphere.service.ModerationService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ChatFrame extends JFrame {
    private final User currentUser;
    private final AuthenticationService authenticationService = new AuthenticationService();
    private final ChatService chatService = new ChatService();
    private final ModerationService moderationService = new ModerationService();
    private final UserDAO userDAO = new UserDAO();
    private final JTextArea messageArea = new JTextArea();
    private final DefaultListModel<String> userListModel = new DefaultListModel<>();
    private final JList<String> onlineUsersList = new JList<>(userListModel);
    private final JTextArea inputArea = new JTextArea();
    private final JLabel statusLabel = new JLabel("Ready");
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
    private User selectedUser;

    public ChatFrame(User currentUser) {
        this.currentUser = currentUser;
        setTitle("SyncSphere Chat");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setSize(1100, 720);
        setLocationRelativeTo(null);
        setContentPane(buildContentPane());
        setVisible(true);
        loadUsers();
        refreshPublicMessages();
        scheduler.scheduleAtFixedRate(this::refreshPublicMessages, 2, 3, TimeUnit.SECONDS);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                handleLogout();
            }
        });
    }

    private JPanel buildContentPane() {
        JPanel root = new JPanel(new BorderLayout(12, 12));
        root.setBorder(new EmptyBorder(16, 16, 16, 16));
        root.setBackground(new Color(240, 235, 230));

        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(new Color(255, 255, 255, 190));
        topBar.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));

        JLabel appTitle = new JLabel("SyncSphere");
        appTitle.setFont(new Font("Segoe UI", Font.BOLD, 24));
        appTitle.setForeground(new Color(28, 35, 39));

        JLabel profileLabel = new JLabel(currentUser.getUsername() + "  •  " + currentUser.getRole());
        profileLabel.setForeground(new Color(123, 152, 143));
        profileLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        JButton logoutButton = new JButton("Logout");
        logoutButton.setBackground(new Color(162, 101, 89));
        logoutButton.setForeground(Color.WHITE);
        logoutButton.addActionListener(e -> handleLogout());

        topBar.add(appTitle, BorderLayout.WEST);
        topBar.add(profileLabel, BorderLayout.CENTER);
        topBar.add(logoutButton, BorderLayout.EAST);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setBorder(null);
        splitPane.setResizeWeight(0.22);
        splitPane.setBackground(new Color(240, 235, 230));

        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setBackground(new Color(255, 255, 255, 190));
        sidebar.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JLabel onlineLabel = new JLabel("Online Users");
        onlineLabel.setForeground(new Color(28, 35, 39));
        onlineLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));

        onlineUsersList.setBackground(new Color(247, 242, 236));
        onlineUsersList.setForeground(new Color(28, 35, 39));
        onlineUsersList.setSelectionBackground(new Color(181, 126, 95));
        onlineUsersList.setSelectionForeground(Color.WHITE);
        onlineUsersList.setCellRenderer((list, value, index, isSelected, cellHasFocus) -> {
            JLabel label = new JLabel("🟢 " + value);
            label.setOpaque(true);
            label.setBackground(isSelected ? list.getSelectionBackground() : new Color(237, 229, 220));
            label.setForeground(isSelected ? list.getSelectionForeground() : new Color(28, 35, 39));
            label.setBorder(new EmptyBorder(6, 10, 6, 10));
            return label;
        });
        onlineUsersList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String selectedName = onlineUsersList.getSelectedValue();
                if (selectedName != null && !selectedName.equals(currentUser.getUsername())) {
                    selectedUser = userDAO.findByUsername(selectedName);
                    statusLabel.setText("Private chat with " + selectedUser.getUsername());
                }
            }
        });

        JScrollPane userScroll = new JScrollPane(onlineUsersList);
        userScroll.setBorder(null);

        JPanel moderationPanel = new JPanel();
        moderationPanel.setLayout(new BoxLayout(moderationPanel, BoxLayout.Y_AXIS));
        moderationPanel.setOpaque(false);
        if (currentUser.isModerator()) {
            JButton muteButton = new JButton("Mute User");
            JButton unmuteButton = new JButton("Unmute User");
            JButton deleteButton = new JButton("Delete Selected");
            muteButton.addActionListener(e -> handleMute());
            unmuteButton.addActionListener(e -> handleUnmute());
            deleteButton.addActionListener(e -> handleDeleteSelected());
            moderationPanel.add(muteButton);
            moderationPanel.add(Box.createVerticalStrut(8));
            moderationPanel.add(unmuteButton);
            moderationPanel.add(Box.createVerticalStrut(8));
            moderationPanel.add(deleteButton);
        }

        sidebar.setLayout(new BorderLayout(6, 10));
        sidebar.add(onlineLabel, BorderLayout.NORTH);
        sidebar.add(userScroll, BorderLayout.CENTER);
        sidebar.add(moderationPanel, BorderLayout.SOUTH);

        JPanel center = new JPanel(new BorderLayout(12, 12));
        center.setBackground(new Color(255, 255, 255, 200));
        center.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        messageArea.setEditable(false);
        messageArea.setLineWrap(true);
        messageArea.setWrapStyleWord(true);
        messageArea.setBackground(new Color(247, 242, 236));
        messageArea.setForeground(new Color(28, 35, 39));
        messageArea.setCaretColor(new Color(28, 35, 39));
        messageArea.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        messageArea.setBorder(new EmptyBorder(10, 10, 10, 10));

        JScrollPane messageScroll = new JScrollPane(messageArea);
        messageScroll.setBorder(null);

        JPanel inputBar = new JPanel(new BorderLayout(10, 0));
        inputBar.setOpaque(false);
        inputArea.setRows(3);
        inputArea.setLineWrap(true);
        inputArea.setWrapStyleWord(true);
        inputArea.setBackground(new Color(247, 242, 236));
        inputArea.setForeground(new Color(28, 35, 39));
        inputArea.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(181, 126, 95, 150)),
                new EmptyBorder(8, 10, 8, 10)
        ));

        JButton sendButton = new JButton("Send");
        sendButton.setPreferredSize(new Dimension(120, 40));
        sendButton.setBackground(new Color(181, 126, 95));
        sendButton.setForeground(Color.WHITE);
        sendButton.addActionListener(e -> sendCurrentMessage());

        inputBar.add(inputArea, BorderLayout.CENTER);
        inputBar.add(sendButton, BorderLayout.EAST);

        center.add(messageScroll, BorderLayout.CENTER);
        center.add(inputBar, BorderLayout.SOUTH);

        statusLabel.setForeground(new Color(96, 109, 114));
        statusLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));

        root.add(topBar, BorderLayout.NORTH);
        root.add(splitPane, BorderLayout.CENTER);
        root.add(statusLabel, BorderLayout.SOUTH);

        splitPane.setLeftComponent(sidebar);
        splitPane.setRightComponent(center);

        return root;
    }

    private void loadUsers() {
        userListModel.clear();
        List<User> users = userDAO.findOnlineUsers();
        for (User user : users) {
            if (!user.getUsername().equals(currentUser.getUsername())) {
                userListModel.addElement(user.getUsername());
            }
        }
    }

    private void refreshPublicMessages() {
        SwingUtilities.invokeLater(() -> {
            List<Message> messages = chatService.getPublicMessages();
            StringBuilder builder = new StringBuilder();
            for (Message msg : messages) {
                if (msg.isDeleted()) {
                    continue;
                }
                String stamp = msg.getCreatedAt().format(formatter);
                builder.append("[").append(stamp).append("] ")
                        .append(msg.getSenderUsername()).append(": ")
                        .append(msg.getContent()).append(System.lineSeparator());
            }
            messageArea.setText(builder.toString());
            messageArea.setCaretPosition(messageArea.getDocument().getLength());
            loadUsers();
        });
    }

    private void sendCurrentMessage() {
        String text = inputArea.getText() == null ? "" : inputArea.getText().trim();
        if (text.isEmpty()) {
            statusLabel.setText("Message cannot be empty.");
            return;
        }

        try {
            if (selectedUser != null) {
                chatService.sendPrivateMessage(currentUser.getId(), selectedUser.getId(), text);
                statusLabel.setText("Private message sent to " + selectedUser.getUsername());
            } else {
                chatService.sendPublicMessage(currentUser.getId(), text);
                statusLabel.setText("Public message sent.");
            }
            inputArea.setText("");
            refreshPublicMessages();
        } catch (Exception e) {
            statusLabel.setText("Message blocked or invalid: " + e.getMessage());
        }
    }

    private void handleMute() {
        if (selectedUser == null) {
            statusLabel.setText("Select a user to mute.");
            return;
        }
        try {
            moderationService.muteUser(currentUser, selectedUser);
            statusLabel.setText("User muted: " + selectedUser.getUsername());
        } catch (Exception e) {
            statusLabel.setText(e.getMessage());
        }
    }

    private void handleUnmute() {
        if (selectedUser == null) {
            statusLabel.setText("Select a user to unmute.");
            return;
        }
        try {
            moderationService.unmuteUser(currentUser, selectedUser);
            statusLabel.setText("User unmuted: " + selectedUser.getUsername());
        } catch (Exception e) {
            statusLabel.setText(e.getMessage());
        }
    }

    private void handleDeleteSelected() {
        String selected = onlineUsersList.getSelectedValue();
        if (selected == null) {
            statusLabel.setText("Select a message to delete.");
            return;
        }
        try {
            moderationService.deleteMessage(currentUser);
            statusLabel.setText("Moderator action recorded.");
        } catch (Exception e) {
            statusLabel.setText(e.getMessage());
        }
    }

    private void handleLogout() {
        authenticationService.logout(currentUser.getUsername());
        scheduler.shutdownNow();
        dispose();
        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }
}
