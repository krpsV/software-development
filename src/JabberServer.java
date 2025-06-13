import java.io.*;
import java.net.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class JabberServer {
    public static int PORT = 8080; // ポート番号を設定する
    private static final ExecutorService threadPool = Executors.newCachedThreadPool();
    private static final AtomicInteger clientCount = new AtomicInteger(0);
    
    public static void main(String[] args) throws IOException {
        // コマンドライン引数からポート番号を取得
        if (args.length >= 1) {
            PORT = Integer.parseInt(args[0]);
        }
        
        ServerSocket serverSocket = new ServerSocket(PORT); // ソケットを作成する
        System.out.println("Multi-client server started: " + serverSocket);
        System.out.println("Waiting for client connections on port " + PORT + "...");
        
        try {
            while (true) {
                // クライアントからの接続を待ち、新しいスレッドで処理
                Socket clientSocket = serverSocket.accept();
                int clientId = clientCount.incrementAndGet();
                System.out.println("New client connected (ID: " + clientId + "): " + clientSocket);
                
                // 各クライアントを個別のスレッドで処理
                threadPool.submit(new ClientHandler(clientSocket, clientId));
            }
        } finally {
            serverSocket.close();
            threadPool.shutdown();
        }
    }
    
    // クライアント接続を処理するスレッドクラス
    static class ClientHandler implements Runnable {
        private final Socket socket;
        private final int clientId;
        
        public ClientHandler(Socket socket, int clientId) {
            this.socket = socket;
            this.clientId = clientId;
        }
        
        @Override
        public void run() {
            try {
                BufferedReader in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(
                    new BufferedWriter(
                        new OutputStreamWriter(socket.getOutputStream())), true);
                
                System.out.println("Client " + clientId + " handler started");
                
                while (true) {
                    String str = in.readLine(); // データの受信
                    if (str == null || str.equals("END")) {
                        break;
                    }
                    System.out.println("Client " + clientId + " says: " + str);
                    out.println("Echo from server (Client " + clientId + "): " + str); // データの送信
                }
                
            } catch (IOException e) {
                System.err.println("Error handling client " + clientId + ": " + e.getMessage());
            } finally {
                try {
                    socket.close();
                    System.out.println("Client " + clientId + " disconnected");
                } catch (IOException e) {
                    System.err.println("Error closing client " + clientId + " socket: " + e.getMessage());
                }
            }
        }
    }
}