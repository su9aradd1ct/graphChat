/*- ServerConneciton.java -----------------------------------------+
 |                                                                 |
 |  Copyright (C) 2002-2003 Joseph Monti, LlamaChat                |
 |                     countjoe@users.sourceforge.net              |
 |                     http://www.42llamas.com/LlamaChat/          |
 |                                                                 |
 | This program is free software; you can redistribute it and/or   |
 | modify it under the terms of the GNU General Public License     |
 | as published by the Free Software Foundation; either version 2  |
 | of the License, or (at your option) any later version           |
 |                                                                 |
 | This program is distributed in the hope that it will be useful, |
 | but WITHOUT ANY WARRANTY; without even the implied warranty of  |
 | MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the   |
 | GNU General Public License for more details.                    |
 |                                                                 |
 | A copy of the GNU General Public License may be found in the    |
 | installation directory named "GNUGPL.txt"                       |
 |                                                                 |
 +-----------------------------------------------------------------+
 */

package client;

import javax.net.ssl.*;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.IOException;
import java.lang.ClassNotFoundException;
import javax.swing.JOptionPane;

import common.*;
import common.sd.*;

/* -------------------- JavaDoc Information ----------------------*/
/**
 * A threaded connection class to maintain the conneciton to the server
 * @author Joseph Monti <a href="mailto:countjoe@users.sourceforge.net">countjoe@users.sourceforge.net</a>
 * @version 0.8
 */
public class ServerConnection implements Runnable, SocketConnection {

	private LlamaChat client;
	private SSLSocket socket;
	private ObjectOutputStream out;
	private ObjectInputStream in;
	public boolean connected;
	public boolean finalized;
	public String channel;
	private MessageQueue queue;
	
	ServerConnection(LlamaChat c) {
		connected = false;
		finalized = false;
		client = c;
		channel = "Lobby";
		queue = new MessageQueue(this);

		try {
			// initialize socket
			SSLSocketFactory sslFact = 
					(SSLSocketFactory)SSLSocketFactory.getDefault();
			socket = (SSLSocket)sslFact.createSocket(c.site, c.PORT);
			// Checking the supported Cipher suites.
			String [] enabledCipher = socket.getSupportedCipherSuites ();
			// Enabled the Cipher suites.
			socket.setEnabledCipherSuites (enabledCipher);

			// make in/out stream connection
			out = new ObjectOutputStream(socket.getOutputStream());
			in = new ObjectInputStream(socket.getInputStream());
			connected = true;
			c.setConnected(true);
			new Thread(this).start();
		} catch (IOException e) {
			c.error("Server not available");
			c.setConnected(false);
			connected = false;
		} 
	}
	
	/**
	 * Method to read an object from the server
	 * @return returns the SocketData object recieved
	 */
	private SocketData readObject() {
		try {
			return (SocketData) in.readObject();
		} catch (Throwable t) {
			return null;
		}
	}

	/**
	 * Method to write an object to the server
	 * @param sd 	A SocketData object to be sent
	 */
	public void writeObject(Object obj) {
		queue.enqueue(obj);
	}

	/**
	 * Method to write an object to the server
	 * @param sd 	A SocketData object to be sent
	 */
	public void _writeObject(Object obj) {
		if (connected) {
			try {
				out.writeObject(obj);
				out.flush();
			} catch (IOException e) {
				close();
			}
		}
	}

	/**
	 * closes the connection to the server
	 */
	public void close() {
		try {
			connected = false;
			client.setConnected(false);
			if (connected) socket.close();
		} catch (IOException e) { }
		client.close();
	}
	
// -- SocketConnection Interface methods -- \\

	/**
	 * receives server configurations
	 * @param type	the type of data being sent
	 * @param obj	the object being sent
	 */
	public void serverCap(char type, Object obj) {
		try {
			switch (type) {
			case SD_ServerCap.T_CREATE:
				char tmp = ((Character) obj).charValue();
				switch (tmp) {
				case 'y':
					client.butCreate.setEnabled(true);
					client.chanAdmin = false;
					break;
				case 'a':
					client.chanAdmin = true;
					break;
				default:
					client.chanAdmin = false;
					break;
				}
				break;
			default:
				break;
			}
		} catch (ClassCastException e) { }
	}

	/**
	 * adds a user to the server
	 * @param username	the name of the user to be added
	 */
	public void userAdd(String username) {
		if (username == null) {
			client.showUserStatus = true;
		} else {
			client.userAdd(username);
		}
	}
	/**
	 * adds an admin to the server. If the name of the user
	 * is yours, set interal admin status
	 * @param username	the name of the user made an admin
	 */
	public void adminAdd(String username) {
		if (username.equals(client.username)) {
			client.setAdmin();
		} if (!client.admins.contains(username)) {
			client.admins.add(username);
			client.serverMessage(username + " is now an admin");
			client.updateList();
		}
	}
	/**
	 * removes a user from the server
	 * @param username	the name of the user to be removed
	 */
	public void userDel(String username) {
		client.userDel(username);
	}
	/**
	 * renames a user, if the name of the user is yours, 
	 * rename yourself first.
	 * @param on	the old name of the user
	 * @param nn	the new name of the user
	 */
	public void rename(String on, String nn) {
		if (on.equals(client.username)) {
			client.username = nn;
		}
		client.rename(on, nn);
	}
	/**
	 * recieves kick notification
	 * @param un	should be null, so not used
	 */
	public void kick(String un) { 
	   client.username = null;
	}
	
	/**
	 * handles a change in channels
	 * @param nc	true if a new channel, false when you have
	 				succesfully changed channels so clear all user
					lists and add yourself; should be recieving
					a list of any connected users in this channel
					shortly
	 * @param n		the name of the channel
	 * @param p		should be null, not used here
	 */
	public void channel(boolean nc, String n, String p) {
		if (nc) {
			client.newChannel(n, (p == null ? false : true));
		} else {
			if (n == null) {
				client.cboChannels.setSelectedItem(channel);
				client.showUserStatus = true;
			} else {
				client.users.clear();
				client.afks.clear();
				client.ignores.clear();
				client.admins.clear();
				if (client.admin) {
					client.admins.add(client.username);
				}
				client.updateList();
				client.serverMessage("You have joined " + n);
				client.userAdd(client.username);
				client.cboChannels.setSelectedItem(n);
				channel = n;
			}
		}
	}
	/**
	 * recieves a chat message
	 * @param username	the user sending the chat
	 * @param message	the message
	 */
	public void chat(String username, String message) {
		client.recieveChat(username, message);
	}
	/**
	 * recieves a private message
	 * @param username	the user sending the message
	 * @param message	the message
	 */
	public void private_msg(String username, String message) {
		client.recievePrivate(username, message);
	}
	/**
	 * recieves a whisper
	 * @param username	the user sending the whisper
	 * @param message	the message being sent
	 */
	public void whisper(String username, String message) {
		client.recieveWhisper(username, message);
	}
	/**
	 * receives notification in change in logging status
	 * @param start		true when starting logging, false otherwise
	 */
	public void chatLog(boolean start) {  
		if (start) {
			client.serverMessage("now logging in " + channel);
		} else {
			client.serverMessage("no longer logging in " + channel);
		}
	}
	/**
	 * receives an error from the server
	 * @param s	the error
	 */
	public void error(String s) {
		client.error(s);
	}

// -- End SocketConnection Interface methods -- \\

	/**
	 * Method to implement runnable first reports its username
	 * to the server in the form of an SD_UserAdd and listens
	 * for incoming objects
	 */
	public void run() {
		writeObject(new SD_UserAdd(client.username));
		SocketData sd;
		while (connected && (sd = readObject()) != null) {
			sd.performAction(this);
		}
		close();
	}
	
}