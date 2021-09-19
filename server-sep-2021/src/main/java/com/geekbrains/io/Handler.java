package com.geekbrains.io;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.net.Socket;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Handler implements Runnable {

    private final Socket socket;
    public Handler(Socket socket) {
        this.socket = socket;
    }
    public Socket getSocket() {
        return socket;
    }

    @Override
    public void run() {
        try (DataOutputStream os = new DataOutputStream(socket.getOutputStream());
             DataInputStream is = new DataInputStream(socket.getInputStream())
        ) {
            while (true) {
                String command = is.readUTF();
   //             log.debug("Received: {}", command);
                os.writeUTF(command);
                os.flush();
            }
        } catch (Exception e) {
           log.error("stacktrace: ", e);
        }
    }
}
