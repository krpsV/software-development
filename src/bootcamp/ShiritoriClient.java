package bootcamp;

import java.io.*;
import java.net.*;
import java.util.Scanner;

public class ShiritoriClient {
    public static void main(String[] args) throws IOException {
        int port = 8081;
        if (args.length >= 1) {
            port = Integer.parseInt(args[0]);
        }
        
        InetAddress addr = InetAddress.getByName("localhost");
        Socket socket = new Socket(addr, port);
        
        try {
            BufferedReader in = new BufferedReader(
                new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(
                new BufferedWriter(
                    new OutputStreamWriter(socket.getOutputStream())), true);
            
            Scanner scanner = new Scanner(System.in);
            
            // サーバーからのメッセージを受信するスレッド
            Thread receiverThread = new Thread(() -> {
                try {
                    String message;
                    while ((message = in.readLine()) != null) {
                        System.out.println(message);
                    }
                } catch (IOException e) {
                    System.err.println("受信エラー: " + e.getMessage());
                }
            });
            receiverThread.start();
            
            System.out.println("しりとりクライアント開始");
            System.out.println("単語を入力してください（/quit で終了、/reset でゲームリセット）:");
            
            // ユーザー入力を送信
            String input;
            while ((input = scanner.nextLine()) != null) {
                out.println(input);
                if (input.equals("/quit")) {
                    break;
                }
            }
            
            receiverThread.interrupt();
            scanner.close();
            
        } finally {
            socket.close();
        }
    }
}