package persistence.server;

import java.rmi.Naming;
import persistence.StoreImpl;

public class Server {
	public static void main(String args[]) throws Exception {
		final StoreImpl store=new StoreImpl("heapspace");
		Naming.rebind(args.length>0?args[0]:"store", store);
		System.out.println("store bound in registry");
	}
}
