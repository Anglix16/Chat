package util;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class chat { // Renombré la clase de 'chat' a 'ChatClient' para seguir convenciones de nomenclatura

    // Componentes de la interfaz de usuario
    private JPanel principal;
    private JTextField tfMensaje;
    private JButton btnEnviar;
    private JTextPane taCliente;
    private JTextPane taServidor;
    private JLabel lblMensaje;
    private JLabel lblMensaje2;

    // Conexión de red
    private Socket socket;
    private OutputStream output;

    private boolean isFirstMessage = true;  // Variable para indicar si es el primer mensaje

    public chat() {
        // Configuración del botón de envío de mensajes
        btnEnviar.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendMessage(); // Método para enviar un mensaje al servidor
            }
        });

        try {
            // Establecer conexión con el servidor en localhost (127.0.0.1) en el puerto 6000
            socket = new Socket("127.0.0.1", 6000);
            output = socket.getOutputStream();
            startListening(); // Iniciar un hilo para escuchar los mensajes entrantes del servidor
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Método para escuchar mensajes entrantes del servidor
    private void startListening() {
        Thread receiveThread = new Thread(() -> {
            try {
                InputStream input = socket.getInputStream();
                byte[] buffer = new byte[1024];
                int bytesRead;

                // Escuchar continuamente mensajes del servidor y mostrarlos en el JTextPane taServidor
                while ((bytesRead = input.read(buffer)) != -1) {
                    String message = new String(buffer, 0, bytesRead);
                    appendServerMessage(message); // Método para agregar mensajes del servidor al JTextPane
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        receiveThread.start();
    }

    // Método para enviar un mensaje al servidor
    private void sendMessage() {
        try {
            String message = tfMensaje.getText();
            output.write(message.getBytes());
            tfMensaje.setText(""); // Limpiar el campo de texto después de enviar el mensaje
            appendClientMessage(message); // Método para agregar mensajes del cliente al JTextPane
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Método para agregar mensajes del cliente al JTextPane taCliente
    private void appendClientMessage(String message) {
        if (!isFirstMessage) {
            appendText(taCliente, "Yo: " + message + "\n");
        } else {
            isFirstMessage = false;  // Marcar que ya se ha mostrado el primer mensaje
        }
    }

    // Método para agregar mensajes del servidor al JTextPane taServidor
    private void appendServerMessage(String message) {
        if (message.equals("El nombre ya está en uso. Por favor, elige otro.\n")) {
            appendColoredText(taServidor, message, Color.RED);
        } else {
            appendText(taServidor, message + "\n");
        }
    }

    // Método para agregar texto al JTextPane
    private void appendText(JTextPane textPane, String text) {
        Document doc = textPane.getDocument();
        try {
            doc.insertString(doc.getLength(), text, null);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    // Método para agregar texto con color al JTextPane
    private void appendColoredText(JTextPane textPane, String text, Color color) {
        StyleContext sc = StyleContext.getDefaultStyleContext();
        AttributeSet aset = sc.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Foreground, color);
        Document doc = textPane.getDocument();
        try {
            doc.insertString(doc.getLength(), text, aset);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    // Método para obtener el panel principal de la interfaz de usuario
    public JPanel getPrincipal() {
        return principal;
    }

    // Método principal para iniciar la aplicación de chat
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Chat");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            chat chatClient = new chat(); // Crear una instancia del cliente de chat

            frame.setContentPane(chatClient.getPrincipal()); // Establecer el contenido del marco
            frame.pack(); // Ajustar el tamaño del marco automáticamente
            frame.setVisible(true); // Hacer visible el marco
        });
    }
}
