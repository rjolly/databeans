package persistence;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Random;

public class Connections {
	private Connections() {}

	public static Connection getConnection(String name) throws NotBoundException, MalformedURLException, RemoteException {
		return getConnection(name,"anonymous","");
	}

	public static Connection getConnection(String name, String username, String password) throws NotBoundException, MalformedURLException, RemoteException {
		Store store=(Store)Naming.lookup(name);
		return store.getConnection(username,crypt(password,store.salt(username)));
	}

	static byte[] crypt(String password) {
		Random r=new SecureRandom();
		byte b[]=new byte[2];
		r.nextBytes(b);
		return crypt(password,(char)((b[0] << 8) | b[1]));
	}

	static byte[] crypt(String password, char salt) {
		byte b[]=password.getBytes();
		byte a[]=new byte[2+b.length];
		a[0]=(byte)(salt >> 8);
		a[1]=(byte)(salt & 0xff);
		System.arraycopy(b,0,a,2,b.length);
		try {
			b=MessageDigest.getInstance("MD5").digest(a);
			byte c[]=new byte[2+b.length];
			System.arraycopy(a,0,c,0,2);
			System.arraycopy(b,0,c,2,b.length);
			return c;
		} catch (NoSuchAlgorithmException e) {
			throw new PersistentException("internal error");
		}
	}
}
