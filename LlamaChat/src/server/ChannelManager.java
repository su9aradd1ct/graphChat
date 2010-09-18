/*- ChannelManager.java -------------------------------------------+
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

import java.util.Vector;
import java.util.Hashtable;
import java.util.Enumeration;

/* -------------------- JavaDoc Information ----------------------*/
/**
 * This class manages the channels for the server. It allows 3 types
 * of channels. The first of which, defaultChannel, is limited to
 * one and is the default channel that users are sent to when
 * connecting. The second, systemChannels, is a collection of channels
 * that are perminent and created upon program initialization from
 * the configuration file. The third, userChannels, are created by users
 * and can only exist if specified in the system configuration file. When
 * a userChannel is empty it is deleted.
 * @author Joseph Monti <a href="mailto:countjoe@users.sourceforge.net">countjoe@users.sourceforge.net</a>
 * @version 0.8
 */
public final class ChannelManager {
	public static char allowUserChannels;
	public String defaultChannel;
	private int defaultCount;
	private static Hashtable systemChannels;
	private static Hashtable userChannels;
	public static final String eName = "Invalid Channel Name";
	public static final String eAllowed = "User Channels not allowed";
	public static final String eAdmin = "Admin only operation";

	/**
	 * Constructor to setup variables
	 */
	ChannelManager() {
		allowUserChannels = 'y';
		defaultChannel = "Lobby";
		defaultCount = 0;
		systemChannels = null;
		userChannels = null;
	}

	/**
	 * sets the default channel for the server
	 * @param name	the name of the channel
	 * @return true on success, false otherwise (if its not a valid channeL)
	 */
	public boolean setDefaultChannel(String name) {
		if (!name.equals(defaultChannel) && !isValidChannel(name)) {
			return false;
		}
		defaultChannel = name;
		return true;
	}

	/**
	 * creates a new system channel (if valid name)
	 * @param name	the name of the channel
         * @param pass  the password for the channel, can be null if no
         *              password required
	 * @return true on success, false otherwise (if its not a valid channeL)
	 */
	public boolean addSystemChannel(String name, String pass) {
		if (!isValidChannel(name)) {
			return false;
		}
		if (systemChannels == null) {
			systemChannels = new Hashtable();
		}
		systemChannels.put(name, new ChannelManagerItem(pass));
		return true;
	}

	/**
	 * creates a new user channel (if valid name and is allowed)
	 * @param name	the name of the channel
	 * @param pass  the password for the channel, can be null if no
	 *              password required
	 * @param cc	the client requesting the channel
	 * @return null on success, reason otherwise (if its not a valid channeL)
	 */
	public String addUserChannel(String name, String pass, ClientConnection cc) {
		if (allowUserChannels == 'n') {
			return eAllowed;
		}
		if (allowUserChannels == 'a' && !cc.isAdmin()) {
			return eAdmin;
		}
		if (!isValidChannel(name)) {
			return eName;
		}
		if (userChannels == null) {
			userChannels = new Hashtable();
		}
		userChannels.put(name, new ChannelManagerItem(pass));
		return null;
	}

	/**
	 * adds a user to the specified channel
	 * @param name	the name of the channel
         * @param pass  the password to use for the channel, can be null
	 * @return true on success, false otherwise (if its not a valid channel,
         *                                           or invalied passphrase)
	 */
	public boolean userAdd(String name, String pass) {
		if (defaultChannel.equals(name)) {
			defaultCount++;
			return true;
		}

		if (systemChannels != null && systemChannels.containsKey(name)) {
		   ChannelManagerItem value = (ChannelManagerItem) systemChannels.get(name);
			if (value == null) {
				return false;
			}
		   if (value.pass == null || value.pass.equals(pass)) {
				value.countpp();
				return true;
			}
			return false;
		}
		if (userChannels != null && userChannels.containsKey(name)) {
			ChannelManagerItem value = (ChannelManagerItem) userChannels.get(name);
			if (value == null) {
				return false;
			}
			if (value.pass == null || value.pass.equals(pass)) {
				value.countpp();
				return true;
			}
		}
		return false;
	}

	/**
	 * removes a user from the specified channel, only used on user channels
	 * @param name	the name of the channel
	 * @return 	true when the operation emptied the channel and the channel
	 *			was removed, false if failed or did not empty channel
	 */
	public boolean userDel(String name) {
		if (defaultChannel.equals(name)) {
			defaultCount--;
			return false;
		}

		if (systemChannels != null && systemChannels.containsKey(name)) {
			ChannelManagerItem value = (ChannelManagerItem) systemChannels.get(name);
			if (value != null) {
				value.countmm();
			}
			return false;
		}
		if (userChannels != null && userChannels.containsKey(name)) {
			ChannelManagerItem value = (ChannelManagerItem) userChannels.get(name);
			if (value == null) {
				return false;
			}
			if (value.countmm()) {
				userChannels.remove(name);
				return true;
			}
		}
		return false;
	}

	/**
	 * checks to see if the channel can be created
	 * @param name	the name of the channel
     * @return true if valid channel
	 */
	public boolean isValidChannel(String name) {
		if (defaultChannel.equals(name) ||
				(systemChannels != null && systemChannels.containsKey(name)) ||
				(userChannels != null && userChannels.containsKey(name)) ||
				name.length() > 12 ||  !name.matches("[\\w_-]+?")) {
			return false;
		}
		return true;
	}

	/**
	 * checks to see if the channel exists
	 * @param name	the name of the channel
         * @return true if channel exists
	 */
	public boolean channelExists(String name) {
		if (defaultChannel.equals(name) ||
				(systemChannels != null && systemChannels.containsKey(name)) ||
				(userChannels != null && userChannels.containsKey(name))) {
			return true;
		}
		return false;
	}
	
	/**
	 * checks to see if channel has password
	 * @return "" if name has pass, null otherwise
	 */
	public String channelHasPass(String name) {
		if (defaultChannel.equals(name)) {
			return null;
		} else if (systemChannels != null && systemChannels.containsKey(name)) {
			ChannelManagerItem value = (ChannelManagerItem) systemChannels.get(name);
			if (value != null) {
				if (value.pass != null) {
					return "";
				}
			}
			return null;
		} else if (userChannels != null && userChannels.containsKey(name)) {
			ChannelManagerItem value = (ChannelManagerItem) userChannels.get(name);
			if (value != null) {
				if (value.pass != null) {
					return "";
				}
			}
			return null;
		}
		return null;
	}

	/**
	 * returns an enumeration of all the contained channels
	 * @return an enumeration of all contained channels
	 */
	public Enumeration enumerate() {
		Vector all = new Vector();
		all.add(defaultChannel);
		if (systemChannels != null) {
			Enumeration e = systemChannels.keys();
			while (e.hasMoreElements()) {
				all.add(e.nextElement());
			}
		}
		if (userChannels != null) {
			Enumeration e = userChannels.keys();
			while (e.hasMoreElements()) {
				all.add(e.nextElement());
			}
		}
		return all.elements();
	}

	/**
	 * storage class for placing in a HashTable
	 */
	public class ChannelManagerItem {
		/**
		 * number of users connected
		 */
		public int count;

		/**
		 * password for channel, null for no password
		 */
		public String pass;

		/**
		 * default constructor for default initialization
		 */
		ChannelManagerItem() {
			count = 0;
			pass = null;
		}

		/**
		 * creates a new channelmanageritem with p as its password
		 * @param p		the password
		 */
		ChannelManagerItem(String p) {
			count = 0;
			pass = (p == null ? null : new String(p));
		}

		/**
		 * increment the user count
		 */
		public void countpp() {
			count++;
		}

		/**
		 * decrement the user count
		 * @return true if decrement emptied room
		 */
		public boolean countmm() {
			count--;
			if (count == 0)
				return true;
			return false;
		}
	}
}

