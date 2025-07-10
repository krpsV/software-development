package bootcamp;

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
    private static String lastWord = "り";
    private static boolean gameEnded = false;
    private static final AtomicInteger playerCount = new AtomicInteger(1);

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

        public void run() {
            out.println("しりとりクライアント開始。最初の文字は『" + lastWord + "』です。単語を入力してください（/quit で終了、/reset でリセット）:");
            broadcast(playerName + "が参加しました。");

            try {
                while (true) {
                    String input = in.readLine();
                    if (input == null || input.equals("/quit")) break;

                    synchronized (ShiritoriServer.class) {
                        if (input.equals("/reset")) {
                            resetGame();
                            broadcast(playerName + "がゲームをリセットしました。新しい最初の文字は『" + lastWord + "』です。");
                            continue;
                        }

                        if (gameEnded) {
                            out.println("ゲームは終了しています。/reset でリセットしてください。");
                            continue;
                        }

                        if (!input.startsWith(lastWord)) {
                            out.println("前の単語の最後の文字『" + lastWord + "』で始まる単語を入力してください。");
                            continue;
                        }

                        if (usedWords.contains(input)) {
                            out.println("この単語は既に使用されています。別の単語を入力してください。");
                            continue;
                        }

                        if (input.endsWith("ん")) {
                            scores.put(playerName, 0);
                            gameEnded = true;
                            broadcast(playerName + "が「ん」で終えたので負け！ゲーム終了！");
                            determineWinner();
                            break;
                        }

                        usedWords.add(input);

                        // 得点加算（文字数×10）
                        int points = input.length() * 10;
                        scores.put(playerName, scores.get(playerName) + points);

                        // 最長単語の更新
                        if (input.length() > longestWords.get(playerName).length()) {
                            longestWords.put(playerName, input);
                        }

                        broadcast(playerName + ": " + input + "（得点: " + scores.get(playerName) + "）");

                        // 最後の文字の変換処理
                        String lastChar = input.substring(input.length() - 1);
                        lastChar = normalizeLastChar(lastChar, input);
                        lastWord = lastChar;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    socket.close();
                } catch (IOException ignored) {}
                broadcast(playerName + "が退出しました。");
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

    private static void resetGame() {
        lastWord = "り";
        gameEnded = false;
        usedWords.clear();
        scores.replaceAll((k, v) -> 0);
        longestWords.replaceAll((k, v) -> "");
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
            broadcast("最も長い単語のプレイヤー " + longestPlayer + " に +100点のボーナス！");
        }

        String winner = Collections.max(scores.entrySet(), Map.Entry.comparingByValue()).getKey();
        int maxScore = scores.get(winner);

        broadcast("勝者: " + winner + "（得点: " + maxScore + "）");
    }

    private static String normalizeLastChar(String lastChar, String word) {
        // 伸ばし棒なら直前の文字
        if (lastChar.equals("ー") && word.length() > 1) {
            lastChar = word.substring(word.length() - 2, word.length() - 1);
        }

        // 小文字なら大文字に変換
        Map<String, String> smallToLarge = Map.of(
            "ぁ", "あ", "ぃ", "い", "ぅ", "う", "ぇ", "え", "ぉ", "お",
            "ゃ", "や", "ゅ", "ゆ", "ょ", "よ",
            "っ", "つ",
            "ゎ", "わ"
        );

        if (smallToLarge.containsKey(lastChar)) {
            lastChar = smallToLarge.get(lastChar);
        }

        return lastChar;
    }
}

