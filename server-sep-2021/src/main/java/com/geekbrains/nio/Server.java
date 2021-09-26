package com.geekbrains.nio;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.*;

public class Server {
    Path serverRoot = Path.of("server-sep-2021/src/main/resources/com/geekbrains/io");

    private ServerSocketChannel serverChannel;
    private Selector selector;
    private ByteBuffer buffer;

    public Server() throws IOException {

        buffer = ByteBuffer.allocate(256);
        serverChannel = ServerSocketChannel.open();
        selector = Selector.open();
        serverChannel.bind(new InetSocketAddress(5678));
        serverChannel.configureBlocking(false);
        serverChannel.register(selector, SelectionKey.OP_ACCEPT);
        System.out.println("Server started");
        while (serverChannel.isOpen()) {

            selector.select();

            Set<SelectionKey> keys = selector.selectedKeys();

            Iterator<SelectionKey> iterator = keys.iterator();
            while (iterator.hasNext()) {
                SelectionKey key = iterator.next();
                if (key.isAcceptable()) {
                    handleAccept(key);
                }
                if (key.isReadable()) {
                    handleRead(key);
                }
                iterator.remove();
            }
        }
    }

    public static void main(String[] args) throws IOException {
        new Server();
    }

    private void handleAccept(SelectionKey key) throws IOException {
        SocketChannel channel = serverChannel.accept();
        channel.configureBlocking(false);
        System.out.println("Client connected. IP:" + channel.getRemoteAddress());
        channel.register(selector, SelectionKey.OP_READ);
    }

    private void handleRead(SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        SocketAddress client = channel.getRemoteAddress();
        buffer.clear();
        int read = 0;
        StringBuilder msg = new StringBuilder();
        while (true) {
            if (read == -1) {
                channel.close();
                return;
            }
            read = channel.read(buffer);
            if (read == 0) {
                break;
            }
            buffer.flip();
            while (buffer.hasRemaining()) {
                msg.append((char) buffer.get());
            }
            buffer.clear();
        }
        String message = msg.toString()
                .replace("\n", "")
                .replace("\r", "");

        String[] arr = message.split(" ", 3);
        int arrLength = arr.length;
        if (arrLength <2){
            arr = new String[]{arr[0],"", ""};
        }

        if ("ls".equals(arr[0])) {
            sendMessage(getFilesList().concat("\n\r"), selector, client);
       } else if ("cat".equals(arr[0]))
            sendMessage(textOutput(arr[1], serverRoot).concat("\n\r"), selector, client);

        channel.write(ByteBuffer.wrap(("[" + LocalDateTime.now() + "] " + message).getBytes(StandardCharsets.UTF_8)));
    }


//самая непонятная часть кода, но она работает.
    private void sendMessage(String message, Selector selector, SocketAddress client) throws IOException {
        for (SelectionKey key : selector.keys()) {
            if (key.isValid() && key.channel() instanceof SocketChannel) {
                if (((SocketChannel) key.channel()).getRemoteAddress().equals(client)) {
                    ((SocketChannel) key.channel()).write(ByteBuffer.wrap(message.getBytes(StandardCharsets.UTF_8)));
                }
            }
        }
    }
    //метод для вывода содержимого директории
    private String getFilesList() {
        String[] servers = new File("server-sep-2021/src/main/resources/com/geekbrains/io").list();
        return String.join(" ", servers);
    }

    //вывод содержимого текстового файла клиенту
    private String textOutput(String file, Path serverRoot) throws IOException {
        if (Files.exists(Path.of(String.valueOf(serverRoot), file))) {
            String d = Files.readString(Path.of(String.valueOf(serverRoot), file));
            return String.join(" ", d);
        } else {
            return "Sorry, some bullshit has happened";
        }
    }
}
