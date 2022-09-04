/*
 * Copyright © 2022 Dutch Arrow Software - All Rights Reserved
 * You may use, distribute and modify this code under the
 * terms of the Apache Software License 2.0.
 *
 * Created 25 Aug 2022.
 */


package nl.das.btclient;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

import javax.bluetooth.BluetoothStateException;
import javax.bluetooth.DeviceClass;
import javax.bluetooth.DiscoveryAgent;
import javax.bluetooth.DiscoveryListener;
import javax.bluetooth.LocalDevice;
import javax.bluetooth.RemoteDevice;
import javax.bluetooth.ServiceRecord;
import javax.bluetooth.UUID;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.json.bind.JsonbConfig;
import javax.microedition.io.Connector;
import javax.microedition.io.StreamConnection;

/**
 *
 */
public class BtClient implements DiscoveryListener {

    private DiscoveryAgent discoveryAgent;
	private List<RemoteDevice> remoteDevices = new ArrayList<>();
    private List<RemoteDevice> discoveredDevices = new ArrayList<>();
    private StreamConnection streamConnection;
    private DataOutputStream dataOut;
    private DataInputStream dataIn;
    private UUID curUuid;

    CompletableFuture<List<RemoteDevice>> completableFuture;

    public BtClient() {
    	try {
			this.discoveryAgent = LocalDevice.getLocalDevice().getDiscoveryAgent();
		} catch (BluetoothStateException e) {
			e.printStackTrace();
		}
    }

    public CompletableFuture<List<RemoteDevice>> searchDevices() {
    	this.completableFuture = new CompletableFuture<>();
        Executors.newCachedThreadPool().submit(() -> {
            this.remoteDevices.clear();
            this.discoveredDevices.clear();
            RemoteDevice[] cachedDevices = this.discoveryAgent.retrieveDevices(DiscoveryAgent.CACHED);
            if (cachedDevices != null) {
                int s = cachedDevices.length;
                for (int i=0; i<s; i++) {
                    this.remoteDevices.add(cachedDevices[i]);
                }
            }
            RemoteDevice[] preknownDevices = this.discoveryAgent.retrieveDevices(DiscoveryAgent.PREKNOWN);
            if (preknownDevices != null) {
                int s = preknownDevices.length;
                for (int i=0; i<s; i++) {
                    this.remoteDevices.add(preknownDevices[i]);
                }
            }
            boolean inquiryStarted = false;
            try {
            	inquiryStarted =this.discoveryAgent.startInquiry(DiscoveryAgent.GIAC, this);
            } catch(BluetoothStateException bse) {
            	System.out.println(bse.getMessage());
            }

            if (inquiryStarted == true) {
                // Display progress message
                System.out.println("Search started");
            } else {
                // Display error message
                System.out.println("Search Failed");
            }
        });

        return this.completableFuture;
    }

    public String sendCommand(UUID uuid, String command, String data) throws CommandException {
    	String res = "";
    	if ((this.curUuid != null) && (this.curUuid != uuid)) {
    		close();
    	}
    	this.curUuid = uuid;
//    	CompletableFuture<String> sendCmd = new CompletableFuture<>();
//    	Executors.newCachedThreadPool().submit(() -> {
    		try {
	            // Select the service. Indicate no
	            // authentication or encryption is required.
    			if (this.streamConnection == null) {
		    		String connectionURL = this.discoveryAgent.selectService(uuid,ServiceRecord.NOAUTHENTICATE_NOENCRYPT,false);
		    		if (connectionURL == null) {
		    			throw new CommandException("Cannot connect to Service.");
		    		}
		            this.streamConnection = (StreamConnection) Connector.open(connectionURL);
		            this.dataOut = this.streamConnection.openDataOutputStream();
		            this.dataIn = this.streamConnection.openDataInputStream();
    			}
	            System.out.println("Sending command");
	            Command msg = new Command();
	            if (data != null) {
					JsonReader jsonReader = Json.createReader(new StringReader(data));
					JsonObject object = jsonReader.readObject();
					msg.setData(object);
	            }
	            msg.setCmd(command);
	    		Jsonb jsonb = JsonbBuilder.create(new JsonbConfig().withFormatting(true));
	            this.dataOut.write(jsonb.toJson(msg).getBytes());
	            this.dataOut.write(0x03);
	            System.out.println("Command send, waiting for response");
				int chr;
				StringBuffer sb = new StringBuffer();
				while ((chr = this.dataIn.read()) != -1) {
					if (chr == 0x03) {
						res = sb.toString();
						break;
					}
					sb.append((char)chr);
				}
	        } catch (IOException ioe) {
	        	this.streamConnection = null;
	        	throw new CommandException(ioe.getMessage());
	        }
//    	});
		Jsonb jsonb = JsonbBuilder.create(new JsonbConfig().withFormatting(true));
		String response = "";
		if ((res != null) && (res.length() > 0)) {
			JsonObject obj = jsonb.fromJson(res, Response.class).getResponse();
			if (obj != null) {
				response = obj.toString();
		        System.out.println("Response received");
//		        System.out.println(response);
			} else {
				System.out.println("No response received");
			}
		} else {
			System.out.println("No response received");
		}
    	return response;
    }

    /**
     * deviceDiscovered() is called by the DiscoveryAgent when
     * it discovers a device during an inquiry.
     */
	@Override
	public void deviceDiscovered (RemoteDevice btDevice, DeviceClass cod) {
        this.discoveredDevices.add(btDevice);
//		try {
//			System.out.println("Added device " + btDevice.getFriendlyName(true));
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
	}

	@Override
	public void inquiryCompleted (int discType) {
        System.out.println("Search completed");
        // Now that the inquiry has been completed, move newly
        // discovered devices into the remoteDevices Vector
        int s = this.discoveredDevices.size();
        if (s > 0) {
        	for (RemoteDevice d : this.discoveredDevices) {
        		boolean found = false;
        		for (RemoteDevice r : this.remoteDevices) {
	                if (d.getBluetoothAddress().equalsIgnoreCase(r.getBluetoothAddress())) {
	                    found = true;
	                    break;
	                }
        		}
        		if (!found) {
	                this.remoteDevices.add(d);
        		}
            }
        }
        this.completableFuture.complete(this.remoteDevices);
	}

	@Override
	public void servicesDiscovered (int transID, ServiceRecord[] servRecord) {
	}

	@Override
	public void serviceSearchCompleted (int transID, int respCode) {
	}

	public void close () {
		try {
			if (this.dataIn != null) {
				this.dataIn.close();
			}
			if (this.dataOut != null) {
				this.dataOut.close();
			}
			if (this.streamConnection != null) {
				this.streamConnection.close();
			}
			this.streamConnection = null;
		} catch (IOException e) {
		}
	}
}
