package persistence.storage;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class FileHeap extends RandomAccessFile implements Heap {
	static final int Long_SIZE=MemoryModel.model.pointerSize;
	static final int Integer_SIZE=4;
	static final long OVERHEAD=normalized(0);
	static final long MAX_SIZE=normalized(Integer.MAX_VALUE);
	Chunk root;
	long last;
	long space;
	long maxSpace;
	long minSpace=4*Long_SIZE;
	long allocatedSpace=minSpace;
	Collector collector;

	public FileHeap(String name, Collector collector) throws FileNotFoundException {
		super(name,"rw");
		this.collector=collector;
		try {
			long l=length();
			if((maxSpace=l&-4)<l) setLength(maxSpace);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		if(minSpace>maxSpace);
		else {
			if((space=space())==0) setSpace(space=minSpace);
			root=root();
		}
		for(Chunk c=first();c!=null;c=c.next()) if(c.status()) allocatedSpace+=c.size;
	}

	public long boot() {
		if(minSpace>maxSpace) return 0;
		return MemoryModel.model.readPointer(this,2*Long_SIZE);
	}

	public void setBoot(long ptr) {
		if(minSpace>maxSpace) return;
		MemoryModel.model.writePointer(this,2*Long_SIZE,ptr);
	}

	public boolean mounted() {
		if(minSpace>maxSpace) return false;
		return MemoryModel.model.readPointer(this,3*Long_SIZE)>0;
	}

	public void mount(boolean n) {
		if(minSpace>maxSpace) return;
		MemoryModel.model.writePointer(this,3*Long_SIZE,n?1:0);
	}

	public synchronized long alloc(int size) {
		long s;
		Chunk c;
		if((c=get(last,s=normalized(size)))==null) {
			extend();
			if((c=get(last,s))==null) {
				collector.gc();
				if((c=get(last,s))==null) throw new StorageException("heap space exhausted");
			}
		}
		c=c.alloc(s);
		allocatedSpace+=c.size;
		return (last=c.pos)+Integer_SIZE;
	}

	public synchronized long realloc(long ptr, int size) {
		long p=alloc(size);
		new Chunk(ptr-Integer_SIZE).copyInto(new Chunk(p-Integer_SIZE));
		free(ptr);
		return p;
	}


	public synchronized void gc() {
		collector.gc();
	}

	public synchronized void free(long ptr) {
		Chunk c=new Chunk(ptr-Integer_SIZE);
		allocatedSpace-=c.size;
		c.free();
	}

	public synchronized boolean mark(long ptr, boolean n) {
		return new Chunk(ptr-Integer_SIZE).mark(n);
	}

	public synchronized boolean status(long ptr) {
		return new Chunk(ptr-Integer_SIZE).status();
	}

	public synchronized long allocatedSpace() {
		return allocatedSpace;
	}

	public long maxSpace() {
		return maxSpace;
	}

	public synchronized Iterator iterator() {
		return new FileHeapIterator();
	}

	public synchronized boolean readBoolean(long ptr) {
		try {
			seek(ptr);
			return readBoolean();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public synchronized byte readByte(long ptr) {
		try {
			seek(ptr);
			return readByte();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public synchronized short readShort(long ptr) {
		try {
			seek(ptr);
			return readShort();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public synchronized char readChar(long ptr) {
		try {
			seek(ptr);
			return readChar();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public synchronized int readInt(long ptr) {
		try {
			seek(ptr);
			return readInt();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public synchronized long readLong(long ptr) {
		try {
			seek(ptr);
			return readLong();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public synchronized float readFloat(long ptr) {
		try {
			seek(ptr);
			return readFloat();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public synchronized double readDouble(long ptr) {
		try {
			seek(ptr);
			return readDouble();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public synchronized byte[] readBytes(long ptr) {
		byte b[]=new byte[realized(new Chunk(ptr-Integer_SIZE).size)];
		try {
			seek(ptr);
			read(b);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return b;
	}

	public synchronized void writeBoolean(long ptr, boolean v) {
		try {
			seek(ptr);
			writeBoolean(v);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public synchronized void writeByte(long ptr, int v) {
		try {
			seek(ptr);
			writeByte(v);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public synchronized void writeShort(long ptr, int v) {
		try {
			seek(ptr);
			writeShort(v);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public synchronized void writeChar(long ptr, int v) {
		try {
			seek(ptr);
			writeChar(v);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public synchronized void writeInt(long ptr, int v) {
		try {
			seek(ptr);
			writeInt(v);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public synchronized void writeLong(long ptr, long v) {
		try {
			seek(ptr);
			writeLong(v);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public synchronized void writeFloat(long ptr, float v) {
		try {
			seek(ptr);
			writeFloat(v);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public synchronized void writeDouble(long ptr, double v) {
		try {
			seek(ptr);
			writeDouble(v);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public synchronized void writeBytes(long ptr, byte b[]) {
		try {
			seek(ptr);
			write(b);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	long space() {
		return MemoryModel.model.readPointer(this,0);
	}

	void setSpace(long l) {
		MemoryModel.model.writePointer(this,0,l);
	}

	Chunk root() {
		long p=MemoryModel.model.readPointer(this,Long_SIZE);
		return p==0?null:new Chunk(p);
	}

	void setRoot(Chunk c) {
		MemoryModel.model.writePointer(this,Long_SIZE,c==null?0:c.pos);
	}

	void extend() {
		long pos=space;
		if(pos+OVERHEAD>maxSpace) return;
		setSpace(space+=Math.min(MAX_SIZE,maxSpace-pos));
		create(pos,space-pos).free();
	}

	Chunk create(long pos, long size) {
		int s=realized(size);
		try {
			seek(pos);
			writeInt(s);
			seek(pos+size-Integer_SIZE);
			writeInt(s);
			return new Chunk(pos);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	static int realized(long size) {
		return (int)size-2*Integer_SIZE;
	}

	static long normalized(int size) {
		return Math.max(((long)(size-1)+(1<<2))&(-1<<2),3*Long_SIZE)+2*Integer_SIZE;
	}

	Chunk get(long pos, long size) {
		Chunk c=new Chunk(pos,size);
		Chunk d=get(c);
		if(d!=null && c.size<d.size) {
			Chunk p=predecessor(d);
			if(p==null?false:c.size==p.size) return p;
			else return get(pos,d.size);
		} else if(d!=null && c.size>d.size) {
			Chunk s=successor(d);
			if(s==null || c.size==s.size) return s;
			else return get(pos,s.size);
		} else return d;
	}

	class Chunk implements Comparable {
		long pos;
		long size;

		Chunk(long pos, long size) {
			this.pos=pos;
			this.size=size;
		}

		Chunk(long pos) {
			try {
				seek(this.pos=pos);
				size=normalized(readInt()&-4);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		Chunk alloc(long size) {
			remove(this);
			Chunk c=split(size);
			c.setColor(false);
			c.setStatus(true);
			return c;
		}

		Chunk split(long size) {
			if(size+OVERHEAD>this.size) return this;
			else {
				put(create(pos+size,this.size-size));
				return create(pos,size);
			}
		}

		void free() {
			setStatus(false);
			put(coalesce());
		}

		Chunk coalesce() {
			long pos=this.pos;
			long size=this.size;
			Chunk p=previous();
			Chunk n=next();

			while(p!=null && !p.status() && size+p.size<=MAX_SIZE) {
				remove(p);
				pos=p.pos;
				size+=p.size;
				p=p.previous();
			}
			while(n!=null && !n.status() && size+n.size<=MAX_SIZE) {
				remove(n);
				size+=n.size;
				n=n.next();
			}
			return pos<this.pos || size>this.size?create(pos,size):this;
		}

		Chunk previous() {
			if(pos>minSpace) {
				try {
					seek(pos-Integer_SIZE);
					return new Chunk(pos-normalized(readInt()));
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			} else return null;
		}

		Chunk next() {
			long pos=this.pos+size;
			return pos<space?new Chunk(pos):null;
		}

		boolean mark(boolean n) {
			boolean p=color();
			setColor(n);
			return p;
		}

		void copyInto(Chunk c) {
			for(int i=0;i<Math.min(size,c.size);i+=Integer_SIZE) {
				try {
					seek(pos+Integer_SIZE+i);
					int n=readInt();
					seek(c.pos+Integer_SIZE+i);
					writeInt(n);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		}

		boolean status() {
			try {
				seek(pos+Integer_SIZE-1);
				return (readByte()&1)!=0;
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		void setStatus(boolean s) {
			try {
				seek(pos+Integer_SIZE-1);
				byte b=readByte();
				seek(pos+Integer_SIZE-1);
				writeByte((s?1:0)|(b&-2));
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		void init() {
			init(null);
		}

		void init(Chunk c) {
			setColor(BLACK);
			setParent(c);
			setLeft(null);
			setRight(null);
		}

		boolean color() {
			try {
				seek(pos+Integer_SIZE-1);
				return (readByte()&2)!=0;
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		void setColor(boolean s) {
			try {
				seek(pos+Integer_SIZE-1);
				int b=readByte();
				seek(pos+Integer_SIZE-1);
				writeByte((s?2:0)|b&-3);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		Chunk parent() {
			long p=MemoryModel.model.readPointer(FileHeap.this,pos+Integer_SIZE);
			return p==0?null:new Chunk(p);
		}

		void setParent(Chunk c) {
			MemoryModel.model.writePointer(FileHeap.this,pos+Integer_SIZE,c==null?0:c.pos);
		}

		Chunk left() {
			long p=MemoryModel.model.readPointer(FileHeap.this,pos+Integer_SIZE+Long_SIZE);
			return p==0?null:new Chunk(p);
		}

		void setLeft(Chunk c) {
			MemoryModel.model.writePointer(FileHeap.this,pos+Integer_SIZE+Long_SIZE,c==null?0:c.pos);
		}

		Chunk right() {
			long p=MemoryModel.model.readPointer(FileHeap.this,pos+Integer_SIZE+2*Long_SIZE);
			return p==0?null:new Chunk(p);
		}

		void setRight(Chunk c) {
			MemoryModel.model.writePointer(FileHeap.this,pos+Integer_SIZE+2*Long_SIZE,c==null?0:c.pos);
		}

		public int compareTo(Object o) {
			Chunk c=(Chunk)o;
			if(size<c.size) return -1;
			else if(size>c.size) return 1;
			else {
				if(pos<c.pos) return -1;
				else if(pos>c.pos) return 1;
				else return 0;
			}
		}

		public boolean equals(Object obj) {
			return this == obj || (obj instanceof Chunk && pos==(((Chunk)obj).pos));
		}

		public String toString() {
			return Long.toHexString(pos);
		}
	}

	Chunk first() {
		long pos=minSpace;
		return pos<space?new Chunk(pos):null;
	}

	class FileHeapIterator implements Iterator {
		Chunk next;

		FileHeapIterator() {
			next = first();
		}

		public boolean hasNext() {
			return next != null;
		}

		public Object next() {
			if (next == null) throw new NoSuchElementException();
			Chunk lastReturned = next;
			next = next.next();
			return new Long(lastReturned.pos+Integer_SIZE);
		}

		public void remove() {
			throw new UnsupportedOperationException();
		}
	}

/*
 * @(#)TreeMap.java	1.56 03/01/23
 *
 * Copyright 2003 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

	Chunk get(Chunk chunk) {
		Chunk q = null;
		Chunk p = root;
		while (p != null) {
			q = p;
			int cmp = chunk.compareTo(p);
			if (cmp == 0) return p;
			else if (cmp < 0) p = p.left();
			else p = p.right();
		}
		return q;
	}

	public void put(Chunk chunk) {
		Chunk t = root;

		if (t == null) {
			setRoot(root=chunk);
			chunk.init();
			return;
		}

		while (true) {
			int cmp = chunk.compareTo(t);
			if (cmp == 0) {
				return;
			} else if (cmp < 0) {
				if (t.left() != null) {
					t = t.left();
				} else {
					t.setLeft(chunk);
					chunk.init(t);
					fixAfterInsertion(t.left());
					return;
				}
			} else { // cmp > 0
				if (t.right() != null) {
					t = t.right();
				} else {
					t.setRight(chunk);
					chunk.init(t);
					fixAfterInsertion(t.right());
					return;
				}
			}
		}
	}

	public void remove(Chunk chunk) {
		deleteChunk(chunk);
	}

	static final boolean RED = false;
	static final boolean BLACK = true;

	Chunk successor(Chunk t) {
		if (t == null)
			return null;
		else if (t.right() != null) {
			Chunk p = t.right();
			while (p.left() != null)
				p = p.left();
			return p;
		} else {
			Chunk p = t.parent();
			Chunk ch = t;
			while (p != null && ch.equals(p.right())) {
				ch = p;
				p = p.parent();
			}
			return p;
		}
	}

	Chunk predecessor(Chunk t) {
		if (t == null)
			return null;
		else if (t.left() != null) {
			Chunk p = t.left();
			while (p.right() != null)
				p = p.right();
			return p;
		} else {
			Chunk p = t.parent();
			Chunk ch = t;
			while (p != null && ch.equals(p.left())) {
				ch = p;
				p = p.parent();
			}
			return p;
		}
	}

	static boolean colorOf(Chunk p) {
		return (p == null ? BLACK : p.color());
	}

	static Chunk parentOf(Chunk p) {
		return (p == null ? null: p.parent());
	}

	static void setColor(Chunk p, boolean c) {
		if (p != null) p.setColor(c);
	}

	static Chunk leftOf(Chunk p) {
		return (p == null)? null: p.left();
	}

	static Chunk rightOf(Chunk p) {
		return (p == null)? null: p.right();
	}

	void rotateLeft(Chunk p) {
		Chunk r = p.right();
		p.setRight(r.left());
		if (r.left() != null)
			r.left().setParent(p);
		r.setParent(p.parent());
		if (p.parent() == null)
			setRoot(root=r);
		else if (p.parent().left().equals(p))
			p.parent().setLeft(r);
		else
			p.parent().setRight(r);
		r.setLeft(p);
		p.setParent(r);
	}

	void rotateRight(Chunk p) {
		Chunk l = p.left();
		p.setLeft(l.right());
		if (l.right() != null) l.right().setParent(p);
		l.setParent(p.parent());
		if (p.parent() == null)
			setRoot(root=l);
		else if (p.parent().right().equals(p))
			p.parent().setRight(l);
		else p.parent().setLeft(l);
		l.setRight(p);
		p.setParent(l);
	}

	void fixAfterInsertion(Chunk x) {
		x.setColor(RED);

		while (x != null && !x.equals(root) && x.parent().color() == RED) {
			if (parentOf(x).equals(leftOf(parentOf(parentOf(x))))) {
				Chunk y = rightOf(parentOf(parentOf(x)));
				if (colorOf(y) == RED) {
					setColor(parentOf(x), BLACK);
					setColor(y, BLACK);
					setColor(parentOf(parentOf(x)), RED);
					x = parentOf(parentOf(x));
				} else {
					if (x.equals(rightOf(parentOf(x)))) {
						x = parentOf(x);
						rotateLeft(x);
					}
					setColor(parentOf(x), BLACK);
					setColor(parentOf(parentOf(x)), RED);
					if (parentOf(parentOf(x)) != null) 
						rotateRight(parentOf(parentOf(x)));
				}
			} else {
				Chunk y = leftOf(parentOf(parentOf(x)));
				if (colorOf(y) == RED) {
					setColor(parentOf(x), BLACK);
					setColor(y, BLACK);
					setColor(parentOf(parentOf(x)), RED);
					x = parentOf(parentOf(x));
				} else {
					if (x.equals(leftOf(parentOf(x)))) {
						x = parentOf(x);
						rotateRight(x);
					}
					setColor(parentOf(x), BLACK);
					setColor(parentOf(parentOf(x)), RED);
					if (parentOf(parentOf(x)) != null) 
						rotateLeft(parentOf(parentOf(x)));
				}
			}
		}
		root.setColor(BLACK);
	}

	void deleteChunk(Chunk p) {
		if (p.left() != null && p.right() != null) {
			Chunk s = successor(p);
			swapPosition(s, p);
		} 

		Chunk replacement = (p.left() != null ? p.left() : p.right());

		if (replacement != null) {
			replacement.setParent(p.parent());
			if (p.parent() == null)
				setRoot(root=replacement);
			else if (p.equals(p.parent().left()))
				p.parent().setLeft(replacement);
			else
				p.parent().setRight(replacement);

			p.setLeft(null);
			p.setRight(null);
			p.setParent(null);

			if (p.color() == BLACK) 
				fixAfterDeletion(replacement);
		} else if (p.parent() == null) {
			setRoot(root=null);
		} else {
			if (p.color() == BLACK) 
				fixAfterDeletion(p);

			if (p.parent() != null) {
				if (p.equals(p.parent().left())) 
					p.parent().setLeft(null);
				else if (p.equals(p.parent().right())) 
					p.parent().setRight(null);
				p.setParent(null);
			}
		}
	}

	void fixAfterDeletion(Chunk x) {
		while (!x.equals(root) && colorOf(x) == BLACK) {
			if (x.equals(leftOf(parentOf(x)))) {
				Chunk sib = rightOf(parentOf(x));

				if (colorOf(sib) == RED) {
					setColor(sib, BLACK);
					setColor(parentOf(x), RED);
					rotateLeft(parentOf(x));
					sib = rightOf(parentOf(x));
				}

				if (colorOf(leftOf(sib)) == BLACK && 
					colorOf(rightOf(sib)) == BLACK) {
					setColor(sib, RED);
					x = parentOf(x);
				} else {
					if (colorOf(rightOf(sib)) == BLACK) {
						setColor(leftOf(sib), BLACK);
						setColor(sib, RED);
						rotateRight(sib);
						sib = rightOf(parentOf(x));
					}
					setColor(sib, colorOf(parentOf(x)));
					setColor(parentOf(x), BLACK);
					setColor(rightOf(sib), BLACK);
					rotateLeft(parentOf(x));
					x = root;
				}
			} else {
				Chunk sib = leftOf(parentOf(x));

				if (colorOf(sib) == RED) {
					setColor(sib, BLACK);
					setColor(parentOf(x), RED);
					rotateRight(parentOf(x));
					sib = leftOf(parentOf(x));
				}

				if (colorOf(rightOf(sib)) == BLACK && 
					colorOf(leftOf(sib)) == BLACK) {
					setColor(sib, RED);
					x = parentOf(x);
				} else {
					if (colorOf(leftOf(sib)) == BLACK) {
						setColor(rightOf(sib), BLACK);
						setColor(sib, RED);
						rotateLeft(sib);
						sib = leftOf(parentOf(x));
					}
					setColor(sib, colorOf(parentOf(x)));
					setColor(parentOf(x), BLACK);
					setColor(leftOf(sib), BLACK);
					rotateRight(parentOf(x));
					x = root;
				}
			}
		}
		setColor(x, BLACK);
	}

	void swapPosition(Chunk x, Chunk y) {
		Chunk px = x.parent(), lx = x.left(), rx = x.right();
		Chunk py = y.parent(), ly = y.left(), ry = y.right();
		boolean xWasLeftChild = px != null && x.equals(px.left());
		boolean yWasLeftChild = py != null && y.equals(py.left());

		if (x.equals(py)) {
			x.setParent(y);
			if (yWasLeftChild) {
				y.setLeft(x);
				y.setRight(rx);
			} else {
				y.setRight(x);
				y.setLeft(lx);
			}
		} else {
			x.setParent(py);
			if (py != null) {
				if (yWasLeftChild)
					py.setLeft(x);
				else
					py.setRight(x);
			}
			y.setLeft(lx);
			y.setRight(rx);
		}

		if (y.equals(px)) {
			y.setParent(x);
			if (xWasLeftChild) {
				x.setLeft(y);
				x.setRight(ry);
			} else {
				x.setRight(y);
				x.setLeft(ly);
			}
		} else {
			y.setParent(px);
			if (px != null) {
				if (xWasLeftChild)
					px.setLeft(y);
				else
					px.setRight(y);
			}
			x.setLeft(ly);
			x.setRight(ry);
		}

		if (x.left() != null)
			x.left().setParent(x);
		if (x.right() != null)
			x.right().setParent(x);
		if (y.left() != null)
			y.left().setParent(y);
		if (y.right() != null)
			y.right().setParent(y);

		boolean c = x.color();
		x.setColor(y.color());
		y.setColor(c);

		if (root.equals(x))
			setRoot(root=y);
		else if (root.equals(y))
			setRoot(root=x);
	}
}
