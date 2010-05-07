package com.habitsoft.kiyaa.util;

import java.lang.annotation.Retention;

/**
 * When generating an HTML view from an element with child elements, the template
 * generator looks for methods like addX() or setX() that takes parameters 
 * matching the attributes of each child element. 
 * 
 * For example:
 * 
 * <myComplexView>
 *    <foo a="1" b="bar"/> <!-- Normal method call during initialization -->
 *    <bar quux="${someStringExpr}>
 *      <!-- Inline generated sub-view would call addBar(String quux, View v), setBar(String quux, View v), setBar(String quux, ViewFactory vf)-->
 *    	<ui:label>baz</ui:label>
 *    </bar>
 * </myComplexView>
 *
 * The attributes on the child element must match the names of the parameters of the associated method
 * or the method won't be matched and the element will be discarded.
 * 
 * Normally we get the parameter name from the GWT type information system in the generator, but sometimes
 * the parameter names are missing due to some issues in GWT.  See GWT Issue #4913
 * 
 * Also, you may just want to use different method/parameter names for this purpose, so the source code and
 * the template are able to differ.
 * 
 * This annotation can be applied to a method or an attribute to indicate its name for the purpose of generating
 * subviews as illustrated above.
 */
public @interface Name {
	String value();
}
