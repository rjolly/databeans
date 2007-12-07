package persistence.server;

import java.rmi.Naming;
import persistence.StoreImpl;

public class Server {
	public static void main(String args[]) throws Exception {
		ClassServer cs=new ClassFileServer(2001, "classes");
		try {
			final StoreImpl store=new StoreImpl("heapspace");
			Naming.rebind("store", store);
			System.out.println("store bound in registry");
			Runtime.getRuntime().addShutdownHook(new Thread() {
				public void run() {
					store.close();
				}
			});
		} catch (Exception e) {
			cs.close();
			throw e;
		}
	}
}
