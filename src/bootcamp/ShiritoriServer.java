package bootcamp;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class ShiritoriServer {
    private static final int PORT = 12345;
    private static final List<ClientHandler> clients = new ArrayList<>();
    private static final Map<String, Integer> scores = new HashMap<>();
    private static final Map<String, String> longestWords = new HashMap<>();
    private static String lastWord = "り";
    private static boolean gameEnded = false;
    private static final AtomicInteger playerCount = new AtomicInteger(1);
    private static final Set<String> usedWords = new HashSet<>();

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(PORT);
        System.out.println("しりとりサーバー起動 ポート: " + PORT);

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
            this.playerName = "プレイヤー" + playerCount.getAndIncrement();
            synchronized (scores) {
                scores.put(playerName, 0);
                longestWords.put(playerName, "");
            }
        }

        private void sendMessage(String msg) {
            out.println(msg);
        }

        @Override
        public void run() {
            sendMessage("しりとりゲームへようこそ！あなたは" + playerName + "です。");
            sendMessage("コマンド: /reset (ゲームリセット), /quit (終了)");
            sendMessage("最初の文字は『" + lastWord + "』です。単語を入力してください。");

            try {
                String input;
                while ((input = in.readLine()) != null) {
                    if (input.equals("/quit")) {
                        break;
                    } else if (input.equals("/reset")) {
                        resetGame();
                        broadcast("ゲームがリセットされました。最初の文字は『" + lastWord + "』です。");
                    } else if (!input.trim().isEmpty()) {
                        boolean valid = processWord(input.trim(), playerName);
                        if (!valid) {
                            sendMessage("無効な単語です。前の単語の最後の文字『" + lastWord + "』で始まる単語を入力してください。");
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try { socket.close(); } catch (IOException ignored) {}
                broadcast(playerName + "が退出しました。");
                synchronized (clients) {
                    clients.remove(this);
                }
            }
        }
    }

    private static synchronized boolean processWord(String word, String playerName) {
        // すでに使用済み単語かチェック
        if (usedWords.contains(word)) {
            return false;
        }

        // 最初の単語は lastWord と比較
        if (!word.startsWith(lastWord)) {
            return false;
        }

        // 「ん」で終わると負け
        if (word.endsWith("ん")) {
            scores.put(playerName, 0);
            gameEnded = true;
            broadcast(playerName + "が「ん」で終わったため負け！ゲーム終了！");
            determineWinner();
            return true;
        }

        // 正常な単語として記録
        usedWords.add(word);

        // 得点加算（文字数×10）
        int points = word.length() * 10;
        scores.put(playerName, scores.get(playerName) + points);

        // 最長単語更新
        if (word.length() > longestWords.get(playerName).length()) {
            longestWords.put(playerName, word);
        }

        // lastWord を更新
        lastWord = word.substring(word.length() - 1);

        broadcast(playerName + ": " + word + "（得点: " + scores.get(playerName) + "）");

        return true;
    }

    private static synchronized void resetGame() {
        lastWord = "り";
        gameEnded = false;
        usedWords.clear();
        for (String player : scores.keySet()) {
            scores.put(player, 0);
            longestWords.put(player, "");
        }
    }

    private static void broadcast(String message) {
        synchronized (clients) {
            for (ClientHandler client : clients) {
                client.sendMessage(message);
            }
        }
    }

    private static void determineWinner() {
        String longestPlayer = null;
        int maxLen = 0;

        for (Map.Entry<String, String> entry : longestWords.entrySet()) {
            String player = entry.getKey();
            String word = entry.getValue();

            // 負けた（得点0）のプレイヤーは長さボーナス対象外
            if (scores.get(player) == 0) continue;

            if (word.length() > maxLen) {
                maxLen = word.length();
                longestPlayer = player;
            }
        }

        // 最長単語ボーナス加算 +100点
        if (longestPlayer != null) {
            scores.put(longestPlayer, scores.get(longestPlayer) + 100);
            broadcast("最も長い単語のプレイヤー " + longestPlayer + " に +100点のボーナス！");
        }

        // トータル得点が最大のプレイヤーを勝者とする
        String winner = Collections.max(scores.entrySet(), Map.Entry.comparingByValue()).getKey();
        int maxScore = scores.get(winner);

        broadcast("勝者: " + winner + "（得点: " + maxScore + "）");
    }
}
