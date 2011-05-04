/**
 * 
 */
package com.habitsoft.kiyaa.rebind.typeinfo;

import java.util.Set;

import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;


public interface GeneratorTypeInfo {
	/**
	 * Given an "expression", return info useful for getting/setting the value
	 * of that expression.  The assumption is that the returned expression(s)
	 * can be appended to an expression of this type with a "."
	 * 
	 * For example if this object represents an instance of class Foo with a public field bar,
	 * calling findAccessors("bar") on this object would return an expression info that
	 * has "bar" as the getter and "bar = (" as the setter.
	 * 
	 * The algorithm must prefer getters over direct field access, if the getter is present
	 * but MAY use direct field access for public fields.
	 * 
	 * @param prefix Prefix to use to locate this object's members and methods.  If not blank, 
	 *               it should end with a '.'.  If it is equal to "this." the implementation may
	 *               choose to include private fields and methods in the search. 
	 * @throws UnableToCompleteException 
	 */
	public ExpressionInfo findAccessors(String prefix, String expr) throws UnableToCompleteException;
	
	/**
	 * Return true if this type (or a superclass) implements the given interface or a subinterface
	 * of that interface.
	 * 
	 * If the target of this method is an interface, this would check whether the given interface
	 * was that interface or a superinterface.
	 * 
	 * @param iface Interface to look for
	 */
	public boolean implementsInterface(GeneratorTypeInfo iface);
	
	/**
	 * Return true if this type is a class, and subclass of the given class.
	 * 
	 * If the class is null (aka java.lang.Object) this returns true if this is not
	 * a primitive type.
	 * 
	 * @param possibleSuperclass Superclass to look for.
	 * @return True if this class is a subclass of a class with the given name
	 */
	public boolean isSubclassOf(GeneratorTypeInfo possibleSuperclass);
	
	/**
	 * Full class name, e.g. foo.bar.Baz
	 */
	public String getName();

	/**
	 * Full class name including type parameters, e.g. "java.lang.ArrayList<String>"
	 */
	public String getParameterizedQualifiedSourceName();

	/**
	 * Find a static method returning a type that is compatible with outType and
	 * which would accept the given parameter types, return true if one exists.
	 * 
	 * i.e. ReturnType result = method(parameterTypes...) would not be an error.
	 */
	public boolean hasStaticMethodMatching(String name, GeneratorTypeInfo returnType, GeneratorTypeInfo ... parameterTypes);

	/**
	 * Find a non-static non-abstract method returning a type that is compatible with outType and
	 * which would accept the given parameter types, return true if one exists.  A null parameter type
	 * or return type will be ignored for purposes of the search (although the number of parameters must still
	 * be a match).
	 * 
	 * i.e. ReturnType result = instance.method(parameterTypes...) would not be an error.
	 * @param matchAbstract TODO
	 */
	public boolean hasMethodMatching(String name, boolean matchAbstract, GeneratorTypeInfo returnType, GeneratorTypeInfo ... parameterTypes);

	/**
	 * Search for a method matching the given return type and parameter types.  A null parameter type
	 * or return type will be ignored for purposes of the search (although the number of parameters must still
	 * be a match).
	 * 
	 * Static methods are not returned by this method.
	 */
	public GeneratorMethodInfo findMethodMatching(String name, GeneratorTypeInfo returnType, GeneratorTypeInfo ... parameterTypes);

	/**
	 * Search for a method matching the given return type and parameter types.  A null parameter type
	 * or return type will be ignored for purposes of the search (although the number of parameters must still
	 * be a match).
	 */
	public GeneratorMethodInfo findStaticMethodMatching(String name, GeneratorTypeInfo returnType, GeneratorTypeInfo ... parameterTypes);
	
	/**
	 * @return true if this is a primitive type like int, long, void, char, short, float, double, etc..
	 */
	public boolean isPrimitive();
	
	/**
	 * @return true if this is a static class, false if it's an inner/nested class.
	 */
	public boolean isStatic();

	/**
	 * @return true if this is an abstract class or an interface
	 */
	public boolean isAbstract();
	
	/**
	 * If this a class nested inside another one, return the outer class.
	 */
	GeneratorTypeInfo getEnclosingType();

	/**
	 * Return the simple source name of the class.
	 * 
	 * For java.lang.Integer this would be "Integer".  For primitive types, this is the
	 * same as the type name.
	 */
	public String getSimpleSourceName();
	
	public int hashCode();
	public boolean equals(Object other);

	/**
	 * 
	 * @return true if this is an enum class
	 */
	public boolean isEnum();
	
	/**
	 * @return the identifiers of the enum values in this enum, if this is an enum
	 * @throws IllegalStateException If this is not an enum
	 */
	public Set<String> getEnumMembers() throws IllegalStateException;

	/**
	 * Return the nearest JClassType for this type, if possible.  Otherwise, return null.
	 * 
	 * For a generated class this would return the superclass which is a JClassType.  For a
	 * JClassType wrapper or JTypeWrapper this would return isClassOrInterface().
	 * 
	 * Other type metadata can probably return null safely.
	 */
	public JClassType getJClassType();

	/**
	 * Return true if this is an array type
	 */
	public boolean isArray();
	
	/**
	 * If this is an array, return the type of the elements of the array.
	 * 
	 * For example, for java.lang.String[] returns java.lang.String.
	 */
	public GeneratorTypeInfo getComponentType();

	/**
	 * Check for an accessible field and if it is found, return its type.
	 * 
	 * @param name Name of the field to check for
	 * @param allowProtected If true, return fields which are protected (normally only a public field would be returned)
	 * @return
	 */
	public GeneratorTypeInfo getFieldType(String name, boolean allowProtected);

}