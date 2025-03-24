import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.*;
import java.rmi.*;
import java.rmi.server.UnicastRemoteObject;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ChatClient extends UnicastRemoteObject implements ChatClientInterface {
    private ChatServerInterface server;
    private String username;
    private boolean darkMode = false;
    private Map<String, String> userStatuses = new ConcurrentHashMap<>();

    // GUI Components
    private JFrame frame;
    private JTextArea chatArea;
    private JList<String> userList;
    private DefaultListModel<String> listModel;
    private JTextField messageField;
    private JButton  privateButton, emojiButton,  fileButton, themeButton;

    public ChatClient(String username) throws RemoteException {
        this.username = username;
        initializeGUI();
        connectToServer();
    }

    private void initializeGUI() {
        frame = new JFrame("Chat Application - " + username);
        frame.setSize(1000, 700);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Main panel with background
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Chat area with timestamp
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        JScrollPane chatScroll = new JScrollPane(chatArea);

        // User list with status indicators
        listModel = new DefaultListModel<>();
        userList = new JList<>(listModel);
        JScrollPane userScroll = new JScrollPane(userList);
        userScroll.setPreferredSize(new Dimension(200, 0));
        userScroll.setBorder(new TitledBorder("Connected Users"));
        // Control buttons panel
        JPanel controlPanel = new JPanel(new GridLayout(1, 6, 5, 5));
        emojiButton = new JButton("😀 Emoji");
        fileButton = new JButton("Send File");
        themeButton = new JButton("Theme");

        controlPanel.add(emojiButton);
        controlPanel.add(fileButton);
        controlPanel.add(themeButton);

        // Input panel
        JPanel inputPanel = new JPanel(new BorderLayout(5, 5));
        messageField = new JTextField();
// Create button panel for broadcast/private
        JPanel sendButtonPanel = new JPanel(new GridLayout(1, 2, 5, 5));
        JButton SendButton = new JButton("Send");
        JButton privateSendButton = new JButton("Private");

        sendButtonPanel.add(SendButton);
        sendButtonPanel.add(privateSendButton);

        inputPanel.add(messageField, BorderLayout.CENTER);
        inputPanel.add(sendButtonPanel, BorderLayout.EAST);

        // Add components to main panel
        mainPanel.add(controlPanel, BorderLayout.NORTH);
        mainPanel.add(chatScroll, BorderLayout.CENTER);
        mainPanel.add(userScroll, BorderLayout.EAST);
        mainPanel.add(inputPanel, BorderLayout.SOUTH);

        frame.setContentPane(mainPanel);

        // Event listener
        SendButton.addActionListener(e -> sendBroadcastMessage());
        privateSendButton.addActionListener(e -> sendPrivateMessage());
        emojiButton.addActionListener(e -> showEmojiPicker());
        fileButton.addActionListener(e -> sendFile());
        themeButton.addActionListener(e -> toggleTheme());
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                try {
                    server.disconnectClient(username);
                } catch (RemoteException ex) {
                    ex.printStackTrace();
                }
            }
        });
        frame.setVisible(true);
    }

    private void connectToServer() {
        try {
            server = (ChatServerInterface) Naming.lookup("rmi://localhost/ChatServer");
            server.registerClient(this, username);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(frame, "Connection error: " + e.getMessage());
            System.exit(0);
        }
    }


    private void sendBroadcastMessage() {
        String message = messageField.getText().trim();
        if (!message.isEmpty()) {
            try {
                server.broadcastMessage(message, username);
                messageField.setText("");
            } catch (RemoteException ex) {
                showError("Failed to send broadcast message");
            }
        }
    }
    private void sendFile() {
        JFileChooser fileChooser = new JFileChooser();
        int returnValue = fileChooser.showOpenDialog(frame);

        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            String recipient = (String) JOptionPane.showInputDialog(frame,
                    "Select recipient:", "File Transfer",
                    JOptionPane.PLAIN_MESSAGE, null,
                    listModel.toArray(), null);

            if (recipient != null) {
                try {
                    FileInputStream fis = new FileInputStream(file);
                    byte[] fileData = new byte[(int) file.length()];
                    fis.read(fileData);
                    fis.close();

                    server.sendFile(fileData, file.getName(), username, recipient);
                } catch (Exception ex) {
                    showError("Error sending file: " + ex.getMessage());
                }
            }
        }
    }

    private void toggleTheme() {
        darkMode = !darkMode;
        try {
            setTheme(darkMode);
        } catch (RemoteException e) {
            showError("Error changing theme");
        }
    }

    private void sendPrivateMessage() {
        String recipient = (String) JOptionPane.showInputDialog(frame,
                "Select recipient:", "Private Message",
                JOptionPane.PLAIN_MESSAGE, null,
                listModel.toArray(), null);

        if (recipient != null) {
            String message = JOptionPane.showInputDialog(frame, "Message to " + recipient + ":");
            if (message != null && !message.trim().isEmpty()) {
                try {
                    server.sendPrivateMessage(message.trim(), username, recipient);
                } catch (RemoteException ex) {
                    showError("Failed to send private message");
                }
            }
        }
    }

    private void showEmojiPicker() {
        JDialog emojiDialog = new JDialog(frame, "Emoji Picker", true);
        emojiDialog.setLayout(new GridLayout(0, 8, 5, 5));

        String[] emojis = {"😀", "😃", "😄", "😁", "😆", "😅", "😂", "🤣",
                "😊", "😇", "🙂", "🙃", "😉", "😌", "😍", "🥰",
                "😎", "🤩", "😘", "😗", "😙", "😚", "😋", "😛"};

        for (String emoji : emojis) {
            JButton btn = new JButton(emoji);
            btn.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 24));
            btn.addActionListener(e -> {
                messageField.setText(messageField.getText() + emoji);
                emojiDialog.dispose();
            });
            emojiDialog.add(btn);
        }

        emojiDialog.pack();
        emojiDialog.setLocationRelativeTo(frame);
        emojiDialog.setVisible(true);
    }



    // ClientInterface implementation
    public void receiveMessage(String message) throws RemoteException {
        SwingUtilities.invokeLater(() -> {
            chatArea.append(message + "\n");
        });
    }

    public void receiveFile(byte[] fileData, String fileName) throws RemoteException {
        SwingUtilities.invokeLater(() -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setSelectedFile(new File(fileName));
            if (fileChooser.showSaveDialog(frame) == JFileChooser.APPROVE_OPTION) {
                try (FileOutputStream fos = new FileOutputStream(fileChooser.getSelectedFile())) {
                    fos.write(fileData);

                } catch (IOException e) {
                    showError("Error saving file");
                }
            }
        });
    }

    public void updateUserList(String[] users) throws RemoteException {
        SwingUtilities.invokeLater(() -> {
            listModel.clear();
            for (String user : users) {
                listModel.addElement(user); // Garder seulement le nom d'utilisateur
            }
            userList.repaint(); // Forcer le rafraîchissement de l'affichage
        });
    }


    public void setTheme(boolean darkMode) throws RemoteException {
        SwingUtilities.invokeLater(() -> {
            Color background = darkMode ? new Color(30, 30, 30) : Color.WHITE;
            Color foreground = darkMode ? Color.WHITE : Color.BLACK;

            // Apply theme to components
            chatArea.setBackground(background);
            chatArea.setForeground(foreground);
            messageField.setBackground(background);
            messageField.setForeground(foreground);
            userList.setBackground(background);
            userList.setForeground(foreground);

            // Update button backgrounds
            for (Component comp : frame.getContentPane().getComponents()) {
                if (comp instanceof JPanel) {
                    JPanel panel = (JPanel) comp;
                    panel.setBackground(background);
                    for (Component innerComp : panel.getComponents()) {
                        if (innerComp instanceof JButton) {
                            innerComp.setBackground(background);
                            innerComp.setForeground(foreground);
                        }
                    }
                }
            }

            frame.repaint();
        });
    }
    private void showError(String message) {
        JOptionPane.showMessageDialog(frame, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    private void refreshUserList() {
        userList.repaint();
    }


    public static void main(String[] args) {
        String username = JOptionPane.showInputDialog("Enter your username:");
        if (username != null && !username.trim().isEmpty()) {
            try {
                new ChatClient(username.trim());
            } catch (RemoteException e) {
                JOptionPane.showMessageDialog(null, "Error creating client");
            }
        }
    }
}