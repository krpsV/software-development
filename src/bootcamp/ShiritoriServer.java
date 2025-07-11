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
        System.out.println("しりとりサーバー開始: ポート " + PORT);
        System.out.println("クライアントの接続を待機中...");
        
        try {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                ClientHandler handler = new ClientHandler(clientSocket, clients.size() + 1);
                clients.add(handler);
                new Thread(handler).start();
                
                broadcastMessage("プレイヤー" + handler.playerId + "が参加しました。現在" + clients.size() + "人");
                if (!gameStarted && clients.size() >= 2) {
                    gameStarted = true;
                    broadcastMessage("ゲーム開始！最初の単語を入力してください。");
                }
            }
        } finally {
            serverSocket.close();
        }
    }
    
    public static synchronized void broadcastMessage(String message) {
        System.out.println("ブロードキャスト: " + message);
        for (ClientHandler client : clients) {
            client.sendMessage(message);
        }
    }
    
    public static synchronized boolean processWord(String word, int playerId) {
        // 文字の正規化
        String normalizedWord = normalizeWord(word);
        byte[] ms932Bytes = normalizedWord.getBytes(Charset.forName("MS932"));
        String decodedNomalizedWord = new String(ms932Bytes, Charset.forName("MS932"));
        
        // 使用済み単語チェック
        if (usedWords.contains(word)) {
            return false;
        }
        
        // しりとりルールチェック
        if (!currentLastChar.isEmpty()) {
            String firstChar = getFirstChar(decodedNomalizedWord);
            if (!firstChar.equals(currentLastChar)) {
                return false;
            }
        }
        
        // 「ん」で終わる単語チェック
        String lastChar = getLastChar(decodedNomalizedWord);
        if (lastChar.equals("ん")) {
            usedWords.add(decodedNomalizedWord);
            broadcastMessage("プレイヤー" + playerId + ": " + word + " →「ん」で終わったのでプレイヤー" + playerId + "の負け！");
            broadcastMessage("ゲーム終了。新しいゲームを開始するには/resetと入力してください。");
            return true;
        }
        
        // 単語を受理
        usedWords.add(decodedNomalizedWord);
        currentLastChar = lastChar;
        broadcastMessage("プレイヤー" + playerId + ": " + word + " (次は「" + currentLastChar + "」から)");
        return true;
    }
    
    public static synchronized void resetGame() {
        usedWords.clear();
        currentLastChar = "";
        broadcastMessage("ゲームリセット！新しいゲームを開始します。");
    }
    
    private static String normalizeWord(String word) {
        // 伸ばし棒を削除
        return word.replaceAll("ー", "");
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

                
                sendMessage("しりとりゲームへようこそ！あなたはプレイヤー" + playerId + "です。");
                sendMessage("コマンド: /reset (ゲームリセット), /quit (終了)");
                
                String input;
                while ((input = in.readLine()) != null) {

                    if (input.equals("/quit") || input.equals("END")) {
                        break;
                    } else if (input.equals("/reset")) {
                        resetGame();
                    } else if (!input.trim().isEmpty()) {
                        if (!processWord(input.trim(), playerId)) {
                            sendMessage("無効な単語です。理由: すでに使用済み または しりとりルール違反");
                        }
                    }
                }
                
            } catch (IOException e) {
                System.err.println("プレイヤー" + playerId + "のエラー: " + e.getMessage());
            } finally {
                clients.remove(this);
                broadcastMessage("プレイヤー" + playerId + "が退出しました。現在" + clients.size() + "人");
                try {
                    socket.close();
                } catch (IOException e) {
                    System.err.println("Socketクローズエラー: " + e.getMessage());
                }
            }
        }
    }
}
