package com.habitsoft.kiyaa.views;

/**
 * Marker interface that indicates to the GeneratedHTMLViewGenerator
 * that it can pass the tag name used in the template to the constructor
 * of this class, and it'll potentially do something with it.
 * 
 * Implementors of this interface must provide a public constructor
 * taking two strings:
 * 
 * public TakesElementName(String tagName, String namespace) {
 * }
 * 
 * This feature is used by some classes to use a specific HTML tag
 * instead of DIV, such as WhenView being used to show/hide LI, H1, 
 * SPAN, etc.
 * 
 * This will not work for abstract classes created by GWT.create()
 * because GWT.create() doesn't accept any parameters to the
 * constructor for the object.
 */
public interface TakesElementName {

}
