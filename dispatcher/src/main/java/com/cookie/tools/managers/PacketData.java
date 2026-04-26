package com.cookie.tools.managers;

import java.util.List;

public class PacketData {
    public final String method;
    public final String url;
    public final List<String> headers; // ogni elemento è "Key : Value"
    public final String body;

    public PacketData(String method, String url, List<String> headers, String body) {
        this.method = method;
        this.url = url;
        this.headers = headers;
        this.body = body;
    }
}