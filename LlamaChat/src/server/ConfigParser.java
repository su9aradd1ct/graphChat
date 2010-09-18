/*- ConfigParser.java ---------------------------------------------+
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

import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.Attributes;

/* -------------------- JavaDoc Information ----------------------*/
/**
 * a default handler for config file parsing in XML
 * @author Joseph Monti <a href="mailto:countjoe@users.sourceforge.net">countjoe@users.sourceforge.net</a>
 * @version 0.8
 */
public class ConfigParser extends DefaultHandler {
	/**
	 * a reference back to the server
	 */
	private LlamaChatServer server;
	
	/**
	 * status indicator for telling if in message tag
	 */
	private boolean inMessage;
	
	/**
	 * buffer to use while parsing welcome message
	 */
	private String messageBuffer;
	
	/**
	 * constructor to initialize data
	 * @param a reference back to the calling server
	 */
	ConfigParser(LlamaChatServer s) {
		server = s;
		inMessage = false;
		messageBuffer = "";
	}
	
	/**
	 * used for gathering welcome message (ignores all other tags)
	 */
	public void characters(char[] ch, int start, int length) {
		String s = new String(ch, start, length);
		if (s != null && inMessage) {
			s = s.replaceAll("[\\t\\n]", " ");
			s = s.replaceAll("[ ]{2,}", " ");
			s = s.trim();
			messageBuffer+= s;
		}
	}
	
	/**
	 * used for gathering welcome message (ignores all other tags)
	 */
	public void endElement(String uri, String localName, String qName) {
		String name = localName;
		if ("".equals(name)) name = qName;
		if ("WelcomeMessage".equals(name)) {
			inMessage = false;
			server.welcomeMessage = messageBuffer;
		}
	}
	
	/**
	 * parses the data within the atributes for the configuration settings
	 */
	public void startElement(String uri, String localName, String qName, 
													Attributes attributes) {
		String name = localName;
		if ("".equals(name)) name = qName;
		if ("Port".equals(name)) {
			String val = attributes.getValue("value");
			try {
				if (val != null) server.PORT = Integer.parseInt(val);
			} catch (NumberFormatException e) { }
		} else if ("SysLogFile".equals(name)) {
			String val = attributes.getValue("value");
			if (val != null) server.sysLogFile = val;
		} else if ("ChatLogPath".equals(name)) {
			String val = attributes.getValue("value");
			if (val != null) {
				if ("".equals(val)) {
					val = ".";
				}
				server.chatLogPath = val;
			}
		} else if ("AllowAdmin".equals(name)) {
			String val = attributes.getValue("value");
			if (val != null) {
				if (val.equals("yes")) {
					server.allowAdmin = true;
				} else if (val.equals("no")) {
					server.allowAdmin = false;
				}
			}
		} else if ("PassPhrase".equals(name)) {
			String val = attributes.getValue("value");
			if (val != null) server.adminPass = val;
		} else if ("UserExportFile".equals(name)) {
			String val = attributes.getValue("value");
			if (val != null) server.userExportFile = val;
		} else if ("AllowUserChannels".equals(name)) {
			String val = attributes.getValue("value");
			if (val != null) {
				if (val.equals("yes")) {
					server.channels.allowUserChannels = 'y';
				} else if (val.equals("admin")) {
					server.channels.allowUserChannels = 'a';
				} if (val.equals("no")) {
					server.channels.allowUserChannels = 'n';
				}
			}
		} else if ("DefaultChannel".equals(name)) {
			String val = attributes.getValue("value");
			if (val != null) server.channels.setDefaultChannel(val);
		} else if ("Channel".equals(name)) {
			String val = attributes.getValue("value");
			String pass = null;
			if (attributes.getLength() > 1) {
				pass = attributes.getValue("pass");
			}
			if (val != null) server.channels.addSystemChannel(val, pass);
		} else if ("WelcomeMessage".equals(name)) {
			inMessage = true;
		} else if ("br".equals(name)) {
			messageBuffer+= "\n";
		}
	}
}
