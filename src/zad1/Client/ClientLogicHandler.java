package zad1.Client;

import javax.swing.*;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ClientLogicHandler {
    private final JTextArea textArea;
    private final JComboBox<String> comboBox;
    private final HashMap<String, String> topicsAndLastNews;
    private final ArrayList<String> topics;
    private SocketChannel socketChannel;

    public ClientLogicHandler(JTextArea textArea, HashMap<String, String> topicsAndLastNews, ArrayList<String> topics, JComboBox<String> comboBox) {
        this.textArea = textArea;
        this.topicsAndLastNews = topicsAndLastNews;
        this.topics = topics;
        this.comboBox = comboBox;
        reconnect();
    }

    private ByteBuffer buffer = ByteBuffer.allocate(1024);
    private CharBuffer charBuffer = buffer.asCharBuffer();
    private final Charset charset = Charset.forName("ISO-8859-2");
    public boolean getMessage() {
        String message = "";

        if (!socketChannel.isConnected()) {
            System.out.println("Client disconnected");
            JOptionPane.showMessageDialog(null, "Connection lost");
            return false;
        }
        while(true) {
            try {
                buffer.clear();
                int read = socketChannel.read(buffer);
                if (read == 0) {
                    continue;
                }else if (read == -1) {
                    JOptionPane.showMessageDialog(null, "Connection lost. (Could not read)");
                    break;
                } else {
                    buffer.flip();
                    charBuffer = charset.decode(buffer);
                    message = charBuffer.toString();
                    System.out.println("Client received: " + message);
                    charBuffer.clear();

                    ArrayList<String> messageParts = new ArrayList<>(Arrays.asList(message.split("&")));

                    if (messageParts.get(0).equals("success")) {
                        System.out.println("returning true");
                        return true;
                    }
                    else if (messageParts.get(0).equals("failure")) {
                        System.out.println("returning false");
                        return false;
                    }
                    else if (messageParts.get(0).equals("update")) {
                        System.out.println("updating");
                        if (messageParts.get(2).equals("null")){
                            JOptionPane.showMessageDialog(null, "No news for this topic");
                            return false;
                        }else if (messageParts.get(2).equals("deleted")){
                            JOptionPane.showMessageDialog(null, "Topic deleted");
                            comboBox.removeItem(messageParts.get(1));
                            return false;
                        }
                        if(messageParts.size() == 5) {
                            String newTopicName = messageParts.get(4);
                            comboBox.removeItem(messageParts.get(1));
                            comboBox.addItem(newTopicName);
                            JOptionPane.showMessageDialog(null, "The topics name has been changed to " + newTopicName);
                        }
                        topicsAndLastNews.put(messageParts.get(1), messageParts.get(2));
                        textArea.setText(messageParts.get(2));
                        return true;
                    } else {
                        textArea.setText("error");
                        return false;
                    }
                }
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null, "Error reading from server");
                reconnect();
            }
        }
        return false;
    }

    public void subscribe(String topic) {
        if (topic.equals("")) {
            JOptionPane.showMessageDialog(null, "Topic cannot be empty");
        }
        sendMessage("subscribe&" + topic);
        if (!getMessage()) {
            JOptionPane.showMessageDialog(null, "Subscription failed");
        }else{
            topics.add(topic);
            comboBox.addItem(topic);
            JOptionPane.showMessageDialog(null, "Subscription successful");
        }
    }

    public void unsubscribe(String topic) {
        sendMessage("unsubscribe&" + topic);
        if (!getMessage()) {
            JOptionPane.showMessageDialog(null, "Unsubscription failed");
        }else{
            topics.remove(topic);
            comboBox.removeItem(topic);
            JOptionPane.showMessageDialog(null, "Unsubscription successful");
        }
    }

    public void update(String topic){
        sendMessage("update&" + topic);
        if (!getMessage()) {
            JOptionPane.showMessageDialog(null, "Update failed");
        }
    }

    public void sendMessage(String message) {
        System.out.println("Client sending: " + message);
        ByteBuffer msg = charset.encode(CharBuffer.wrap("client&" + message + '\n'));
        while(true) {
            try {
                socketChannel.write(msg);
                return;
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null, "Could not send message, trying to reconnect");
                reconnect();
            }
        }
    }

    private void reconnect() {
        while(true) {
            if(socketChannel != null) {
                try {
                    socketChannel.close();
                } catch (Exception e) {
                    System.out.println("Could not close socket");
                }
            }
            try {
                InetSocketAddress address = new InetSocketAddress("localhost", 8888);
                try {
                    socketChannel = SocketChannel.open();
                    socketChannel.configureBlocking(false);
                    socketChannel.connect(address);
                    while (!socketChannel.finishConnect()) {
                    }
                    System.out.println("Client connected");
                    return;
                } catch (UnknownHostException e) {
                    JOptionPane.showMessageDialog(null, "Unknown host");
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(null, "Unknown error");
                }
            } catch (Exception e) {
                System.out.println("Could not open socket");
            }
        }
    }
}