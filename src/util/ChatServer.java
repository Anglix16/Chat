package util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ChatServer {

    private static final int PORT = 6000;
    private static List<ClientHandler> clients = new ArrayList<>();
    private static Set<String> connectedClients = new HashSet<>();
    private static List<String> messageHistory = new ArrayList<>(); // Historial de mensajes

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

                // Verificar si el nombre del cliente ya está en uso
                synchronized (connectedClients) {
                    while (connectedClients.contains(clientName)) {
                        System.out.println("Nombre de cliente duplicado: " + clientName);
                        // Enviar un mensaje al cliente indicando que el nombre ya está en uso
                        output.write("El nombre ya está en uso. Por favor, elige otro.\n".getBytes());
                        output.flush();
                        // Leer el siguiente nombre del cliente
                        bytesRead = input.read(buffer);
                        clientName = new String(buffer, 0, bytesRead);
                    }
                    connectedClients.add(clientName);
                }

                // Notificar a los demás clientes sobre la conexión del nuevo cliente
                broadcastMessage(clientName + " se ha conectado");

                // Enviar historial de mensajes a los demás clientes
                synchronized (messageHistory) {
                    for (String message : messageHistory) {
                        if (!message.startsWith(clientName)) { // Evitar enviar los propios mensajes al cliente
                            output.write(message.getBytes());
                            output.flush();
                        }
                    }
                }

                while ((bytesRead = input.read(buffer)) != -1) {
                    String message = new String(buffer, 0, bytesRead);
                    System.out.println("Mensaje recibido de " + clientName + ": " + message);

                    // Guardar mensaje en historial
                    synchronized (messageHistory) {
                        messageHistory.add(clientName + ": " + message + "\n");
                    }

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
                    connectedClients.remove(clientName);  // Eliminar el nombre del cliente del conjunto
                    // Notificar a los demás clientes sobre la desconexión del cliente
                    broadcastMessage(clientName + " se ha desconectado.");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        public void sendMessage(String message) {
            try {
                output.write(message.getBytes());
                output.flush();
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
