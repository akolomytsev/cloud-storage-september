package com.geekbrains.io;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Handler implements Runnable {

    private static final int BUFFER_SIZE = 256;
    private final String ROOT_DIR = "server-sep-2021/root";
    private final byte[] buffer = new byte[BUFFER_SIZE];
    private final Socket socket;
    private DataOutputStream os;
    private DataInputStream is;

    public Handler(Socket socket) {
        this.socket = socket;
    }

    public Socket getSocket() {
        return socket;
    }

    @SneakyThrows
    @Override
    public void run() {
        try  {
            is = new DataInputStream(socket.getInputStream());
            os = new DataOutputStream(socket.getOutputStream());
            while (true) {
                String fileName = is.readUTF();
                log.debug("Received fileName: {}", fileName);
                long size = is.readLong();
                log.debug("File size: {}", size);
                int read;
                try (OutputStream fos = Files.newOutputStream(Paths.get(ROOT_DIR, fileName))) {
                    for (int i = 0; i < (size + BUFFER_SIZE - 1) / BUFFER_SIZE; i++) {
                        read = is.read(buffer);
                        fos.write(buffer, 0 , read);
                    }
                } catch (Exception e) {
                    log.error("problem with file system");
                }
                os.writeUTF("OK");
            }
        } catch (Exception e) {
           log.error("stacktrace: ", e);
           os.writeUTF("Error! " + e.getMessage());
        }
        is.close();
        os.close();
    }
}
