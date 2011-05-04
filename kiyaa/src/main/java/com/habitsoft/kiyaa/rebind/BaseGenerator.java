package com.habitsoft.kiyaa.rebind;

import java.io.PrintWriter;

import com.google.gwt.core.ext.Generator;
import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JParameter;
import com.google.gwt.core.ext.typeinfo.NotFoundException;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.user.rebind.ClassSourceFileComposerFactory;
import com.google.gwt.user.rebind.SourceWriter;
import com.habitsoft.kiyaa.rebind.typeinfo.CommonTypes;
import com.habitsoft.kiyaa.rebind.typeinfo.GeneratorTypeInfo;
import com.habitsoft.kiyaa.rebind.typeinfo.JClassTypeWrapper;
import com.habitsoft.kiyaa.util.Name;

public abstract class BaseGenerator extends Generator {
	protected final static LocalTreeLogger logger = LocalTreeLogger.logger;
	
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

	/**
	 * Get a parameter name, taking into account the @Name annotation, if present.
	 */
	public static String getParameterName(JParameter parameter) {
		Name parameterNameAnnotation = parameter.getAnnotation(Name.class);
		String parameterName = parameterNameAnnotation!=null?parameterNameAnnotation.value():parameter.getName();
		return parameterName;
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

		LocalTreeLogger.pushLogger(logger);
		try {
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
				instance.context = context;
				instance.className = className;
				instance.types = types;
				instance.baseType = baseType;
				instance.implName = implName;
				instance.packageName = packageName;
				instance.printWriter = printWriter;
				instance.commonTypes = new CommonTypes(types);
				
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
		} finally {
			LocalTreeLogger.popLogger();
		}
	}
	
	protected abstract GeneratorInstance createGeneratorInstance();

	public abstract static class GeneratorInstance {
		public CommonTypes commonTypes;
		public GeneratorContext context;
		public String className;
		public TypeOracle types;
		public JClassType baseType;
		public String implName;
		public String packageName;
		public PrintWriter printWriter;
		public ClassSourceFileComposerFactory composerFactory;
		public SourceWriter sw;
		
		/**
		 * Override this to add your own imports.  To add the import,
		 * call:
		 * 
		 * <code>composerFactory.addImport(className);</code>
		 * 
		 */
		protected void addImports() {
			if(!baseType.isMemberType())
				composerFactory.addImport(className);
			else
				composerFactory.addImport(baseType.getEnclosingType().getQualifiedSourceName());
		}
		
		public void addImport(String typeName) {
            composerFactory.addImport(typeName);
        }

        void pushLogger(String message) {
        	LocalTreeLogger.pushLogger(LocalTreeLogger.logger.branch(TreeLogger.TRACE, message, null));
		}
		void popLogger() {
			LocalTreeLogger.popLogger();
		}
		protected abstract void generateClassBody() throws UnableToCompleteException;

		public void init() throws UnableToCompleteException {
			composerFactory = new ClassSourceFileComposerFactory(
					packageName, implName);
			setupSuperclass();
			setupImplementedInterfaces();
		}

        /**
         * Subclasses can override this to replace the default
         * interfaces (the base type, if it's an interface) with
         * their own superclass.  The method should call:
         * 
         * composerFactory.addImplementedInterface("inter.face.Name");
         * 
         */
		protected void setupImplementedInterfaces() {
		    if(baseType.isInterface() != null)
		        composerFactory.addImplementedInterface(baseType.getName());
        }

        /**
		 * Subclasses can override this to replace the default
		 * superclass (the base type, if it's a class) with
		 * their own superclass.  The method should call:
		 * 
		 * composerFactory.setSuperclass("super.class.Name");
		 * 
		 */
        protected void setupSuperclass() {
            if(baseType.isInterface() == null)
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

			sw.commit(LocalTreeLogger.logger);
		}

		protected static String capitalize(String name) {
			if(name.length() == 0) return name;
			return Character.toUpperCase(name.charAt(0)) + name.substring(1);
		}
		protected static String uncapitalize(String name) {
			if(name.length() == 0) return name;
			return Character.toLowerCase(name.charAt(0)) + name.substring(1);
		}

//		private final HashMap<String, JClassType> typeCache = new HashMap<String, JClassType>();
		protected GeneratorTypeInfo getType(String name) throws UnableToCompleteException {
			
			try {
		        return JClassTypeWrapper.wrap(types.getType(name));
		        
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
	                return JClassTypeWrapper.wrap(nestedType);
	            
	            // Check for a class in the same package
	            JClassType packageType = baseType.getPackage().findType(name);
	            if(packageType != null)
	                return JClassTypeWrapper.wrap(packageType);
	            
				//logger.log(TreeLogger.ERROR, "Missing class "+name, caught);
				final UnableToCompleteException unableToCompleteException = new UnableToCompleteException();
				unableToCompleteException.initCause(caught);
				throw unableToCompleteException;
			}
		}
		
	}


}