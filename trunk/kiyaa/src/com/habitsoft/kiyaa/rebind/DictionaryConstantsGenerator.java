package com.habitsoft.kiyaa.rebind;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.TreeLogger.Type;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JPrimitiveType;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.user.rebind.rpc.SerializableTypeOracle;
import com.google.gwt.user.rebind.rpc.SerializableTypeOracleBuilder;
import com.google.gwt.user.rebind.rpc.TypeSerializerCreator;
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
            HashSet<JType> serializedTypes = new HashSet<JType>();
		    
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
                boolean serializedObject=false;
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
                    serializedObject = true;
                }
                
                if(serializedObject) {
                    sw.println("private native com.google.gwt.core.client.JavaScriptObject _"+method.getName()+"() /*-{");
                    sw.indent();
                    sw.println("var dict = $wnd[\""+escape(dictionaryName)+"\"];");
                    sw.println("var value = dict[\""+escape(key)+"\"];");
                    if(defaultValueExpr != null) {
                        sw.println("if(dict == undefined || dict[\""+escape(key)+"\"] == undefined) return "+defaultValueExpr+";");
                    }
                    sw.println("return value;");
                    sw.outdent();
                    sw.println("}-*/;");
                    sw.println("public "+returnType.getQualifiedSourceName()+" "+method.getName()+"() {");
                    sw.indent();
                    sw.println("try {");
                    sw.indent();
                    sw.println("return ("+returnType.getQualifiedSourceName()+") createStreamReader(_"+method.getName()+"()).readObject();");
                    sw.outdent();
                    sw.println("} catch(com.google.gwt.user.client.rpc.SerializationException e) {");
                    sw.indentln("throw new Error(e);");
                    sw.println("}");
                    sw.outdent();
                    sw.println("};");
                    
                    serializedTypes.add(returnType);
                } else {
                    sw.println("public native "+returnType.getQualifiedSourceName()+" "+method.getName()+"() /*-{");
                    sw.indent();
                    sw.println("var dict = $wnd[\""+escape(dictionaryName)+"\"];");
                    if(defaultValueExpr != null) {
                        sw.println("if(dict == undefined || dict[\""+escape(key)+"\"] == undefined) return "+defaultValueExpr+";");
                    }
                    sw.println("var value = dict[\""+escape(key)+"\"];");
                    sw.println("return value;");
                    sw.outdent();
                    sw.println("}-*/;");
                }
                
		    }
		    
		    if(!serializedTypes.isEmpty()) {
		        final TreeLogger tempLogger = createFilteredLogger(logger.branch(TreeLogger.DEBUG, "Generating serialization code"), TreeLogger.ERROR);
                SerializableTypeOracleBuilder serializerBuilder = new SerializableTypeOracleBuilder(tempLogger, context.getPropertyOracle(), context.getTypeOracle());
    		    for(JType cls : serializedTypes) {
                    serializerBuilder.addRootType(tempLogger, cls);
    		    }
    		    SerializableTypeOracle serializableTypeOracle = serializerBuilder.build(tempLogger);
    		    TypeSerializerCreator typeSerializerCreator = new TypeSerializerCreator(tempLogger, serializableTypeOracle, context, baseType);
                typeSerializerCreator.realize(tempLogger);
                sw.println("com.google.gwt.user.client.rpc.impl.Serializer serializer = new "+baseType.getQualifiedSourceName()+"_TypeSerializer();");
                sw.println("public com.habitsoft.kiyaa.util.DictionaryConstantsSerializationStreamReader createStreamReader(com.google.gwt.core.client.JavaScriptObject encoded)");
                sw.println("throws com.google.gwt.user.client.rpc.SerializationException {");
                sw.indent();
                sw.println("com.habitsoft.kiyaa.util.DictionaryConstantsSerializationStreamReader reader = new com.habitsoft.kiyaa.util.DictionaryConstantsSerializationStreamReader(serializer);");
                sw.println("reader.prepareToRead(encoded);");
                sw.println("return reader;");
                sw.outdent();
                sw.println("}");
		    }
		}

		/**
		 * Create a logger which filters logs below the given level.
		 */
        private TreeLogger createFilteredLogger(final TreeLogger delegateLogger, final Type minType) {
            return new TreeLogger() {
                @Override
                public void log(Type type, String msg, Throwable caught, HelpInfo helpInfo) {
                    if(isLoggable(type))
                        delegateLogger.log(type, msg, caught, helpInfo);
                }
                @Override
                public boolean isLoggable(Type type) {
                    return !type.isLowerPriorityThan(minType);
                }
                @Override
                public TreeLogger branch(Type type, String msg, Throwable caught, HelpInfo helpInfo) {
                    return createFilteredLogger(delegateLogger.branch(type, msg, caught, helpInfo), minType);
                }
            };
        }
	}
}
