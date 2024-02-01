package util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class ChatServer {

    private static final int PORT = 6000;
    private static List<ClientHandler> clients = new ArrayList<>();

    public static void main(String[] args) {
        try {
            ServerSocket serverSocket = new ServerSocket(PORT);
            System.out.println("Servidor iniciado en el puerto " + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Nuevo cliente conectado");

                ClientHandler clientHandler = new ClientHandler(clientSocket);
                clients.add(clientHandler);

                Thread clientThread = new Thread(clientHandler);
                clientThread.start();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class ClientHandler implements Runnable {

        private Socket clientSocket;
        private InputStream input;
        private OutputStream output;
        private String clientName;  // Nuevo campo para almacenar el nombre del cliente

        public ClientHandler(Socket clientSocket) {
            this.clientSocket = clientSocket;
            try {
                this.input = clientSocket.getInputStream();
                this.output = clientSocket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            try {
                byte[] buffer = new byte[1024];
                int bytesRead;

                // Leer el nombre del cliente
                bytesRead = input.read(buffer);
                clientName = new String(buffer, 0, bytesRead);
                broadcastMessage(clientName + " se ha conectado");
                while ((bytesRead = input.read(buffer)) != -1) {
                    String message = new String(buffer, 0, bytesRead);
                    System.out.println("Mensaje recibido de " + clientName + ": " + message);

                    // Reenviar el mensaje a todos los clientes incluyendo el nombre del cliente
                    broadcastMessage(clientName + ": " + message);
                }

            } catch (IOException e) {
                // Manejar la desconexión del cliente
                System.out.println(clientName + " desconectado");
            } finally {
                // Cerrar recursos cuando el cliente se desconecta
                try {
                    input.close();
                    output.close();
                    clientSocket.close();
                    clients.remove(this);
                    broadcastMessage(clientName + " se ha desconectado.");  // Informar a los demás clientes
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        public void sendMessage(String message) {
            try {
                output.write(message.getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static void broadcastMessage(String message) {
        for (ClientHandler clientHandler : clients) {
            clientHandler.sendMessage(message);
        }
    }
}
