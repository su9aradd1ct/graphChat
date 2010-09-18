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

import javax.swing.JTextPane;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTML;
import java.lang.String;
import java.lang.StringBuffer;
import java.nio.CharBuffer;
import java.util.regex.Pattern;
import java.awt.Color;

/* -------------------- JavaDoc Information ----------------------*/
/**
 * This represents the chat pane where chat dialoge takes place
 * @author Joseph Monti <a href="mailto:countjoe@users.sourceforge.net">countjoe@users.sourceforge.net</a>
 * @version 0.8
 */
public class ChatPane extends JTextPane {
	
	String icoSad;
	String icoSmily;
	String icoTongue;
	String icoWinking;
	String icoOh;
	LlamaChat llamaChat;

	ChatPane(LlamaChat lc) {
		super();
		
		llamaChat = lc;
		
		setEditable(false);
		setContentType("text/html");
		HTMLEditorKit kit = new HTMLEditorKit();
		StyleSheet css = new StyleSheet();
		css.addRule("BODY{ margin : 0;}");
		css.addRule("P{ margin : 0;}");
		css.addRule("A{ color:#0000FF; text-decoration:underline;}");
        kit.setStyleSheet(css);
		setEditorKit(kit);
		setBackground(new Color(249, 249, 250));

		icoSad = "<img src=\"" + lc.locationURL + "images/sad.gif\" height=\"14\" width=\"14\">";
		icoSmily = "<img src=\"" + lc.locationURL + "images/smiley.gif\" height=\"14\" width=\"14\">";
		icoOh = "<img src=\"" + lc.locationURL + "images/oh.gif\" height=\"14\" width=\"14\">";
		icoTongue = "<img src=\"" + lc.locationURL + "images/tongue.gif\" height=\"14\" width=\"14\">";
		icoWinking = "<img src=\"" + lc.locationURL + "images/winking.gif\" height=\"14\" width=\"14\">";
		
		addHyperlinkListener(lc.myHyperlinkListener);
	}

	/**
	 * Sends a text to the chat window. Parses the message to pick
	 * out emoticons and links.
	 * @param un	the name of the user sending the message
	 * @param message	the message to be sent
	 * @param whisper	indicates the message was a wisper and makes the 
	 *					message italic
	 */
	public void sendText(String un, String message) {
		sendText(un,message,false);
	}
	
	/**
	 * Sends a text to the chat window. Parses the message to pick
	 * out emoticons and links.
	 * @param un	the name of the user sending the message
	 * @param message	the message to be sent
	 */
	public void sendText(String un, String message, boolean whisper) {
		StringBuffer buff = new StringBuffer();
		HTMLDocument doc = (HTMLDocument) getDocument();
		HTMLEditorKit kit = (HTMLEditorKit) getEditorKit();

		if (un == null || llamaChat.username == null) {
			return;
		}
		
		if (!"server".equals(un)) {
			message = message.replaceAll("<", "&lt;");
			message = message.replaceAll(">", "&gt;");
		}
		message = message.replaceAll("\n", "<br>");

		if (llamaChat.username.equals(un)) {
			buff.append("<font color=#009900><b>");
		} else if (un.equals("server")) {
			buff.append("<font color=#990000><b>");
		} else {
			buff.append("<font color=#000099><b>");
		}
		buff.append(un);
		buff.append("</b></font>");

		buff.append("&nbsp;&nbsp;:&nbsp;");
		if (whisper) {
			buff.append("<i>");
		}
		char[] tmp = message.toCharArray();
		int start, end;
		for (start = 0, end = 0; end < (tmp.length-1); ++end) {
			if (tmp[end] == ';') {
				if (tmp[end+1] == ')') {
					if (end-start > 0) {
						buff.append(tmp,start,end-start);
					}
					buff.append(icoWinking);
					start = end += 2;
					end--;
				}
			} else if (tmp[end] == ':') {
				switch(tmp[end+1]) {
				case ')':
					if (end-start > 0) {
						buff.append(tmp,start,end-start);
					}
					buff.append(icoSmily);
					start = end += 2;
					end--;
					break;
				case '(':
					if (end-start > 0) {
						buff.append(tmp,start,end-start);
					}
					buff.append(icoSad);
					start = end += 2;
					end--;
					break;
				case 'P':
					if (end-start > 0) {
						buff.append(tmp,start,end-start);
					}
					buff.append(icoTongue);
					start = end += 2;
					end--;
					break;
				case 'o':
					if (end-start > 0) {
						buff.append(tmp,start,end-start);
					}
					buff.append(icoOh);
					start = end += 2;
					end--;
					break;
				}
			} else if (end + 10 < tmp.length  && tmp[end] == 'h' &&
												tmp[end+1] == 't' &&
												tmp[end+2] == 't' && 
												tmp[end+3] == 'p' &&
												tmp[end+4] == ':' &&
												tmp[end+5] == '/' &&
												tmp[end+6] == '/') {
				if (end-start > 0) {
					buff.append(tmp,start,end-start);
				}
				int index;
				for (index = 7; index < tmp.length && 
					Pattern.matches("[\\w.:/\\?\\=&%_\\-~]", CharBuffer.wrap(tmp, end+index, 1));
					index++) { }

				buff.append("<a href=\"");
				buff.append(tmp, end, index);
				buff.append("\">");
				buff.append(tmp, end, index);
				buff.append("</a>");
				start = end += index;
				end--;
			}
		}
		if (start < tmp.length) {
			buff.append(tmp, start, tmp.length - start);
		}
		if (whisper) {
			buff.append("</i>");
		}
		if (buff.length() > 0) {
			try {
				buff.append("<br>");
				kit.insertHTML(doc,doc.getLength(), buff.toString(), 0,0,HTML.Tag.FONT);
				setCaretPosition(doc.getLength());
			} catch (Throwable t) { t.printStackTrace(); }
		}
		if (message.startsWith("afk") || message.startsWith("brb")) {
			llamaChat.afks.add(un);
			llamaChat.updateList();
		} else {
			if (llamaChat.afks.contains(un)) {
				llamaChat.afks.remove(un);
				llamaChat.updateList();
			}
		}
	}

	/**
	 * signifies an error and reports it to the user
	 * @param s	the error message
	 */
	public void error(String s) {
		HTMLDocument doc = (HTMLDocument) getDocument();
		HTMLEditorKit kit = (HTMLEditorKit) getEditorKit();
		try {
			kit.insertHTML(doc, doc.getLength(), "<font color=#CC0000><b>ERROR  : " + s + "<b><br></font>", 0,0,HTML.Tag.FONT);
			setCaretPosition(doc.getLength());
		} catch (Throwable t) { }
	}
}
