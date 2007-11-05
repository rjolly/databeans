package persistence.server;

import java.io.*;

public class MakeHeapSpace {
	public static void main(String args[]) throws Exception {
		RandomAccessFile file=new RandomAccessFile("heapspace","rw");
		file.setLength(args.length>0?new Long(args[0]).longValue():262144);
		file.close();
	}
}
