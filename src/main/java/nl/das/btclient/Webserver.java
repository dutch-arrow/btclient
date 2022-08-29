/*
 * Copyright © 2022 Dutch Arrow Software - All Rights Reserved
 * You may use, distribute and modify this code under the
 * terms of the Apache Software License 2.0.
 *
 * Created 24 Aug 2022.
 */


package nl.das.btclient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import javax.bluetooth.RemoteDevice;
import javax.bluetooth.UUID;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.json.bind.JsonbConfig;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.router.RouterNanoHTTPD;

/**
 *
 */
public class Webserver extends RouterNanoHTTPD {

	private static Properties props;
	private static BtClient btclient;
	private static Map<String, UUID> uuids = new HashMap<>();

	/**
	 * @param hostname
	 * @param port
	 */
	public Webserver(Properties properties) {
        super(properties.getProperty("host"), Integer.parseInt(properties.getProperty("port")));
        props = properties;
		for (int i = 1; i <= 4; i++) {
			uuids.put(props.getProperty("bt" + i + ".host"), new UUID(props.getProperty("bt" + i + ".uuid"), false));
		}
        btclient = new BtClient();
        addMappings();
	}

	@Override
	public void addMappings() {
		setNotFoundHandler(Error404UriHandler.class);
		setNotImplementedHandler(NotImplementedHandler.class);
		addRoute("/devices", GetDevices.class);
		addRoute("/command/:device/:command", SendCommand.class);
    	addRoute("/", GetPage.class);
	}

    @Override
	public void stop () {
		super.stop();
		btclient.close();
	}

	/*
     * Handlers
     */
    public static class GetIcon extends GeneralHandler {

        @Override
        public Response get(UriResource uriResource, Map<String, String> urlParams, IHTTPSession session) {
			InputStream in = getClass().getResourceAsStream("/favicon.ico");
			return newChunkedResponse(Response.Status.OK, "image/icon", in);
		}
    }

    public static class GetPage extends GeneralHandler {

        @Override
        public Response get(UriResource uriResource, Map<String, String> urlParams, IHTTPSession session) {
			try {
				InputStream in = getClass().getResourceAsStream("/mainpage.html");
				BufferedReader reader = new BufferedReader(new InputStreamReader(in));
			    char[] arr = new char[8 * 1024];
			    StringBuilder buffer = new StringBuilder();
			    int numCharsRead;
			    while ((numCharsRead = reader.read(arr, 0, arr.length)) != -1) {
			        buffer.append(arr, 0, numCharsRead);
			    }
			    reader.close();
			    String page = buffer.toString();
				return newFixedLengthResponse(page);
			} catch (IOException e) {
				return newFixedLengthResponse(e.getMessage());
			}
        }
    }

    public static class GetDevices extends GeneralHandler {

        @Override
        public Response post(UriResource uriResource, Map<String, String> urlParams, IHTTPSession session) {
        	try {
        		CompletableFuture<List<RemoteDevice>> searchDevices = btclient.searchDevices();
				while(!searchDevices.isDone()) {
				    Thread.sleep(1000);
				}
				List<RemoteDevice> remoteDevices = searchDevices.get();
				System.out.println("Future returned " + remoteDevices.size() + " devices");
				List<String> list = new ArrayList<>();
				for (RemoteDevice rd : remoteDevices) {
					if (rd.getFriendlyName(true).startsWith("tcu")) {
						list.add(rd.getFriendlyName(true));
					}
				}
				Jsonb jsonb = JsonbBuilder.create(new JsonbConfig().withFormatting(true));
				return newFixedLengthResponse(jsonb.toJson(list));
			} catch (InterruptedException | ExecutionException | IOException | ConcurrentModificationException e ) {
				return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, NanoHTTPD.MIME_PLAINTEXT, e.getMessage());
			}
        }
    }

    public static class SendCommand extends GeneralHandler {

        @Override
        public Response post(UriResource uriResource, Map<String, String> urlParams, IHTTPSession session) {
        	String device = urlParams.get("device");
        	String cmd = urlParams.get("command");
        	final HashMap<String, String> map = new HashMap<String, String>();
			try {
				session.parseBody(map);
			} catch (IOException | ResponseException e) {
				return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, NanoHTTPD.MIME_PLAINTEXT, e.getMessage());
			}
            String requestBody = map.get("postData");
        	System.out.println("Sending command '" + cmd + "' to tcu '" + device +"' with data " + requestBody);
        	String response = "";
			try {
				response = btclient.sendCommand(uuids.get(device), cmd, requestBody);
				return newFixedLengthResponse(response);
			} catch (CommandException e) {
				if (e.getMessage().startsWith("Failed to write")) { // Connection lost. Try again.
					try {
						response = btclient.sendCommand(uuids.get(device), cmd, requestBody);
						return newFixedLengthResponse(response);
					} catch (CommandException e1) {
						return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, NanoHTTPD.MIME_PLAINTEXT, e1.getMessage());
					}
				}
				return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, NanoHTTPD.MIME_PLAINTEXT, e.getMessage());
			}
        }
    }
}
