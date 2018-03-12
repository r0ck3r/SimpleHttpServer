package ru.webgrozny;

import ru.webgrozny.model.ExampleBackend;
import ru.webgrozny.simplehttpserver.ContentProvider;
import ru.webgrozny.simplehttpserver.ProviderGenerator;
import ru.webgrozny.simplehttpserver.Server;
import ru.webgrozny.simplehttpserver.ServerSettings;

public class Main {
    public static void main(String[] args) {
        ServerSettings serverSettings = ServerSettings.createDefaultConfig()
                .setDocumentRoot("/var/www/html")
                .setPostFileTempDir("/tmp/web")
                .setPort(80)
                .setProviderGenerator(new ProviderGenerator() {
                    @Override
                    public ContentProvider generate() {
                        return new ExampleBackend();
                    }
                });
        new Server(serverSettings).start();
    }
}
