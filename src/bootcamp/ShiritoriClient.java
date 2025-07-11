//package bootcamp;

import java.io.*;
import java.net.*;
import java.util.Scanner;
import java.nio.charset.Charset;

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
                new InputStreamReader(socket.getInputStream(), Charset.forName("MS932")));
            PrintWriter out = new PrintWriter(
                new BufferedWriter(
                    new OutputStreamWriter(socket.getOutputStream(), Charset.forName("MS932"))), true);

            Scanner scanner = new Scanner(System.in, Charset.forName("MS932"));
            
            // ?T?[?o?[???????b?Z?[?W????M????X???b?h
            Thread receiverThread = new Thread(() -> {
                try {
                    String message;
                    while ((message = in.readLine()) != null) {
                        System.out.println(message);
                    }
                } catch (IOException e) {
                    System.err.println("??M?G???[: " + e.getMessage());
                }
            });
            receiverThread.start();
            
            System.out.println("??????N???C?A???g?J?n");
            System.out.println("?P?????????????????i/quit ??I???A/reset ??Q?[?????Z?b?g?j:");
            
            // ???[?U?[?????M
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
