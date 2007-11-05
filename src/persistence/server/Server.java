package persistence.server;

import java.rmi.*;
import persistence.*;

public class Server {
	public static void main(String args[]) throws Exception {
		if (System.getSecurityManager() == null) System.setSecurityManager(new RMISecurityManager());
		ClassServer cs=new ClassFileServer(2001, "classes");
		try {
			final StoreImpl store=new StoreImpl("heapspace");
			Naming.rebind("store", store);
			System.out.println("store bound in registry");
			AdminImpl admin=new AdminImpl(store);
			Naming.rebind("//localhost:2000/admin", admin);
			System.out.println("admin bound in registry");
			Runtime.getRuntime().addShutdownHook(new Thread() {
				public void run() {
					try {
						store.close();
					} catch (RemoteException e) {
						throw new RuntimeException("remote error");
					}
				}
			});
		} catch (Exception e) {
			cs.close();
			throw e;
		}
	}
}
