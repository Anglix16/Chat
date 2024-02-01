package util;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class chat {
    private JPanel principal;
    private JTextField tfMensaje;
    private JButton btnEnviar;
    private JTextArea taCliente;
    private JTextArea taServidor;
    private JLabel lblMensaje;

    private Socket socket;
    private OutputStream output;

    private boolean isFirstMessage = true;  // Variable para indicar si es el primer mensaje
    public chat() {
        btnEnviar.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendMessage();
            }
        });

        try {
            socket = new Socket("127.0.0.1", 6000);
            output = socket.getOutputStream();
            startListening();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void startListening() {
        Thread receiveThread = new Thread(() -> {
            try {
                InputStream input = socket.getInputStream();
                byte[] buffer = new byte[1024];
                int bytesRead;

                while ((bytesRead = input.read(buffer)) != -1) {
                    String message = new String(buffer, 0, bytesRead);
                    appendServerMessage(message);
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        receiveThread.start();
    }

    private void sendMessage() {
        try {
            String message = tfMensaje.getText();
            output.write(message.getBytes());
            tfMensaje.setText("");
            appendClientMessage(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void appendClientMessage(String message) {
        // Verificar si es el primer mensaje (nombre del cliente)
        lblMensaje.setText("Introduce un mensaje");
        if (!isFirstMessage) {
            taCliente.append("Yo: " + message + "\n");
            taCliente.setCaretPosition(taCliente.getDocument().getLength());

        } else {
            isFirstMessage = false;  // Marcar que ya se ha mostrado el primer mensaje
        }
    }

    private void appendServerMessage(String message) {
        taServidor.append(message + "\n");
        taServidor.setCaretPosition(taServidor.getDocument().getLength());
    }

    public JPanel getPrincipal() {
        return principal;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Chat");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            chat chat = new chat();

            frame.setContentPane(chat.getPrincipal());
            frame.pack();
            frame.setVisible(true);
        });
    }
}
