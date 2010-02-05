package com.habitsoft.kiyaa.rebind;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JArrayType;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JField;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JParameterizedType;
import com.google.gwt.core.ext.typeinfo.JType;
import com.habitsoft.kiyaa.util.Cloner;

public class ClonerGenerator extends BaseGenerator {

	@Override
	protected GeneratorInstance createGeneratorInstance() {
		return new ClonerGeneratorInstance();
	}

	class ClonerGeneratorInstance extends GeneratorInstance {

		Set<String> doneTypes = new TreeSet<String>();
		List<JClassType> beanTypes = new ArrayList<JClassType>();
		
		@Override
		protected void generateClassBody() throws UnableToCompleteException {
			// Find the type to clone
			JClassType[] implementedInterfaces = baseType.getImplementedInterfaces();
			JClassType desiredInterface = getType(Cloner.class.getName());
			for(JClassType iface : implementedInterfaces) {
				JParameterizedType parameterizedType = iface.isParameterized();
				if(parameterizedType == null)
					continue;
				if(parameterizedType.getSimpleSourceName().equals("Cloner")) {
					JClassType[] typeArgs = parameterizedType.getTypeArgs();
					if(typeArgs.length != 1) {
						logger.log(TreeLogger.ERROR, "Cloner interface must be parameterized to the type of object to clone", null);
						throw new UnableToCompleteException();
					}
					beanTypes.add(typeArgs[0]);
				}
			}
			
			if(beanTypes.isEmpty()) {
				logger.log(TreeLogger.ERROR, "Cloner class must implement the Cloner<T> interface", null);
				throw new UnableToCompleteException();
			}
			for(int i=0; i < beanTypes.size(); i++) {
				JClassType beanType = beanTypes.get(i);
				generateCloner(beanType);
			}
			
		}

		private void generateCloner(JClassType beanType) {
			String className = beanType.getParameterizedQualifiedSourceName();
			if(doneTypes.add(className)) {
				sw.println("public "+className+" clone("+className+" src) {");
				sw.indent();
				sw.println("if(src == null) return null;");
				sw.println(className+" dest = new "+className+"();");
				sw.println("clone(src, dest);");
				sw.println("return dest;");
				sw.outdent();
				sw.println("}");
				
				sw.println("public void clone("+className+" src, "+className+" dest) {");
				sw.indent();
				generateCloneFields(beanType);
				sw.outdent();
				sw.println("}");
				
				sw.println("public java.util.Set<String> diff("+className+" a, "+className+" b) {");
				sw.indent();
				sw.println("java.util.TreeSet<String> diffs = new java.util.TreeSet<String>();");
				generateGetDifferences("", beanType, "a", "b");
				sw.println("return diffs;");
				sw.outdent();
				sw.println("}");
				
			}
		}

		private void generateCloneFields(JClassType beanType) {
			if(beanType.getQualifiedSourceName().startsWith("java.lang"))
				return;
			generateCloneFields(beanType.getSuperclass());
			JField[] fields = beanType.getFields();
			for(JField field : fields) {
				JClassType targetType = beanType;
				String capFieldName = capitalize(field.getName());
				JMethod getter = targetType.findMethod("get"+capFieldName, new JType[0]);
				if(getter == null) getter = targetType.findMethod("is"+capFieldName, new JType[0]);
				JType type = field.getType();
				JMethod setter = targetType.findMethod("set"+capFieldName, new JType[]{type});
				if(getter != null && setter != null) {
					String copyValue = "src."+getter.getName()+"()";
					
					copyValue = copyField(type, copyValue, capFieldName);
					sw.println("dest.set"+capFieldName+"("+copyValue+");");
				}
			}
		}

		private void generateGetDifferences(String prefix, JClassType beanType, String firstObject, String secondObject) {
			if(beanType.getQualifiedSourceName().startsWith("java.lang"))
				return;
			generateCloneFields(beanType.getSuperclass());
			JField[] fields = beanType.getFields();
			for(JField field : fields) {
				JClassType targetType = beanType;
				String capFieldName = capitalize(field.getName());
				JMethod getter = targetType.findMethod("get"+capFieldName, new JType[0]);
				if(getter == null) getter = targetType.findMethod("is"+capFieldName, new JType[0]);
				JType type = field.getType();
				if(getter != null) {
					String firstValue = firstObject+"."+getter.getName()+"()";
					String secondValue = secondObject+"."+getter.getName()+"()";
					generateCompareValue(prefix, field, type, firstValue,
							secondValue);
				}
			}
		}

		private void generateCompareValue(String prefix, JField field,
				JType type, String firstValue, String secondValue) {
			JArrayType arrayType = type.isArray();
			if(arrayType != null) {
				JType eltType = type.isArray().getComponentType();
				generateCompareArray(prefix, field, firstValue, secondValue,
						eltType);
			} else {
				String test = type.isPrimitive() != null ? firstValue+" != "+secondValue 
						: firstValue+" != "+secondValue+" && ("+firstValue+" == null || "+secondValue+" == null || ! "+firstValue+".equals("+secondValue+"))";
				sw.println("if("+test+") diffs.add(\""+prefix+field.getName()+"\");");
			}
		}

		private void generateCompareArray(String prefix, JField field,
				String firstValue, String secondValue, JType eltType) {
			sw.println("if(!java.util.Arrays.equals("+firstValue+", "+secondValue+")) {");
			sw.indent();
			sw.println("diffs.add(\""+prefix+field.getName()+"\");");
			sw.println("int count = Math.min("+firstValue+" == null ? 0 : "+firstValue+".length, "+secondValue+" == null ? 0 : "+secondValue+".length);");
			sw.println("for(int i=0; i < count; i++) {");
			sw.indent();
			if(eltType.isPrimitive() != null)
				sw.println("if("+firstValue+"[i] != "+secondValue+"[i])");
			else
				sw.println("if("+firstValue+"[i] != null && "+secondValue+"[i] != null ? !"+firstValue+"[i].equals("+secondValue+"[i]):"+firstValue+"[i] == "+secondValue+"[i])");
			sw.indentln("diffs.add(\""+prefix+field.getName()+"[\"+i+\"]\");");
			sw.outdent();
			sw.println("}");
			sw.println("int maxCount = Math.max("+firstValue+" == null ? 0 : "+firstValue+".length, "+secondValue+" == null ? 0 : "+secondValue+".length);");
			sw.println("for(int i=count; i < maxCount; i++) {");
			sw.indent();
			sw.indentln("diffs.add(\""+prefix+field.getName()+"[\"+i+\"]\");");
			sw.outdent();
			sw.println("}");
			sw.outdent();
			sw.println("}");
		}
		
		private String copyField(JType type, String copyValue, String capFieldName) {
			// Don't copy primitive types, or immutable types
			if(type.isPrimitive() != null 
				|| type.isEnum() != null 
				|| type.getQualifiedSourceName().startsWith("java.lang."))
				return copyValue;
			
			// Deep copy arrays
			JArrayType array = type.isArray();
			if(array != null) {
				String arrayTypeName = array.getParameterizedQualifiedSourceName();
				String compTypeName = array.getComponentType().getParameterizedQualifiedSourceName();
				String arrayName = "dest"+capFieldName;
				sw.println(arrayTypeName+" src"+capFieldName+" = "+copyValue+";");
				sw.println(arrayTypeName+" "+arrayName+";");
				sw.println("if(src"+capFieldName+" != null) {");
				sw.indent();
				sw.println(arrayName+" = new "+compTypeName+"[src"+capFieldName+".length];");
				sw.println("int "+arrayName+"Count = 0;");
				sw.println("for(int i=0; i < "+arrayName+".length; i++) {");
				sw.indent();
				sw.println(arrayName+"[i] = "+copyField(array.getComponentType(), "src"+capFieldName+"[i]", capFieldName+"Elt")+";");
				sw.outdent();
				sw.println("}");
				sw.outdent();
				sw.println("} else {");
				sw.indentln(arrayName+" = null;");
				sw.println("}");
				copyValue = "dest"+capFieldName;
			}
			
			// Deep copy objects
			JClassType cls = type.isClassOrInterface();
			if(cls != null) {
    			if(cls.findMethod("clone", new JType[0]) != null) {
    				copyValue = "("+cls.getParameterizedQualifiedSourceName()+") "+copyValue; 
    			} else {
    				beanTypes.add(cls);
    				copyValue = "clone("+copyValue+")";
    			}
			}
			return copyValue;
		}
		
	}
}
