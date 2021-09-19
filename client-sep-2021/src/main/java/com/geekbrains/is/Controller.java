package com.geekbrains.is;

import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.util.ResourceBundle;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import lombok.extern.slf4j.Slf4j;


@Slf4j
public class Controller implements Initializable {
    public ListView<String> listView;
    public TextField input;
    private DataInputStream is;
    private DataOutputStream os;
    private Socket socket;

    public void send(ActionEvent actionEvent) throws IOException {
        String[] msg = input.getText().split(" ");
        if ("upload".equals(msg[0])) {
            sendFile(msg[1]);  // для передачи файла
        } else if ("download".equals(msg[0])) {
            getFile(msg[1]);
        }else if ("exit".equals(msg[0])){
            exitProgram(msg[0]);
        } else {
            os.writeUTF("Нет такой команды");
        }
        input.clear();
        //os.writeUTF(String.valueOf(msg));// не понадобилось, может и зря
        //os.flush();
    }

    private void exitProgram(String s) throws IOException {
        os.writeUTF("exit");
        os.flush();
    }

    //
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            socket = new Socket("localhost", 8189);
            is = new DataInputStream(socket.getInputStream());
            os = new DataOutputStream(socket.getOutputStream());
            Thread daemon = new Thread(() -> {
                try {
                    while (true) {
                        String command = is.readUTF();
                        log.debug("received: {}", command);
                        Platform.runLater(() -> listView.getItems().add(command));
                        if ("upload".equals(command)) {  // пришла такая команда
                            try {
                                File file = new File("server-sep-2021/src/main/resources/com/geekbrains/io" + File.separator + is.readUTF());  // читаем наименование
                                if (!file.exists()) {  // проверяем есть ли такой файл
                                    file.createNewFile();
                                }
                                FileOutputStream fos = new FileOutputStream(file); // записываем файл
                                long size = is.readLong();  // читаем размерность
                                byte[] buffer = new byte[8 * 1024]; // буфер делаем того же размера [разбиваем таким образом для более быстрой передачи файлов и более корректной ее сборки]
                                for (int i = 0; i < (size + (buffer.length - 1)) / (buffer.length); i++) { // такая запись сделана для корректной сборки файла и что бы лишние куски не выкидывало
                                    int read = is.read(buffer); // считываем в буфер
                                    fos.write(buffer, 0, read);  // из буфера в файлик
                                }
                                fos.close(); // передаем статус
                                //os.writeUTF("OK");
                            } catch (Exception e) {
                                os.writeUTF("FATAL ERROR");
                            }
                        }
                        if ("download".equals(command)) { // реализовать загрузку с сервера,
                            try {
                                File file = new File("client-sep-2021/src/main/resources/com/geekbrains/is" + File.separator + is.readUTF());  // читаем наименование
                                if (!file.exists()) {  // проверяем есть ли такой файл
                                    file.createNewFile();
                                }
                                FileOutputStream fos = new FileOutputStream(file); // записываем файл
                                long size = is.readLong();  // читаем размерность
                                byte[] buffer = new byte[8 * 1024]; // буфер делаем того же размера [разбиваем таким образом для более быстрой
                                // передачи файлов и более корректной ее сборки]
                                for (int i = 0; i < (size + (buffer.length - 1)) / (buffer.length); i++) { // такая запись сделана для корректной
                                    // сборки файла и что бы лишние куски не выкидывало
                                    int read = is.read(buffer); // считываем в буфер
                                    fos.write(buffer, 0, read);  // из буфера в файлик
                                }
                                fos.close(); // передаем статус
                               // os.writeUTF("OK");
                            } catch (Exception e) {
                                os.writeUTF("FATAL ERROR");
                            }
                        }
                        if ("exit".equals(command)) { // если набрать это сообщение то выходишь из программы
                            System.out.printf("Client %s disconnected correctly\n", socket.getInetAddress()); //  сообщение в консоль
                            break; // выход
                        }
                    }
                } catch (Exception e) {
                    log.error("exception while read from input stream");
                }
            });
            daemon.setDaemon(true);
            daemon.start();
        } catch (IOException ioException) {
            log.error("e=", ioException);
        }
    }

    private void sendFile(String filename) {
        try {
            File file = new File("client-sep-2021/src/main/resources/com/geekbrains/is" + File.separator + filename); // путь (создаем объект класса файл)
            if (!file.exists()) { // если нет такого файла
                throw new FileNotFoundException();
            }
            long fileLength = file.length(); // читаем размер
            FileInputStream fis = new FileInputStream(file); // файл оборачиваем в исходящий поток (чтобы корректно считать)
            os.writeUTF("upload"); // говорим чтo хотим загрузить
            os.writeUTF(filename); // файл с таким именем
            os.writeLong(fileLength); // с такой длинной
            int read = 0; // читаем
            byte[] buffer = new byte[8 * 1024]; // создаем массив байт [разбиваем таким образом для более быстрой передачи файлов и более корректной ее сборки на том конце]
            while ((read = fis.read(buffer)) != -1) { // считываем в цикле до тех пор пока что то есть
                os.write(buffer, 0, read); // и передаем с указанием его размерности
            }
            os.flush();  // и освобождаем канал
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void getFile(String filename) {
        try {
            File file = new File("server-sep-2021/src/main/resources/com/geekbrains/io" + File.separator + filename); // путь (создаем объект класса файл)
            if (!file.exists()) { // если нет такого файла
                throw new FileNotFoundException();
            }
            long fileLength = file.length(); // читаем размер
            FileInputStream fis = new FileInputStream(file); // файл оборачиваем в исходящий поток (что бы корректно считать)
            os.writeUTF("download"); // говорим что что то хотим загрузить
            os.writeUTF(filename); // файл с таким именем
            os.writeLong(fileLength); // с такой длинной
            int read = 0; // читаем
            byte[] buffer = new byte[8 * 1024]; // создаем буфер [разбиваем таким образом для более быстрой передачи файлов и более корректной ее сборки на том конце]
            while ((read = fis.read(buffer)) != -1) { // считываем в цикле до тех пор пока что то есть
                os.write(buffer, 0, read); // и передаем с указанием его размерности
            }
            os.flush();  // и освобождаем канал
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
