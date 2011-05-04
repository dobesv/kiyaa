package com.habitsoft.kiyaa.rebind.typeinfo;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Set;
import java.util.TreeSet;
import java.util.WeakHashMap;

import com.google.gwt.core.ext.typeinfo.JArrayType;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JEnumConstant;
import com.google.gwt.core.ext.typeinfo.JEnumType;
import com.google.gwt.core.ext.typeinfo.JPrimitiveType;
import com.google.gwt.core.ext.typeinfo.JType;

public class JTypeWrapper implements GeneratorTypeInfo {
	final JType type;
	
	protected JTypeWrapper(JType type) {
		this.type = type;
	}

	public JType getJType() {
		return type;
		
	}
	@Override
	public ExpressionInfo findAccessors(String prefix, String expr) {
		return null;
	}

	@Override
	public String getName() {
		return type.getErasedType().getQualifiedSourceName();
	}
	
	@Override
	public String toString() {
		return type.toString();
	}

	@Override
	public boolean implementsInterface(GeneratorTypeInfo interfaceName) {
		return false;
	}
	
	public boolean equals(GeneratorTypeInfo otherType) {
		return this == otherType || getName().equals(otherType.getName());
	}
	
	@Override
	public boolean equals(Object other) {
		return this == other || (other instanceof GeneratorTypeInfo && this.equals((GeneratorTypeInfo)other)); 
	}
	
	@Override
	public String getParameterizedQualifiedSourceName() {
		return type.getParameterizedQualifiedSourceName();
	}
	
	@Override
	public boolean hasStaticMethodMatching(String name, GeneratorTypeInfo returnType, GeneratorTypeInfo... parameterTypes) {
		return false;
	}

	@Override
	public boolean isPrimitive() {
		return type.isPrimitive() != null;
	}
	
	@Override
	public boolean isSubclassOf(GeneratorTypeInfo superclassName) {		
		return false;
	}

	@Override
	public boolean hasMethodMatching(String name, boolean matchAbstract, GeneratorTypeInfo returnType, GeneratorTypeInfo... parameterTypes) {
		return false;
	}
	
	@Override
	public GeneratorMethodInfo findMethodMatching(String name, GeneratorTypeInfo returnType, GeneratorTypeInfo... parameterTypes) {
		return null;
	}

	@Override
	public GeneratorMethodInfo findStaticMethodMatching(String name, GeneratorTypeInfo returnType, GeneratorTypeInfo... parameterTypes) {
		return null;
	}

	@Override
	public boolean isStatic() {
		JClassType classType = type.isClassOrInterface();
		if(classType != null) {
			return classType.isStatic();
		}
		return true;
	}

	@Override
	public boolean isAbstract() {
		JClassType classType = type.isClassOrInterface();
		if(classType != null) {
			return classType.isAbstract();
		}
		return false;
	}

	@Override
	public GeneratorTypeInfo getEnclosingType() {
		JClassType classType = type.isClassOrInterface();
		if(classType != null) {
			JClassType enclosingClassType = classType.getEnclosingType();
			if(enclosingClassType != null) {
				return new JClassTypeWrapper(enclosingClassType);
			}
		}
		return null;
	}

	@Override
	public String getSimpleSourceName() {
		return type.getSimpleSourceName();
	}

	@Override
	public boolean isEnum() {
		return type.isEnum() != null;
	}

	transient Set<String> enumMembers;
	@Override
	public Set<String> getEnumMembers() throws IllegalStateException {
		if(enumMembers != null)
			return enumMembers;
		
		JEnumType enumType = type.isEnum();
		if(enumType == null)
			return null;
		enumMembers = new TreeSet<String>();
		for(JEnumConstant ec : enumType.getEnumConstants()) {
			enumMembers.add(ec.getName());
		}
		return enumMembers;
	}

	@Override
	public JClassType getJClassType() {
		return type.isClassOrInterface();
	}

	@Override
	public boolean isArray() {
		return type.isArray() != null;
	}

	@Override
	public GeneratorTypeInfo getComponentType() {
		return type.isArray() != null ? wrap(type.isArray().getComponentType()) : null;
	}
	
	private static WeakHashMap<JType, GeneratorTypeInfo> wrapperCache = new WeakHashMap<JType, GeneratorTypeInfo>();
	/**
	 * Create an appropriate subclass of JTypeWrapper for the given JType
	 */
	public static GeneratorTypeInfo wrap(JType t) {
		if(t == null)
			return null;
		
		GeneratorTypeInfo result = wrapperCache.get(t);
		if(result != null)
			return result;
		JClassType classType = t.isClassOrInterface();
		if(classType != null) {
			result = new JClassTypeWrapper(classType);
		} else {
			JPrimitiveType primitiveType = t.isPrimitive();
			if(primitiveType != null) {
				result = PrimitiveTypeInfo.valueOf(primitiveType.getSimpleSourceName());
			} else {
				result = new JTypeWrapper(t);
			}
		}
		wrapperCache.put(t, result);
		return result;
	}

	@Override
	public GeneratorTypeInfo getFieldType(String name, boolean allowProtected) {
		JArrayType arrayType = type.isArray();
		if(arrayType != null && "length".equals(name))
			return PrimitiveTypeInfo.INT;
		return null;
	}
}
