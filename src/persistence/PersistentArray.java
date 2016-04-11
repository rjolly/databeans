package persistence;

public final class PersistentArray<C> extends PersistentObject implements Array<C> {
	public PersistentArray() {
	}

	@SuppressWarnings("unchecked")
	public PersistentArray(final Store store, C component[]) {
		this(store, (Class<C>)component.getClass().getComponentType(), component.length);
		copy(component, 0, this, 0, component.length);
	}

	public PersistentArray(final Store store, final Class<C> componentType, final int length) {
		super(store, new ArrayClass<>(store, componentType, length));
	}

	protected PersistentClass createClass() {
		throw new UnsupportedOperationException();
	}

	@SuppressWarnings("unchecked")
	private ArrayClass<C> arrayClass() {
		return (ArrayClass<C>)clazz;
	}

	public int length() {
		return arrayClass().getLength();
	}

	public char typeCode() {
		return arrayClass().getTypeCode();
	}

	public C get(int index) {
		return get(arrayClass().getField(index));
	}

	public void set(int index, C value) {
		set(arrayClass().getField(index),value);
	}

	public static <C> void copy(Array<? extends C> src, int src_position, Array<C> dst, int dst_position, int length) {
		if(src_position<dst_position) for(int i=length-1;i>=0;i--) dst.set(dst_position+i,src.get(src_position+i));
		else for(int i=0;i<length;i++) dst.set(dst_position+i,src.get(src_position+i));
	}

	public static <C> void copy(C src[], int src_position, Array<C> dst, int dst_position, int length) {
		if(src_position<dst_position) for(int i=length-1;i>=0;i--) dst.set(dst_position+i,src[src_position+i]);
		else for(int i=0;i<length;i++) dst.set(dst_position+i,src[src_position+i]);
	}

	public static <C> void copy(Array<? extends C> src, int src_position, C dst[], int dst_position, int length) {
		if(src_position<dst_position) for(int i=length-1;i>=0;i--) dst[dst_position+i]=src.get(src_position+i);
		else for(int i=0;i<length;i++) dst[dst_position+i]=src.get(src_position+i);
	}

	public String toString() {
		StringBuffer s=new StringBuffer();
		s.append("{");
		int n=length();
		for(int i=0;i<n;i++) s.append((i==0?"":", ")+get(i));
		s.append("}");
		return s.toString();
	}
}
