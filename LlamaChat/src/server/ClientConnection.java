/*- ClientConnection.java -----------------------------------------+
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

import javax.net.ssl.*;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.IOException;
import java.lang.ClassNotFoundException;
import common.*;
import common.sd.*;

/* -------------------- JavaDoc Information ----------------------*/
/**
 * A threaded connection class to maintain the conneciton to the client
 * a new ClientConnection must be create for each new client
 * @author Joseph Monti <a href="mailto:countjoe@users.sourceforge.net">countjoe@users.sourceforge.net</a>
 * @version 0.8
 */
public class ClientConnection implements Runnable, SocketConnection {
	private LlamaChatServer server;
	private SSLSocket socket;
	public String name;
	public String ip;
	public String channel;

	private ObjectInputStream in;
	private ObjectOutputStream out;
	private boolean finalized;
	private boolean admin;
	
	private MessageQueue queue;

	ClientConnection(LlamaChatServer serv, SSLSocket sock) {
		try {
			name = null;
			finalized = false;
			admin = false;
			channel = serv.channels.defaultChannel;
			server = serv;
			socket = sock;
			ip = sock.getInetAddress().getHostName();
			out = new ObjectOutputStream(sock.getOutputStream());
			in = new ObjectInputStream(sock.getInputStream());
			queue = new MessageQueue(this);
			new Thread(this).start();
		} catch (IOException e) {
			server.log(this, "failed connection");
			server.connectingUsers.remove(this);
		}
	}

	/**
	 * Method to read an object from the client
	 * @return returns the SocketData object recieved
	 */
	private SocketData readObject() {
		try {
			return (SocketData) in.readObject();
		} catch (IOException e) {
			return null;
		} catch (ClassNotFoundException e) {
			return null;
		}
	}

	/**
	 * Method to write an object to the server via MessageQueue
	 * @param sd 	A SocketData object to be sent
	 */
	public void writeObject(Object obj) {
		queue.enqueue(obj);
	}

	/**
	 * Method to actually write an object to the server
	 * @param sd 	A SocketData object to be sent
	 */
	public void _writeObject(Object obj) {
		try {
			out.writeObject(obj);
			out.flush();
		} catch (IOException e) {
			close();
		}
	}

	/**
	 * closes the connection to the client
	 */
	public void close() {
		server.kill(this);
		try {
			socket.close();
		} catch (IOException e) { e.printStackTrace(); }
	}

	/**
	 * utility method to retrieve the admin status
         * @return true if admin
	 */
	public boolean isAdmin() {
		return admin;
	}

// -- Interface methods -- \\
	/**
	 * receives server configurations
	 * @param type	the type of data being sent
	 * @param obj	the object being sent
	 */
	public void serverCap(char type, Object obj) {
		// does nothing ... eventually will be able to configure from client
	}

	/**
	 * used to finalize the conneciton to the client.
	 * when a user connects he must send a SD_UserAdd to tell
	 * the server the requested name
	 * @param username	the name of the client
	 */
	public void userAdd(String username) {
		if (username == null) {
			writeObject(new SD_Error("no username recieved"));
			return;
		}
		if (name == null && !finalized) {
			name = username;
			finalized = server.finalizeUser(username, this);
			if (!finalized) {
				name = null;
				writeObject(new SD_Kick(null));
			}
		} else {
			writeObject(new SD_Error("already recieved name: " + name));
		}
	}

	/**
	 * used by a client to attain administrative status
	 * @param password	the password provided by the client
	 */
	public void adminAdd(String password) {
		if (!server.allowAdmin) {
			writeObject(new SD_Error("admins are not allow on this server"));
		} else if (password.equals(server.adminPass)) {
			admin = true;
			server.broadcast(new SD_AdminAdd(name), null, channel);
		} else {
			writeObject(new SD_Error("incorrect administrative password"));
		}
	}

	/**
	 * Tells the server that the client wishes to disconnect
     * @param username the name of the user
	 */
	public void userDel(String username) {
		close();
	}

	/**
	 * used to rename the client
	 * <i>ADD USERNAME VALIDITY CHECK</a>
	 * @param on	the old name of the client
	 * @param nn	the new (requested) name of the client
	 */
	public void rename(String on, String nn) {
		if (server.connectedUsers.containsKey(nn)) {
			writeObject(new SD_Error("username already exists"));
			//writeObject(new SD_Rename(nn, name));
		} else {
			server.connectedUsers.remove(name);
			server.connectedUsers.put(nn, this);
			server.broadcast(new SD_Rename(name, nn), nn, channel);
			server.updateUserExport();
			name = nn;
		}
	}

	/**
	 * used to kick a user; eventually IP banning will be an added
	 * option (called ban), but will use SD_Kick
     * @param username the name of the user to kicko
	 */
	public void kick(String username) {
		if (admin) {
			ClientConnection cc =
				(ClientConnection) server.connectedUsers.get(username);
			if (cc == null) {
				writeObject(new SD_Error("user " + username +
												" does not exist"));
			} else {
				server.sendTo(new SD_Error("You have been kicked by " + name),
												username);
				cc.close();
			}
		} else {
			writeObject(new SD_Error("Couldn't verify administrative status"));
		}
	}

	/**
	 * recieves a channel change
	 * @param nc	true for a new channel, if the channel exists an error is
	 				sent back to the user, otherwise the channel is created
					and a recursive call is made to switch the user to
					the new channel; if false it checks the validity of the
					request and move the user
	 * @param n		the name of the channel
     * @param p     password for the channel, if one
	 */
	public void channel(boolean nc, String n, String p) {
		if (nc) {
			String reason;
			if ((reason = server.newChannel(n, p, this)) == null) {
				server.log(this, "channel " + n + " created");
				channel(false, n, p);
			} else {
				writeObject(new SD_Error(reason));
			}
		} else {
			if (server.channels.channelExists(n)) {
				if (channel.equals(n)) {
					writeObject(new SD_Error("Already a member of " + n));
					writeObject(new SD_Channel(false,null,null));
				} else if (!server.channels.userAdd(n, p)) {
					writeObject(new SD_Error("invalid passphrase or none " +
								"provided, use \\join " + n + " &lt;password&gt;"));
					writeObject(new SD_Channel(false,null,null));
				} else {
					server.broadcast(new SD_UserDel(name), name, channel);
					if (server.channels.userDel(channel)) {
						server.broadcast(new SD_Channel(true, channel, null), null);
						server.chatLog(this, false);
						server.log(this, "channel " + channel + " removed");
					}
					channel = n;
					server.broadcast(new SD_UserAdd(name), name, channel);
					if (admin) {
						server.broadcast(new SD_AdminAdd(name), name, channel);
					}

					writeObject(new SD_Channel(false, channel, null));
					server.sendUserList(this);
					server.updateUserExport();
				}
			} else {
				writeObject(new SD_Error(n  + " does not exist"));
				writeObject(new SD_Channel(false,null,null));
			}
		}
	}


	/**
	 * recives a chat message from the user
	 * @param username	the name of the user, not used in this case
	 					(it should be null)
	 * @param message	the messsage sent by the user
	 */
	public void chat(String username, String message) {
		if (finalized) {
			server.chatLog(this, message);
			server.broadcast(new SD_Chat(name, message), name, channel);
		} else {
			writeObject(new SD_Error("connection not confirmed"));
		}
	}

	/**
	 * recives a private message from the user
	 * @param username	the name of the user to whom the message is sent
	 * @param message	the messsage sent by the user
	 */
	public void private_msg(String username, String message) {
		if (finalized) {
			server.sendTo(new SD_Private(name, message), username);
		} else {
			writeObject(new SD_Error("connection not confirmed"));
		}
	}

	/**
	 * whispers to a user
	 * @param username	the name of the user that the whisper is to be sent
	 * @param message	the message that is to be whispered
	 */
	public void whisper(String username, String message) {
		if (finalized) {
			server.sendTo(new SD_Whisper(name, message), username);
		} else {
			writeObject(new SD_Error("connection not confirmed"));
		}
	}

	/**
	 * used to control the chat logging status
	 * @param start		true to start logging, false to stop
	 */
	public void chatLog(boolean start) {
		if (admin) {
			if (server.chatLog(this, start)) {
				writeObject(new SD_Log(start));
			} else {
				writeObject(new SD_Error("unable to modify log for " + channel));
			}
	   } else {
		   writeObject(new SD_Error("Could not verify administrative status"));
	   }
	}

	/**
	 * recieves an error from the client
     * @param err the error to report
	 */
	public void error(String err) {
		server.log(this, err);
	}

// -- Interface methods -- \\

	/**
	 * Method to implement runnable, listens
	 * for incoming objects
	 */
	public void run() {
		SocketData sd;
		while ((sd = readObject()) != null) {
			sd.performAction(this);
		}
		close();
	}
}
