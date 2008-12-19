package persistence.server;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import javax.security.auth.callback.PasswordCallback;

public class LocalPasswordCallback extends PasswordCallback {
	RemotePasswordCallback callback;

	LocalPasswordCallback(String prompt, boolean echoOn) throws RemoteException {
		super(prompt,echoOn);
		callback=new RemotePasswordCallbackImpl(prompt,echoOn);
	}

	public String getPrompt() {
		try {
			return callback.getPrompt();
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}

	public boolean isEchoOn() {
		try {
			return callback.isEchoOn();
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}

	public void setPassword(char[] password) {
		try {
			callback.setPassword(password);
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}

	public char[] getPassword() {
		try {
			return callback.getPassword();
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}

	public void clearPassword() {
		try {
			callback.clearPassword();
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}
}

interface RemotePasswordCallback extends Remote {
	String getPrompt() throws RemoteException;
	boolean isEchoOn() throws RemoteException;
	void setPassword(char[] password) throws RemoteException;
	char[] getPassword() throws RemoteException;
	void clearPassword() throws RemoteException;
}

class RemotePasswordCallbackImpl extends UnicastRemoteObject implements RemotePasswordCallback {

	private String prompt;
	private boolean echoOn;
	private char[] inputPassword;

	public RemotePasswordCallbackImpl(String prompt, boolean echoOn) throws RemoteException {
		if (prompt == null || prompt.length() == 0)
			throw new IllegalArgumentException();

		this.prompt = prompt;
		this.echoOn = echoOn;
	}

	public String getPrompt() {
		return prompt;
	}

	public boolean isEchoOn() {
		return echoOn;
	}

	public void setPassword(char[] password) {
		this.inputPassword = (password == null ?
						null : (char[])password.clone());
	}

	public char[] getPassword() {
		return (inputPassword == null?
						null : (char[])inputPassword.clone());
	}

	public void clearPassword() {
		if (inputPassword != null) {
			for (int i = 0; i < inputPassword.length; i++)
				inputPassword[i] = ' ';
		}
	}
}
