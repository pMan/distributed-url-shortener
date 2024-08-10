package com.pman.distributedurlshortener.server;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pman.distributedurlshortener.zk.NodeState;
import com.pman.distributedurlshortener.zk.ZooKeeperClient;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class HttpResponseHandler implements HttpHandler {

    static final String HOME = "/";
    static final String SERVER_STATUS = "/status";
    static final String ZNODE_INFO = "/info";
    static final String LIST_ALL_ZNODES = "/nodes";
    static final String NEW_NEXT = "/next";
    static final String SHORTEN = "/shorten";

    private ZooKeeperClient zooKeeperClient;
    private WebServer webServer;

    public HttpResponseHandler(ZooKeeperClient zooKeeperClient, WebServer webServer) {
        this.zooKeeperClient = zooKeeperClient;
        this.webServer = webServer;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String response = "";
        String uri = exchange.getRequestURI().getPath();
        System.out.println("URI: " + uri);

        switch (uri) {
        case HOME:
            response = readStatic("/ui/index.html");
            exchange.getResponseHeaders().set("Content-Type", "text/html");
            flush(exchange, HttpURLConnection.HTTP_OK, response);
            break;

        case SERVER_STATUS:
            response = "{\"status\": \"LIVE\"}";
            flushREST(exchange, HttpURLConnection.HTTP_OK, response);
            break;

        case ZNODE_INFO:
            NodeState state = zooKeeperClient.getState();
            response = new ObjectMapper().writeValueAsString(state);
            flushREST(exchange, HttpURLConnection.HTTP_OK, response);
            break;

        case LIST_ALL_ZNODES:
            List<NodeState> nodes = zooKeeperClient.getAllZnodes();
            response = new ObjectMapper().writeValueAsString(nodes);
            flushREST(exchange, HttpURLConnection.HTTP_OK, "{ \"znodes\": " + response + "}");
            break;

        case SHORTEN:
            if (exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                Headers requestHeaders = exchange.getRequestHeaders();
                int contentLength = Integer.parseInt(requestHeaders.getFirst("Content-length"));
                byte[] data = new byte[contentLength];

                InputStream inputStream = exchange.getRequestBody();
                int length = inputStream.read(data);
                String input = new String(data, StandardCharsets.UTF_8);

                if (length == 0 || input.trim().length() == 0) {
                    response = "Invalid input";
                    flush(exchange, HttpURLConnection.HTTP_BAD_REQUEST, response);
                    return;
                }

                long cur = zooKeeperClient.getCurrentHash();
                String shortURL = Base64Converter.longToBase64(cur);
                response = "{\"url\": \"" + webServer.getAddress() + "/" + shortURL + "\"}";
                flushREST(exchange, HttpURLConnection.HTTP_OK, response);
            }
            break;
        default:
            if (!uri.isBlank()) {
                response = readStatic(uri);
                if (uri.endsWith(".js")) {
                    exchange.getResponseHeaders().set("Content-Type", "text/javascript");
                } else if (uri.endsWith(".css")) {
                    exchange.getResponseHeaders().set("Content-Type", "text/css");
                } else {
                    exchange.getResponseHeaders().set("Content-Type", "text/html");
                }
            }
            flush(exchange, HttpURLConnection.HTTP_OK, response);
        }
    }

    private void flush(HttpExchange exchange, int code, String message) throws IOException {
        exchange.sendResponseHeaders(code, message.length());
        exchange.getResponseBody().write(message.getBytes());
        exchange.close();
    }

    private void flushREST(HttpExchange exchange, int code, String message) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        flush(exchange, code, message);
    }

    private String readStatic(String asset) {
        InputStream assetStream = getClass().getResourceAsStream(asset);
        if (assetStream == null) {
            return "";
        }

        try {
            return new String(assetStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }

}
