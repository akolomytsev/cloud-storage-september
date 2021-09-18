package com.geekbrains.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class IoIntro {

    private static final byte [] buffer = new byte[1024];
    private static final String APP_NAME = "server-sep-2021/";
    private static final String ROOT_DIR = "server-sep-2021/root/";

    private void createServerDir(String dirName) {
        File dir = new File(APP_NAME + dirName);
        if (!dir.exists()) {
            dir.mkdir();
        }
    }

    private String readAsString(String resourceName) throws IOException {
        InputStream inputStream = getClass().getResourceAsStream(resourceName);
        int read = inputStream.read(buffer);
        return new String(buffer, 0, read);
    }

    private void transfer(File src, File dst) {
        try (FileInputStream is = new FileInputStream(src);
             FileOutputStream os = new FileOutputStream(dst)
        ) {
            int read;
            while ((read = is.read(buffer)) != -1) {
                os.write(buffer, 0, read);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException {
        IoIntro intro = new IoIntro();
        System.out.println(intro.readAsString("hello.txt"));
        intro.createServerDir("root");
        intro.transfer(
                new File("/Users/mikelevin/IdeaProjects/gb/backend/test/cloud-storage-sep-2021/server-sep-2021/src/main/resources/com/geekbrains/io/hello.txt"),
                new File(ROOT_DIR + "copy.txt")
        );
    }
}
