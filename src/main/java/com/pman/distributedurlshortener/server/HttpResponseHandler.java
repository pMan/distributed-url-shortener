package com.pman.distributedurlshortener.server;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pman.distributedurlshortener.db.ConnectionPool;
import com.pman.distributedurlshortener.zk.LocalState;
import com.pman.distributedurlshortener.zk.ZooKeeperClient;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class HttpResponseHandler implements HttpHandler {

    static final String HOME = "/";
    static final String SERVER_STATUS = "/status";
    static final String ZNODE_INFO = "/info";
    static final String LIST_ALL_ZNODES = "/nodes";
    static final String SHORTEN = "/shorten";

    static final String STATUS = "{\"status\": \"LIVE\"}";
    static final String INVALID_INPUT = "Invalid input";
    static final String SHORTEN_ERROR = "{\"error\": \"Error occurred while connecting to DB.\"}";
    static final String ZNODES_RESPONSE = "{ \"znodes\": {ZNODES}}";
    static final String TARGET_URL_NOT_FOUND = "Target URL not found";
    static String SHORTEN_RESPONSE;

    private ZooKeeperClient zooKeeperClient;
    private ConnectionPool pool;
    private static final String HASH_PATTERN = "^/[a-zA-Z0-9-_]+$";

    /**
     * constructor
     * 
     * @param zooKeeperClient
     * @param webServer
     */
    public HttpResponseHandler(ZooKeeperClient zooKeeperClient, String hostport) {
        this.zooKeeperClient = zooKeeperClient;
        pool = new ConnectionPool();

        SHORTEN_RESPONSE = "{\"url\": \"" + hostport + "/{URL}\"}";
    }

    /**
     * redirect a hash to its mapped URL
     * 
     * @param hash
     * @param exchange
     * @throws IOException
     */
    private void redirect(HttpExchange exchange, String hash) throws IOException {
        try {
            String url = pool.getUrl(hash);

            if (url == null)
                flush(exchange, HttpURLConnection.HTTP_NOT_FOUND, TARGET_URL_NOT_FOUND);

            exchange.getResponseHeaders().set("Content-Type", "text/html");
            exchange.getResponseHeaders().set("Location", url);
            flush(exchange, HttpURLConnection.HTTP_MOVED_PERM, "");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Create a hash of the given URL, save it, and return the hash
     * 
     * @param url
     * @return hash
     * @throws SQLException
     */
    public String persist(String url) throws SQLException {
        String shortURL = pool.getHash(url);
        if (shortURL != null)
            return shortURL;

        shortURL = CustomBase64Encoder.longToBase64(zooKeeperClient.getCurrentHash());
        pool.save(shortURL, url);
        return shortURL;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String response = "";
        String uri = exchange.getRequestURI().getPath();
        System.out.println(String.format("%-4s", exchange.getRequestMethod()) + ": " + uri);

        switch (uri) {
        case HOME:
            if (!exchange.getRequestMethod().equalsIgnoreCase("GET"))
                exchange.close();

            response = readStatic("/ui/index.html");
            exchange.getResponseHeaders().set("Content-Type", "text/html");
            flush(exchange, HttpURLConnection.HTTP_OK, response);
            break;

        case SERVER_STATUS:
            if (!exchange.getRequestMethod().equalsIgnoreCase("GET"))
                exchange.close();

            flushREST(exchange, HttpURLConnection.HTTP_OK, STATUS);
            break;

        case ZNODE_INFO:
            if (!exchange.getRequestMethod().equalsIgnoreCase("GET"))
                exchange.close();

            LocalState state = zooKeeperClient.getState();
            response = new ObjectMapper().writeValueAsString(state);
            flushREST(exchange, HttpURLConnection.HTTP_OK, response);
            break;

        case LIST_ALL_ZNODES:
            if (!exchange.getRequestMethod().equalsIgnoreCase("GET"))
                exchange.close();

            List<LocalState> nodes = zooKeeperClient.getAllZnodes();
            String znodes = new ObjectMapper().writeValueAsString(nodes);
            flushREST(exchange, HttpURLConnection.HTTP_OK, ZNODES_RESPONSE.replace("{ZNODES}", znodes));
            break;

        case SHORTEN:
            if (!exchange.getRequestMethod().equalsIgnoreCase("POST"))
                exchange.close();

            Headers requestHeaders = exchange.getRequestHeaders();
            int contentLength = Integer.parseInt(requestHeaders.getFirst("Content-length"));
            byte[] data = new byte[contentLength];

            InputStream inputStream = exchange.getRequestBody();
            int length = inputStream.read(data);
            String input = new String(data, StandardCharsets.UTF_8);

            if (length == 0 || input.trim().length() == 0) {
                response = INVALID_INPUT;
                flush(exchange, HttpURLConnection.HTTP_BAD_REQUEST, response);
                return;
            }

            try {
                String shortUrl = persist(input);
                response = SHORTEN_RESPONSE.replace("{URL}", shortUrl);
            } catch (SQLException e) {
                e.printStackTrace();
                response = SHORTEN_ERROR;
            }

            flushREST(exchange, HttpURLConnection.HTTP_OK, response);
            break;

        default:
            if (uri.matches(HASH_PATTERN)) {
                redirect(exchange, uri.substring(1));
            }

            if (!uri.isBlank()) {
                response = readStatic(uri);
                if (uri.endsWith(".js")) {
                    exchange.getResponseHeaders().set("Content-Type", "text/javascript");
                } else if (uri.endsWith(".css")) {
                    exchange.getResponseHeaders().set("Content-Type", "text/css");
                } else if (uri.endsWith("favicon.ico")) {
                    exchange.getResponseHeaders().set("Content-Type", "image/x-icon");
                    InputStream assetStream = getClass().getResourceAsStream(uri);
                    byte[] bytes = assetStream.readAllBytes();
                    exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, bytes.length);
                    exchange.getResponseBody().write(bytes);
                    exchange.close();
                    return;
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
