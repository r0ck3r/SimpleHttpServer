package ru.webgrozny.simplehttpserver;

import java.io.*;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Paths;

public class PostFile {
    private String name;
    private String temporaryPath;
    private String contentType;
    private int size;

    public PostFile(String name, String temporaryPath) {
        this.name = name;
        this.temporaryPath = temporaryPath;
        parseFileMetaData();
    }

    public String getName() {
        return this.name;
    }

    public int getSize() {
        return size;
    }

    public String getContentType() {
        return this.contentType;
    }

    private void parseFileMetaData() {
        InputStream is = null;
        try {
            is = new BufferedInputStream(new FileInputStream(temporaryPath));
            size = is.available();
            contentType = URLConnection.guessContentTypeFromStream(is);
        } catch (FileNotFoundException e) {
            //e.printStackTrace();
        } catch (IOException e) {
            //e.printStackTrace();
        }

    }

    public void saveTo(String newPath, String name) {
        String fullName = newPath + "/" + name;
        try {
            OutputStream outputStream = new FileOutputStream(fullName);
            Files.copy(Paths.get(temporaryPath), outputStream);
            outputStream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
         //   e.printStackTrace();
        }
    }

    public void saveTo(String newPath) {
        saveTo(newPath, getName());
    }

    public void remove () {
        try {
            Files.delete(Paths.get(temporaryPath));
        } catch (IOException e) {

        }
    }
}
