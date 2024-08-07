package com.pman.distributedurlshortener.server;

import java.io.IOException;
import java.util.List;

import org.apache.zookeeper.ZooKeeper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pman.distributedurlshortener.zk.State;
import com.pman.distributedurlshortener.zk.ZKClient;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class HttpResponseHandler implements HttpHandler {

	private ZKClient zkClient;
	private String uri;
	private static String homeTemplate = "<html style=\"color:gray; font-family: Arial; margin: 20px 10px;\"><body>"
			+ "<h2>Distributed URL shortener</h2>" + "<a href=\"/status\">Node Stauts</a> | "
			+ "<a href=\"/info\">Node Info</a> | " + "<a href=\"/nodes\">List Cluster Nodes</a> " + "</body></html>";

	public HttpResponseHandler(ZKClient zkClient, String endpoint) {
		this.zkClient = zkClient;
		this.uri = endpoint;
	}

	@Override
	public void handle(HttpExchange exchange) throws IOException {
		String response = "";

		switch (uri) {
		case WebServer.HOME:
			response = homeTemplate;
			exchange.sendResponseHeaders(200, response.length());
			exchange.getResponseBody().write(response.getBytes());
			break;

		case WebServer.SERVER_STATUS:
			response = "{\"status\": \"LIVE\"}";
			exchange.getResponseHeaders().set("Content-Type", "application/json");
			exchange.sendResponseHeaders(200, response.length());
			exchange.getResponseBody().write(response.getBytes());
			break;

		case WebServer.ZNODE_INFO:
			State state = zkClient.getNodeState();
			response = new ObjectMapper().writeValueAsString(state);
			exchange.getResponseHeaders().set("Content-Type", "application/json");
			exchange.sendResponseHeaders(200, response.length());
			exchange.getResponseBody().write(response.getBytes());
			break;

		case WebServer.LIST_ALL_ZNODES:
			List<State> nodes = zkClient.znodes();
			response = new ObjectMapper().writeValueAsString(nodes);
			exchange.getResponseHeaders().set("Content-Type", "application/json");
			exchange.sendResponseHeaders(200, response.length());
			exchange.getResponseBody().write(response.getBytes());
			break;
		case WebServer.NEW_RANGE:
			long[] range = this.zkClient.getNewRange();
			response = "{\"range\": " + new ObjectMapper().writeValueAsString(range) + "}";
			exchange.getResponseHeaders().set("Content-Type", "application/json");
			exchange.sendResponseHeaders(200, response.length());
			exchange.getResponseBody().write(response.getBytes());
			break;
		}
	}

}
