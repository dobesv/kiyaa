package com.habitsoft.kiyaa.rebind;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JField;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JPrimitiveType;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.core.ext.typeinfo.NotFoundException;
import com.google.gwt.core.ext.typeinfo.TypeOracle;

public class MetadataGenerator extends BaseGenerator {

	public static class GeneratorInstance extends BaseGenerator.GeneratorInstance {
		protected JClassType modelType;
		protected String modelClassName;
		protected ArrayList<JClassType> referencedMetadata;
		protected JClassType metadataType;
		
		protected void addImports() {
			composerFactory.addImport("com.google.gwt.core.client.GWT");
			composerFactory.addImport("com.habitsoft.kiyaa.metamodel.*");
			composerFactory.addImport("com.habitsoft.kiyaa.metamodel.fields.*");
			composerFactory.addImport("com.habitsoft.kiyaa.metamodel.ActionContext");
			composerFactory.addImport("com.google.gwt.user.client.rpc.AsyncCallback");
			composerFactory.addImport(modelType.getQualifiedSourceName());
		}
	
		public void init() throws UnableToCompleteException {
			super.init();
			metadataType = baseType;
			referencedMetadata = new ArrayList<JClassType>();
			try {
				modelClassName = metadataType.getMetaData("kiyaa.modelClass")[0][0];
				modelType = types.findType(modelClassName);
			} catch(IndexOutOfBoundsException iobe) {
				modelType = metadataType.getEnclosingType();
				if(modelType == null) {
					logger.log(TreeLogger.WARN, "Metadata subclass "+metadataType.getName()+" doesn't specify @kiyaa.modelClass, and isn't a nested class", null);
					throw new UnableToCompleteException();
				}
				modelClassName = modelType.getSimpleSourceName();
			}
		}
		protected void generateClassBody() throws UnableToCompleteException {
			generateCtor();
			
			
			generateInitFields();
			
			generateIsCorrectType();
			
			generateGetTitle();
			generateGetName();
			generateNewInstance();
			generateMetadataGetters();
		}

		private void generateInitFields() throws UnableToCompleteException {
			sw.println("public void initFields() {");
			sw.indent();
			//sw.println("if(this.fields == null) {");
			//sw.indent();
			sw.println("this.fields = new Field[] {");
			sw.indent();
			generateFieldsBody(logger.branch(TreeLogger.TRACE, "Generating field metadata for "+modelType.getName(), null));
			sw.outdent();
			sw.println("};");
			//sw.outdent();
			//sw.println("}");
			//sw.println("return this.fields;");
			sw.outdent();
			sw.println("}");
		}
		
		protected void generateCtor() throws UnableToCompleteException {
			sw.println("public "+implName+"() {");
			sw.indent();
			sw.println("initFields();");
			sw.outdent();
			sw.println("}");
		}
		
		protected void generateSerializableField(
				final String name, String ctorParams, JType type) {
			sw.println("new SerializableField("+ctorParams+") {");
			sw.indent();
			sw.println("public void getDefaultValue(AsyncCallback callback) { callback.onSuccess(null); }");
			sw.println("public void getValue(Object model, AsyncCallback callback) {");
			sw.indentln("callback.onSuccess((("+modelClassName+")model).get"+capitalize(name)+"());");
			sw.println("}");
			sw.println("public void setValue(Object model, Object value, AsyncCallback callback) {");
			sw.indent();
			sw.println("try {");
			sw.indentln("(("+modelClassName+")model).set"+capitalize(name)+"(("+type.getQualifiedSourceName()+")value);");
			sw.indentln("callback.onSuccess(null);");
			sw.println("} catch(Throwable caught) { callback.onFailure(caught); }");
			sw.outdent();
			sw.println("}");
			sw.outdent();
			sw.println("},");
		}
		protected void generateEmbeddedModelField(
				final String name, String ctorParams, String fieldType, JClassType childMetaData) {
			sw.println("new EmbeddedModelField("+ctorParams+", get"+getSimpleClassName(childMetaData, "")+"()) {");
			sw.indent();
			sw.println("protected Object getValue(Object model) {");
			sw.indentln("return (("+modelClassName+")model).get"+capitalize(name)+"();");
			sw.println("}");
			sw.outdent();
			sw.println("},");
		}
	
		protected void generateFieldsBody(TreeLogger logger) throws UnableToCompleteException {
			final JField[] fields = modelType.getFields();
			for (int i = 0; i < fields.length; i++) {
				JField field = fields[i];
				if(field.isStatic() 
						|| field.isTransient()
						|| field.isFinal())
					continue;
				final JType fieldType = field.getType();
				final String name = field.getName();
				generateField(logger, field, fieldType, name);
			}
			
			/*
			JMethod[] methods = modelType.getMethods();
			for (int i = 0; i < methods.length; i++) {
				JMethod method = methods[i];
				String name;
				if(method.getName().startsWith("get"))
					name = uncapitalize(method.getName().substring(3));
				else if(method.getName().startsWith("is"))
					name = uncapitalize(method.getName().substring(2));
				else
					continue;
				// Don't process the same field twice
				if(modelType.getField(name) != null) {
					continue;
				}
				JField fakeField = new JField(modelType, name);
				fakeField.setType(method.getReturnType());
				generateField(logger, fakeField, method.getReturnType(), name);
			}
			*/
			
			// Collections
			generateCollections(logger);
		}

		private void generateCollections(TreeLogger logger)
				throws UnableToCompleteException {
			HashSet<String> collections = new HashSet<String>();
			String[][] modelCollections = modelType.getMetaData("kiyaa.collection");
			for (int i = 0; i < modelCollections.length; i++) {
				String[] collection = modelCollections[i];
				collections.add(collection[0]);
			}
			String[][] metadataCollections = metadataType.getMetaData("kiyaa.collection");
			for (int i = 0; i < metadataCollections.length; i++) {
				String[] collection = metadataCollections[i];
				collections.add(collection[0]);
			}
			for (String name : collections) {
				String label = getFieldMetaData(logger, metadataType, modelType, name, "kiyaa.label");
				if(label == null) label = name;
				JClassType childMetaData = getAndValidateChildMetadata(logger,
						metadataType, modelType, types, name);
				if(!referencedMetadata.contains(childMetaData))
					referencedMetadata.add(childMetaData);
				sw.println("new CollectionField(\""+escape(label)+"\", \""+name+"\", get"+getSimpleClassName(childMetaData, "")+"()),");
			}
		}

		private void generateField(TreeLogger logger, JField field,
				final JType fieldType, final String name)
				throws UnableToCompleteException {
			TreeLogger flogger = logger.branch(TreeLogger.TRACE, "Processing field "+name, null);
			String label = getFieldMetaData(logger, metadataType, modelType, field, "kiyaa.label");
			if(label == null) label = name;
			String ctorParams = "\""+escape(label)+"\", \""+name+"\"";
			String fieldMapping = getFieldMapping(flogger, metadataType, modelType, field);
			
			if("TextField".equals(fieldMapping)) {
				boolean password = isPasswordField(flogger, field, metadataType, modelType);
				boolean multiline = !password && isMultilineField(flogger, field, metadataType, modelType);
				generateField(name, ctorParams+(multiline?", true":""), (password?"Password":"")+"TextField", "String");
			} else if("ModelIdField".equals(fieldMapping)) {
				JClassType childMetaData = getAndValidateChildMetadata(flogger,
						metadataType, modelType, types, field);
				if(!referencedMetadata.contains(childMetaData))
					referencedMetadata.add(childMetaData);
				generateModelIdField(name, ctorParams, childMetaData);
			} else if("EmbeddedModelField".equals(fieldMapping)) {
				JClassType childMetaData = getAndValidateChildMetadata(flogger,
						metadataType, modelType, types, field);
				if(!referencedMetadata.contains(childMetaData))
					referencedMetadata.add(childMetaData);
				generateEmbeddedModelField(name, ctorParams, fieldType.getQualifiedSourceName(), childMetaData);
			} else if("SerializableField".equals(fieldMapping)) {
				generateSerializableField(name, ctorParams, fieldType);
			} else if(fieldMapping != null) {
				generateField(name, ctorParams, fieldMapping, field.getType().getQualifiedSourceName());
			} else {
				flogger.log(TreeLogger.WARN, "Type not supported yet: "+fieldType.getQualifiedSourceName(), null);
			}
		}
	
		private void generateGetName() {
			String name = getClassMetadata(metadataType, modelType, "kiyaa.name");
			if(name == null) name = modelType.getName();
			sw.println("public String getName() { return \""+escape(name)+"\"; }");
		}
	
		private void generateGetTitle() {
			String title = getClassMetadata(metadataType, modelType, "kiyaa.title");
			if(title == null) title = modelType.getName();
			sw.println("public String getTitle() { return \""+escape(title)+"\"; }");
		}
	
		protected void generateIsCorrectType() {
			sw.println("public boolean isCorrectType(Object x) {");
			sw.indentln("return x instanceof "+modelType.getName()+";");
			sw.println("}");
		}
	
		protected void generateMetadataGetters() {
			for (Iterator i = referencedMetadata.iterator(); i
					.hasNext();) {
				JClassType metadataType = (JClassType) i.next();
				String shortName = getSimpleClassName(metadataType, "");
				
				sw.println("public static "+metadataType.getQualifiedSourceName()+" get"+shortName+"() {");
				//sw.indentln("return AllMetadata.getInstance().get"+shortName+"();");
				//sw.indentln("return ("+shortName+") GWT.create("+shortName+".class);");
				sw.indentln("return "+metadataType.getQualifiedSourceName()+".getInstance();");
				sw.println("}");
			}
		}
	
		protected void generateModelIdField(
				final String name, String ctorParams, JClassType childMetaData) {
			generateField(name, ctorParams+", get"+getSimpleClassName(childMetaData, "")+"()", "ModelIdField", "Long");
		}
	
		protected void generateNewInstance() {
			sw.println("public void newInstance(final AsyncCallback callback) {");
			sw.indent();
			boolean haveSuperMethod;
			try {
				JMethod method = metadataType.getMethod("newInstance", new JType[] {types.getType("com.google.gwt.user.client.rpc.AsyncCallback")});
				if(method.isAbstract())
					throw new NotFoundException();
				sw.println("super.newInstance(new AsyncCallback() {");
				sw.indent();
				sw.println("public void onFailure(Throwable caught) { callback.onFailure(caught); }");
				sw.println("public void onSuccess(Object param) { ");
				sw.indent();
				sw.println(modelType.getName()+" result = ("+modelType.getName()+") param;");
				haveSuperMethod = true;
			} catch (NotFoundException caught) {
				JMethod[] overloads = metadataType.getOverloads("newInstance");
				if(overloads.length > 0 && !(overloads.length == 1 && overloads[0].isAbstract())) {
					TreeLogger overloadLogger = logger.branch(TreeLogger.WARN, "Found newInstance() overloads that don't match the expected signature", null);
					for (int i = 0; i < overloads.length; i++) {
						JMethod method = overloads[i];
						overloadLogger.log(TreeLogger.WARN, method.toString(), null);
					}
				}
				if(!modelType.isAbstract()) {
					sw.println(modelType.getName()+" result = new "+modelType.getName()+"();");
				} else {
					sw.println(modelType.getName()+" result = ("+modelType.getName()+") GWT.create("+modelType.getName()+".class);");
				}
				        
				haveSuperMethod = false;
			}
			JField[] fields = modelType.getFields();
			for (int i = 0; i < fields.length; i++) {
				JField field = fields[i];
				if(field.isFinal() || field.isStatic())
					continue;
				String defaultValue = getFieldMetaData(logger, metadataType, modelType, field, "kiyaa.default");
				if(defaultValue != null) {
					sw.println("result.set"+capitalize(field.getName())+"("+defaultValue+");");
				}
			}
			sw.println("callback.onSuccess(result);");
			sw.outdent();
			sw.println("}");
			
			if(haveSuperMethod) {
				sw.outdent();
				sw.println("});");
				sw.outdent();
				sw.println("}");
			}
		}
	
		protected void generateField(
			final String name, String ctorParams, String fieldClassName, String fieldTypeName) {
    		sw.println("new "+fieldClassName+"("+ctorParams+") {");
    		sw.indent();
    		sw.println("protected void setValue(Object model, "+fieldTypeName+" value) {");
    		sw.indentln("(("+modelClassName+")model).set"+capitalize(name)+"(value);");
    		sw.println("}");
    		sw.println("protected "+fieldTypeName+" getValue(Object model) {");
    		
    		sw.indentln("return (("+modelClassName+")model)."+(fieldTypeName.equals("boolean")?"is":"get")+capitalize(name)+"();");
    		sw.println("}");
    		sw.outdent();
    		sw.println("},");
    	}
		private JClassType getAndValidateChildMetadata(TreeLogger flogger,
				JClassType metadataType, JClassType modelType, TypeOracle types,
				JField field) throws UnableToCompleteException {
			String childMetaData = getFieldMetaData(flogger, metadataType, modelType, field, "kiyaa.childMetadata");
			if(childMetaData == null) {
				flogger.log(TreeLogger.ERROR, "ModelIdField "+field.getName()+" must set kiyaa.childMetadata", null);
				throw new UnableToCompleteException();
			}
			
			try {
				JClassType type = types.getType(childMetaData);
				if(!type.isAssignableTo(types.getType("com.habitsoft.kiyaa.metamodel.Metadata"))) {
					flogger.log(TreeLogger.ERROR, "ModelIdField "+field.getName()+" must set kiyaa.childMetadata to a subclass of Metadata", null);
					throw new UnableToCompleteException();
				}
				return type;
			} catch (NotFoundException caught) {
				flogger.log(TreeLogger.ERROR, "ModelIdField "+field.getName()+" must set kiyaa.childMetadata to a subclass of Metadata", caught);
				throw new UnableToCompleteException();
			}
		}
		private JClassType getAndValidateChildMetadata(TreeLogger flogger,
				JClassType metadataType, JClassType modelType, TypeOracle types,
				String fieldName) throws UnableToCompleteException {
			String childMetaData = getFieldMetaData(flogger, metadataType, modelType, fieldName, "kiyaa.childMetadata");
			if(childMetaData == null) {
				flogger.log(TreeLogger.ERROR, "ModelIdField "+fieldName+" must set kiyaa.childMetadata", null);
				throw new UnableToCompleteException();
			}
			
			try {
				JClassType type = types.getType(childMetaData);
				if(!type.isAssignableTo(types.getType("com.habitsoft.kiyaa.metamodel.Metadata"))) {
					flogger.log(TreeLogger.ERROR, "ModelIdField "+fieldName+" must set kiyaa.childMetadata to a subclass of Metadata", null);
					throw new UnableToCompleteException();
				}
				return type;
			} catch (NotFoundException caught) {
				flogger.log(TreeLogger.ERROR, "ModelIdField "+fieldName+" must set kiyaa.childMetadata to a subclass of Metadata", caught);
				throw new UnableToCompleteException();
			}
		}
	
		protected String getFieldMapping(TreeLogger logger, JClassType metadataType, JClassType modelType, JField field) {
			String mapping = getFieldMetaData(logger, metadataType, modelType, field, "kiyaa.mapping");
			if(mapping != null) return mapping;
			final JType fieldType = field.getType();
			if(fieldType instanceof JPrimitiveType) {
				if(fieldType.getSimpleSourceName().equals("boolean")) {
					return "BooleanField";
				} else if(fieldType.getSimpleSourceName().equals("int")) {
					return "IntegerField";
				} else if(fieldType.getSimpleSourceName().equals("long")) {
					return "LongField";
				} else if(fieldType.getSimpleSourceName().equals("double")) {
					return "DoubleField";
				} else {
					return null;
				}
			} else if(fieldType.getQualifiedSourceName().equals("java.lang.Long")) {
				return "IdField";
			} else if(fieldType.getQualifiedSourceName().equals("java.lang.String")) {
				return "TextField";
			} else if(fieldType.getQualifiedSourceName().equals("java.util.Date")) {
				return "DateField";
			} else if(fieldType.isArray() != null) {
				return "SerializableField";
			} else {
				return null;
			}
		}
	
		protected boolean isMultilineField(TreeLogger logger, JField field, JClassType metadataType, JClassType modelType) {
			return getFieldMetaData(logger, metadataType, modelType, field, "kiyaa.multiline") != null;
		}
		protected boolean isPasswordField(TreeLogger logger, JField field, JClassType metadataType, JClassType modelType) {
			return getFieldMetaData(logger, metadataType, modelType, field, "kiyaa.password") != null;
		}
	
	}
	
	protected GeneratorInstance createGeneratorInstance() {
		return new GeneratorInstance();
	}
}