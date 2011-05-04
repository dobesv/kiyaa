package com.habitsoft.kiyaa.rebind.typeinfo;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PrimitiveTypeInfo extends RuntimeClassWrapper implements GeneratorTypeInfo {

	private static final Map<String, PrimitiveTypeInfo> BUILT_IN_MAP = 
	    new ConcurrentHashMap<String, PrimitiveTypeInfo>();

	static {
	    for(Class<?> c : new Class[]{Void.class, Boolean.class, Byte.class, Character.class,
	    		Short.class, Integer.class, Float.class, Double.class, Long.class}) {
			try {
				Class<?> prim = (Class<?>) c.getField("TYPE").get(null);
		        final PrimitiveTypeInfo pti = new PrimitiveTypeInfo(prim, new RuntimeClassWrapper(c));
		    	BUILT_IN_MAP.put(c.getName(), pti);
				BUILT_IN_MAP.put(prim.getName(), pti);
			} catch (Exception e) {
				e.printStackTrace();
				continue;
			}
	    }
	}

	public final static PrimitiveTypeInfo VOID = valueOf("void");
	public final static PrimitiveTypeInfo INT = valueOf("int");
	public final static PrimitiveTypeInfo CHAR = valueOf("char");
	public final static PrimitiveTypeInfo BOOLEAN = valueOf("boolean");
	public final static PrimitiveTypeInfo BYTE = valueOf("byte");
	public final static PrimitiveTypeInfo SHORT = valueOf("short");
	public final static PrimitiveTypeInfo FLOAT = valueOf("float");
	public final static PrimitiveTypeInfo DOUBLE = valueOf("double");
	public final static PrimitiveTypeInfo LONG = valueOf("long");
	
	/**
	 * <p>Get a PrimitiveTypeInfo instance for the given type or class name.</p>
	 * 
	 * <p>Will accept primitive type names like: </p>
	 * <ul>
	 * <li>void</li>
	 * <li>int</li>
	 * <li>char</li>
	 * <li>boolean</li>
	 * <li>byte</li>
	 * <li>short</li>
	 * <li>float</li>
	 * <li>double</li>
	 * <li>long</li>
	 * </ul>
	 * <p> And their boxed counterparts, like: </p>
	 * <ul>
	 * <li>java.lang.Void</li>
	 * <li>java.lang.Integer</li>
	 * <li>java.lang.Character</li>
	 * <li>java.lang.Boolean</li>
	 * <li>java.lang.Byte</li>
	 * <li>java.lang.Short</li>
	 * <li>java.lang.Float</li>
	 * <li>java.lang.Double</li>
	 * <li>java.lang.Long</li>
	 * </ul>
	 * <p>Note: When the boxed class name is passed, this still returns the unboxed class information 
	 * (i.e. java.lang.Integer returns metadata for "int")</p>
	 * 
	 * @param name Class name or type name to get metadata for
	 * @return The metadata for the primitive type requested, or null if no matching primitive type was found
	 */
	public static PrimitiveTypeInfo valueOf(String name) {
	    return BUILT_IN_MAP.get(name);
	}

	private final RuntimeClassWrapper boxedType;

	private PrimitiveTypeInfo(Class<?> clazz, RuntimeClassWrapper boxedType) {
		super(clazz);
		this.boxedType = boxedType;
	}

	/**
	 * Get the boxed type name for this primitive type.  For "long" this would return "java.lang.Long".
	 */
	public String getBoxedTypeName() {
		return boxedType.getName();
	}
	
	/**
	 * Return true if the given type is the "boxed" representation
	 * of this primitive type.
	 * 
	 * The test is based on class name; i.e. the other one is expected
	 * to be named something like "java.lang.Integer".  It doesn't have
	 * to be any particular type.
	 */
	public boolean unboxesTo(GeneratorTypeInfo otherType) {
		return getBoxedTypeName().equals(otherType.getName());
	}
	
	/**
	 * Return true if the given boxedClassName corresponds to the primitive
	 * version of the given unboxedTypeName.
	 * 
	 * @param boxedClassName Name of the boxed class; e.g. "java.lang.Long"
	 * @param unboxedTypeName Name of the primitive type; e.g. "long"
	 * @return true if the two are a match
	 */
	public static boolean unboxesTo(String boxedClassName, String unboxedTypeName) {
		return valueOf(unboxedTypeName).getBoxedTypeName().equals(boxedClassName);
	}
}
