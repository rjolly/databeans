package persistence.beans;

import java.lang.reflect.Proxy;

public class ReflectUtil {

	private ReflectUtil() {
	}

	public static void checkPackageAccess(Class<?> clazz) {
		checkPackageAccess(clazz.getName());
		if (isNonPublicProxyClass(clazz)) {
			checkProxyPackageAccess(clazz);
		}
	}

	public static void checkPackageAccess(String name) {
		SecurityManager s = System.getSecurityManager();
		if (s != null) {
			String cname = name.replace('/', '.');
			if (cname.startsWith("[")) {
				int b = cname.lastIndexOf('[') + 2;
				if (b > 1 && b < cname.length()) {
					cname = cname.substring(b);
				}
			}
			int i = cname.lastIndexOf('.');
			if (i != -1) {
				s.checkPackageAccess(cname.substring(0, i));
			}
		}
	}

	public static boolean isPackageAccessible(Class clazz) {
		try {
			checkPackageAccess(clazz);
		} catch (SecurityException e) {
			return false;
		}
		return true;
	}

	public static void checkProxyPackageAccess(Class<?> clazz) {
		SecurityManager s = System.getSecurityManager();
		if (s != null) {
			// check proxy interfaces if the given class is a proxy class
			if (Proxy.isProxyClass(clazz)) {
				for (Class<?> intf : clazz.getInterfaces()) {
					checkPackageAccess(intf);
				}
			}
		}
	}

	public static final String PROXY_PACKAGE = "com.sun.proxy";

	public static boolean isNonPublicProxyClass(Class<?> cls) {
		String name = cls.getName();
		int i = name.lastIndexOf('.');
		String pkg = (i != -1) ? name.substring(0, i) : "";
		return Proxy.isProxyClass(cls) && !pkg.equals(PROXY_PACKAGE);
	}
}
