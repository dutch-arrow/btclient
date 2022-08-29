/*
 * Copyright © 2022 Dutch Arrow Software - All Rights Reserved
 * You may use, distribute and modify this code under the
 * terms of the Apache Software License 2.0.
 *
 * Created 31 Jul 2022.
 */


package nl.das.btclient;

/**
 * 
 */
public class CommandException extends Exception {

	private static final long serialVersionUID = 1731788999308759797L;

	public CommandException(String msg) {
		super(msg);
	}
	
}
