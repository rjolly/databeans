package persistence.server;

import java.rmi.Naming;
import java.rmi.RemoteException;
import persistence.Store;

public class Server {
	public static void main(String args[]) throws Exception {
		final Store store=new Store("heapspace");
		Naming.rebind(args.length>0?args[0]:"store", store);
		System.out.println("store bound in registry");
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				try {
					store.close();
				} catch (RemoteException e) {
					throw new RuntimeException(e);
				}
			}
		});
	}
}
