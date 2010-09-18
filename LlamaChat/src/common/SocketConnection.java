/*- SocketConnection.java -----------------------------------------+
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

package common;

/* -------------------- JavaDoc Information ----------------------*/
/**
 * The inteface to connection classes.
 * @author Joseph Monti <a href="mailto:countjoe@users.sourceforge.net">countjoe@users.sourceforge.net</a>
 * @version 0.8
 * @see common.sd.SocketData
 */
public interface SocketConnection {
	public void userAdd(String username);
	public void adminAdd(String text);
	public void userDel(String username);
	public void rename(String on, String nn);
	public void kick(String username);
	public void channel(boolean new_channel, String name, String pass);
	public void chat(String username, String message);
	public void private_msg(String username, String message);
	public void whisper(String username, String message);
	public void chatLog(boolean start);
	public void error(String s);
	public void _writeObject(Object obj);
	public void serverCap(char type, Object obj);
	public void close();
}
