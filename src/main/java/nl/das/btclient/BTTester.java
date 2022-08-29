/*
 * Copyright © 2022 Dutch Arrow Software - All Rights Reserved
 * You may use, distribute and modify this code under the
 * terms of the Apache Software License 2.0.
 *
 * Created 24 Aug 2022.
 */

package nl.das.btclient;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import fi.iki.elonen.NanoHTTPD;

/**
 *
 */
public class BTTester {

	private Webserver srv;

	/**
	 * @param args
	 */
	public static void main (String[] args) {
		BTTester client = new BTTester();
		client.runProgram();
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
			@Override
			public void run () {
				client.stopProgram();
			}
		}));
	}

	/**
	 *
	 */
	private void runProgram () {
		try {
			Properties props = new Properties();
			props.load(new FileInputStream("btclient.properties"));
			this.srv = new Webserver(props);
			this.srv.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 *
	 */
	private void stopProgram () {
		this.srv.stop();
	}
}
