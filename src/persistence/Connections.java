package persistence;

import java.io.IOException;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;

public class Connections {
	private Connections() {}

	public static Connection getConnection(String name) throws NotBoundException, MalformedURLException, RemoteException {
		Store store=(Store)Naming.lookup(name);
		return store.getConnection(new LocalCallbackHandler(new com.sun.security.auth.callback.DialogCallbackHandler()));
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
