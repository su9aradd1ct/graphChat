/*- MessageQueue.java ---------------------------------------------+
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

import java.util.LinkedList;

/* -------------------- JavaDoc Information ----------------------*/
/**
 * It defines 2 methods, enqueu, to put SocketData items into the internal 
 * Vector, and run, which is requred be Runnable (this is a threaded class)
 * and grabs any items off the Vector. It uses a combination of wait() and 
 * notify() to keep the run method from polling on an empty queue.
 * @author Joseph Monti 
 * 		<a href="mailto:countjoe@users.sourceforge.net">countjoe@users.sourceforge.net</a>
 * @author <a href="http://codeconnector.sf.net/">Code Connector</a>
 * @version 0.8
 */

public class MessageQueue implements Runnable {
	private static final int MAX_SIZE = 15;
	private LinkedList queue;
	private SocketConnection writer;
	private boolean running;
	private boolean empty;
	
	public MessageQueue(SocketConnection w) {
		writer = w;
		queue = new LinkedList();
		running  = true;
		empty = true;
		new Thread(this).start();
	}
	
	synchronized public boolean enqueue(Object obj) {
		queue.addLast(obj);
		if (empty) {
			empty = false;
			notify();
		}
		if (queue.size() > MAX_SIZE) {
			writer.close();
		}
		return true;
	}
	
	public void run() {
		while (running) {
			try {
				if (queue.size() != 0) {
					writer._writeObject(queue.removeFirst());
				} else {
					empty = true;
					synchronized(this) {
						while (empty) {
							wait();
						}
					}
				}
				Thread.sleep(10);
			} catch ( InterruptedException inte ) {
				running = false;
			}
		}
	}
}
