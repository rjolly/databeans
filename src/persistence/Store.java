package persistence;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.WeakHashMap;
import persistence.beans.XMLDecoder;
import persistence.beans.XMLEncoder;
import persistence.storage.Collector;
import persistence.storage.FileHeap;
import persistence.storage.Heap;
import persistence.storage.MemoryModel;

public class Store implements Collector, Closeable {
	final Heap heap;
	final Map<Long, Reference<PersistentObject>> cache=new WeakHashMap<>();
	PersistentSystem system;
	boolean readOnly;
	boolean closed;
	Map<String, PersistentClass> classes;
	long boot;

	public Store(String name) {
		try {
			heap=new FileHeap(name,this);
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
		if((boot=heap.boot())==0) {
			heap.mount(true);
			create();
		} else {
			if(heap.mounted()) {
				System.out.println("store was not cleanly unmounted, running in recovery mode");
				readOnly=true;
			} else heap.mount(true);
			instantiate();
		}
	}

	synchronized void create() {
		classes=new LinkedHashMap<>();
		createSystem();
		system.getClasses().putAll(new LinkedHashMap<>(classes));
		system.getClasses().putAll(classes);
		classes=system.getClasses();
	}

	void createSystem() {
		system = new PersistentSystem(this);
		incRefCount(boot=system.base);
		heap.setBoot(boot);
	}

	synchronized void instantiate() {
		release();
		system=(PersistentSystem)instantiate(boot);
		classes=system.getClasses();
		if(readOnly) return;
	}

	void release() {
		Iterator<Long> t=heap.iterator();
		while(t.hasNext()) {
			long ptr=((Long)t.next()).longValue();
			if(heap.status(ptr)) clearRefCount(ptr,true);
		}
	}

	public PersistentClass get(final Class<? extends PersistentObject> clazz) {
		final PersistentObject obj = PersistentClass.newInstance(clazz);
		obj.store = this;
		return persistentClass(obj);
	}

	PersistentClass persistentClass(final PersistentObject obj) {
		final String name = obj.getClass().getName();
		synchronized(classes) {
			PersistentClass clazz = classes.get(name);
			if (clazz == null) classes.put(name, clazz = obj.createClass());
			return clazz;
		}
	}

	public <T> T root() {
		return system.getRoot();
	}

	public <T> void setRoot(T obj) {
		system.setRoot(obj);
	}

	synchronized void create(final PersistentObject obj) {
		if(readOnly) throw new RuntimeException("read only");
		final PersistentClass clazz = obj.clazz;
		final byte b[] = new byte[clazz.size()];
		final long base = heap.alloc(b.length);
		heap.writeBytes(base, b);
		setClass(base, clazz);
		obj.base = base;
		synchronized(cache) {
			incRefCount(base, true);
			cache(obj);
		}
	}

	PersistentObject instantiate(final long base) {
		synchronized(cache) {
			PersistentObject o=get(base);
			if(o==null) {
				incRefCount(base,true);
				cache(o=selfClass(base)?instantiateClass(base):instantiate(base, getClass(base)));
			}
			return o;
		}
	}

	PersistentClass instantiateClass(final long base) {
		PersistentClass c=(PersistentClass)instantiate(base, new ClassClass());
		c.setup();
		c.setClass(c);
		return c;
	}

	PersistentObject instantiate(final long base, final PersistentClass clazz) {
		PersistentObject obj = clazz.newInstance();
		obj.base = base;
		obj.clazz = clazz;
		obj.store = this;
		return obj;
	}

	void cache(PersistentObject obj) {
		cache.remove(obj.base);
		cache.put(obj.base,new WeakReference<>(obj));
	}

	PersistentObject get(long base) {
		Reference<PersistentObject> w=cache.get(base);
		return w==null?null:w.get();
	}

	synchronized void release(PersistentObject obj) {
		if(closed) return;
		if(readOnly) return;
		decRefCount(obj.base,true);
	}

	public void inport(String name) throws IOException {
		try (final InputStream is = new FileInputStream(name)) {
			XMLDecoder d = new XMLDecoder(this,new BufferedInputStream(is));
			system.setRoot(d.readObject());
			d.close();
		}
	}

	public void export(String name) throws IOException {
		try (final OutputStream os = new FileOutputStream(name)) {
			XMLEncoder e = new XMLEncoder(this,new BufferedOutputStream(os));
			e.writeObject(system.getRoot());
			e.close();
		}
	}

	void updateClasses() {
		for(Iterator<PersistentClass> it=classes.values().iterator();it.hasNext();) {
			if(refCount(it.next())==1) it.remove();
		}
	}

	synchronized long refCount(PersistentClass clazz) {
		return refCount(clazz.base);
	}

	public long allocatedSpace() {
		return heap.allocatedSpace();
	}

	public long maxSpace() {
		return heap.maxSpace();
	}

	public synchronized void close() {
		if(closed) return;
		try {
			if(!readOnly) heap.mount(false);
		} finally {
			heap.close();
		}
		closed=true;
	}

	public synchronized void gc() {
		if(closed) return;
		if(readOnly) return;
		System.gc();
		mark();
		mark(boot);
		sweep();
		synchronized(classes) {
			updateClasses();
		}
	}

	void mark() {
		Iterator<Long> t=heap.iterator();
		while(t.hasNext()) {
			long ptr=((Long)t.next()).longValue();
			if(heap.status(ptr)) {
				markClass(ptr);
				if(refCount(ptr,true)>0) mark(ptr);
			}
		}
	}

	void markClass(long base) {
		long ptr=((Long)Field.CLASS.get(heap,base)).longValue();
		if (ptr!=0) mark(ptr);
	}

	void sweep() {
		Iterator<Long> t=heap.iterator();
		while(t.hasNext()) {
			long ptr=((Long)t.next()).longValue();
			if(heap.status(ptr)) {
				if(!heap.mark(ptr,false)) free(ptr);
			}
		}
	}

	void mark(long base) {
		if(heap.mark(base,true)) return;
		PersistentClass c=getClass(base);
		if(c!=null) {
			mark(c.base);
			Iterator<Field> t=c.fieldIterator();
			while(t.hasNext()) {
				Field field=(Field)t.next();
				switch (field.typeCode) {
				case 'Z':
				case 'B':
				case 'C':
				case 'S':
				case 'I':
				case 'F':
				case 'J':
				case 'D':
					break;
				case '[':
				case 'L':
					long ptr=((Long)field.get(heap,base)).longValue();
					if(ptr!=0) mark(ptr);
					break;
				default:
					throw new RuntimeException("internal error");
				}
			}
		}
	}

	void free(long base) {
		if(!heap.status(base)) return;
		Heap patch=patch(base);
		heap.free(base);
		PersistentClass c=getClass(patch,base);
		if(c!=null) {
//			Field.CLASS.set(patch,base,new Long(0));
			decRefCount(c.base);
			Iterator<Field> t=c.fieldIterator();
			while(t.hasNext()) {
				Field field=(Field)t.next();
				switch (field.typeCode) {
				case 'Z':
				case 'B':
				case 'C':
				case 'S':
				case 'I':
				case 'F':
				case 'J':
				case 'D':
					break;
				case '[':
				case 'L':
					long ptr=((Long)field.get(patch,base)).longValue();
//					field.set(patch,base,new Long(0));
					if(ptr!=0) decRefCount(ptr);
					break;
				default:
					throw new RuntimeException("internal error");
				}
			}
		}
	}

	long refCount(long base) {
		return refCount(base,false);
	}

	long refCount(long base, boolean memory) {
		int n=MemoryModel.model.lastByteShift;
		long r=((Long)Field.REF_COUNT.get(heap,base)).longValue();
		long s=r&MemoryModel.model.lastByteMask;
		return memory?s>>>n:r^s;
	}

	void incRefCount(long base) {
		incRefCount(base,false);
	}

	void incRefCount(long base, boolean memory) {
		int n=MemoryModel.model.lastByteShift;
		long r=((Long)Field.REF_COUNT.get(heap,base)).longValue();
		long s=r&MemoryModel.model.lastByteMask;
		r=r^s;
		if(memory) s=((s>>>n)+1)<<n;
		else r++;
		r=r|s;
		Field.REF_COUNT.set(heap,base,new Long(r));
	}

	void decRefCount(long base) {
		if(!heap.status(base)) return;
		decRefCount(base,false);
	}

	void decRefCount(long base, boolean memory) {
		int n=MemoryModel.model.lastByteShift;
		long r=((Long)Field.REF_COUNT.get(heap,base)).longValue();
		long s=r&MemoryModel.model.lastByteMask;
		r=r^s;
		if(memory) s=((s>>>n)-1)<<n;
		else r--;
		r=r|s;
		Field.REF_COUNT.set(heap,base,new Long(r));
		if(r==0) free(base);
	}

	void clearRefCount(long base, boolean memory) {
		long r=((Long)Field.REF_COUNT.get(heap,base)).longValue();
		long s=r&MemoryModel.model.lastByteMask;
		r=r^s;
		if(memory) s=0;
		r=r|s;
		Field.REF_COUNT.set(heap,base,new Long(r));
	}

	synchronized Object get(long base, Field field) {
		return field.reference?getReference(base,field):field.get(heap,base);
	}

	synchronized void set(long base, Field field, Object value) {
		if(field.reference) setReference(base,field,value);
		else field.set(heap,base,value);
	}

	Object getReference(long base, Field field) {
		long ptr=((Long)field.get(heap,base)).longValue();
		return ptr==0?null:flat(ptr)?readObject(ptr):instantiate(ptr);
	}

	void setReference(long base, Field field, Object value) {
		long src=((Long)field.get(heap,base)).longValue();
		long dst=value==null?0:value instanceof PersistentObject?((PersistentObject)value).base:writeObject(value);
		if(dst!=0) incRefCount(dst);
		field.set(heap,base,new Long(dst));
		if(src!=0) decRefCount(src);
	}

	PersistentClass getClass(long base) {
		return getClass(heap,base);
	}

	PersistentClass getClass(Heap heap, long base) {
		long ptr=((Long)Field.CLASS.get(heap,base)).longValue();
		return ptr==0?null:(PersistentClass)instantiate(ptr);
	}

	void setClass(long base, PersistentClass clazz) {
		long ptr=clazz.base==null?base:clazz.base;
		incRefCount(ptr);
		Field.CLASS.set(heap,base,new Long(ptr));
	}

	boolean selfClass(long base) {
		long ptr=((Long)Field.CLASS.get(heap,base)).longValue();
		return ptr==base;
	}

	boolean flat(long base) {
		long ptr=((Long)Field.CLASS.get(heap,base)).longValue();
		return ptr==0;
	}

	Object readObject(long base) {
		return readObject(heap,base);
	}

	Object readObject(Heap heap, long base) {
		Object obj;
		byte b[]=heap.readBytes(base);
		InputStream is=new ByteArrayInputStream(b,Field.HEADER_SIZE,b.length-Field.HEADER_SIZE);
		try {
			obj=new ObjectInputStream(is).readObject();
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return obj;
	}

	long writeObject(Object obj) {
		ByteArrayOutputStream os=new ByteArrayOutputStream();
		try {
			new ObjectOutputStream(os).writeObject(obj);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		byte b[]=os.toByteArray();
		byte s[]=new byte[Field.HEADER_SIZE+b.length];
		System.arraycopy(b,0,s,s.length-b.length,b.length);
		long base=heap.alloc(s.length);
		heap.writeBytes(base,s);
		return base;
	}

	Heap patch(long ptr) {
		return new Patch(ptr);
	}

	class Patch implements Heap {
		DataInputStream is;
		byte cache[];
		long ptr;

		Patch(long ptr) {
			this.ptr=ptr;
			cache=heap.readBytes(ptr);
			is=new DataInputStream(new ByteArrayInputStream(cache));
		}

		public long boot() {
			return 0;
		}

		public void setBoot(long ptr) {
		}

		public boolean mounted() {
			return false;
		}

		public void mount(boolean n) {
		}

		public void close() {
		}

		public long alloc(int size) {
			return 0;
		}

		public long realloc(long ptr, int size) {
			return 0;
		}

		public void free(long ptr) {
		}

		public boolean mark(long ptr, boolean n) {
			return false;
		}

		public boolean status(long ptr) {
			return false;
		}

		public long allocatedSpace() {
			return 0;
		}

		public long maxSpace() {
			return 0;
		}

		public Iterator<Long> iterator() {
			return null;
		}

		public boolean readBoolean(long ptr) {
			try {
				is.reset();
				is.skip(ptr-this.ptr);
				return is.readBoolean();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		public byte readByte(long ptr) {
			try {
				is.reset();
				is.skip(ptr-this.ptr);
				return is.readByte();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		public short readShort(long ptr) {
			try {
				is.reset();
				is.skip(ptr-this.ptr);
				return is.readShort();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		public char readChar(long ptr) {
			try {
				is.reset();
				is.skip(ptr-this.ptr);
				return is.readChar();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		public int readInt(long ptr) {
			try {
				is.reset();
				is.skip(ptr-this.ptr);
				return is.readInt();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		public long readLong(long ptr) {
			try {
				is.reset();
				is.skip(ptr-this.ptr);
				return is.readLong();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		public float readFloat(long ptr) {
			try {
				is.reset();
				is.skip(ptr-this.ptr);
				return is.readFloat();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		public double readDouble(long ptr) {
			try {
				is.reset();
				is.skip(ptr-this.ptr);
				return is.readDouble();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		public byte[] readBytes(long ptr) {
			return cache;
		}

		public void writeBoolean(long ptr, boolean v) {}
		public void writeByte(long ptr, int v) {}
		public void writeShort(long ptr, int v) {}
		public void writeChar(long ptr, int v) {}
		public void writeInt(long ptr, int v) {}
		public void writeLong(long ptr, long v) {}
		public void writeFloat(long ptr, float v) {}
		public void writeDouble(long ptr, double v) {}
		public void writeBytes(long ptr, byte b[]) {}
	}
}
