package persistence;

import java.awt.Frame;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PushbackInputStream;
import java.io.Serializable;
import java.rmi.Naming;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Arrays;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.TextOutputCallback;
import javax.security.auth.callback.UnsupportedCallbackException;

public class Connections {
	private Connections() {}

	public static Connection getConnection(String name) throws Exception {
		return getConnection(name,Connection.TRANSACTION_READ_UNCOMMITTED);
	}

	public static Connection getConnection(String name, int level) throws Exception {
		LocalCallbackHandler local=new LocalCallbackHandler(handler());
		Connection conn;
		try {
			conn=((Store)Naming.lookup(name)).getConnection(local,level);
		} finally {
			UnicastRemoteObject.unexportObject(local.handler,true);
		}
		return conn;
	}

	public static AdminConnection getAdminConnection(String name) throws Exception {
		LocalCallbackHandler local=new LocalCallbackHandler(handler());
		AdminConnection conn;
		try {
			conn=((Store)Naming.lookup(name)).getAdminConnection(local);
		} finally {
			UnicastRemoteObject.unexportObject(local.handler,true);
		}
		return conn;
	}

	static CallbackHandler handler() {
		Frame frames[]=Frame.getFrames();
		return frames.length>0?(CallbackHandler)new DialogCallbackHandler(frames[0]):new MyCallbackHandler();
	}
}

class LocalCallbackHandler implements CallbackHandler, Serializable {
	RemoteCallbackHandler handler;

	LocalCallbackHandler(CallbackHandler handler) throws RemoteException {
		this.handler=new RemoteCallbackHandlerImpl(handler);
	}

	public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
		handler.handle((Callback[])callbacks);
	}
}

interface RemoteCallbackHandler extends Remote {
	void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException, RemoteException;
}

class RemoteCallbackHandlerImpl extends UnicastRemoteObject implements RemoteCallbackHandler {
	CallbackHandler handler;

	RemoteCallbackHandlerImpl(CallbackHandler handler) throws RemoteException {
		this.handler=handler;
	}

	public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
		handler.handle(callbacks);
	}
}

class MyCallbackHandler implements CallbackHandler {

	public void handle(Callback[] callbacks)
	throws IOException, UnsupportedCallbackException {

		for (int i = 0; i < callbacks.length; i++) {
			if (callbacks[i] instanceof TextOutputCallback) {

				// display the message according to the specified type
				TextOutputCallback toc = (TextOutputCallback)callbacks[i];
				switch (toc.getMessageType()) {
				case TextOutputCallback.INFORMATION:
					System.out.println(toc.getMessage());
					break;
				case TextOutputCallback.ERROR:
					System.out.println("ERROR: " + toc.getMessage());
					break;
				case TextOutputCallback.WARNING:
					System.out.println("WARNING: " + toc.getMessage());
					break;
				default:
					throw new IOException("Unsupported message type: " +
										toc.getMessageType());
				}

			} else if (callbacks[i] instanceof NameCallback) {

				// prompt the user for a username
				NameCallback nc = (NameCallback)callbacks[i];

				System.err.print(nc.getPrompt());
				System.err.flush();
				nc.setName((new BufferedReader
						(new InputStreamReader(System.in))).readLine());

			} else if (callbacks[i] instanceof PasswordCallback) {

				// prompt the user for sensitive information
				PasswordCallback pc = (PasswordCallback)callbacks[i];
				System.err.print(pc.getPrompt());
				System.err.flush();
				pc.setPassword(readPassword(System.in));

			} else {
				throw new UnsupportedCallbackException
						(callbacks[i], "Unrecognized Callback");
			}
		}
	}

	// Reads user password from given input stream.
	private char[] readPassword(InputStream in) throws IOException {

		char[] lineBuffer;
		char[] buf;
		int i;

		buf = lineBuffer = new char[128];

		int room = buf.length;
		int offset = 0;
		int c;

		loop:   while (true) {
			switch (c = in.read()) {
			case -1:
			case '\n':
				break loop;

			case '\r':
				int c2 = in.read();
				if ((c2 != '\n') && (c2 != -1)) {
					if (!(in instanceof PushbackInputStream)) {
						in = new PushbackInputStream(in);
					}
					((PushbackInputStream)in).unread(c2);
				} else
					break loop;

			default:
				if (--room < 0) {
					buf = new char[offset + 128];
					room = buf.length - offset - 1;
					System.arraycopy(lineBuffer, 0, buf, 0, offset);
					Arrays.fill(lineBuffer, ' ');
					lineBuffer = buf;
				}
				buf[offset++] = (char) c;
				break;
			}
		}

		if (offset == 0) {
			return null;
		}

		char[] ret = new char[offset];
		System.arraycopy(buf, 0, ret, 0, offset);
		Arrays.fill(buf, ' ');

		return ret;
	}
}
