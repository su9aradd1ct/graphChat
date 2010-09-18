/*- CommandHistory.java -------------------------------------------+
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

/* -------------------- JavaDoc Information ----------------------*/
/**
 * Class to manage the history of messages/commands from the user
 * Is in the form of a fixed length stack that drops items from
 * the end of the stack when the maximum length is reached exends
 * the linked list class; all items on the list are Strings
 * @author Joseph Monti <a href="mailto:countjoe@users.sourceforge.net">countjoe@users.sourceforge.net</a>
 * @version 0.8
 */
public final class CommandHistory extends java.util.LinkedList {
	/**
	 * the maximum size of the list
	 */
	private static int MAX_SIZE;
	
	/** 
	 * the current retrieved position
	 */
	private static int current;
	
	CommandHistory(int s) {
		super();
		MAX_SIZE = s;
		current = -1;
	}
	
	/**
	 * Adds a string to the list and checks to see
	 * if the new item breached the maximum size of the list
	 * also sets the current to be -1 to reset the current position
	 * @param s	the string to be added
	 */
	public void add(String s) {
		addFirst(s);
		if (size() > MAX_SIZE) {
			removeLast();
		}
		current = -1;
	}
	
	/**
	 * retrives the next oldest item from the list
	 * while stopping at the top and incrementing
	 * the current item
	 * @return	the appropriate item from the list, or null if at top
	 */
	public String getUp() {
		if (size() == 0) return null;
		if (current+1 >= size())
			return null;
		current++;
		return (String) get(current);
	}
	
	/**
	 * retrieves the next newest item from the list
	 * while stopping that the bottom and decrementing
	 * the current item
	 * @return	the appropriate item from the list, or null if at bottom
	 */
	public String getDown() {
		if (size() == 0) return null;
		if (current > 0) {
			current--;
			return (String) get(current);
		} else if (current == 0) {
			current--;
			return "";
		}
			return null;
	}
}
