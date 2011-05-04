/**
 * 
 */
package com.habitsoft.kiyaa.rebind.typeinfo;



public class GeneratedInnerClassInfo extends GeneratedClassInfo {
	final GeneratorTypeInfo outerClass;
	final boolean staticClass;
	
	public GeneratedInnerClassInfo(String name, GeneratorTypeInfo outerClass, JClassTypeWrapper superclass, boolean staticClass) {
		super(name, superclass);
		this.outerClass = outerClass;
		this.staticClass = staticClass;
	}
	
	@Override
	public boolean isStatic() {
		return staticClass;
	}
	
	@Override
	public GeneratorTypeInfo getEnclosingType() {
		return outerClass;
	}
}