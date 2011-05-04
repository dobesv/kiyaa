/**
 * 
 */
package com.habitsoft.kiyaa.rebind.typeinfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;

public class GeneratedClassInfo implements GeneratorTypeInfo {
	final private HashMap<String,ExpressionInfo> members = new HashMap<String, ExpressionInfo>();
	final private HashMap<String,ExpressionInfo> getters = new HashMap<String, ExpressionInfo>();
	final private HashSet<GeneratorTypeInfo> interfaces = new HashSet<GeneratorTypeInfo>();
	final private JClassTypeWrapper superclass;
	final private String name;
	final private HashMap<String, ArrayList<GeneratorMethodInfo>> methods = new HashMap<String, ArrayList<GeneratorMethodInfo>>();
	
	/**
	 * 
	 * @param name Full class name, e.g. foo.bar.Baz
	 */
	public GeneratedClassInfo(String name, JClassTypeWrapper superclass) {
		this.name = name;
		this.superclass = superclass;
	}

	@Override
	public boolean implementsInterface(GeneratorTypeInfo destType) {
		for(GeneratorTypeInfo iface : interfaces) {
			if(iface.implementsInterface(destType))
				return true;
		}
		if(superclass != null && superclass.implementsInterface(destType))
			return true;
		return false;
	}

	public HashMap<String, ExpressionInfo> getMembers() {
		return members;
	}

	public HashSet<GeneratorTypeInfo> getInterfaces() {
		return interfaces;
	}

	public JClassTypeWrapper getSuperclass() {
		return superclass;
	}

	/**
	 * Full class name, e.g. foo.bar.Baz
	 */
	public String getName() {
		return name;
	}

	@Override
	public String toString() {
		return name;
	}
	
	/**
	 * Note the availability of a field in the generated class.
	 * 
	 * @param fieldName Name of the field
	 * @param fieldType Type of the field
	 */
	public void addField(String fieldName, GeneratorTypeInfo fieldType) {
		members.put(fieldName, new ExpressionInfo(fieldName, fieldName, fieldName+" = ", fieldType));
	}

	@Override
	public boolean isStatic() {
		return true; // top-level class is always static
	}

	@Override
	public GeneratorTypeInfo getEnclosingType() {
		return null; // no enclosing type, we're top-level.  Use GeneratedInnerClass for an inner class
	}

	/**
	 * Note the availability of a getter in this generated class.
	 * 
	 * @param propertyName Property name this getter is for, i.e. "foo" for a method "getFoo" or "isFoo"
	 * @param returnType Return type of the getter
	 * @param methodName Method name of the getter
	 * @param asynchronous If true, the getter takes a single parameter which is an async callback; if false, the getter takes zero parameters
	 */
	public void addGetter(String propertyName, GeneratorTypeInfo returnType, String methodName, boolean asynchronous) {
		getters.put(propertyName, new ExpressionInfo(propertyName, methodName, null, returnType, asynchronous, false, false));
	}

	@Override
	public ExpressionInfo findAccessors(String prefix, String expr) throws UnableToCompleteException {
		int dotPos = expr.indexOf('.');
		if(dotPos == -1) {
			// No dot, leaf lookup
			ExpressionInfo getter = getters.get(expr);
			if(getter != null) return getter;
			ExpressionInfo member = members.get(expr);
			if(member != null) return member;
		} else {
			String firstPart = expr.substring(0, dotPos);
			ExpressionInfo firstPartInfo = findAccessors(prefix, firstPart);
			if(firstPartInfo != null) {
				String rest = expr.substring(dotPos+1);
				ExpressionInfo restInfo = firstPartInfo.getType().findAccessors("", rest);
				if(restInfo != null) {
					return firstPartInfo.asSubexpression(restInfo);
				}
			}
		}
		return null;
	}

	@Override
	public boolean isSubclassOf(GeneratorTypeInfo superclass) {
		return superclass == null 
		|| this.equals(superclass) 
		|| this.superclass.equals(superclass) 
		|| this.superclass.isSubclassOf(superclass);
	}

	@Override
	public String getParameterizedQualifiedSourceName() {
		return name;
	}

	@Override
	public boolean hasStaticMethodMatching(String name, GeneratorTypeInfo returnType, GeneratorTypeInfo... parameterTypes) {
		return superclass != null && superclass.hasStaticMethodMatching(name, returnType, parameterTypes);
	}

	@Override
	public boolean isPrimitive() {
		return false;
	}

	@Override
	public String getSimpleSourceName() {
		return name;
	}

	/**
	 * Make note that the given method is available
	 * @param string
	 * @param returnType
	 * @param parameterTypes
	 */
	public void addMethod(String name, GeneratorTypeInfo returnType, GeneratorTypeInfo ... parameterTypes) {
		GeneratorMethodInfo m = new GeneratorMethodInfo(name, returnType, parameterTypes, null, null);
		ArrayList<GeneratorMethodInfo> overloads = methods.get(name);
		if(overloads == null) methods.put(name, overloads = new ArrayList<GeneratorMethodInfo>());
		overloads.add(m);
	}

	/**
	 * Add an implemented interface to the generated class.
	 */
	public void addImplementedInterface(GeneratorTypeInfo type) {
		interfaces.add(type);
	}

	@Override
	public boolean hasMethodMatching(String name, boolean matchAbstract, GeneratorTypeInfo returnType, GeneratorTypeInfo... parameterTypes) {
		return GeneratorMethodInfo.checkForMethod(methods, name, matchAbstract, returnType, parameterTypes)
		|| superclass.hasMethodMatching(name, matchAbstract, returnType, parameterTypes);
	}
	
	@Override
	public GeneratorMethodInfo findMethodMatching(String name, GeneratorTypeInfo returnType, GeneratorTypeInfo... parameterTypes) {
		GeneratorMethodInfo method = GeneratorMethodInfo.findMethod(methods, name, returnType, parameterTypes);
		if(method != null)
			return method;
		return superclass.findMethodMatching(name, returnType, parameterTypes);
	}

	@Override
	public GeneratorMethodInfo findStaticMethodMatching(String name, GeneratorTypeInfo returnType, GeneratorTypeInfo... parameterTypes) {
		GeneratorMethodInfo method = GeneratorMethodInfo.findMethod(methods, name, returnType, parameterTypes);
		if(method != null)
			return method;
		return superclass.findStaticMethodMatching(name, returnType, parameterTypes);
	}

	@Override
	public boolean isAbstract() {
		return false;
	}

	@Override
	public boolean isEnum() {
		return false;
	}

	@Override
	public Set<String> getEnumMembers() throws IllegalStateException {
		return null;
	}

	@Override
	public JClassType getJClassType() {
		return null;
	}

	@Override
	public boolean isArray() {
		return false;
	}

	@Override
	public GeneratorTypeInfo getComponentType() {
		return null;
	}

	@Override
	public GeneratorTypeInfo getFieldType(String name, boolean allowProtected) {
		ExpressionInfo memberExpr = members.get(name);
		if(memberExpr != null)
			return memberExpr.getType();
		else
			return null;
	}

}