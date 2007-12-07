package persistence.server;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import javax.security.auth.callback.NameCallback;

public class LocalNameCallback extends NameCallback {
	RemoteNameCallback callback;

	public LocalNameCallback(String prompt) throws RemoteException {
		super(prompt);
		callback=new RemoteNameCallbackImpl(prompt);
	}

	public LocalNameCallback(String prompt, String defaultName) throws RemoteException {
		super(prompt,defaultName);
		callback=new RemoteNameCallbackImpl(prompt,defaultName);
	}

	public String getPrompt() {
		try {
			return callback.getPrompt();
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}

	public String getDefaultName() {
		try {
			return callback.getDefaultName();
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}

	public void setName(String name) {
		try {
			callback.setName(name);
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}

	public String getName() {
		try {
			return callback.getName();
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}
}

interface RemoteNameCallback extends Remote {
	String getPrompt() throws RemoteException;
	String getDefaultName() throws RemoteException;
	void setName(String name) throws RemoteException;
	String getName() throws RemoteException;
}

class RemoteNameCallbackImpl extends UnicastRemoteObject implements RemoteNameCallback {

	private String prompt;
	private String defaultName;
	private String inputName;

	public RemoteNameCallbackImpl(String prompt) throws RemoteException {
		if (prompt == null || prompt.length() == 0)
			throw new IllegalArgumentException();
		this.prompt = prompt;
	}

	public RemoteNameCallbackImpl(String prompt, String defaultName) throws RemoteException {
		if (prompt == null || prompt.length() == 0 ||
			defaultName == null || defaultName.length() == 0)
			throw new IllegalArgumentException();

		this.prompt = prompt;
		this.defaultName = defaultName;
	}

	public String getPrompt() {
		return prompt;
	}

	public String getDefaultName() {
		return defaultName;
	}

	public void setName(String name) {
		this.inputName = name;
	}

	public String getName() {
		return inputName;
	}
}
