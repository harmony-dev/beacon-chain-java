package org.ethereum.beacon.validator.api.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.handler.LightHttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HttpString;
import org.ethereum.beacon.validator.api.ResourceFactory;

public class NodeVersionGetHandler implements LightHttpHandler {
    
    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        exchange.getResponseHeaders().add(new HttpString("Content-Type"), "application/json");
        exchange.setStatusCode(200);
        exchange.getResponseSender().send(new ObjectMapper().writeValueAsString(ResourceFactory.getVersion()));
    }
}
