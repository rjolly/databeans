package persistence;

public interface Array<C> {
	int length();
	char typeCode();
	C get(int index);
	void set(int index, C value);
}
