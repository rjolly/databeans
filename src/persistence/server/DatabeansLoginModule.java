package persistence.server;

import java.rmi.Naming;
import java.rmi.RemoteException;
import java.util.Map;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.FailedLoginException;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;
import persistence.Store;

public class DatabeansLoginModule implements LoginModule {

	// initial state
	private Subject subject;
	private CallbackHandler callbackHandler;
	private Map sharedState;
	private Map options;

	// configurable option
	private boolean debug = false;
	private Store store;

	// the authentication status
	private boolean succeeded = false;
	private boolean commitSucceeded = false;

	// username and password
	private String username;
	private char[] password;

	// testUser's SamplePrincipal
	private DatabeansPrincipal userPrincipal;

	public void initialize(Subject subject, CallbackHandler callbackHandler,
						Map sharedState, Map options) {

		this.subject = subject;
		this.callbackHandler = callbackHandler;
		this.sharedState = sharedState;
		this.options = options;

		// initialize any configured options
		debug = "true".equalsIgnoreCase((String)options.get("debug"));
		try {
			store=(Store)Naming.lookup(options.containsKey("store")?(String)options.get("store"):"//localhost/store");
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
//		return store.getConnection(username,crypt(password,store.salt(username)));
	}

	public boolean login() throws LoginException {

		// prompt for a user name and password
		if (callbackHandler == null)
			throw new LoginException("Error: no CallbackHandler available " +
						"to garner authentication information from the user");

		Callback[] callbacks = new Callback[2];
		callbacks[0] = new NameCallback("user name: ");
		callbacks[1] = new PasswordCallback("password: ", false);

		try {
			callbackHandler.handle(callbacks);
			username = ((NameCallback)callbacks[0]).getName();
			char[] tmpPassword = ((PasswordCallback)callbacks[1]).getPassword();
			if (tmpPassword == null) {
				// treat a NULL password as an empty password
				tmpPassword = new char[0];
			}
			password = new char[tmpPassword.length];
			System.arraycopy(tmpPassword, 0,
						password, 0, tmpPassword.length);
			((PasswordCallback)callbacks[1]).clearPassword();

		} catch (java.io.IOException ioe) {
			throw new LoginException(ioe.toString());
		} catch (UnsupportedCallbackException uce) {
			throw new LoginException("Error: " + uce.getCallback().toString() +
				" not available to garner authentication information " +
				"from the user");
		}

		// print debugging information
		if (debug) {
			System.out.println("\t\t[DatabeansLoginModule] " +
								"user entered user name: " +
								username);
			System.out.print("\t\t[DatabeansLoginModule] " +
								"user entered password: ");
			for (int i = 0; i < password.length; i++)
				System.out.print(password[i]);
			System.out.println();
		}

		// verify the username/password
		boolean usernameCorrect = false;
		boolean passwordCorrect = false;
		boolean success = false;
		try {
			success = store.authenticate(username,password);
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		}
		if(success) {
			usernameCorrect = true;

			// authentication succeeded!!!
			passwordCorrect = true;
			if (debug)
				System.out.println("\t\t[DatabeansLoginModule] " +
								"authentication succeeded");
			succeeded = true;
			return true;
		} else {

			// authentication failed -- clean out state
			if (debug)
				System.out.println("\t\t[DatabeansLoginModule] " +
								"authentication failed");
			succeeded = false;
			username = null;
			for (int i = 0; i < password.length; i++)
				password[i] = ' ';
			password = null;
			if (!usernameCorrect) {
				throw new FailedLoginException("User Name Incorrect");
			} else {
				throw new FailedLoginException("Password Incorrect");
			}
		}
	}

	public boolean commit() throws LoginException {
		if (succeeded == false) {
			return false;
		} else {
			// add a Principal (authenticated identity)
			// to the Subject

			// assume the user we authenticated is the SamplePrincipal
			userPrincipal = new DatabeansPrincipal(username);
			if (!subject.getPrincipals().contains(userPrincipal))
				subject.getPrincipals().add(userPrincipal);

			if (debug) {
				System.out.println("\t\t[DatabeansLoginModule] " +
								"added DatabeansPrincipal to Subject");
			}

			// in any case, clean out state
			username = null;
			for (int i = 0; i < password.length; i++)
				password[i] = ' ';
			password = null;

			commitSucceeded = true;
			return true;
		}
	}

	public boolean abort() throws LoginException {
		if (succeeded == false) {
			return false;
		} else if (succeeded == true && commitSucceeded == false) {
			// login succeeded but overall authentication failed
			succeeded = false;
			username = null;
			if (password != null) {
				for (int i = 0; i < password.length; i++)
					password[i] = ' ';
				password = null;
			}
			userPrincipal = null;
		} else {
			// overall authentication succeeded and commit succeeded,
			// but someone else's commit failed
			logout();
		}
		return true;
	}

	public boolean logout() throws LoginException {

		subject.getPrincipals().remove(userPrincipal);
		succeeded = false;
		succeeded = commitSucceeded;
		username = null;
		if (password != null) {
			for (int i = 0; i < password.length; i++)
				password[i] = ' ';
			password = null;
		}
		userPrincipal = null;
		return true;
	}
}
