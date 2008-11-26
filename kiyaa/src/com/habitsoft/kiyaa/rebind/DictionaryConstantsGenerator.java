package com.habitsoft.kiyaa.rebind;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JPrimitiveType;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.i18n.client.Dictionary;
import com.habitsoft.kiyaa.util.DictionaryConstants;

/**
 * Generate a concrete implementation of a given DictionaryConstants
 * interface.
 */
public class DictionaryConstantsGenerator extends BaseGenerator {

	@Override
	protected GeneratorInstance createGeneratorInstance() {
		return new DictionaryConstantsGeneratorInstance();
	}

	class DictionaryConstantsGeneratorInstance extends GeneratorInstance {

		Set<String> doneTypes = new TreeSet<String>();
		List<JClassType> beanTypes = new ArrayList<JClassType>();
		
		@Override
		protected void setupSuperclass() {
		    composerFactory.setSuperclass(Dictionary.class.getName());
		}
		
		@Override
		protected void generateClassBody() throws UnableToCompleteException {
		    String dictionaryName = uncapitalize(baseType.getSimpleSourceName());
		    DictionaryConstants.Dictionary dictAnnotation = baseType.getAnnotation(DictionaryConstants.Dictionary.class);
		    if(dictAnnotation != null) dictionaryName = dictAnnotation.value();
            sw.println("private dict = Dictionary.getDictionary(\""+escape(dictionaryName)+"\");");
		    
		    JMethod[] methods = baseType.getMethods();
		    for(JMethod method : methods) {
                if(method.getParameters().length != 0) {
                    logger.log(TreeLogger.ERROR, "DictionaryConstants methods should not take any parameters: "+method.getName());
                    throw new UnableToCompleteException();
                }
		        String key = method.getName();
		        DictionaryConstants.Key keyAnnotation = method.getAnnotation(DictionaryConstants.Key.class);
                if(keyAnnotation != null) {
                    key = keyAnnotation.value();
                }
                JType returnType = method.getReturnType();
                String expr;
                String defaultValueExpr;
                if(JPrimitiveType.BOOLEAN.equals(returnType)) {
                    expr = "Boolean.parseBoolean(value)";
                    DictionaryConstants.DefaultBooleanValue dflt = method.getAnnotation(DictionaryConstants.DefaultBooleanValue.class);
                    defaultValueExpr = dflt != null?String.valueOf(dflt.value()):null;
                } else if(JPrimitiveType.DOUBLE.equals(returnType)) {
                    expr = "Double.parseDouble(value)";
                    DictionaryConstants.DefaultDoubleValue dflt = method.getAnnotation(DictionaryConstants.DefaultDoubleValue.class);
                    defaultValueExpr = dflt != null?String.valueOf(dflt.value())+"L":null;
                } else if(JPrimitiveType.FLOAT.equals(returnType)) {
                    expr = "Float.parseFloat(value)";
                    DictionaryConstants.DefaultFloatValue dflt = method.getAnnotation(DictionaryConstants.DefaultFloatValue.class);
                    defaultValueExpr = dflt != null?String.valueOf(dflt.value())+"f":null;
                } else if(JPrimitiveType.INT.equals(returnType)) {
                    expr = "Integer.parseInt(value)";
                    DictionaryConstants.DefaultIntValue dflt = method.getAnnotation(DictionaryConstants.DefaultIntValue.class);
                    defaultValueExpr = dflt != null?String.valueOf(dflt.value()):null;
                } else if("java.lang.String".equals(returnType.getQualifiedSourceName())) {
                    expr = "value";
                    DictionaryConstants.DefaultStringValue dflt = method.getAnnotation(DictionaryConstants.DefaultStringValue.class);
                    defaultValueExpr = dflt != null?'"'+escape(dflt.value())+'"':null;
                } else {
                    logger.log(TreeLogger.ERROR, "DictionaryConstants does not support type: "+returnType.getParameterizedQualifiedSourceName());
                    throw new UnableToCompleteException();
                }
                sw.println("public "+returnType.getQualifiedSourceName()+" "+method.getName()+"() {");
                sw.indent();
                if(defaultValueExpr != null) {
                    sw.println("try {");
                    sw.indent();
                }
                sw.println("String value = dict.get(\""+escape(key)+"\");");
                if(defaultValueExpr != null) {
                    // TODO Each time Dictionary throws this MissingResourceException it also does a bunch of other work we'd rather avoid ... Hrm.
                    sw.println("} catch(java.util.MissingResourceException mre) { return "+defaultValueExpr+"; }");
                }
                sw.println("return "+expr+";");
                sw.outdent();
                sw.println("}");
		    }
		}
	}
}
