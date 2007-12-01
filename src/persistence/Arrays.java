package persistence;

public class Arrays {
	private Arrays() {}

	public static void copy(Array src, int src_position, Array dst, int dst_position, int length) {
		if(src_position<dst_position) for(int i=length-1;i>=0;i--) dst.set(dst_position+i,src.get(src_position+i));
		else for(int i=0;i<length;i++) dst.set(dst_position+i,src.get(src_position+i));
	}

	public static void copy(Object src[], int src_position, Array dst, int dst_position, int length) {
		if(src_position<dst_position) for(int i=length-1;i>=0;i--) dst.set(dst_position+i,src[src_position+i]);
		else for(int i=0;i<length;i++) dst.set(dst_position+i,src[src_position+i]);
	}

	public static void copy(Array src, int src_position, Object dst[], int dst_position, int length) {
		if(src_position<dst_position) for(int i=length-1;i>=0;i--) dst[dst_position+i]=src.get(src_position+i);
		else for(int i=0;i<length;i++) dst[dst_position+i]=src.get(src_position+i);
	}
}
