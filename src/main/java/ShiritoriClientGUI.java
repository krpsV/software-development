import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.nio.charset.Charset;

public class ShiritoriClientGUI extends JFrame {
    private JTextArea messageArea;
    private JTextField inputField;
    private JButton sendButton;
    private JButton resetButton;
    private JButton quitButton;

    private BufferedReader in;
    private PrintWriter out;
    private Socket socket;

    public ShiritoriClientGUI(String host, int port) {
        setTitle("����Ƃ�N���C�A���g");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(500, 400);
        setLayout(new BorderLayout());

        messageArea = new JTextArea();
        messageArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(messageArea);
        add(scrollPane, BorderLayout.CENTER);

        JPanel inputPanel = new JPanel(new BorderLayout());
        inputField = new JTextField();
        sendButton = new JButton("���M");
        quitButton = new JButton("�ޏo");
        resetButton = new JButton("���Z�b�g");

        inputPanel.add(inputField, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(sendButton);
        buttonPanel.add(quitButton);
        buttonPanel.add(resetButton);

        inputPanel.add(buttonPanel, BorderLayout.EAST);

        add(inputPanel, BorderLayout.SOUTH);

        // �\�P�b�g�ڑ�
        try {
            socket = new Socket(host, port);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), Charset.forName("MS932")));
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), Charset.forName("MS932")), true);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "�T�[�o�[�ɐڑ��ł��܂���ł����B", "�G���[", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }

        // ��M�p�X���b�h
        Thread receiverThread = new Thread(() -> {
            try {
                String message;
                while ((message = in.readLine()) != null) {
                    String finalMessage = message;
                    SwingUtilities.invokeLater(() -> {
                        messageArea.append(finalMessage + "\n");
                        messageArea.setCaretPosition(messageArea.getDocument().getLength());
                    });
                }
            } catch (IOException e) {
                SwingUtilities.invokeLater(() -> {
                    messageArea.append("��M�G���[: " + e.getMessage() + "\n");
                });
            }
        });
        receiverThread.start();

        // ���M�{�^���̏���
        sendButton.addActionListener(e -> sendWord());

        // Enter�L�[�ő��M
        inputField.addActionListener(e -> sendWord());

        // ���Z�b�g�{�^���̏���
        resetButton.addActionListener(e -> {
            out.println("/reset");
        });

        quitButton.addActionListener(e -> {
            out.println("/quit");
            System.exit(0);
        });

        setVisible(true);
    }

    private void sendWord() {
        String input = inputField.getText().trim();
        if (input.isEmpty()) {
            return;
        }

        if (input.equals("/quit") || input.equals("/reset")) {
            out.println(input);
        } else {
            // �P��̂݃��[�U�[�ɓ��͂����A���M���Ɏ�����t�����đ���
            long timestamp = System.currentTimeMillis();
            out.println(input + "##" + timestamp);
        }
        inputField.setText("");
    }
public static void main(String[] args) {
    final String host = args.length >= 1 ? args[0] : "localhost";
    final int port = args.length >= 2 ? Integer.parseInt(args[1]) : 8081;

    SwingUtilities.invokeLater(() -> new ShiritoriClientGUI(host, port));
}

}
