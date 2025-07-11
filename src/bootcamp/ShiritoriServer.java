//package bootcamp;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.nio.charset.Charset;

public class ShiritoriServer {
    public static int PORT = 8081;
    private static final Set<String> usedWords = ConcurrentHashMap.newKeySet();
    private static final List<ClientHandler> clients = new CopyOnWriteArrayList<>();
    private static String currentLastChar = "";
    private static boolean gameStarted = false;
    
    public static void main(String[] args) throws IOException {
        if (args.length >= 1) {
            PORT = Integer.parseInt(args[0]);
        }
        
        ServerSocket serverSocket = new ServerSocket(PORT);
        System.out.println("����Ƃ�T�[�o�[�J�n: �|�[�g " + PORT);
        System.out.println("�N���C�A���g�̐ڑ���ҋ@��...");
        
        try {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                ClientHandler handler = new ClientHandler(clientSocket, clients.size() + 1);
                clients.add(handler);
                new Thread(handler).start();
                
                broadcastMessage("�v���C���[" + handler.playerId + "���Q�����܂����B����" + clients.size() + "�l");
                if (!gameStarted && clients.size() >= 2) {
                    gameStarted = true;
                    broadcastMessage("�Q�[���J�n�I�ŏ��̒P�����͂��Ă��������B");
                }
            }
        } finally {
            serverSocket.close();
        }
    }
    
    public static synchronized void broadcastMessage(String message) {
        System.out.println("�u���[�h�L���X�g: " + message);
        for (ClientHandler client : clients) {
            client.sendMessage(message);
        }
    }
    
    public static synchronized boolean processWord(String word, int playerId) {
        // �����̐��K��
        String normalizedWord = normalizeWord(word);
        byte[] ms932Bytes = normalizedWord.getBytes(Charset.forName("MS932"));
        String decodedNomalizedWord = new String(ms932Bytes, Charset.forName("MS932"));
        
        // �g�p�ςݒP��`�F�b�N
        if (usedWords.contains(word)) {
            return false;
        }
        
        // ����Ƃ胋�[���`�F�b�N
        if (!currentLastChar.isEmpty()) {
            String firstChar = getFirstChar(decodedNomalizedWord);
            if (!firstChar.equals(currentLastChar)) {
                return false;
            }
        }
        
        // �u��v�ŏI���P��`�F�b�N
        String lastChar = getLastChar(decodedNomalizedWord);
        if (lastChar.equals("��")) {
            usedWords.add(decodedNomalizedWord);
            broadcastMessage("�v���C���[" + playerId + ": " + word + " ���u��v�ŏI������̂Ńv���C���[" + playerId + "�̕����I");
            broadcastMessage("�Q�[���I���B�V�����Q�[�����J�n����ɂ�/reset�Ɠ��͂��Ă��������B");
            return true;
        }
        
        // �P�����
        usedWords.add(decodedNomalizedWord);
        currentLastChar = lastChar;
        broadcastMessage("�v���C���[" + playerId + ": " + word + " (���́u" + currentLastChar + "�v����)");
        return true;
    }
    
    public static synchronized void resetGame() {
        usedWords.clear();
        currentLastChar = "";
        broadcastMessage("�Q�[�����Z�b�g�I�V�����Q�[�����J�n���܂��B");
    }
    
    private static String normalizeWord(String word) {
        // �L�΂��_���폜
        return word.replaceAll("�[", "");
    }
    
    private static String getFirstChar(String word) {
        if (word.isEmpty()) return "";
        return String.valueOf(word.charAt(0));
    }
    
    private static String getLastChar(String word) {
        if (word.isEmpty()) return "";
        return String.valueOf(word.charAt(word.length() - 1));
    }
    
    static class ClientHandler implements Runnable {
        private final Socket socket;
        private final int playerId;
        private PrintWriter out;
        
        public ClientHandler(Socket socket, int playerId) {
            this.socket = socket;
            this.playerId = playerId;
        }
        
        public void sendMessage(String message) {
            if (out != null) {
                out.println(message);
            }
        }
        
        @Override
        public void run() {
            try {
                BufferedReader in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), Charset.forName("MS932")));
                out = new PrintWriter(
                    new BufferedWriter(
                        new OutputStreamWriter(socket.getOutputStream(), Charset.forName("MS932"))), true);

                
                sendMessage("����Ƃ�Q�[���ւ悤�����I���Ȃ��̓v���C���[" + playerId + "�ł��B");
                sendMessage("�R�}���h: /reset (�Q�[�����Z�b�g), /quit (�I��)");
                
                String input;
                while ((input = in.readLine()) != null) {

                    if (input.equals("/quit") || input.equals("END")) {
                        break;
                    } else if (input.equals("/reset")) {
                        resetGame();
                    } else if (!input.trim().isEmpty()) {
                        if (!processWord(input.trim(), playerId)) {
                            sendMessage("�����ȒP��ł��B���R: ���łɎg�p�ς� �܂��� ����Ƃ胋�[���ᔽ");
                        }
                    }
                }
                
            } catch (IOException e) {
                System.err.println("�v���C���[" + playerId + "�̃G���[: " + e.getMessage());
            } finally {
                clients.remove(this);
                broadcastMessage("�v���C���[" + playerId + "���ޏo���܂����B����" + clients.size() + "�l");
                try {
                    socket.close();
                } catch (IOException e) {
                    System.err.println("Socket�N���[�Y�G���[: " + e.getMessage());
                }
            }
        }
    }
}
