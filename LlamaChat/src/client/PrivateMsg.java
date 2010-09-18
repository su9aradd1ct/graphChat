/*- ChatPane.java -------------------------------------------------+
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

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.net.*;
import java.util.Hashtable;
import java.util.Enumeration;

/* -------------------- JavaDoc Information ----------------------*/
/**
 * This handles private messages
 * @author Joseph Monti <a href="mailto:countjoe@users.sourceforge.net">countjoe@users.sourceforge.net</a>
 * @version 0.8
 */
public class PrivateMsg {
	LlamaChat llamaChat;
	private Hashtable privates;
	
	PrivateMsg(LlamaChat lc) {
		llamaChat = lc;
		privates = new Hashtable();
	}
	
	
	public void recievePrivate(String username, String message) {
		MsgWindow msg = newPrivate(username);
		if (msg != null) {
			msg.cp.sendText(username, message);
		}
	}
	
	public MsgWindow newPrivate(String username) {
		if (!llamaChat.users.contains(username)) {
			llamaChat.error(username  + " does not exist");
			return null;
		}
		if (llamaChat.ignores.contains(username)) {
			llamaChat.error(username + " is ignored, cannot private message");
			return null;
		}
		MsgWindow msg = null;
		if (privates.containsKey(username)) {
			msg = (MsgWindow) privates.get(username);
		} else {
			msg = new MsgWindow(username);
			privates.put(username, msg);
		}
		return msg;
	}
	
	public void serverMessage(String username, String message) {
		if (privates.containsKey(username)) {
			MsgWindow msg = (MsgWindow) privates.get(username);
			msg.cp.sendText("server", message);
		}
	}
	
	private class MsgWindow extends JFrame {
		ChatPane cp;
		JTextField messageText;
		String username;
		MsgWindow(String un) {
			super("Private Message: " + un);
			username = un;
			setSize(new Dimension(350, 200));
			setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
			addWindowListener(new WindowAdapter() {
				public void windowClosed(WindowEvent e) {
					MsgWindow cur = (MsgWindow) e.getWindow();
					Enumeration enum = privates.keys();
					while (enum.hasMoreElements()) {
						String user = (String) enum.nextElement();
						MsgWindow tmpFra = (MsgWindow) privates.get(user);
						if (cur.equals(tmpFra)) {
							privates.remove(user);
							break;
						}
					}
				}
			});
			cp = new ChatPane(llamaChat);
			messageText = new JTextField();
			
			messageText.addKeyListener(new KeyListener() {
				public void keyTyped(KeyEvent ke) {
					if (ke.getKeyChar() == KeyEvent.VK_ENTER) {
						String txt = new String(messageText.getText());
						if (!txt.equals("")) {
							if (!llamaChat.users.contains(username)) {
								cp.error(username  + " does not exist");
							} else {
								llamaChat.sendPrivate(username, txt);
								cp.sendText(llamaChat.username, txt);
							}
							messageText.setText("");
						}
					}
				}
				public void keyPressed(KeyEvent ke) {
					//
				}
				public void keyReleased(KeyEvent ke) {
					//
				}
			});
			getContentPane().add(new JScrollPane(cp, 
								JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
								JScrollPane.HORIZONTAL_SCROLLBAR_NEVER), BorderLayout.CENTER);
			getContentPane().add(messageText, BorderLayout.SOUTH);
			setVisible(true);
			cp.sendText("server", "Private message session started with " + un);
		}
	}
}
