/**
 * 
 */
package com.habitsoft.kiyaa.rebind.typeinfo;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.TreeSet;

import com.google.gwt.core.ext.typeinfo.JClassType;
import com.habitsoft.kiyaa.util.Name;

public class RuntimeClassWrapper implements GeneratorTypeInfo {
	private static final class RuntimeMethodWrapper extends GeneratorMethodInfo {
		private final Method m;

		private RuntimeMethodWrapper(String name, GeneratorTypeInfo returnType, GeneratorTypeInfo[] parameterTypes, String[] parameterNames,
				GeneratorTypeInfo asyncReturnType, Method m) {
			super(name, returnType, parameterTypes, parameterNames, asyncReturnType);
			this.m = m;
		}

		@Override
		public boolean isAbstract() {
			return (m.getModifiers() & Modifier.ABSTRACT) == Modifier.ABSTRACT || m.getDeclaringClass().isInterface();
		}
		@Override
		public <T extends Annotation> T getAnnotation(Class<T> annotationToLookFor) {
			return m.getAnnotation(annotationToLookFor);
		}
		@Override
		public String toString() {
			return m.toString();
		}
	}

	public static final RuntimeClassWrapper OBJECT = new RuntimeClassWrapper(Object.class);
	public static final RuntimeClassWrapper STRING = new RuntimeClassWrapper(String.class);

	final Class<?> clazz;
	
	private HashMap<String, ArrayList<GeneratorMethodInfo>> methods;
	private HashMap<String, ArrayList<GeneratorMethodInfo>> staticMethods;
	
	protected RuntimeClassWrapper(Class<?> clazz) {
		super();
		this.clazz = clazz;
	}

	@Override
	public ExpressionInfo findAccessors(String prefix, String expr) {
		return null;
	}

	@Override
	public String getName() {
		return clazz.getName();
	}

	@Override
	public String toString() {
		return clazz.toString();
	}
	
	@Override
	public boolean implementsInterface(GeneratorTypeInfo iface) {
		try {
			return Class.forName(iface.getName()).isAssignableFrom(clazz);
		} catch (ClassNotFoundException e) {
			// Can't implement an interface that does not exist
			return false;
		}
	}
	
	public boolean equals(GeneratorTypeInfo otherType) {
		return this == otherType || (otherType != null && getName().equals(otherType.getName()));
	}
	
	@Override
	public boolean equals(Object other) {
		return this == other || (other instanceof GeneratorMethodInfo && this.equals((GeneratorTypeInfo)other)); 
	}
	
	@Override
	public String getParameterizedQualifiedSourceName() {
		return getName();
	}
	
	@Override
	public boolean hasStaticMethodMatching(String name, GeneratorTypeInfo desiredReturnType, GeneratorTypeInfo... parameterTypes) {
		if(staticMethods == null) {
			loadMethods();
		}
		
		return GeneratorMethodInfo.checkForMethod(staticMethods, name, true, true, desiredReturnType, parameterTypes);
	}

	
	@Override
	public boolean hasMethodMatching(String name, boolean matchAbstract, GeneratorTypeInfo desiredReturnType, GeneratorTypeInfo... parameterTypes) {
		if(methods == null) {
			loadMethods();
		}
		return GeneratorMethodInfo.checkForMethod(methods, name, matchAbstract, true, desiredReturnType, parameterTypes);
	}

	@Override
	public GeneratorMethodInfo findMethodMatching(String name, boolean allowCastable, GeneratorTypeInfo returnType, GeneratorTypeInfo... parameterTypes) {
		if(methods == null) {
			loadMethods();
		}
		return GeneratorMethodInfo.findMethod(methods, name, allowCastable, returnType, parameterTypes);
	}
	
	@Override
	public GeneratorMethodInfo findStaticMethodMatching(String name, boolean allowCastable, GeneratorTypeInfo returnType, GeneratorTypeInfo... parameterTypes) {
		if(staticMethods == null) {
			loadMethods();
		}
		return GeneratorMethodInfo.findMethod(staticMethods, name, allowCastable, returnType, parameterTypes);
	}
	
	/**
	 * Load all methods available on this class into a cache for quick lookups
	 */
	private void loadMethods() {
		methods = new HashMap<String, ArrayList<GeneratorMethodInfo>>();
		for(Method m : clazz.getMethods()) {
			if((m.getModifiers() & Modifier.STATIC) == 0) {
				addMethod(m, methods);
			}
		}
		staticMethods = new HashMap<String, ArrayList<GeneratorMethodInfo>>();
		for(Method m : clazz.getDeclaredMethods()) {
			if((m.getModifiers() & Modifier.STATIC) == Modifier.STATIC) {
				addMethod(m, staticMethods);
			}
		}
	}

	/**
	 * Helper method to convert a java.lang.reflect.Method into a GeneratorMethodInfo
	 * and add it to the methods map.
	 */
	static void addMethod(final Method m, HashMap<String, ArrayList<GeneratorMethodInfo>> map) {
		String name = m.getName();
		Name nameAnnotation = m.getAnnotation(Name.class);
		if(nameAnnotation != null) name = nameAnnotation.value();
		ArrayList<GeneratorMethodInfo> overloads = map.get(name);
		if(overloads == null) map.put(m.getName(), overloads = new ArrayList<GeneratorMethodInfo>());
		GeneratorTypeInfo[] parameterTypes = new GeneratorTypeInfo[m.getParameterTypes().length];
		for(int i=0; i < parameterTypes.length; i++) {
			parameterTypes[i] = wrap(m.getParameterTypes()[i]);
		}
		GeneratorTypeInfo returnType = wrap(m.getReturnType());
		overloads.add(new RuntimeMethodWrapper(m.getName(), returnType, parameterTypes, null, null, m));
	}

	@Override
	public boolean isSubclassOf(GeneratorTypeInfo potentialSuperclass) {
		for(Class<?> superclass = clazz; superclass != null; superclass = superclass.getSuperclass()) {
			if(superclass.equals(potentialSuperclass))
				return true;
		}
		return false;
	}

	@Override
	public boolean isPrimitive() {
		return clazz.isPrimitive();
	}

	@Override
	public boolean isStatic() {
		return (clazz.getModifiers() & Modifier.STATIC) == Modifier.STATIC;
	}

	@Override
	public boolean isAbstract() {
		return (clazz.getModifiers() & Modifier.ABSTRACT) == Modifier.ABSTRACT;
	}

	@Override
	public GeneratorTypeInfo getEnclosingType() {
		return wrap(clazz.getEnclosingClass());
	}

	@Override
	public String getSimpleSourceName() {
		return clazz.getSimpleName();
	}

	@Override
	public boolean isEnum() {
		return clazz.isEnum();
	}

	transient TreeSet<String> enumMembers;
	@Override
	public Set<String> getEnumMembers() throws IllegalStateException {
		if(!clazz.isEnum())
			return null;
		enumMembers = new TreeSet<String>();
		for(Object ec : clazz.getEnumConstants()) {
			try {
				enumMembers.add((String)ec.getClass().getMethod("name").invoke(ec));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return enumMembers;
	}

	@Override
	public JClassType getJClassType() {
		return null;
	}

	@Override
	public boolean isArray() {
		return clazz.isArray();
	}

	@Override
	public GeneratorTypeInfo getComponentType() {
		return wrap(clazz.getComponentType());
	}
	
	private static HashMap<Class<?>, RuntimeClassWrapper> cache = new HashMap<Class<?>, RuntimeClassWrapper>();
	
	/**
	 * Get a class info for the given class.  Uses a cache to re-use the same instance
	 * over and over which can be useful sometimes.
	 */
	public static RuntimeClassWrapper wrap(Class<?> clazz) {
		if(clazz == null)
			return null;
		if(clazz.isPrimitive())
			return PrimitiveTypeInfo.valueOf(clazz.getName());
		RuntimeClassWrapper result = cache.get(clazz);
		if(result != null)
			return result;
		cache.put(clazz, result = new RuntimeClassWrapper(clazz));
		return result;
	}

	@Override
	public GeneratorTypeInfo getFieldType(String name, boolean allowProtected) {
		Field field;
		try {
			field = clazz.getField(name);
		} catch (NoSuchFieldException e) {
			return null;
		}
		if((field.getModifiers() & (Modifier.PUBLIC | (allowProtected ? Modifier.PROTECTED : 0))) != 0) {
			return wrap(field.getType());
		}
		return null;
	}
	
}