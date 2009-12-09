package com.habitsoft.kiyaa.rebind;

import com.google.gwt.core.ext.UnableToCompleteException;

public class EntityMetadataGenerator extends MetadataGenerator {

	public static class GeneratorInstance extends
		MetadataGenerator.GeneratorInstance {

		protected void generateClassBody() throws UnableToCompleteException {
			super.generateClassBody();
			generateIsNew();
		}

		protected void generateIsNew() {
			sw.println("public boolean isNew(Object x) {");
			sw.indentln("return ((" + modelType.getName()
					+ ")x).getId() == null;");
			sw.println("}");
		}

	}

	protected GeneratorInstance createGeneratorInstance() {
		return new GeneratorInstance();
	}
}
