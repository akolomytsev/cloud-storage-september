package com.geekbrains.is;

import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import lombok.extern.slf4j.Slf4j;


@Slf4j
public class Controller implements Initializable {
    private static String ROOT_DIR_1 = "client-sep-2021/src/main/resources/com/geekbrains/is";
    private static String ROOT_DIR_2 = "server-sep-2021/src/main/resources/com/geekbrains/io";
    public ListView<String> listView1;
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
 //       os.writeUTF(msg);// не понадобилось, может и зря
        os.flush();
    }

    private void exitProgram(String s) throws IOException {
        os.writeUTF("exit");
        os.flush();
    }
    //
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            FillFilesInCurrentDir();
            socket = new Socket("localhost", 8189);
            is = new DataInputStream(socket.getInputStream());
            os = new DataOutputStream(socket.getOutputStream());

            Thread daemon = new Thread(() -> {
                try {
                    while (true) {
                        String msg = is.readUTF();
                        log.debug("received: {}", msg);
 //                       Platform.runLater(() -> listView1.getItems().add(msg));
                        if ("upload".equals(msg)) {  // пришла такая команда
                            try {
                                File file = new File(ROOT_DIR_2 + File.separator + is.readUTF());  // читаем наименование
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
                                //fos.close(); // передаем статус
                                //os.writeUTF("OK");
                            } catch (Exception e) {
                                os.writeUTF("FATAL ERROR");
                            }
                        }
                        if ("download".equals(msg)) { // реализовать загрузку с сервера,
                            try {
                                File file = new File(ROOT_DIR_1 + File.separator + is.readUTF());  // читаем наименование
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
                                //fos.close(); // передаем статус
                               // os.writeUTF("OK");
                            } catch (Exception e) {
                                os.writeUTF("FATAL ERROR");
                            }
                        }
                        if ("exit".equals(msg)) { // если набрать это сообщение то выходишь из программы
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
//метод для вывода содержимого папки в listView по конкретному пути
    private void FillFilesInCurrentDir() throws IOException{
        listView1.getItems().clear();// изначально чистим (на всякий случай)
        listView1.getItems().addAll(
                Files.list(Paths.get(ROOT_DIR_1))
                        .map(p -> p.getFileName().toString())
                        .collect(Collectors.toList())

        );
        // При двойном нажатии на enter имя папки передается в TextField input
        listView1.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2){
                String item = listView1.getSelectionModel().getSelectedItem();
                input.setText(item);
            }
        });

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
