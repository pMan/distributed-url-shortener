package com.pman.distributedurlshortener.server;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pman.distributedurlshortener.zk.State;
import com.pman.distributedurlshortener.zk.ZooKeeperClient;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class HttpResponseHandler implements HttpHandler {

	private ZooKeeperClient zkClient;
	private String uri;
	private static String homeTemplate = "<html style=\"color:gray; font-family: Arial; margin: 20px 10px;\"><body>"
			+ "<h2>Distributed URL shortener</h2>" + "<a href=\"/status\">Node Stauts</a> | "
			+ "<a href=\"/info\">Node Info</a> | " + "<a href=\"/nodes\">List Cluster Nodes</a> " + "</body></html>";

	public HttpResponseHandler(ZooKeeperClient zkClient, String endpoint) {
		this.zkClient = zkClient;
		this.uri = endpoint;
	}

	private void flush(HttpExchange exchange, int code, String message) throws IOException {
		exchange.sendResponseHeaders(code, message.length());
		exchange.getResponseBody().write(message.getBytes());
		exchange.close();
	}
	
	@Override
	public void handle(HttpExchange exchange) throws IOException {
		String response = "";

		switch (uri) {
		case WebServer.HOME:
			response = homeTemplate;
			flush(exchange, HttpURLConnection.HTTP_OK, response);
			break;

		case WebServer.SERVER_STATUS:
			response = "{\"status\": \"LIVE\"}";
			exchange.getResponseHeaders().set("Content-Type", "application/json");
			flush(exchange, HttpURLConnection.HTTP_OK, response);
			break;

		case WebServer.ZNODE_INFO:
			State state = zkClient.getState();
			response = new ObjectMapper().writeValueAsString(state);
			exchange.getResponseHeaders().set("Content-Type", "application/json");
			flush(exchange, HttpURLConnection.HTTP_OK, response);
			break;

		case WebServer.LIST_ALL_ZNODES:
			List<State> nodes = zkClient.znodes();
			response = new ObjectMapper().writeValueAsString(nodes);
			exchange.getResponseHeaders().set("Content-Type", "application/json");
			flush(exchange, HttpURLConnection.HTTP_OK, response);
			break;
			
		case WebServer.NEW_NEXT:
			long next = this.zkClient.getGlobalNext();
			response = "{\"next\": " + next + "}";
			exchange.getResponseHeaders().set("Content-Type", "application/json");
			flush(exchange, HttpURLConnection.HTTP_OK, response);
			break;

		case WebServer.SHORTEN:
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
				
				long cur = zkClient.getNext();
				String shortURL = Base64Converter.longToBase64(cur);
				response = "{\"url\": \"https://d.us/" + shortURL + "\"}";
				exchange.getResponseHeaders().set("Content-Type", "application/json");
				flush(exchange, HttpURLConnection.HTTP_OK, response);
			}
			break;
		}
	}

}
