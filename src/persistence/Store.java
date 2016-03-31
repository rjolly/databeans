package persistence;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
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

public class Store extends UnicastRemoteObject implements Collector {
	final Heap heap;
	final Map cache=new WeakHashMap();
	PersistentSystem system;
	boolean readOnly;
	boolean closed;
	Map classes;
	long boot;

	public Store(String name) throws RemoteException {
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
		classes=new LinkedHashMap();
		createSystem();
		system.getClasses().putAll(new LinkedHashMap(classes));
		system.getClasses().putAll(classes);
		classes=system.getClasses();
	}

	void createSystem() {
		system=(PersistentSystem)create(PersistentSystem.class);
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
		Iterator t=heap.iterator();
		while(t.hasNext()) {
			long ptr=((Long)t.next()).longValue();
			if(heap.status(ptr)) clearRefCount(ptr,true);
		}
	}

	public synchronized PersistentClass get(String name) {
		try {
			return get(Class.forName(name));
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	PersistentClass get(Class clazz) {
		return get(PersistentClass.create(clazz,this));
	}

	PersistentClass get(PersistentClass clazz) {
		String name=clazz.getName();
		synchronized(classes) {
			PersistentClass c=(PersistentClass)classes.get(name);
			if(c==null) classes.put(name,c=clazz);
			return c;
		}
	}

	PersistentClass get(Class componentType, int length) {
		return ArrayClass.create(componentType,length,this);
	}

	public Object root() {
		return system.root();
	}

	public void setRoot(Object obj) {
		system.setRoot(obj);
	}

	public PersistentObject create(String name) {
		return create(get(name),new Class[] {},new Object[] {});
	}

	public PersistentObject create(Class clazz) {
		return create(get(clazz),new Class[] {},new Object[] {});
	}

	public PersistentObject create(Class clazz, Class types[], Object args[]) {
		return create(get(clazz),types,args);
	}

	public PersistentArray create(Class componentType, int length) {
		return (PersistentArray)create(get(componentType,length),new Class[] {},new Object[] {});
	}

	public PersistentArray create(Object component[]) {
		Class componentType=component.getClass().getComponentType();
		int length=component.length;
		return (PersistentArray)create(get(componentType,length),new Class[] {Object[].class},new Object[] {component});
	}

	synchronized PersistentObject create(PersistentClass clazz, Class types[], Object args[]) {
		if(readOnly) throw new RuntimeException("read only");
		try {
			PersistentObject obj=create(clazz);
			obj.getClass().getMethod("init",types).invoke(obj,args);
			return obj;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	synchronized PersistentObject create(PersistentClass clazz) {
		byte b[]=new byte[clazz.size()];
		long base=heap.alloc(b.length);
		heap.writeBytes(base,b);
		setClass(base,clazz);
		synchronized(cache) {
			PersistentObject o;
			incRefCount(base,true);
			cache(o=PersistentObject.newInstance(base,clazz,this));
			return o;
		}
	}

	PersistentObject instantiate(long base) {
		synchronized(cache) {
			PersistentObject o=get(base);
			if(o==null) {
				incRefCount(base,true);
				cache(o=selfClass(base)?PersistentClass.newInstance(base,this):PersistentObject.newInstance(base,getClass(base),this));
			}
			return o;
		}
	}

	void cache(PersistentObject obj) {
		cache.remove(obj.base);
		cache.put(obj.base,new WeakReference(obj));
	}

	PersistentObject get(long base) {
		Reference w=(Reference)cache.get(base);
		return w==null?null:(PersistentObject)w.get();
	}

	synchronized void release(PersistentObject obj) {
		if(closed) return;
		if(readOnly) return;
		decRefCount(obj.base,true);
	}

	void inport(String name) {
		try {
			XMLDecoder d = new XMLDecoder(this,new BufferedInputStream(new FileInputStream(name)));
			system.setRoot(d.readObject());
			d.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	void export(String name) {
		try {
			XMLEncoder e = new XMLEncoder(this,new BufferedOutputStream(new FileOutputStream(name)));
			e.writeObject(system.getRoot());
			e.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	void shutdown() throws RemoteException {
		close();
		System.gc();
	}

	void userGc() {
		gc();
		synchronized(classes) {
			updateClasses();
		}
	}

	void updateClasses() {
		for(Iterator it=classes.values().iterator();it.hasNext();) {
			if(refCount((PersistentClass)it.next())==1) it.remove();
		}
	}

	synchronized long refCount(PersistentClass clazz) {
		return refCount(clazz.base);
	}

	long allocatedSpace() {
		return heap.allocatedSpace();
	}

	long maxSpace() {
		return heap.maxSpace();
	}

	public synchronized void close() throws RemoteException {
		if(closed) return;
		UnicastRemoteObject.unexportObject(this,true);
		if(!readOnly) heap.mount(false);
		closed=true;
	}

	public synchronized void gc() {
		if(closed) return;
		if(readOnly) return;
		System.gc();
		mark();
		mark(boot);
		sweep();
	}

	void mark() {
		Iterator t=heap.iterator();
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
		Iterator t=heap.iterator();
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
			Iterator t=c.fieldIterator();
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
			Iterator t=c.fieldIterator();
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
		int n=MemoryModel.model.lastByteShift;
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

		public Iterator iterator() {
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
