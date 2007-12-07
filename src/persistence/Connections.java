package persistence;

import com.sun.security.auth.callback.DialogCallbackHandler;
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
		return ((Store)Naming.lookup(name)).getConnection(new LocalCallbackHandler(new DialogCallbackHandler()));
	}

	public static Admin getAdmin(String name) throws NotBoundException, MalformedURLException, RemoteException {
		return ((Store)Naming.lookup(name)).getAdmin(new LocalCallbackHandler(new DialogCallbackHandler()));
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
