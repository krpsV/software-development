// package bootcamp;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class ShiritoriServer {
    private static final int PORT = 8081;
    private static final List<ClientHandler> clients = new ArrayList<>();
    private static final Map<String, Integer> scores = new HashMap<>();
    private static final Map<String, String> longestWords = new HashMap<>();
    private static final Set<String> usedWords = new HashSet<>();
    private static String lastWord = "��";
    private static boolean gameEnded = false;
    private static final AtomicInteger playerCount = new AtomicInteger(1);

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
            out.println("����Ƃ�J�n�B�ŏ��̕����́w" + lastWord + "�x�ł��B�P�����́i/quit �܂��� /reset�j:");
            broadcast(playerName + "���Q�����܂����B");

            try {
                while (true) {
                    String input = in.readLine();
                    if (input == null || input.equals("/quit")) break;

                    synchronized (ShiritoriServer.class) {
                        if (input.equals("/reset")) {
                            resetGame(playerName);
                            continue;
                        }

                        if (gameEnded) {
                            out.println("�Q�[���͏I�����܂����B/reset �ōĊJ�ł��܂��B");
                            continue;
                        }

                        String normalized = normalize(input);
                        if (usedWords.contains(normalized)) {
                            out.println("���Ɏg�p�ς݂̒P��ł��B");
                            continue;
                        }
                        if (!normalized.startsWith(lastWord)) {
                            out.println("�w" + lastWord + "�x�Ŏn�܂�P�����͂��Ă��������B");
                            continue;
                        }

                        if (normalized.endsWith("��")) {
                            scores.put(playerName, 0);
                            gameEnded = true;
                            broadcast(playerName + "���u��v�ŏI���B�Q�[���I���B");
                            determineWinner();
                            continue; // �� �ޏo�������̓��͂�҂�
                        }

                        usedWords.add(normalized);
                        int points = input.length() * 10;
                        scores.put(playerName, scores.get(playerName) + points);

                        if (input.length() > longestWords.get(playerName).length()) {
                            longestWords.put(playerName, input);
                        }

                        broadcast(playerName + ": " + input + "�i���_: " + scores.get(playerName) + "�j");
                        lastWord = getNextHead(normalized);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    socket.close();
                } catch (IOException ignored) {}
                broadcast(playerName + "���ޏo���܂����B");
            }
        }
    }

    private static void broadcast(String message) {
        synchronized (clients) {
            for (ClientHandler client : clients) {
                client.out.println(message);
            }
        }
    }

    private static void resetGame(String requestedBy) {
        lastWord = "��";
        gameEnded = false;
        usedWords.clear();
        scores.replaceAll((k, v) -> 0);
        longestWords.replaceAll((k, v) -> "");
        broadcast("? " + requestedBy + "���Q�[�������Z�b�g���܂����B�ŏ��̕����́w" + lastWord + "�x�ł��B");
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
            broadcast("? �Œ��P��� " + longestPlayer + " �� +100�_�{�[�i�X�I");
        }

        String winner = Collections.max(scores.entrySet(), Map.Entry.comparingByValue()).getKey();
        broadcast("? ����: " + winner + "�i���_: " + scores.get(winner) + "�j");
    }

    private static String normalize(String word) {
        return word.replace("�[", "");
    }

    private static String getNextHead(String word) {
        String ch = word.substring(word.length() - 1);
        if (ch.equals("�[") && word.length() >= 2) {
            ch = word.substring(word.length() - 2, word.length() - 1);
        }

        Map<String, String> small = Map.of(
            "��", "��", "��", "��", "��", "��", "��", "��", "��", "��",
            "��", "��", "��", "��", "��", "��", "��", "��", "��", "��"
        );

        if (small.containsKey(ch)) {
            ch = small.get(ch);
        }

        return ch;
    }
}
