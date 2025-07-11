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
        
        // UI Components
        messageArea = new JTextArea();
        messageArea.setEditable(false);
        messageArea.setLineWrap(true);
        messageArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(messageArea);
        
        inputField = new JTextField();
        sendButton = new JButton("送信");
        resetButton = new JButton("/reset");
        quitButton = new JButton("/quit");

        // Layout
        JPanel southPanel = new JPanel();
        southPanel.setLayout(new BorderLayout());
        southPanel.add(inputField, BorderLayout.CENTER);
        
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(sendButton);
        buttonPanel.add(resetButton);
        buttonPanel.add(quitButton);
        southPanel.add(buttonPanel, BorderLayout.EAST);

        add(scrollPane, BorderLayout.CENTER);
        add(southPanel, BorderLayout.SOUTH);

        // Event Listeners
        sendButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendMessage();
            }
        });

        inputField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendMessage();
            }
        });

        resetButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendCommand("/reset");
            }
        });

        quitButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendCommand("/quit");
                dispose(); // Close the window
                try {
                    if (socket != null) {
                        socket.close();
                    }
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
                System.exit(0); // Terminate the application
            }
        });

        // Frame settings
        setSize(500, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);

        // Connect to server
        connectToServer();
    }

    private void connectToServer() {
        try {
            int port = 8081; // Default port
            // You can add a dialog here to ask for port if needed
            
            InetAddress addr = InetAddress.getByName("localhost");
            socket = new Socket(addr, port);

            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), Charset.forName("MS932")));
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), Charset.forName("MS932")), true);

            // Start a new thread to listen for messages from the server
            new Thread(() -> {
                try {
                    String receivedMessage; // 新しい変数を宣言
                    while ((receivedMessage = in.readLine()) != null) { // ここで代入
                        final String messageToDisplay = receivedMessage; // effectively finalにするための新しい変数
                        SwingUtilities.invokeLater(() -> messageArea.append(messageToDisplay + "\n"));
                    }
                } catch (IOException e) {
                    SwingUtilities.invokeLater(() -> messageArea.append("サーバーから切断されました: " + e.getMessage() + "\n"));
                } finally {
                    try {
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();

            SwingUtilities.invokeLater(() -> messageArea.append("サーバーに接続しました。\n"));
            SwingUtilities.invokeLater(() -> messageArea.append("しりとりゲーム開始！\n"));

        } catch (IOException e) {
            SwingUtilities.invokeLater(() -> messageArea.append("サーバー接続エラー: " + e.getMessage() + "\n"));
            e.printStackTrace();
        }
    }

    private void sendMessage() {
        String message = inputField.getText().trim();
        if (!message.isEmpty()) {
            out.println(message);
            inputField.setText("");
        }
    }

    private void sendCommand(String command) {
        out.println(command);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new ShiritoriClientGUI();
            }
        });
    }
}
