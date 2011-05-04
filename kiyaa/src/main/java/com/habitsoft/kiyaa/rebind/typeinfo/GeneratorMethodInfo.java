package com.habitsoft.kiyaa.rebind.typeinfo;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.HashMap;

public class GeneratorMethodInfo {
	private final String name;
	private final GeneratorTypeInfo returnType;
	private final GeneratorTypeInfo[] parameterTypes;
	private final String[] parameterNames;
	private final GeneratorTypeInfo asyncReturnType;
	
	public GeneratorMethodInfo(String name, GeneratorTypeInfo returnType, GeneratorTypeInfo[] parameterTypes, String[] parameterNames, GeneratorTypeInfo asyncReturnType) {
		super();
		this.name = name;
		this.returnType = returnType;
		this.parameterTypes = parameterTypes;
		this.parameterNames = parameterNames;
		this.asyncReturnType = asyncReturnType;
	}
	public String getName() {
		return name;
	}
	public GeneratorTypeInfo getReturnType() {
		return returnType;
	}
	public GeneratorTypeInfo[] getParameterTypes() {
		return parameterTypes;
	}
	
	/**
	 * Get parameter names, if available.  Returns null if parameter names
	 * are not known.
	 */
	public String[] getParameterNames() {
		return parameterNames;
	}
	
	/**
	 * Verify that this method matches the given return type and parameter types.
	 * 
	 * Note that null parameter types or return types are treated as a wildcard and not checked.
	 * 
	 * @param returnType Expected return type of the method
	 * @param parameterTypes Passed parameter types of the method
	 * @return true if the return type and parameter types match the given expectations
	 */
	public boolean matchesSignature(GeneratorTypeInfo returnType, GeneratorTypeInfo[] parameterTypes) {
		if(returnType != null && !ExpressionInfo.directlyAssignable(returnType, null, this.returnType))
			return false;
		if(parameterTypes.length != this.parameterTypes.length)
			return false;
		for(int i=0; i < parameterTypes.length; i++) {
			if(parameterTypes[i] != null && !
					(ExpressionInfo.directlyAssignable(this.parameterTypes[i], null, parameterTypes[i])
					 || ExpressionInfo.castable(this.parameterTypes[i], null, parameterTypes[i])))
				return false;
		}
		return true;
	}
	public static boolean checkForMethod(HashMap<String, ArrayList<GeneratorMethodInfo>> methods, String methodName,
			boolean matchAbstract, GeneratorTypeInfo desiredReturnType, GeneratorTypeInfo[] desiredParameterTypes) {
		ArrayList<GeneratorMethodInfo> overloads = methods.get(methodName);
		if(overloads == null)
			return false;
		for(GeneratorMethodInfo method : overloads) {
			if(!matchAbstract && method.isAbstract())
				continue;
			if(method.matchesSignature(desiredReturnType, desiredParameterTypes))
				return true;
		}
		return false;
	}
	
	/**
	 * Return true if this method has the "abstract" modifier or it is
	 * declared in an interface rather than a class.
	 */
	public boolean isAbstract() {
		return false;
	}
	public static GeneratorMethodInfo findMethod(HashMap<String, ArrayList<GeneratorMethodInfo>> methods, String methodName,
			GeneratorTypeInfo desiredReturnType, GeneratorTypeInfo[] desiredParameterTypes) {
		ArrayList<GeneratorMethodInfo> overloads = methods.get(methodName);
		if(overloads == null)
			return null;
		for(GeneratorMethodInfo method : overloads) {
			if(method.matchesSignature(desiredReturnType, desiredParameterTypes))
				return method;
		}
		return null;
	}
	public GeneratorTypeInfo getAsyncReturnType() {
		return asyncReturnType;
	}
	
	/**
	 * Look for a method annotation
	 */
	public <T extends Annotation> T getAnnotation(Class<T> annotationToLookFor) {
		return null;
	}
	
}