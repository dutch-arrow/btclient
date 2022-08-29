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
public class Command {
	private UUID msgId;
	private String cmd;
	private JsonObject data;

	public Command() { this.msgId = UUID.randomUUID();}

	public Command(String cmd, JsonObject data) {
		this.msgId = UUID.randomUUID();
		this.cmd = cmd;
		this.data = data;
	}

	public UUID getMsgId () { return this.msgId; }
	public void setMsgId (UUID msgId) {	this.msgId = msgId;	}
	public String getCmd () { return this.cmd; }
	public void setCmd (String cmd) { this.cmd = cmd; }
	public JsonObject getData () { return this.data; }
	public void setData (JsonObject data) {	this.data = data; }

}
