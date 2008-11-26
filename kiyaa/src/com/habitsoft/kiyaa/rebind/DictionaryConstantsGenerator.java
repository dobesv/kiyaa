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
		protected void generateClassBody() throws UnableToCompleteException {
		    String dictionaryName = uncapitalize(baseType.getSimpleSourceName());
		    DictionaryConstants.Dictionary dictAnnotation = baseType.getAnnotation(DictionaryConstants.Dictionary.class);
		    if(dictAnnotation != null) dictionaryName = dictAnnotation.value();
		    
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
                String defaultValueExpr;
                if(JPrimitiveType.BOOLEAN.equals(returnType)) {
                    DictionaryConstants.DefaultBooleanValue dflt = method.getAnnotation(DictionaryConstants.DefaultBooleanValue.class);
                    defaultValueExpr = dflt != null?String.valueOf(dflt.value()):null;
                } else if(JPrimitiveType.DOUBLE.equals(returnType)) {
                    DictionaryConstants.DefaultDoubleValue dflt = method.getAnnotation(DictionaryConstants.DefaultDoubleValue.class);
                    defaultValueExpr = dflt != null?String.valueOf(dflt.value())+"L":null;
                } else if(JPrimitiveType.FLOAT.equals(returnType)) {
                    DictionaryConstants.DefaultFloatValue dflt = method.getAnnotation(DictionaryConstants.DefaultFloatValue.class);
                    defaultValueExpr = dflt != null?String.valueOf(dflt.value())+"f":null;
                } else if(JPrimitiveType.INT.equals(returnType)) {
                    DictionaryConstants.DefaultIntValue dflt = method.getAnnotation(DictionaryConstants.DefaultIntValue.class);
                    defaultValueExpr = dflt != null?String.valueOf(dflt.value()):null;
                } else if("java.lang.String".equals(returnType.getQualifiedSourceName())) {
                    DictionaryConstants.DefaultStringValue dflt = method.getAnnotation(DictionaryConstants.DefaultStringValue.class);
                    defaultValueExpr = dflt != null?'"'+escape(dflt.value())+'"':null;
                } else {
                    defaultValueExpr = "null";
                }
                sw.println("public native "+returnType.getQualifiedSourceName()+" "+method.getName()+"() /*-{");
                sw.indent();
                sw.println("var value = $wnd[\""+dictionaryName+"\"][\""+escape(key)+"\"];");
                if(defaultValueExpr != null) {
                    sw.println("if(value == undefined) return "+defaultValueExpr+";");
                }
                sw.println("return value;");
                sw.outdent();
                sw.println("}-*/;");
		    }
		}
	}
}
