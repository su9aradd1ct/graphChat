/*- LlamaChatServer.java ------------------------------------------+
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

package server;

import java.io.IOException;
import javax.net.ssl.*;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.LinkedList;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.BufferedReader;
import java.io.FileReader;
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;

import common.*;
import common.sd.*;

/* -------------------- JavaDoc Information ----------------------*/
/**
 * The main class for the LlamaChat server
 * @author Joseph Monti <a href="mailto:countjoe@users.sourceforge.net">countjoe@users.sourceforge.net</a>
 * @version 0.8
 */
public final class LlamaChatServer extends Thread {

	private static LlamaChatServer listeningServer;
	public static int PORT = 42412;
	public static String sysLogFile = "llamachat.log";
	public static String adminPass = "llamachat";
	public static LinkedList connectingUsers;
	public static Hashtable connectedUsers;
	public static boolean running;
	public static boolean allowAdmin = true;
	private static PrintWriter sysLogOut;
	public static String chatLogPath = ".";
	private static Hashtable channelFiles;
	public static String userExportFile = null;
	public static ChannelManager channels;
	public static String welcomeMessage = null;
	public static String serverConfigFile = "llamachatconf.xml";

	/**
	 * called when a new user has connected.
	 * @param s	the secure socket that the user connected on
	 */
	synchronized public void newUser(SSLSocket s) {
		ClientConnection cc = new ClientConnection(this, s);
		connectingUsers.add(cc);
	}

	/**
	 * finalizes the connection to the client by setting its username
	 * and updating all lists of users by sending the new user to all the
	 * connected users and sending the user list to the new user, also
	 * checks the validity of the username
	 * @param uname	the desired user name for the new user
	 * @param cc	the object representing the connecting user
	 * @return		true on success, false otherwise
	 */
	synchronized public boolean finalizeUser(String uname,
											ClientConnection cc) {
	    if (connectedUsers.containsKey(uname) ||
									connectedUsers.contains(cc)) {
			cc.writeObject(new SD_Error("Already a user of that name or " +
					"already connected. \nType \\rename &lt;new name&gt; to " +
					"choose a different name"));
			log(cc, "failed [duplicate exists]");
	       return false;
		} else if (uname.length() > 10) {
			cc.writeObject(new SD_Error("Username too long.\n" +
							"Type \\rename &lt;new name&gt; to choose " +
							"a different name"));
			log(cc, "failed [bad name]");
			return false;
		} else if (uname.equals("server") || !uname.matches("[\\w_-]+?")) {
			cc.writeObject(new SD_Error("Invalid username " + uname + ".\n" +
							"Type \\rename &lt;new name&gt; to choose " +
							"a different name"));
			log(cc, "failed [bad name]");
			return false;
	    } else if (connectingUsers.remove(cc)) {
			cc.writeObject(new SD_ServerCap(SD_ServerCap.T_CREATE, 
							new Character(channels.allowUserChannels)));
			if (welcomeMessage != null) {
				cc.writeObject(new SD_Chat("server", welcomeMessage));
			}
			cc.writeObject(new SD_Channel(false, cc.channel, null));
			sendUserList(cc);
			Enumeration e = channels.enumerate();
			while (e.hasMoreElements()) {
				String channel = (String) e.nextElement();
				cc.writeObject(new SD_Channel(true, channel, 
						channels.channelHasPass(channel)));
			}
			connectedUsers.put(uname, cc);
		    broadcast(new SD_UserAdd(uname), uname, cc.channel);
			updateUserExport();
			log(cc, "connected as " + uname);
		    return true;
	    }

		cc.writeObject(new SD_Error("invalid connection procedure, please try again"));
		return false;
	}

	/**
	 * sends the current user listing to the specified user
	 * @param cc	the user requesting the list
	 */
	synchronized public void sendUserList(ClientConnection cc) {
		Enumeration e = connectedUsers.keys();
		while (e.hasMoreElements()) {
			String un = (String) e.nextElement();
			if (cc.name.equals(un)) {
				continue;
			}
			ClientConnection o = (ClientConnection) connectedUsers.get(un);
			if (o.channel.equals(cc.channel)) {
				cc.writeObject(new SD_UserAdd(un));
				if (o.isAdmin()) {
					cc.writeObject(new SD_AdminAdd(un));
				}
			}
		}
		cc.writeObject(new SD_UserAdd(null)); // send End of List
	}

	/**
	 * sends the specified SocketData to the client, to
	 * @param sd	the SocketData object to be sent
	 * @param to	the user to be sent the data
	 * @return 	status of the sendTo
	 */
	synchronized public boolean sendTo(SocketData sd, String to) {
		ClientConnection o = (ClientConnection) connectedUsers.get(to);
		if (o != null) {
			o.writeObject(sd);
			return true;
		} else {
			return false;
		}
	}

	/**
	 * broadcases a message to all connected user except from
	 * @param sd	the SocketData object to be sent
	 * @param from	the user to be avoided when sending data
	 *				(usually the user sending the data)
	 */
	synchronized public void broadcast(SocketData sd, String from) {
		Enumeration e = connectedUsers.keys();
		while (e.hasMoreElements()) {
			String to = (String) e.nextElement();
			if (!to.equals(from)) {
				ClientConnection o = (ClientConnection) connectedUsers.get(to);
				o.writeObject(sd);
			}
		}
	}

	/**
	 * broadcases a message to all connected user except from
	 * and is a member of c
	 * @param sd	the SocketData object to be sent
	 * @param from	the user to be avoided when sending data
	 *				(usually the user sending the data)
	 * @param c		the channel the user is connected to
	 */
	synchronized public void broadcast(SocketData sd, String from, String c) {
		Enumeration e = connectedUsers.keys();
		while (e.hasMoreElements()) {
			String to = (String) e.nextElement();
			if (!to.equals(from)) {
				ClientConnection o = (ClientConnection) connectedUsers.get(to);
				if (o.channel.equals(c)) {
					o.writeObject(sd);

				}
			}
		}
	}

	/**
	 * sends a SD_UserDel object to all users announcing that un has left
	 * the building
	 * @param cc	the user leaving
	 */
	synchronized public void delete(ClientConnection cc) {
		broadcast(new SD_UserDel(cc.name), null, cc.channel);
	}

	/**
	 * Removes a user from the list of connected users and calls delete
	 * @param cc	the associated ClientConnection.
	 * @see #delete(ClientConnection cc)
	 */
	synchronized public void kill(ClientConnection cc) {
		try {
			if (cc.name != null && connectedUsers.remove(cc.name) == cc) {
				log(cc, cc.name + " disconnected");
				updateUserExport();
				if (channels.userDel(cc.channel)) {
					broadcast(new SD_Channel(true, cc.channel, null), null);
					chatLog(cc, false);
				}
				delete(cc);
			}
		} catch (NullPointerException e) {
			e.printStackTrace();
			if (cc != null) {
				log("null pointer on kill: " + cc.name + " (" + cc.channel + ")");
			} else {
				log("got sent a null cc");
			}
		}
	}

	/**
	 * gets the current date/time of the form MM/DD/YY HH:MM:SS
	 * used for loggin purposes
	 * @return a string specifying the current date/time
	 */
	public static String getTimestamp() {
	   Calendar rightNow = Calendar.getInstance();
		return rightNow.get(Calendar.MONTH) + "/" +
				rightNow.get(Calendar.DATE) + "/" +
				rightNow.get(Calendar.YEAR) + " " +
				rightNow.get(Calendar.HOUR_OF_DAY) + ":" +
				rightNow.get(Calendar.MINUTE) + ":" +
				rightNow.get(Calendar.SECOND);
	}

	/**
	 * writes to the servers log file preceded by a timestamp
	 * @param s	the string to be logged
	 */
	synchronized private void log(String s) {
		if (sysLogOut != null) {
			sysLogOut.println(getTimestamp() + " - " + s);
			sysLogOut.flush();
		}
	}

	/**
	 * a public method used by clients to send log data
	 * @param cc	the client sending the log, used to identify
	 				the logger in the log
	 * @param s		the string to be logged
	 */
	public void log(ClientConnection cc, String s) {
		log(cc.ip + " - " + s);
	}

	/**
	 * manages the chat log
	 * <br><br><i>do something about channels</i>
	 * @param cc	the client attempting to change the log status. used to
	 *				determine what channel to work with, if null close all
	 * @param start	enables chat logging when true, stops otherwise
	 * @return true on succes
	 */
	synchronized public boolean chatLog(ClientConnection cc, boolean start) {
		if (cc == null) {
			Enumeration e = channelFiles.keys();
			while (e.hasMoreElements()) {
				String name = (String) e.nextElement();
				ChatFileItem item = (ChatFileItem)channelFiles.get(name);
				closeLog(null, item);
			}
	      	return true;
		}
		ChatFileItem currentWriter=(ChatFileItem)channelFiles.get(cc.channel);
		if (start) {
			if (currentWriter != null && currentWriter.logging) {
				cc.writeObject(new SD_Error("already logging " + cc.channel));
				return false;
			}
			if (currentWriter == null) {
				currentWriter = new ChatFileItem();
				channelFiles.put(cc.channel, currentWriter);
			}
			String fname = chatLogPath + System.getProperty("file.separator") + "chat-" + cc.channel + ".log";
			try {
				currentWriter.chatOut =
						new PrintWriter(new BufferedOutputStream(
						new FileOutputStream(new File(fname), true)));
				currentWriter.chatOut.println("log started by " +
											(cc!=null?cc.name:"server") +
											" at " + getTimestamp());
				currentWriter.chatOut.println("====================================================");
				currentWriter.chatOut.flush();
				currentWriter.logging = true;
			} catch (FileNotFoundException e) {
				cc.writeObject(new SD_Error("error opening " + fname));
				listeningServer.running = false;
			   return false;
			}
			return true;
		}

		return closeLog(cc, currentWriter);
	}

	/**
	 * utility method to close a specified log
	 * @param cc	the client attempting to close
	 * @param currentWriter	the ChatFileItem to close
	 * @return true on success, false on failure
	 */
	private boolean closeLog(ClientConnection cc, ChatFileItem currentWriter) {
		if (currentWriter == null || currentWriter.chatOut == null) {
			//cc.writeObject(new SD_Error("not logging"));
			return false;
		}
		currentWriter.chatOut.println("====================================================");
		currentWriter.chatOut.println("log stopped by " +
											(cc!=null?cc.name:"server") +
											" at " + getTimestamp());
		currentWriter.chatOut.println("====================================================");
		currentWriter.chatOut.flush();
		currentWriter.chatOut.close();
		currentWriter.chatOut = null;
		currentWriter.logging = false;
		if (cc != null) {
			channelFiles.remove(cc.channel);
		}
		return true;
	}

	/**
	 * logs the specified chat message
	 * @param cc		the client chatting
	 * @param message	the message to be sent
	 */
	synchronized public void chatLog(ClientConnection cc, String message) {
		ChatFileItem currentWriter = (ChatFileItem)channelFiles.get(cc.channel);
		if (currentWriter != null && currentWriter.logging &&
						currentWriter.chatOut != null) {
			currentWriter.chatOut.println(cc.name + ": " + message);
			currentWriter.chatOut.flush();
		}
	}

	/**
	 * allows for the user listing (newline separated) to be exported
	 * to the filesystem, should be called whenever users are added or
	 * removed or renamed
	 * @return true on succes, false otherwise
	 */
	synchronized static public boolean updateUserExport() {
		if (userExportFile == null) {
			return false;
		}
		try {
			PrintWriter pw = new PrintWriter(new BufferedOutputStream(
					new FileOutputStream(new File(userExportFile), false)));
			Enumeration e = connectedUsers.keys();
			int i;
			for (i = 0; e.hasMoreElements(); i++) {
				String usr = (String) e.nextElement();
				ClientConnection o = (ClientConnection)connectedUsers.get(usr);
				pw.println(usr + " (" + o.channel + ")");
			}

			if (i == 0) {
				pw.println("(no connected users)");
			}

			pw.close();
		} catch (FileNotFoundException e) {
			System.err.println("unable to access userExportFile: " +
														userExportFile);
			userExportFile = null;
		}
		return true;
	}

	/**
	 * creates a new channel, assuming the channel can be created
	 * @param name	the name of the channel to be craeted
     * @param pass  the password for the channel
	 * @param cc	the client requesting the channel
     * @return true if created
	 */
	synchronized public String newChannel(String name, String pass, ClientConnection cc) {
		String ret;
		if ((ret = channels.addUserChannel(name, pass, cc)) != null) {
			return ret;
		}
		broadcast(new SD_Channel(true, name, (pass == null ? null : "")), null);
		return null;
	}

	/**
	 * checks to see if the specified channel exists
	 * @param name	the name of the channel
     * @return true if name exists exists
	 */
	synchronized public boolean channelExists(String name) {
		return channels.channelExists(name);
	}

	/**
	 * the main loop to recieving incoming clients listens on a secure
	 * server socket creates a shutdown hook to catch a server kill
	 * and exit gracefully
	 */
	public void run() {
		Runtime.getRuntime().addShutdownHook(new ShutdownThread(this));
		try {
			SSLServerSocketFactory factory =
				(SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
			SSLServerSocket sslIncoming =
				(SSLServerSocket) factory.createServerSocket (PORT);
			String [] enabledCipher = sslIncoming.getSupportedCipherSuites ();
			sslIncoming.setEnabledCipherSuites (enabledCipher);

			log("Server Started");
			while(running) {
				SSLSocket s = (SSLSocket)sslIncoming.accept();
				newUser(s);
			}
		} catch (IOException e) { System.out.println("Error: " + e); }
		log("Server Stopped");
	}


	/**
	 * a useless constructor
	 */
	LlamaChatServer() {
		running = true;
	}

	/**
	 * main method for class; initializes lists and system log file
     * @param args the command line arguments
	 */
	public static void main(String args[]) {
		listeningServer = new LlamaChatServer();
		//listeningServer.running = true;

		connectingUsers = new LinkedList();
		connectedUsers = new Hashtable();
		channels = new ChannelManager();
		channelFiles = new Hashtable();


		try {
			DefaultHandler handler = new ConfigParser(listeningServer);
			SAXParserFactory factory = SAXParserFactory.newInstance();
			SAXParser saxParser = factory.newSAXParser();
			saxParser.parse(new File(serverConfigFile), handler);
		} catch (Throwable t) {
			System.err.println("error parsing " + serverConfigFile);
			running = false;
		}

		try {
			sysLogOut = new PrintWriter(new BufferedOutputStream(
						new FileOutputStream(new File(sysLogFile), true)));
		} catch (FileNotFoundException e) {
			System.err.println(sysLogFile + " not found.");
			running = false;
		}

		updateUserExport();

		listeningServer.start();
		try {
			listeningServer.join();
		} catch (InterruptedException e) { }
	}

	/**
	 * A class to enable gracefull shutdown when unexpectadly killed
	 */
	public class ShutdownThread extends Thread {
        private LlamaChatServer lcs;
        ShutdownThread(LlamaChatServer l) {
            lcs = l;
        }
        public void run() {
			log("Server Stopped");
			sysLogOut.close();
			chatLog(null, false);
		}
	}

	/**
	 * a class to hold logging information on a Hashtable
	 */
	private class ChatFileItem {
		public boolean logging;
		public PrintWriter chatOut;

		ChatFileItem() {
			logging = false;
			chatOut = null;
		}
	}
}
