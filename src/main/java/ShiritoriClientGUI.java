

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.*;
import java.nio.charset.Charset;

public class ShiritoriClientGUI extends JFrame {
    private JTextArea messageArea;
    private JTextField inputField;
    private JButton sendButton;
    private JButton resetButton;
    private JButton quitButton;

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    public ShiritoriClientGUI() {
        super("しりとりゲームクライアント");

        messageArea = new JTextArea();
        messageArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(messageArea);

        inputField = new JTextField();
        sendButton = new JButton("送信");
        resetButton = new JButton("/reset");
        quitButton = new JButton("/quit");

        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.add(inputField, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(sendButton);
        buttonPanel.add(resetButton);
        buttonPanel.add(quitButton);
        inputPanel.add(buttonPanel, BorderLayout.EAST);

        add(scrollPane, BorderLayout.CENTER);
        add(inputPanel, BorderLayout.SOUTH);

        sendButton.addActionListener(e -> sendMessage());
        inputField.addActionListener(e -> sendMessage());
        resetButton.addActionListener(e -> sendCommand("/reset"));
        quitButton.addActionListener(e -> {
            sendCommand("/quit");
            dispose();
            try { if (socket != null) socket.close(); } catch (IOException ignored) {}
            System.exit(0);
        });

        setSize(500, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);

        connectToServer();
    }

    private void connectToServer() {
        try {
            socket = new Socket("localhost", 8081);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), Charset.forName("MS932")));
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), Charset.forName("MS932")), true);

            new Thread(() -> {
                try {
                    String msg;
                    while ((msg = in.readLine()) != null) {
                        final String displayMsg = msg;
                        SwingUtilities.invokeLater(() -> messageArea.append(displayMsg + "\n"));
                    }
                } catch (IOException e) {
                    SwingUtilities.invokeLater(() -> messageArea.append("切断: " + e.getMessage() + "\n"));
                }
            }).start();

            messageArea.append("サーバーに接続しました。\nしりとりゲーム開始！\n");
        } catch (IOException e) {
            messageArea.append("接続失敗: " + e.getMessage() + "\n");
        }
    }

    private void sendMessage() {
        String text = inputField.getText().trim();
        if (!text.isEmpty()) {
            out.println(text);
            inputField.setText("");
        }
    }

    private void sendCommand(String cmd) {
        out.println(cmd);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ShiritoriClientGUI::new);
    }
}
