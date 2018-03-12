package ru.webgrozny.model;

import ru.webgrozny.simplehttpserver.ContentProvider;
import ru.webgrozny.simplehttpserver.ServerStatus;

public class ExampleBackend extends ContentProvider{
    @Override
    public void execute() {
        if(get("component") == null) {
            setAnswer(ServerStatus.MOVED);
            setHeader("Location: http://127.0.0.1/?component=test");
            echo("");
        }
    }
}