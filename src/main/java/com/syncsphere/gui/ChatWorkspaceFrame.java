package com.syncsphere.gui;

import com.syncsphere.dao.MessageDAO;
import com.syncsphere.dao.UserDAO;
import com.syncsphere.model.Friend;
import com.syncsphere.model.FriendRequest;
import com.syncsphere.model.Message;
import com.syncsphere.model.Notification;
import com.syncsphere.model.PrivateMessage;
import com.syncsphere.model.User;
import com.syncsphere.service.AuthenticationService;
import com.syncsphere.service.ChatService;
import com.syncsphere.service.FriendService;
import com.syncsphere.service.MessageFeatureService;
import com.syncsphere.service.ModerationService;
import com.syncsphere.service.TypingIndicatorService;
import com.syncsphere.util.ThemePreferences;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.KeyEvent;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Primary workspace frame.
 * Architecture: GUI -> Service -> DAO -> MySQL (never bypassed).
 */
public class ChatWorkspaceFrame extends JFrame {

    private static final Color DARK_BG    = DesignSystem.BACKGROUND;
    private static final Color DARK_PANEL = DesignSystem.SURFACE;
    private static final Color DARK_INPUT = DesignSystem.INPUT;
    private static final Color PURPLE     = DesignSystem.PRIMARY;
    private static final Color CYAN       = DesignSystem.ACCENT;
    private static final Color GREEN      = DesignSystem.SUCCESS;
    private static final Color RED        = DesignSystem.DANGER;
    private static final Color YELLOW     = DesignSystem.WARNING;
    private static final Color TEXT       = DesignSystem.TEXT;
    private static final Color MUTED      = DesignSystem.MUTED;

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private final User currentUser;
    private final AuthenticationService authService    = new AuthenticationService();
    private final ChatService            chatService    = new ChatService();
    private final MessageFeatureService  featureService = new MessageFeatureService();
    private final ModerationService      moderationService = new ModerationService();
    private final FriendService          friendService  = new FriendService();
    private final UserDAO                userDAO        = new UserDAO();
    private final MessageDAO             messageDAO     = new MessageDAO();
    private final TypingIndicatorService typingService  = new TypingIndicatorService();

    private boolean darkTheme = ThemePreferences.isDark();
    private User selectedUser;

    private DesignSystem.NavButton navHome, navChats, navFriends, navRequests, navNotifs, navSearch, navSettings;

    private final DefaultListModel<User>    userModel    = new DefaultListModel<>();
    private final JList<User>              userList     = new JList<>(userModel);
    private final DefaultListModel<Message> messageModel = new DefaultListModel<>();
    private final JList<Message>           messageList  = new JList<>(messageModel);
    private final List<Message>            currentMessages = new ArrayList<>();

    private final JTextArea  inputArea       = new JTextArea();
    private final JTextField searchField     = new JTextField();
    private final JLabel     conversationLbl = new JLabel("Public sphere");
    private final JLabel     statusLabel     = new JLabel("Ready");
    private final JLabel     typingLabel     = new JLabel(" ");
    private final JLabel     notifBadgeLbl   = new JLabel("");
    private final JLabel     pinnedLabel     = new JLabel("No pinned messages");

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(3);

    public ChatWorkspaceFrame(User currentUser) {
        this.currentUser = currentUser;
        setTitle("SyncSphere");
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setMinimumSize(new Dimension(600, 520));
        setSize(1280, 820);
        setLocationRelativeTo(null);
        setContentPane(buildContentPane());
        if (!darkTheme) applyLightTheme();
        installShortcuts();
        loadUsers();
        refreshConversation();
        scheduler.scheduleAtFixedRate(() -> SwingUtilities.invokeLater(this::refreshConversation),   3,  3, TimeUnit.SECONDS);
        scheduler.scheduleAtFixedRate(() -> SwingUtilities.invokeLater(this::refreshNotifBadge),     5,  5, TimeUnit.SECONDS);
        scheduler.scheduleAtFixedRate(() -> SwingUtilities.invokeLater(this::refreshTyping),         1,  1, TimeUnit.SECONDS);
        scheduler.scheduleAtFixedRate(() -> SwingUtilities.invokeLater(this::loadUsers),            15, 15, TimeUnit.SECONDS);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowClosing(java.awt.event.WindowEvent e) { handleLogout(); }
        });
    }

    private JPanel buildContentPane() {
        DesignSystem.AmbientPanel root = new DesignSystem.AmbientPanel(darkTheme);
        root.setLayout(new BorderLayout(0, 0));
        root.add(buildNavRail(),  BorderLayout.WEST);
        root.add(buildWorkspace(), BorderLayout.CENTER);
        return root;
    }

    private JPanel buildNavRail() {
        DesignSystem.GlassPanel rail = new DesignSystem.GlassPanel(darkTheme, 0);
        rail.setLayout(new BorderLayout());
        rail.setPreferredSize(new Dimension(58, 0));
        rail.setBorder(new EmptyBorder(18, 7, 18, 7));
        JLabel logo = new JLabel("\u25C9", SwingConstants.CENTER);
        logo.setFont(new Font("Segoe UI", Font.BOLD, 26));
        logo.setForeground(PURPLE);
        logo.setBorder(new EmptyBorder(0, 0, 18, 0));
        navHome     = navBtn("\u2302", PURPLE,  "Public room");
        navChats    = navBtn("\u25CC", CYAN,    "Messages");
        navFriends  = navBtn("\u2668", GREEN,   "Friends");
        navRequests = navBtn("+",      YELLOW,  "Friend requests");
        navNotifs   = navBtn("\u25D4", PURPLE,  "Notifications");
        navSearch   = navBtn("\u2315", CYAN,    "Search");
        navHome.setActive(true);
        navHome.addActionListener(    e -> switchToPublic());
        navFriends.addActionListener( e -> showFriends());
        navRequests.addActionListener(e -> showFriendRequests());
        navNotifs.addActionListener(  e -> showNotifications());
        navSearch.addActionListener(  e -> showGlobalSearch());
        JPanel notifWrap = new JPanel(null);
        notifWrap.setOpaque(false);
        notifWrap.setPreferredSize(new Dimension(44, 48));
        navNotifs.setBounds(0, 0, 44, 44);
        notifBadgeLbl.setFont(new Font("Segoe UI", Font.BOLD, 9));
        notifBadgeLbl.setForeground(TEXT);
        notifBadgeLbl.setOpaque(false);
        notifBadgeLbl.setBounds(26, 0, 18, 14);
        notifBadgeLbl.setVisible(false);
        notifWrap.add(navNotifs); notifWrap.add(notifBadgeLbl);
        JPanel navItems = new JPanel();
        navItems.setOpaque(false);
        navItems.setLayout(new BoxLayout(navItems, BoxLayout.Y_AXIS));
        for (JComponent c : new JComponent[]{navHome, navChats, navFriends, navRequests, notifWrap, navSearch}) {
            c.setAlignmentX(Component.CENTER_ALIGNMENT);
            navItems.add(c);
            navItems.add(Box.createVerticalStrut(8));
        }
        navSettings = navBtn("\u2699", MUTED, "Settings & Profile");
        navSettings.addActionListener(e -> showSettings());
        navSettings.setAlignmentX(Component.CENTER_ALIGNMENT);
        rail.add(logo,        BorderLayout.NORTH);
        rail.add(navItems,    BorderLayout.CENTER);
        rail.add(navSettings, BorderLayout.SOUTH);
        return rail;
    }

    private JPanel buildWorkspace() {
        JPanel ws = new JPanel(new BorderLayout(0, 0));
        ws.setOpaque(false);
        ws.add(buildHeader(),    BorderLayout.NORTH);
        ws.add(buildMainSplit(), BorderLayout.CENTER);
        ws.add(buildStatusBar(), BorderLayout.SOUTH);
        return ws;
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout(12, 0));
        header.setBackground(DARK_PANEL);
        header.setBorder(BorderFactory.createCompoundBorder(
            new DesignSystem.RoundedBorder(DesignSystem.BORDER, 0),
            new EmptyBorder(10, 16, 10, 16)));
        JLabel title   = lbl("SyncSphere", 20, Font.BOLD, TEXT);
        JPanel right   = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);
        JLabel profile = lbl(currentUser.getUsername() + "   \u2022   " + currentUser.getRole(), 12, Font.PLAIN, CYAN);
        right.add(profile);
        JButton themeBtn = btn(darkTheme ? "\u2600 Light" : "\u263D Dark", MUTED);
        themeBtn.addActionListener(e -> toggleTheme());
        right.add(themeBtn);
        if (currentUser.isModerator()) {
            JButton modBtn = btn("\u26A0 Mod", RED);
            modBtn.addActionListener(e -> showModeration());
            right.add(modBtn);
        }
        JButton logoutBtn = btn("Logout", RED);
        logoutBtn.addActionListener(e -> handleLogout());
        right.add(logoutBtn);
        header.add(title, BorderLayout.WEST);
        header.add(right,  BorderLayout.EAST);
        return header;
    }

    private JSplitPane buildMainSplit() {
        JSplitPane leftCenter = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, buildSidebar(), buildCenter());
        leftCenter.setResizeWeight(0.22);
        leftCenter.setBorder(null);
        leftCenter.setDividerSize(4);
        JSplitPane main = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftCenter, buildRightPanel());
        main.setResizeWeight(0.78);
        main.setBorder(null);
        main.setDividerSize(4);
        return main;
    }

    private JPanel buildSidebar() {
        DesignSystem.GlassPanel sidebar = new DesignSystem.GlassPanel(darkTheme, 0);
        sidebar.setLayout(new BorderLayout(0, 8));
        sidebar.setBorder(new EmptyBorder(16, 12, 12, 12));
        sidebar.setPreferredSize(new Dimension(220, 0));
        JLabel usersTitle = lbl("Conversations", 15, Font.BOLD, TEXT);
        userList.setBackground(darkTheme ? DARK_INPUT : DesignSystem.LIGHT_INPUT);
        userList.setForeground(TEXT);
        userList.setSelectionBackground(PURPLE);
        userList.setCellRenderer(new UserCellRenderer());
        userList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                User sel = userList.getSelectedValue();
                if (sel != null) {
                    selectedUser = sel;
                    activateNav(navChats);
                    typingService.clearConversation(sel.getId());
                    conversationLbl.setText("Private chat with " + sel.getUsername());
                    refreshConversation();
                    updateUnreadBadge();
                }
            }
        });
        JScrollPane userScroll = new JScrollPane(userList);
        userScroll.setBorder(null);
        userScroll.getViewport().setBackground(darkTheme ? DARK_INPUT : DesignSystem.LIGHT_INPUT);
        JPanel actions = new JPanel(new GridLayout(0, 1, 0, 5));
        actions.setOpaque(false);
        JButton publicBtn  = btn("\u25CE  Public sphere", CYAN);
        JButton findBtn    = btn("\u2315  Find People",    CYAN);
        JButton requestBtn = btn("+  Requests",           YELLOW);
        JButton friendsBtn = btn("\u2668  Friends",        GREEN);
        publicBtn.addActionListener(  e -> switchToPublic());
        findBtn.addActionListener(    e -> showFindPeople());
        requestBtn.addActionListener( e -> showFriendRequests());
        friendsBtn.addActionListener( e -> showFriends());
        actions.add(publicBtn);
        actions.add(findBtn);
        actions.add(requestBtn);
        actions.add(friendsBtn);
        pinnedLabel.setForeground(MUTED);
        pinnedLabel.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        pinnedLabel.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) { showPinnedMessages(); }
            @Override public void mouseEntered(java.awt.event.MouseEvent e) { pinnedLabel.setForeground(CYAN); }
            @Override public void mouseExited(java.awt.event.MouseEvent e)  { pinnedLabel.setForeground(MUTED); }
        });
        pinnedLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        actions.add(pinnedLabel);
        sidebar.add(usersTitle, BorderLayout.NORTH);
        sidebar.add(userScroll, BorderLayout.CENTER);
        sidebar.add(actions,    BorderLayout.SOUTH);
        return sidebar;
    }

    private JPanel buildCenter() {
        DesignSystem.GlassPanel center = new DesignSystem.GlassPanel(darkTheme, 0);
        center.setLayout(new BorderLayout(0, 8));
        center.setBorder(new EmptyBorder(14, 14, 14, 14));
        JPanel convBar = new JPanel(new BorderLayout(8, 0));
        convBar.setOpaque(false);
        conversationLbl.setFont(new Font("Segoe UI", Font.BOLD, 16));
        conversationLbl.setForeground(TEXT);
        styleInput(searchField);
        searchField.setToolTipText("Search in this conversation (Ctrl+F)");
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { refreshConversation(); }
            public void removeUpdate(DocumentEvent e) { refreshConversation(); }
            public void changedUpdate(DocumentEvent e) { refreshConversation(); }
        });
        JButton searchBtn = btn("Search", CYAN);
        searchBtn.addActionListener(e -> refreshConversation());
        convBar.add(conversationLbl, BorderLayout.WEST);
        convBar.add(searchField,     BorderLayout.CENTER);
        convBar.add(searchBtn,       BorderLayout.EAST);
        messageList.setBackground(darkTheme ? DARK_BG : DesignSystem.LIGHT_BACKGROUND);
        messageList.setForeground(TEXT);
        messageList.setSelectionBackground(new Color(76, 58, 160, 120));
        messageList.setCellRenderer(new MessageCellRenderer());
        messageList.addListSelectionListener(e -> { Message sel = messageList.getSelectedValue(); if (sel != null) markRead(sel); });
        JPopupMenu msgMenu = buildMessageContextMenu();
        messageList.setComponentPopupMenu(msgMenu);
        JScrollPane msgScroll = new JScrollPane(messageList);
        msgScroll.setBorder(null);
        msgScroll.getViewport().setBackground(darkTheme ? DARK_BG : DesignSystem.LIGHT_BACKGROUND);
        inputArea.setRows(3);
        inputArea.setLineWrap(true);
        inputArea.setWrapStyleWord(true);
        styleInput(inputArea);
        inputArea.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { signalTyping(); }
            public void removeUpdate(DocumentEvent e) {}
            public void changedUpdate(DocumentEvent e) {}
        });
        typingLabel.setForeground(CYAN);
        typingLabel.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        JButton sendBtn = new DesignSystem.GlassButton("  Send \u2192", PURPLE, TEXT);
        sendBtn.addActionListener(e -> sendMessage());
        sendBtn.setPreferredSize(new Dimension(90, 0));
        JPanel composer = new JPanel(new BorderLayout(8, 0));
        composer.setOpaque(false);
        composer.add(inputArea, BorderLayout.CENTER);
        composer.add(sendBtn,   BorderLayout.EAST);
        JPanel composerWrap = new JPanel(new BorderLayout(0, 4));
        composerWrap.setOpaque(false);
        composerWrap.add(typingLabel, BorderLayout.NORTH);
        composerWrap.add(composer,    BorderLayout.CENTER);
        center.add(convBar,      BorderLayout.NORTH);
        center.add(msgScroll,    BorderLayout.CENTER);
        center.add(composerWrap, BorderLayout.SOUTH);
        return center;
    }

    private JPanel buildRightPanel() {
        DesignSystem.GlassPanel right = new DesignSystem.GlassPanel(darkTheme, 0);
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
        right.setBorder(new EmptyBorder(24, 20, 24, 20));
        right.setPreferredSize(new Dimension(240, 0));
        JLabel avatar    = lbl("\u25C9", 52, Font.PLAIN, PURPLE);
        avatar.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel nameLabel = lbl(currentUser.getUsername(), 18, Font.BOLD, TEXT);
        nameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel roleLabel = lbl(currentUser.getRole(), 12, Font.PLAIN, CYAN);
        roleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        DesignSystem.GlassPanel infoCard = new DesignSystem.GlassPanel(darkTheme, 14);
        infoCard.setLayout(new BoxLayout(infoCard, BoxLayout.Y_AXIS));
        infoCard.setBorder(new EmptyBorder(14, 14, 14, 14));
        infoCard.setAlignmentX(Component.CENTER_ALIGNMENT);
        infoCard.setMaximumSize(new Dimension(220, 300));
        JLabel emailHdr = lbl("Email",    10, Font.BOLD, MUTED); emailHdr.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel emailVal = lbl(currentUser.getEmail() != null ? currentUser.getEmail() : "Not provided", 13, Font.PLAIN, TEXT);
        emailVal.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel provHdr  = lbl("Sign-in",  10, Font.BOLD, MUTED); provHdr.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel provVal  = lbl(currentUser.getAuthProvider(), 13, Font.PLAIN, TEXT);
        provVal.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel msgsHdr  = lbl("Messages sent", 10, Font.BOLD, MUTED); msgsHdr.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel msgsVal  = lbl("Loading\u2026", 13, Font.PLAIN, TEXT);
        msgsVal.setAlignmentX(Component.LEFT_ALIGNMENT);
        infoCard.add(emailHdr); infoCard.add(emailVal); infoCard.add(Box.createVerticalStrut(10));
        infoCard.add(provHdr);  infoCard.add(provVal);  infoCard.add(Box.createVerticalStrut(10));
        infoCard.add(msgsHdr);  infoCard.add(msgsVal);
        JButton logoutBtn = new DesignSystem.GlassButton("Logout", RED, TEXT);
        logoutBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        logoutBtn.setMaximumSize(new Dimension(200, 38));
        logoutBtn.addActionListener(e -> handleLogout());
        right.add(avatar); right.add(Box.createVerticalStrut(8));
        right.add(nameLabel); right.add(Box.createVerticalStrut(2));
        right.add(roleLabel); right.add(Box.createVerticalStrut(20));
        right.add(infoCard);  right.add(Box.createVerticalGlue());
        right.add(logoutBtn);
        new SwingWorker<Long, Void>() {
            @Override protected Long doInBackground() {
                return messageDAO.searchMessages(currentUser.getUsername()).stream()
                    .filter(m -> currentUser.getId().equals(m.getSenderId())).count();
            }
            @Override protected void done() {
                try { msgsVal.setText(String.valueOf(get())); }
                catch (Exception ex) { msgsVal.setText("\u2014"); }
            }
        }.execute();
        return right;
    }

    private JPanel buildStatusBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setOpaque(false);
        bar.setBorder(new EmptyBorder(2, 16, 4, 16));
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        statusLabel.setForeground(MUTED);
        bar.add(statusLabel, BorderLayout.WEST);
        return bar;
    }

    private class UserCellRenderer implements ListCellRenderer<User> {
        @Override public Component getListCellRendererComponent(JList<? extends User> list, User user,
                int idx, boolean sel, boolean focus) {
            DesignSystem.HoverPanel row = new DesignSystem.HoverPanel(
                darkTheme ? DARK_INPUT : DesignSystem.LIGHT_INPUT,
                darkTheme ? new Color(38, 35, 78) : new Color(230, 225, 255));
            row.setLayout(new BorderLayout(8, 0));
            row.setBorder(new EmptyBorder(8, 10, 8, 10));
            if (sel) row.setBackground(PURPLE);
            JLabel dot  = lbl(user.isOnline() ? "\u25CF" : "\u25CB", 10, Font.PLAIN, user.isOnline() ? GREEN : MUTED);
            JLabel name = lbl(user.getUsername(), 13, Font.BOLD, sel ? TEXT : (darkTheme ? TEXT : DesignSystem.LIGHT_TEXT));
            row.add(dot,  BorderLayout.WEST);
            row.add(name, BorderLayout.CENTER);
            return row;
        }
    }

    private class MessageCellRenderer extends JPanel implements ListCellRenderer<Message> {
        private final JLabel body = new JLabel();
        MessageCellRenderer() { setLayout(new BorderLayout()); add(body, BorderLayout.CENTER); setBorder(new EmptyBorder(6, 12, 6, 12)); }
        @Override public Component getListCellRendererComponent(JList<? extends Message> list, Message msg,
                int idx, boolean sel, boolean focus) {
            boolean mine = currentUser.getId().equals(msg.getSenderId());
            Color bg = sel ? new Color(76, 58, 160, 80) : (mine
                ? (darkTheme ? new Color(30, 22, 60) : new Color(235, 230, 255))
                : (darkTheme ? DARK_BG : DesignSystem.LIGHT_BACKGROUND));
            setBackground(bg);
            String esc = msg.getContent().replace("&","&amp;").replace("<","&lt;").replace(">","&gt;");
            String hi = esc.replaceAll("(@[A-Za-z0-9_]{3,20})", "<font color='#22D3EE'>$1</font>");
            String edit = msg.isEdited() ? " <font color='#94A3B8'><i>(edited)</i></font>" : "";
            String repl = msg.getReplyToMessageId() != null ? "<font color='#94A3B8'>\u21AA reply</font><br>" : "";
            String nc = mine ? "#C4B5FD" : "#7C5CFC";
            body.setText("<html><b style='color:" + nc + "'>" + msg.getSenderUsername() +
                "</b>&nbsp;<font color='#94A3B8'>" + msg.getCreatedAt().format(TIME_FMT) + "</font>" +
                edit + "<br>" + repl + hi + "</html>");
            body.setForeground(darkTheme ? TEXT : DesignSystem.LIGHT_TEXT);
            setBorder(new EmptyBorder(8, 12, 8, 12));
            return this;
        }
    }

    private JPopupMenu buildMessageContextMenu() {
        JPopupMenu menu = new JPopupMenu();
        JMenuItem reply  = new JMenuItem("\u21AA Reply");           reply.addActionListener(e -> replySelected());
        JMenuItem edit   = new JMenuItem("\u270E Edit");            edit.addActionListener(e -> editSelected());
        JMenuItem copy   = new JMenuItem("\u2398 Copy");            copy.addActionListener(e -> copySelected());
        JMenuItem react  = new JMenuItem("\uD83D\uDC4D React");     react.addActionListener(e -> reactSelected());
        JMenuItem pin    = new JMenuItem("\uD83D\uDCCC Pin/Unpin"); pin.addActionListener(e -> pinSelected());
        JMenuItem del    = new JMenuItem("\uD83D\uDDD1 Delete");    del.addActionListener(e -> deleteSelected());
        menu.add(reply); menu.add(edit); menu.add(copy); menu.add(react); menu.add(pin); menu.add(del);
        if (currentUser.isModerator()) {
            menu.addSeparator();
            JMenuItem modDel  = new JMenuItem("MOD \u2014 Delete message"); modDel.setForeground(RED);
            JMenuItem modMute = new JMenuItem("MOD \u2014 Mute sender");    modMute.setForeground(YELLOW);
            modDel.addActionListener(e -> {
                Message m = messageList.getSelectedValue();
                if (m == null || m.getId() == null) return;
                int c = confirmDialog("Delete this message as moderator?", "Confirm");
                if (c != JOptionPane.YES_OPTION) return;
                new SwingWorker<Void, Void>() {
                    @Override protected Void doInBackground() throws Exception { moderationService.deleteMessage(currentUser, m.getId(), "Moderator deleted"); return null; }
                    @Override protected void done() { try { get(); status("Message deleted."); refreshConversation(); } catch (Exception ex) { status(ex.getMessage()); } }
                }.execute();
            });
            modMute.addActionListener(e -> {
                Message m = messageList.getSelectedValue();
                if (m == null || m.getSenderId() == null) return;
                new SwingWorker<Void, Void>() {
                    @Override protected Void doInBackground() throws Exception {
                        User target = userDAO.findById(m.getSenderId());
                        if (target != null) featureService.mute(currentUser, target, 300, "Moderator muted");
                        return null;
                    }
                    @Override protected void done() { try { get(); status("User muted."); } catch (Exception ex) { status(ex.getMessage()); } }
                }.execute();
            });
            menu.add(modDel); menu.add(modMute);
        }
        return menu;
    }

    private void refreshConversation() {
        new SwingWorker<List<Message>, Void>() {
            @Override protected List<Message> doInBackground() {
                List<Message> msgs = selectedUser == null
                    ? chatService.getPublicMessages()
                    : chatService.getPrivateMessages(currentUser.getId(), selectedUser.getId());
                String q = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
                if (!q.isBlank()) {
                    msgs = msgs.stream()
                        .filter(m -> m.getContent().toLowerCase().contains(q) || m.getSenderUsername().toLowerCase().contains(q))
                        .collect(Collectors.toList());
                }
                return msgs;
            }
            @Override protected void done() {
                try {
                    List<Message> msgs = get();
                    currentMessages.clear(); currentMessages.addAll(msgs);
                    messageModel.clear(); msgs.forEach(messageModel::addElement);
                    updatePinnedLabel();
                    if (!msgs.isEmpty()) messageList.ensureIndexIsVisible(msgs.size() - 1);
                } catch (Exception ex) { status("Conversation could not be loaded."); }
            }
        }.execute();
    }

    private void sendMessage() {
        String text = inputArea.getText() == null ? "" : inputArea.getText().trim();
        if (text.isBlank()) { status("Message cannot be empty."); return; }
        if (text.startsWith("/")) { handleCommand(text); inputArea.setText(""); return; }
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() throws Exception {
                Message reply = (Message) inputArea.getClientProperty("replyTo");
                Long replyId = reply == null ? null : reply.getId();
                if (selectedUser == null) chatService.sendPublicMessage(currentUser.getId(), text, replyId);
                else chatService.sendPrivateMessage(currentUser.getId(), selectedUser.getId(), text, replyId);
                return null;
            }
            @Override protected void done() {
                try { get(); inputArea.setText(""); clearReply(); status("Message sent."); refreshConversation(); }
                catch (Exception ex) { Throwable c = ex.getCause(); status(c != null ? c.getMessage() : ex.getMessage()); }
            }
        }.execute();
    }

    private void copySelected()  { Message m = messageList.getSelectedValue(); if (m != null) { Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(m.getContent()), null); status("Copied."); } }
    private void replySelected() { Message m = messageList.getSelectedValue(); if (m == null) return; inputArea.putClientProperty("replyTo", m); typingLabel.setText("\u21AA Replying to " + m.getSenderUsername()); inputArea.requestFocusInWindow(); }
    private void editSelected()  {
        Message m = messageList.getSelectedValue();
        if (m == null || !currentUser.getId().equals(m.getSenderId())) { status("You can only edit your own messages."); return; }
        String edited = JOptionPane.showInputDialog(this, "Edit message:", m.getContent());
        if (edited == null) return;
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() throws Exception { chatService.editMessage(m.getId(), currentUser, edited); return null; }
            @Override protected void done() { try { get(); status("Message edited."); refreshConversation(); } catch (Exception ex) { status(ex.getCause() != null ? ex.getCause().getMessage() : "Edit failed."); } }
        }.execute();
    }
    private void reactSelected() {
        Message m = messageList.getSelectedValue(); if (m == null || m.getId() == null) return;
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() throws Exception { featureService.toggleReaction(m.getId(), currentUser, "\uD83D\uDC4D"); return null; }
            @Override protected void done() { try { get(); status("Reaction updated."); refreshConversation(); } catch (Exception ex) { status("Reaction failed."); } }
        }.execute();
    }
    private void pinSelected() {
        Message m = messageList.getSelectedValue(); if (m == null || m.getId() == null) return;
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() throws Exception { featureService.togglePin(m.getId(), currentUser); return null; }
            @Override protected void done() { try { get(); status("Pin updated."); updatePinnedLabel(); } catch (Exception ex) { status(ex.getMessage()); } }
        }.execute();
    }
    private void deleteSelected() {
        Message m = messageList.getSelectedValue(); if (m == null || m.getId() == null) return;
        int choice = confirmDialog("Delete this message?", "Delete message");
        if (choice != JOptionPane.YES_OPTION) return;
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() throws Exception { chatService.deleteMessage(m.getId(), currentUser); return null; }
            @Override protected void done() { try { get(); status("Message deleted."); refreshConversation(); } catch (Exception ex) { status(ex.getCause() != null ? ex.getCause().getMessage() : "Delete failed."); } }
        }.execute();
    }
    private void markRead(Message m) {
        if (m.getId() != null && selectedUser != null && !currentUser.getId().equals(m.getSenderId())) {
            new SwingWorker<Void, Void>() {
                @Override protected Void doInBackground() throws Exception { featureService.markRead(m.getId(), currentUser); return null; }
                @Override protected void done() {}
            }.execute();
        }
    }

    private void loadUsers() {
        new SwingWorker<List<User>, Void>() {
            @Override protected List<User> doInBackground() { return userDAO.findOnlineUsers(); }
            @Override protected void done() {
                try {
                    List<User> users = get();
                    userModel.clear();
                    users.stream().filter(u -> !u.getId().equals(currentUser.getId())).forEach(userModel::addElement);
                    updateUnreadBadge();
                } catch (Exception ignored) {}
            }
        }.execute();
    }

    private void refreshNotifBadge() {
        new SwingWorker<Integer, Void>() {
            @Override protected Integer doInBackground() throws Exception { return featureService.getUnreadNotifications(currentUser).size(); }
            @Override protected void done() {
                try {
                    int count = get();
                    if (count > 0) { notifBadgeLbl.setText(count > 99 ? "99+" : String.valueOf(count)); notifBadgeLbl.setForeground(RED); notifBadgeLbl.setVisible(true); }
                    else { notifBadgeLbl.setVisible(false); }
                } catch (Exception ignored) { notifBadgeLbl.setVisible(false); }
            }
        }.execute();
    }

    private void updateUnreadBadge() {
        if (selectedUser == null) return;
        new SwingWorker<Integer, Void>() {
            @Override protected Integer doInBackground() throws Exception { return featureService.countUnreadPrivateMessages(currentUser, selectedUser.getId()); }
            @Override protected void done() {
                try { int u = get(); status(u == 0 ? "Private conversation ready." : u + " unread private message(s)."); }
                catch (Exception ignored) {}
            }
        }.execute();
    }

    private void refreshTyping() {
        if (selectedUser == null) { typingLabel.setText(" "); return; }
        typingLabel.setText(typingService.isTyping(selectedUser.getId(), currentUser.getId()) ? selectedUser.getUsername() + " is typing\u2026" : " ");
    }

    private void signalTyping() {
        if (selectedUser != null && !inputArea.getText().isBlank())
            typingService.setTyping(currentUser.getId(), selectedUser.getId());
    }

    private void updatePinnedLabel() {
        new SwingWorker<List<Long>, Void>() {
            @Override protected List<Long> doInBackground() throws Exception { return featureService.getPinnedMessageIds(); }
            @Override protected void done() {
                try { List<Long> ids = get(); pinnedLabel.setText(ids.isEmpty() ? "No pinned messages" : ids.size() + " pinned"); }
                catch (Exception ignored) {}
            }
        }.execute();
    }

    private void switchToPublic() {
        selectedUser = null; userList.clearSelection();
        conversationLbl.setText("Public sphere"); activateNav(navHome); refreshConversation();
    }

    private void activateNav(DesignSystem.NavButton active) {
        for (DesignSystem.NavButton b : new DesignSystem.NavButton[]{navHome, navChats, navFriends, navRequests, navNotifs, navSearch, navSettings}) {
            if (b != null) b.setActive(b == active);
        }
    }

    private void toggleTheme() {
        darkTheme = !darkTheme;
        ThemePreferences.save(darkTheme);
        status(darkTheme ? "Dark theme selected." : "Light theme selected.");
        SwingUtilities.invokeLater(() -> { setContentPane(buildContentPane()); revalidate(); repaint(); });
    }

    private void applyLightTheme() {
        applyThemeTo(getContentPane());
    }

    private void applyThemeTo(Component c) {
        Color panel = darkTheme ? DARK_PANEL : DesignSystem.LIGHT_SURFACE;
        Color input = darkTheme ? DARK_INPUT : DesignSystem.LIGHT_INPUT;
        Color text  = darkTheme ? TEXT        : DesignSystem.LIGHT_TEXT;
        if (c instanceof JPanel)   c.setBackground(panel);
        if (c instanceof JTextArea || c instanceof JTextField || c instanceof JList<?>) c.setBackground(input);
        if (c instanceof JLabel l) l.setForeground(text);
        if (c instanceof Container ct) for (Component child : ct.getComponents()) applyThemeTo(child);
    }

    private DesignSystem.NavButton navBtn(String icon, Color accent, String tooltip) {
        DesignSystem.NavButton btn = new DesignSystem.NavButton(icon, accent);
        btn.setToolTipText(tooltip);
        return btn;
    }

    private JButton btn(String text, Color accent) {
        return new DesignSystem.GlassButton(text, accent, TEXT);
    }

    private void styleInput(javax.swing.text.JTextComponent component) {
        component.setBackground(darkTheme ? DARK_INPUT : DesignSystem.LIGHT_INPUT);
        component.setForeground(darkTheme ? TEXT : DesignSystem.LIGHT_TEXT);
        component.setCaretColor(CYAN);
        component.setBorder(BorderFactory.createCompoundBorder(
            new DesignSystem.RoundedBorder(DesignSystem.BORDER, 8),
            new EmptyBorder(6, 10, 6, 10)));
    }

    private void installShortcuts() {
        InputMap im = getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = getRootPane().getActionMap();
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_F, KeyEvent.CTRL_DOWN_MASK), "focusSearch");
        am.put("focusSearch", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { searchField.requestFocusInWindow(); }
        });
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, KeyEvent.CTRL_DOWN_MASK), "sendMessage");
        am.put("sendMessage", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { sendMessage(); }
        });
    }

    private void showFriends() {
        activateNav(navFriends);
        new SwingWorker<List<Friend>, Void>() {
            @Override protected List<Friend> doInBackground() throws Exception { return friendService.getFriends(currentUser); }
            @Override protected void done() {
                try {
                    List<Friend> list = get();
                    StringBuilder sb = new StringBuilder("Friends (" + list.size() + "):\n\n");
                    list.forEach(f -> sb.append("\u2022 ").append(f.user().getUsername()).append("\n"));
                    JOptionPane.showMessageDialog(ChatWorkspaceFrame.this, sb.toString(), "Friends", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) { status("Failed to fetch friends."); }
            }
        }.execute();
    }

    private void showFriendRequests() {
        activateNav(navRequests);
        new SwingWorker<List<FriendRequest>, Void>() {
            @Override protected List<FriendRequest> doInBackground() throws Exception { return friendService.incomingRequests(currentUser); }
            @Override protected void done() {
                try {
                    List<FriendRequest> reqs = get();
                    if (reqs.isEmpty()) { JOptionPane.showMessageDialog(ChatWorkspaceFrame.this, "No pending friend requests.", "Requests", JOptionPane.INFORMATION_MESSAGE); return; }
                    for (FriendRequest r : reqs) {
                        int choice = JOptionPane.showConfirmDialog(ChatWorkspaceFrame.this, "Accept request from " + r.sender().getUsername() + "?", "Friend Request", JOptionPane.YES_NO_CANCEL_OPTION);
                        if (choice == JOptionPane.YES_OPTION) friendService.acceptRequest(currentUser, r.id());
                        else if (choice == JOptionPane.NO_OPTION) friendService.declineRequest(currentUser, r.id());
                    }
                    status("Requests processed.");
                } catch (Exception ex) { status("Failed to process requests."); }
            }
        }.execute();
    }

    private void showNotifications() {
        activateNav(navNotifs);
        new SwingWorker<List<Notification>, Void>() {
            @Override protected List<Notification> doInBackground() throws Exception { return featureService.getUnreadNotifications(currentUser); }
            @Override protected void done() {
                try {
                    List<Notification> notifs = get();
                    if (notifs.isEmpty()) { JOptionPane.showMessageDialog(ChatWorkspaceFrame.this, "No new notifications.", "Notifications", JOptionPane.INFORMATION_MESSAGE); return; }
                    StringBuilder sb = new StringBuilder("Notifications:\n\n");
                    for (Notification n : notifs) {
                        sb.append("\u2022 ").append(n.content()).append("\n");
                    }
                    featureService.markNotificationsRead(currentUser);
                    refreshNotifBadge();
                    JOptionPane.showMessageDialog(ChatWorkspaceFrame.this, sb.toString(), "Notifications", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) { status("Failed to fetch notifications."); }
            }
        }.execute();
    }


    private void showGlobalSearch() {
        activateNav(navSearch);
        String query = JOptionPane.showInputDialog(this, "Search all messages:");
        if (query == null || query.isBlank()) return;
        new SwingWorker<List<Message>, Void>() {
            @Override protected List<Message> doInBackground() throws Exception { return chatService.searchMessages(query); }
            @Override protected void done() {
                try {
                    List<Message> results = get();
                    StringBuilder sb = new StringBuilder("Search results for '" + query + "' (" + results.size() + "):\n\n");
                    results.forEach(m -> sb.append("[").append(m.getSenderUsername()).append("]: ").append(m.getContent()).append("\n"));
                    JOptionPane.showMessageDialog(ChatWorkspaceFrame.this, sb.toString(), "Search Results", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) { status("Search failed."); }
            }
        }.execute();
    }

    private void showFindPeople() {
        String username = JOptionPane.showInputDialog(this, "Enter username to send friend request:");
        if (username == null || username.isBlank()) return;
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() throws Exception {
                User target = userDAO.findByUsername(username.trim());
                if (target == null) throw new IllegalArgumentException("User not found.");
                friendService.sendRequest(currentUser, target.getId());
                return null;
            }
            @Override protected void done() {
                try { get(); status("Friend request sent!"); }

                catch (Exception ex) { status(ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage()); }
            }
        }.execute();
    }

    private void showSettings() {
        activateNav(navSettings);
        String msg = "User Profile & Settings:\n\nUsername: " + currentUser.getUsername() +
            "\nEmail: " + (currentUser.getEmail() != null ? currentUser.getEmail() : "N/A") +
            "\nRole: " + currentUser.getRole() +
            "\nAuth Provider: " + currentUser.getAuthProvider() +
            "\nTheme: " + (darkTheme ? "Dark Mode" : "Light Mode");
        JOptionPane.showMessageDialog(this, msg, "Settings & Profile", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showModeration() {
        if (!currentUser.isModerator()) return;
        String msg = "Moderator Control Panel:\n\nAvailable moderator actions:\n" +
            "1. Delete messages (Right-click message -> MOD Delete)\n" +
            "2. Mute users (Right-click message -> MOD Mute)\n" +
            "3. View audit logs in DB";
        JOptionPane.showMessageDialog(this, msg, "Moderator Panel", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showPinnedMessages() {
        new SwingWorker<List<Long>, Void>() {
            @Override protected List<Long> doInBackground() throws Exception { return featureService.getPinnedMessageIds(); }
            @Override protected void done() {
                try {
                    List<Long> ids = get();
                    JOptionPane.showMessageDialog(ChatWorkspaceFrame.this, "Pinned Message IDs:\n" + ids.toString(), "Pinned Messages", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) { status("Could not retrieve pinned messages."); }
            }
        }.execute();
    }

    private JLabel lbl(String text, int size, int style, Color fg) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", style, size));
        l.setForeground(fg);

        return l;
    }

    private void status(String text) {
        if (statusLabel != null) statusLabel.setText(text);
    }

    private int confirmDialog(String message, String title) {
        return JOptionPane.showConfirmDialog(this, message, title, JOptionPane.YES_NO_OPTION);
    }

    private void clearReply() {
        inputArea.putClientProperty("replyTo", null);
        typingLabel.setText(" ");
    }

    private void stopPolling() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
        }
    }

    private void handleLogout() {
        authService.logout(currentUser.getUsername());
        stopPolling();
        dispose();
        new LoginFrame().setVisible(true);
    }

    private void handleCommand(String input) {
        if (input.startsWith("/help")) {
            status("Commands: /help, /theme, /clear, /logout, /status <text>");
        } else if (input.startsWith("/theme")) {
            toggleTheme();
        } else if (input.startsWith("/clear")) {
            clearReply();
            status("Cleared.");
        } else if (input.startsWith("/logout")) {
            handleLogout();
        } else if (input.startsWith("/status ")) {
            String newStatus = input.substring(8).trim();
            currentUser.setCustomStatus(newStatus);
            new SwingWorker<Void, Void>() {
                @Override protected Void doInBackground() throws Exception {
                    userDAO.updateUserStatus(currentUser.getId(), currentUser.isOnline(), newStatus);
                    return null;
                }
                @Override protected void done() { status("Status updated."); }
            }.execute();
        } else {
            status("Unknown command. Type /help for available commands.");
        }
    }
}



