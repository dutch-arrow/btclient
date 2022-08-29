/*
 * Copyright © 2022 Dutch Arrow Software - All Rights Reserved
 * You may use, distribute and modify this code under the
 * terms of the Apache Software License 2.0.
 *
 * Created 28 Aug 2022.
 */


package nl.das.btclient;

import java.util.UUID;

import javax.json.JsonObject;

/**
 *
 */
public class Response {
	private UUID msgId;
	private String cmd;
	private JsonObject response;

	public Response() { }

	public UUID getMsgId () { return this.msgId; }
	public void setMsgId (UUID msgId) {	this.msgId = msgId;	}
	public String getCmd () { return this.cmd; }
	public void setCmd (String cmd) { this.cmd = cmd; }
	public JsonObject getResponse () { return this.response; }
	public void setResponse (JsonObject response) {	this.response = response; }

}
