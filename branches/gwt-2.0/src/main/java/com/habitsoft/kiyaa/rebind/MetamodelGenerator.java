package com.habitsoft.kiyaa.rebind;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;

import com.google.gwt.core.ext.Generator;
import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JField;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.user.rebind.ClassSourceFileComposerFactory;
import com.google.gwt.user.rebind.SourceWriter;

public class MetamodelGenerator extends Generator {

	public String generate(TreeLogger logger, GeneratorContext context,
			String className) throws UnableToCompleteException {
		TypeOracle types = context.getTypeOracle();
		final JClassType metadataType = types.findType(className);
		String implName = metadataType.getName() + "Impl";
		final String packageName = metadataType.getPackage().getName();
		PrintWriter printWriter = context.tryCreate(logger, metadataType.getPackage().getName(),
				implName);
		if (printWriter != null) {
			generateClass(logger, context, className, types, metadataType,
					implName, packageName, printWriter);
		}
		return packageName + "." + implName;
	}
	private void generateClass(TreeLogger logger, GeneratorContext context,
			String className, TypeOracle types, final JClassType metadataType,
			String implName, final String packageName, PrintWriter printWriter) {
		ClassSourceFileComposerFactory composerFactory = new ClassSourceFileComposerFactory(
				packageName, implName);
		addImports(composerFactory);
		composerFactory.addImport(className);
		composerFactory.setSuperclass(className);
		final SourceWriter sw = composerFactory.createSourceWriter(context, printWriter);

		ArrayList referencedMetadata = new ArrayList();

		sw.indent();
		
		generateClassBody(logger, types, metadataType, implName, sw,
				referencedMetadata);
		
		for (Iterator i = referencedMetadata.iterator(); i
				.hasNext();) {
			String name = (String) i.next();
			//sw.println("// Should import "+name);
			composerFactory.addImport(name);
		}
		
		sw.outdent();

		sw.commit(logger);
	}
	protected void addImports(ClassSourceFileComposerFactory composerFactory) {
		composerFactory.addImport("com.google.gwt.core.client.GWT");
		composerFactory.addImport("com.habitsoft.kiyaa.metamodel.*");
		composerFactory.addImport("com.habitsoft.kiyaa.metamodel.fields.*");
	}
	protected void generateClassBody(TreeLogger logger, TypeOracle types,
			final JClassType metadataType, String implName,
			final SourceWriter sw, ArrayList referencedMetadata) {
		generateCtor(logger, metadataType, implName, sw, types,
				referencedMetadata);
		
	}
	protected void generateCtor(TreeLogger logger,
			final JClassType metadataType, String implName,
			final SourceWriter sw, TypeOracle types, 
			ArrayList referencedMetadata) {
		sw.println("public "+implName+"() {");
		sw.indent();
		
		final JClassType baseMetadataType = types.findType("com.habitsoft.kiyaa.metamodel.BaseMetadata");
		//sw.println("// base metadata type "+baseMetadataType);
		
		final JField[] fields = metadataType.getFields();
		for (int i = 0; i < fields.length; i++) {
			JField field = fields[i];
			//sw.println("// "+field.getName()+ " "+field.getType().getQualifiedSourceName()+" "+field.getType().isClassOrInterface());
			JClassType type = field.getType().isClassOrInterface();
			if(type != null) {
				boolean isMetadata = type.isAssignableTo(baseMetadataType);
				if(isMetadata) {
					referencedMetadata.add(type.getQualifiedSourceName());
					final String shortTypeName = type.getName();
					sw.println("this."+field.getName()+" = ("+shortTypeName+") GWT.create("+shortTypeName+".class);");
				}
			}
		}
		
		sw.outdent();
		sw.println("}");
	}

}
