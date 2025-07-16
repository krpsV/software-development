import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import com.atilika.kuromoji.ipadic.Tokenizer; // �ǉ�
import com.atilika.kuromoji.ipadic.Token;

public class ShiritoriServer {
    private static final int PORT = 8081;
    private static final List<ClientHandler> clients = new ArrayList<>();
    private static final Map<String, Integer> scores = new HashMap<>();
    private static final Map<String, String> longestWords = new HashMap<>();
    private static final Set<String> usedWords = new HashSet<>();
    private static String lastWord = "��";
    private static boolean gameEnded = false;
    private static final AtomicInteger playerCount = new AtomicInteger(1);
    private static final Tokenizer tokenizer = new Tokenizer();

    // �P�\���Ԓ��̓��͂��ꎞ�I�ɕۊ�
    private static final List<PendingEntry> pendingEntries = new ArrayList<>();
    private static boolean waiting = false;

    // 30�b�^�C�}�[
    private static final Timer inactivityTimer = new Timer(true);
    private static TimerTask currentInactivityTask;

    private static class PendingEntry {
        String playerName;
        String word;
        long timestamp;

        PendingEntry(String playerName, String word, long timestamp) {
            this.playerName = playerName;
            this.word = word;
            this.timestamp = timestamp;
        }
    }

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(PORT);
        System.out.println("����Ƃ�T�[�o�[�N�� �|�[�g: " + PORT);

        try {
            while (true) {
                Socket socket = serverSocket.accept();
                ClientHandler handler = new ClientHandler(socket);
                synchronized (clients) {
                    clients.add(handler);
                }
                new Thread(handler).start();
            }
        } finally {
            serverSocket.close();
        }
    }

    private static class ClientHandler implements Runnable {
        private final Socket socket;
        private final BufferedReader in;
        private final PrintWriter out;
        private final String playerName;

        public ClientHandler(Socket socket) throws IOException {
            this.socket = socket;
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "MS932"));
            this.out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "MS932"), true);
            this.playerName = "�v���C���[" + playerCount.getAndIncrement();
            synchronized (scores) {
                scores.put(playerName, 0);
                longestWords.put(playerName, "");
            }
        }

        public void run() {
            out.println("����Ƃ�J�n�B�ŏ��̕����́w" + lastWord + "�x�ł��B�P�����͂��Ă�������:");
            broadcast(playerName + "���Q�����܂����B");

            try {
                while (true) {
                    String input = in.readLine();
                    if (input == null || input.equals("/quit")) break;

                    if (input.equals("/reset")) {
                        resetGame();
                        broadcast(playerName + "���Q�[�������Z�b�g���܂����B�ŏ��̕����́w" + lastWord + "�x�ł��B");
                        continue;
                    }

                    if (gameEnded) {
                        out.println("�Q�[���͏I�����܂����B/reset �ōĊJ�ł��܂��B");
                        continue;
                    }

                    // �`��: �P��##���M����
                    if (!input.contains("##")) {
                        out.println("�`��������������܂���B��: ������##1651234567890");
                        continue;
                    }

                    String[] parts = input.split("##");
                    if (parts.length != 2) {
                        out.println("���͌`���G���[: �P��##���M����");
                        continue;
                    }

                    String word = parts[0];
                    long timestamp;
                    try {
                        timestamp = Long.parseLong(parts[1]);
                    } catch (NumberFormatException e) {
                        out.println("���M�����̌`�����s���ł��B");
                        continue;
                    }

                    synchronized (ShiritoriServer.class) {
                        String normalized = normalize(word);

                        if (!isNoun(normalized)) { // �V�����w���p�[���\�b�h���Ăяo��
                            out.println("���͂��ꂽ�P��͖����ł͂���܂���B");
                            continue;
                        }
                        if (usedWords.contains(normalized) || !normalized.startsWith(lastWord)) {
                            out.println("�����ȒP��ł��B");
                            continue;
                        }

                        pendingEntries.add(new PendingEntry(playerName, word, timestamp));

                        if (!waiting) {
                            waiting = true;
                            new Timer().schedule(new TimerTask() {
                                public void run() {
                                    processPendingEntries();
                                }
                            }, 1000);
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try { socket.close(); } catch (IOException ignored) {}
                broadcast(playerName + "���ޏo���܂����B");
            }
        }

        public void send(String message) {
            out.println(message);
        }
    }

    private static void processPendingEntries() {
        synchronized (ShiritoriServer.class) {
            if (pendingEntries.isEmpty()) {
                waiting = false;
                return;
            }

            PendingEntry selected = Collections.min(pendingEntries, Comparator.comparingLong(e -> e.timestamp));
            String normalized = normalize(selected.word);

            usedWords.add(normalized);
            String playerName = selected.playerName;

            if (normalized.endsWith("��")) {
                scores.put(playerName, 0);
                gameEnded = true;
                broadcast(playerName + "���u��v�ŏI���B�Q�[���I���B");
                determineWinner();
            } else {
                int points = selected.word.length() * 10;
                scores.put(playerName, scores.get(playerName) + points);
                if (selected.word.length() > longestWords.get(playerName).length()) {
                    longestWords.put(playerName, selected.word);
                }
                lastWord = getNextHead(normalized);
                broadcast(playerName + ": " + selected.word + "�i���_: " + scores.get(playerName) + "�j");

                // 30�b�^�C�}�[�Đݒ�
                if (currentInactivityTask != null) {
                    currentInactivityTask.cancel();
                }
                currentInactivityTask = new TimerTask() {
                    public void run() {
                        synchronized (ShiritoriServer.class) {
                            if (!gameEnded) {
                                gameEnded = true;
                                broadcast("30�b�ԓ��͂��Ȃ��������߁A�Q�[�����I�����܂��B");
                                determineWinner();
                            }
                        }
                    }
                };
                inactivityTimer.schedule(currentInactivityTask, 30_000);
            }

            pendingEntries.clear();
            waiting = false;
        }
    }

    private static boolean isNoun(String word) {
        if (word == null || word.trim().isEmpty()) {
            return false;
        }
        List<Token> tokens = tokenizer.tokenize(word);
        if (tokens.isEmpty()) {
            return false;
        }
        // �Ō�̃g�[�N���������ł��邩���`�F�b�N
        Token lastToken = tokens.get(tokens.size() - 1);
        return lastToken.getPartOfSpeechLevel1().equals("����");
    }

    private static void broadcast(String message) {
        synchronized (clients) {
            for (ClientHandler client : clients) {
                client.send(message);
            }
        }
    }

    private static void resetGame() {
        lastWord = "��";
        gameEnded = false;
        usedWords.clear();
        scores.replaceAll((k, v) -> 0);
        longestWords.replaceAll((k, v) -> "");
        pendingEntries.clear();
        waiting = false;
        if (currentInactivityTask != null) {
            currentInactivityTask.cancel();
        }
    }

    private static void determineWinner() {
        String longestPlayer = null;
        int maxLen = 0;

        for (Map.Entry<String, String> entry : longestWords.entrySet()) {
            String player = entry.getKey();
            String word = entry.getValue();
            if (scores.get(player) == 0) continue;
            if (word.length() > maxLen) {
                maxLen = word.length();
                longestPlayer = player;
            }
        }

        if (longestPlayer != null) {
            scores.put(longestPlayer, scores.get(longestPlayer) + 100);
            broadcast("�Œ��P��� " + longestPlayer + " �� +100�_");
        }

        String winner = Collections.max(scores.entrySet(), Map.Entry.comparingByValue()).getKey();
        broadcast("����: " + winner + "�i���_: " + scores.get(winner) + "�j");
    }

    private static String normalize(String word) {
        return word.replace("�[", "");
    }

    private static String getNextHead(String word) {
        String ch = word.substring(word.length() - 1);
        Map<String, String> small = Map.of("��", "��", "��", "��", "��", "��", "��", "��", "��", "��",
            "��", "��", "��", "��", "��", "��", "��", "��", "��", "��");
        if (ch.equals("�[") && word.length() >= 2) {
            ch = word.substring(word.length() - 2, word.length() - 1);
        }
        if (small.containsKey(ch)) {
            ch = small.get(ch);
        }
        return ch;
    }
}
