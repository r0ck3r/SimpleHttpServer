package ru.webgrozny.model;

import ru.webgrozny.simplehttpserver.ContentProvider;

public class ExampleBackend extends ContentProvider{
    @Override
    public void execute() {
        echo("This host's name: " + getHost() + "<br>");
        echo("Remote address: "  + getRemoteAddress() + "<br>");
        echo("Remote port: " + getRemotePort() + "<br>");
        echo("Value for get-parameter test: " + get("test") + "<br>");
    }
}