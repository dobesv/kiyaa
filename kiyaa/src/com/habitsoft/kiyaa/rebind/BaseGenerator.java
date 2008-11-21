package com.habitsoft.kiyaa.rebind;

import java.io.PrintWriter;
import java.util.ArrayList;

import com.google.gwt.core.ext.Generator;
import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JField;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.core.ext.typeinfo.NotFoundException;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.user.rebind.ClassSourceFileComposerFactory;
import com.google.gwt.user.rebind.SourceWriter;

public abstract class BaseGenerator extends Generator {

	public BaseGenerator() {
		super();
	}

	/**
	 * Return a "simple" class name - for top-level classes, this is
	 * the SimpleSourceName() - the name with the package and generics
	 * removed.
	 * 
	 * For nested classes this prepends the parent class(es) name to
	 * the simple name of the nested class (with the given seperator).
	 * 
	 * @param nestedClassSeperator Seperator to use for nested classes
	 */
	protected static String getSimpleClassName(final JClassType type, String nestedClassSeperator) {
		String clsName = type.getSimpleSourceName();
		if (type.getEnclosingType() != null) {
			clsName = getSimpleClassName(type.getEnclosingType(), nestedClassSeperator)+nestedClassSeperator+clsName;
		}
		return clsName;
	}

	public static String joinWithSpaces(int startIndex, String[] strings) {
		StringBuffer result = new StringBuffer();
		for (int i = startIndex; i < strings.length; i++) {
			String string = strings[i];
			if (i > startIndex)
				result.append(' ');
			result.append(string);
		}
		return result.toString();
	}

	public static String joinWithCommas(int startIndex, String[] strings) {
		StringBuffer result = new StringBuffer();
		for (int i = startIndex; i < strings.length; i++) {
			String string = strings[i];
			if (i > startIndex)
				result.append(", ");
			result.append(string);
		}
		return result.toString();
	}

	@Override
	public String generate(TreeLogger logger, GeneratorContext context,
			String className) throws UnableToCompleteException {

		long start = System.currentTimeMillis();
		TypeOracle types = context.getTypeOracle();
		final JClassType baseType = types.findType(className);
		String clsName = getSimpleClassName(baseType, "");
		String implName = clsName + "Impl";
		final String packageName = baseType.getPackage().getName();

		PrintWriter printWriter = context.tryCreate(logger, baseType
				.getPackage().getName(), implName);
		if (printWriter != null) {
			GeneratorInstance instance = createGeneratorInstance();
			instance.logger = logger;
			instance.context = context;
			instance.className = className;
			instance.types = types;
			instance.baseType = baseType;
			instance.implName = implName;
			instance.packageName = packageName;
			instance.printWriter = printWriter;
			try {
				instance.generateClass();
			} catch(UnableToCompleteException uatc) {
				logger.log(TreeLogger.ERROR, "Failed to generate "+className, uatc.getCause());
				throw uatc;
			} catch(Throwable t) {
				logger.log(TreeLogger.ERROR, "Failed to generate "+className, t);
				throw new UnableToCompleteException();
			}
		}
		long end = System.currentTimeMillis();
		if(end-start > 150) {
			System.out.println("Took "+(end-start)+"ms to generate "+implName);
		}
		return packageName + "." + implName;
	}
	
	protected abstract GeneratorInstance createGeneratorInstance();

	public abstract static class GeneratorInstance {
		public TreeLogger logger;
		ArrayList<TreeLogger> pushedLoggers = new ArrayList();
		public GeneratorContext context;
		public String className;
		public TypeOracle types;
		public JClassType baseType;
		public String implName;
		public String packageName;
		public PrintWriter printWriter;
		public ClassSourceFileComposerFactory composerFactory;
		public SourceWriter sw;
		
		protected void addImports() {
			if(!baseType.isMemberType())
				composerFactory.addImport(className);
			else
				composerFactory.addImport(baseType.getEnclosingType().getQualifiedSourceName());
		}
		
		void pushLogger(String message) {
			pushedLoggers.add(logger);
			logger = logger.branch(TreeLogger.TRACE, message, null);
			// System.out.println(message);
		}
		void popLogger() {
			logger = pushedLoggers.remove(pushedLoggers.size()-1);
		}
		protected abstract void generateClassBody() throws UnableToCompleteException;

		public void init() throws UnableToCompleteException {
			composerFactory = new ClassSourceFileComposerFactory(
					packageName, implName);
			composerFactory.setSuperclass(baseType.getName());
		}
		public void generateClass()
				throws UnableToCompleteException {

			init();
			
			addImports();
			
			sw = composerFactory.createSourceWriter(context, printWriter);

			sw.indent();

			try {
				pushLogger("Generate class body of "+implName);
				generateClassBody();
			} finally {
				popLogger();
			}

			sw.outdent();

			
			sw.commit(logger);
		}
		
		protected String getClassMetadata(JClassType metadataType,
				JClassType modelType, String tagName) {
			String[][] metadataFields = metadataType.getMetaData(tagName);
			for (int i = 0; i < metadataFields.length; i++) {
				String[] metadataField = metadataFields[i];
				return joinWithSpaces(0, metadataField);
			}
			String[][] modelFields = modelType.getMetaData(tagName);
			for (int i = 0; i < modelFields.length; i++) {
				String[] modelField = modelFields[i];
				return joinWithSpaces(0, modelField);
			}
			return null;
		}

		protected String getClassMetadata(JClassType cls, String tagName) {
			String[][] metadataFields = cls.getMetaData(tagName);
			for (int i = 0; i < metadataFields.length; i++) {
				String[] metadataField = metadataFields[i];
				return joinWithSpaces(0, metadataField);
			}
			return null;
		}

		/**
		 * Allow a piece of metadata to be specified on the field, the class (with
		 * the first parameter equal to the field name), the metaclass (with the
		 * first parameter equal to the field name), or the getter.
		 */
		protected String getFieldMetaData(TreeLogger logger,
				JClassType metadataType, JClassType modelType, JField field,
				String tagName) {
			String[][] tagFields = field.getMetaData(tagName);
			if (tagFields.length != 0) {
				String[] tagField = tagFields[0];
				return joinWithSpaces(0, tagField);
			}
			final String fieldName = field.getName();
			return getFieldMetaData(logger, metadataType, modelType, fieldName,
					tagName);
		}

		protected String getFieldMetaData(TreeLogger logger,
				JClassType metadataType, JClassType modelType,
				final String fieldName, String tagName) {
			if(metadataType != null) {
				String[][] metadataFields = metadataType.getMetaData(tagName);
				for (int i = 0; i < metadataFields.length; i++) {
					String[] metadataField = metadataFields[i];
					if (metadataField.length < 2)
						continue;
					if (metadataField[0].equals(fieldName)) {
						return joinWithSpaces(1, metadataField);
					}
				}
			}
			if(modelType != null) {
				String[][] modelFields = modelType.getMetaData(tagName);
				for (int i = 0; i < modelFields.length; i++) {
					String[] modelField = modelFields[i];
					if (modelField.length < 2)
						continue;
					if (modelField[0].equals(fieldName)) {
						return joinWithSpaces(1, modelField);
					}
				}
				try {
					JMethod method;
					try {
						method = modelType.getMethod("get"
								+ capitalize(fieldName), new JType[] {});
					} catch (NotFoundException caught) {
						method = modelType.getMethod(
								"is" + capitalize(fieldName), new JType[] {});
					}
					String[][] methodFields = method.getMetaData(tagName);
					if (methodFields.length != 0) {
						String[] methodField = methodFields[0];
						return joinWithSpaces(0, methodField);
					}
				} catch (NotFoundException caught) {
					//logger.log(TreeLogger.WARN, "Warning: no getter found for "
					//		+ fieldName, new Exception());
				}
			}
			return null;
		}

		/**
		 * Allow method metadata to be attached to the method, the class, or the
		 * metadata class.
		 * 
		 * When not specified on the method, the first string after the tag is the
		 * name of the method.
		 */
		protected String getMethodMetaData(TreeLogger logger,
				JClassType metadataType, JClassType modelType, JMethod method,
				String tagName) {
			String[][] tagFields = method.getMetaData(tagName);
			if (tagFields.length != 0) {
				String[] tagField = tagFields[0];
				return joinWithSpaces(0, tagField);
			}
			String[][] metadataFields = metadataType.getMetaData(tagName);
			for (int i = 0; i < metadataFields.length; i++) {
				String[] metadataField = metadataFields[i];
				if (metadataField.length < 2)
					continue;
				if (metadataField[0].equals(method.getName())) {
					return joinWithSpaces(1, metadataField);
				}
			}
			String[][] modelFields = modelType.getMetaData(tagName);
			for (int i = 0; i < modelFields.length; i++) {
				String[] modelField = modelFields[i];
				if (modelField.length < 2)
					continue;
				if (modelField[0].equals(method.getName())) {
					return joinWithSpaces(1, modelField);
				}
			}
			return null;
		}

		protected String capitalize(String name) {
			if(name.length() == 0) return name;
			return Character.toUpperCase(name.charAt(0)) + name.substring(1);
		}
		protected String uncapitalize(String name) {
			if(name.length() == 0) return name;
			return Character.toLowerCase(name.charAt(0)) + name.substring(1);
		}

//		private final HashMap<String, JClassType> typeCache = new HashMap<String, JClassType>();
		protected JClassType getType(String name) throws UnableToCompleteException {
			
			try {
		        return types.getType(name);
		        
//				JClassType result = typeCache.get(name);
//				if(result == null) {
//					result = types.getType(name);
//					typeCache.put(name, result);
//				}
//				return result;
			} catch (NotFoundException caught) {
	            // Check for a nested class
	            JClassType nestedType = baseType.findNestedType(name);
	            if(nestedType != null)
	                return nestedType;
	            
	            // Check for a class in the same package
	            JClassType packageType = baseType.getPackage().findType(name);
	            if(packageType != null)
	                return packageType;
	            
				//logger.log(TreeLogger.ERROR, "Missing class "+name, caught);
				final UnableToCompleteException unableToCompleteException = new UnableToCompleteException();
				unableToCompleteException.initCause(caught);
				throw unableToCompleteException;
			}
		}
		
	}


}