/**
 * 
 */
package com.habitsoft.kiyaa.rebind.typeinfo;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.HashMap;

import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JField;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JParameter;
import com.google.gwt.core.ext.typeinfo.JParameterizedType;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.habitsoft.kiyaa.util.Name;

public class JClassTypeWrapper extends JTypeWrapper implements GeneratorTypeInfo {
	private static final class JMethodWrapper extends GeneratorMethodInfo {
		private final JMethod m;

		private JMethodWrapper(String name, GeneratorTypeInfo returnType, GeneratorTypeInfo[] parameterTypes, String[] parameterNames,
				GeneratorTypeInfo asyncReturnType, JMethod m) {
			super(name, returnType, parameterTypes, parameterNames, asyncReturnType);
			this.m = m;
		}

		@Override
		public <T extends Annotation> T getAnnotation(Class<T> annotationToLookFor) {
			return m.getAnnotation(annotationToLookFor);
		}
		
		@Override
		public boolean isAbstract() {
			return m.isAbstract() || m.getEnclosingType().isInterface() != null;
		}
		@Override
		public String toString() {
			return m.toString();
		}
	}

	final JClassType classType;
	private HashMap<String, ArrayList<GeneratorMethodInfo>> methods;
	private HashMap<String, ArrayList<GeneratorMethodInfo>> staticMethods;
	private ArrayList<GeneratorTypeInfo> implementedInterfaces;
	private JClassTypeWrapper superclass;
	
	protected JClassTypeWrapper(JClassType classType) {
		super(classType);
		this.classType = classType;
		if(classType.getSuperclass() != null)
			superclass = wrap(classType.getSuperclass());
	}

	public JClassType getClassType() {
		return classType;
	}

	@Override
	public ExpressionInfo findAccessors(String prefix, String expr) {
		return null;
	}

	@Override
	public boolean implementsInterface(GeneratorTypeInfo destType) {
		// If we are that interface, we implement it (ourselves)
		if(equals(destType))
			return true;
		
		if(implementedInterfaces == null)
			loadInterfaces();
		for(GeneratorTypeInfo iface : implementedInterfaces) {
			if(iface.implementsInterface(destType))
				return true;
		}
		if(superclass != null)
			return superclass.implementsInterface(destType);
		return false;
	}
	
	@Override
	public String getName() {
		return classType.getErasedType().getQualifiedSourceName();
	}
	
	/**
	 * Load all methods available on this class into a cache for quick lookups
	 */
	private void loadMethods() {
		methods = new HashMap<String, ArrayList<GeneratorMethodInfo>>();
		for(JMethod m : classType.getInheritableMethods()) {
			if(!m.isStatic()) {
				addMethod(m, methods);
			}
		}
		staticMethods = new HashMap<String, ArrayList<GeneratorMethodInfo>>();
		for(JMethod m : classType.getMethods()) {
			if(m.isStatic()) {
				addMethod(m, staticMethods);
			}
		}
	}

	/**
	 * Helper method to convert a java.lang.reflect.Method into a GeneratorMethodInfo
	 * and add it to the methods map.
	 */
	static void addMethod(final JMethod m, HashMap<String, ArrayList<GeneratorMethodInfo>> map) {
		String name = m.getName();
		Name nameAnnotation = m.getAnnotation(Name.class);
		if(nameAnnotation != null) name = nameAnnotation.value();
		ArrayList<GeneratorMethodInfo> overloads = map.get(name);
		if(overloads == null) map.put(name, overloads = new ArrayList<GeneratorMethodInfo>());
		GeneratorTypeInfo[] parameterTypes = new GeneratorTypeInfo[m.getParameterTypes().length];
		for(int i=0; i < parameterTypes.length; i++) {
			parameterTypes[i] = wrap(m.getParameterTypes()[i]);
		}
		GeneratorTypeInfo returnType = wrap(m.getReturnType());
		overloads.add(new JMethodWrapper(name, returnType, parameterTypes, null, JTypeWrapper.wrap(getAsyncReturnType(m)), m));
	}
	
	public static JType getAsyncReturnType(JMethod method) {
    	JParameter[] parameters = method.getParameters();
		int parameterCount = parameters.length;
		if(parameterCount > 0) {
    		JParameterizedType parameterized = parameters[parameterCount-1].getType().isParameterized();
			if(parameterized != null) {
    			if(parameterized.getQualifiedSourceName().equals(AsyncCallback.class.getName())) {
    				return parameterized.getTypeArgs()[0];
    			}
    		}
    	}
    	return null;
	}
	
	
	@Override
	public boolean hasStaticMethodMatching(String name, GeneratorTypeInfo desiredReturnType, GeneratorTypeInfo... parameterTypes) {
		if(staticMethods == null) {
			loadMethods();
		}
		
		return GeneratorMethodInfo.checkForMethod(staticMethods, name, true, desiredReturnType, parameterTypes);
	}

	@Override
	public GeneratorMethodInfo findStaticMethodMatching(String name, GeneratorTypeInfo desiredReturnType, GeneratorTypeInfo... parameterTypes) {
		if(staticMethods == null) {
			loadMethods();
		}
		return GeneratorMethodInfo.findMethod(staticMethods, name, desiredReturnType, parameterTypes);
	}
	

	@Override
	public boolean hasMethodMatching(String name, boolean matchAbstract, GeneratorTypeInfo desiredReturnType, GeneratorTypeInfo... parameterTypes) {
		if(methods == null) {
			loadMethods();
		}
		return GeneratorMethodInfo.checkForMethod(methods, name, matchAbstract, desiredReturnType, parameterTypes);
	}

	@Override
	public GeneratorMethodInfo findMethodMatching(String name, GeneratorTypeInfo desiredReturnType, GeneratorTypeInfo... parameterTypes) {
		if(methods == null) {
			loadMethods();
		}
		return GeneratorMethodInfo.findMethod(methods, name, desiredReturnType, parameterTypes);
	}
	

	@Override
	public boolean isSubclassOf(GeneratorTypeInfo superclassToCheckFor) {
		for(JClassTypeWrapper superclass = this; superclass != null; superclass = superclass.superclass) {
			if(superclass.equals(superclassToCheckFor))
				return true;
		}
		return false;
	}
	
	private void loadInterfaces() {
		implementedInterfaces = new ArrayList<GeneratorTypeInfo>();
		JClassType[] ifaces = classType.getImplementedInterfaces();
		if(ifaces == null)
			return;
		for(JClassType iface : ifaces) {
			implementedInterfaces.add(wrap(iface));
		}
	}

	@Override
	public GeneratorTypeInfo getFieldType(String name, boolean allowProtected) {
		JField field = classType.getField(name);
		if(field != null && (field.isPublic() || (allowProtected && field.isProtected())))
			return wrap(field.getType());
		return null;
	}
	
	/**
	 * Return a GeneratorTypeInfo for the given JClassType.
	 * 
	 * Note that this uses a cache internall so that same instance
	 * will be returned with subsequent calls if the same type is
	 * passed in again.
	 */
	public static JClassTypeWrapper wrap(JClassType type) {
		return (JClassTypeWrapper) JTypeWrapper.wrap(type);
	}
}