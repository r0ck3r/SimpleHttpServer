package ru.webgrozny;

import ru.webgrozny.model.ExampleBackend;
import ru.webgrozny.simplehttpserver.ContentProvider;
import ru.webgrozny.simplehttpserver.ProviderGenerator;
import ru.webgrozny.simplehttpserver.Server;
import ru.webgrozny.simplehttpserver.ServerSettings;

public class Main {
    public static void main(String[] args) {

        //Running https server
        new Thread(new Runnable() {
            @Override
            public void run() {
                ServerSettings serverSettings = ServerSettings.createDefaultConfig()
                        .setDocumentRoot("/var/www/html")
                        .setPostFileTempDir("/tmp/web")
                        .disableDocumentRoot()
                        .setJKSKey("/home/user/.keystore", "storepass")
                        .setPort(10443)
                        .setProviderGenerator(new ProviderGenerator() {
                            @Override
                            public ContentProvider generate() {
                                return new ExampleBackend();
                            }
                        });
                new Server(serverSettings).start();
            }
        }).start();

        //Running http server
        ServerSettings serverSettings2 = ServerSettings.createDefaultConfig()
                .setDocumentRoot("/var/www/html")
                .setPostFileTempDir("/tmp/web")
                .setPort(1080)
                .disableDocumentRoot()
                .setProviderGenerator(new ProviderGenerator() {
                    @Override
                    public ContentProvider generate() {
                        return new ExampleBackend();
                    }
                });
        new Server(serverSettings2).start();
    }
}
