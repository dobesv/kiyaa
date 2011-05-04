/**
 * 
 */
package com.habitsoft.kiyaa.rebind.typeinfo;

import java.util.ArrayList;
import java.util.Iterator;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JType;
import com.habitsoft.kiyaa.rebind.BaseGenerator;
import com.habitsoft.kiyaa.rebind.GeneratedHTMLViewGenerator.OperatorInfo;
import com.habitsoft.kiyaa.rebind.LocalTreeLogger;

public class ExpressionInfo {
	static final LocalTreeLogger logger = LocalTreeLogger.logger;
	private final String getter;
	private final String setter;
	private final String asyncGetter;
	private final String asyncSetter;
	private final boolean constant;
	private final GeneratorTypeInfo type;
    private ArrayList<OperatorInfo> operators;
	private final String originalExpr;
    
    public ExpressionInfo(String originalExpr, String getter, String setter, GeneratorTypeInfo type, boolean asyncGetter, boolean asyncSetter, boolean constant) {
        super();
        if(asyncGetter) {
        	this.asyncGetter = getter;
        	this.getter = null;
        }
        else {
        	this.getter = getter;
        	this.asyncGetter = null;
        }
        if(asyncSetter) {
        	this.asyncSetter = setter;
        	this.setter = null;
        }
        else {
        	this.setter = setter;
        	this.asyncSetter = null;
        }
        this.type = type;
        this.constant = constant;
        this.originalExpr = originalExpr;
    }

    public boolean hasSynchronousSetter() {
        return setter != null;
    }

    public boolean hasAsyncGetter() {
        return asyncGetter != null;
    }

    public boolean hasSynchronousGetter() {
        return getter != null;
    }

    public String setterString() {
        if(asyncSetter != null) return asyncSetter;
        if(setter != null) return setter;
        return toString();
    }

    public boolean hasSetter() {
		return setter != null || asyncSetter != null;
	}

	public boolean hasGetter() {
		return getter != null || asyncGetter != null;
	}

	public ExpressionInfo(String originalExpr, String getter, String setter, GeneratorTypeInfo type) {
		this(originalExpr, getter, setter, type, false, false, false);
	}

    public ExpressionInfo(String originalExpr, String expr, GeneratorTypeInfo type, boolean constant) {
		this(originalExpr, expr, null, type, false, false, constant);
	}

    public ExpressionInfo(ExpressionInfo x, OperatorInfo operator) {
    	this.getter = x.getter;
    	this.setter = x.setter;
    	this.asyncGetter = x.asyncGetter;
    	this.asyncSetter = x.asyncSetter;
    	this.constant = false;
    	this.type = x.type;
    	this.originalExpr = x.originalExpr;
    	addOperator(operator);
	}

	public ExpressionInfo(String originalExpr, String expr, JClassType type, boolean constant) {
		this(originalExpr, expr, new JClassTypeWrapper(type), constant);
	}

	public ExpressionInfo(String originalExpr, String expr, JType type, boolean constant) {
		this(originalExpr, expr, JTypeWrapper.wrap(type), constant);
	}
	
	public ExpressionInfo(String originalExpr, String getter, String setter, JType type, boolean asyncGetter, boolean asyncSetter, boolean constant) {
		this(originalExpr, getter, setter, JTypeWrapper.wrap(type), asyncGetter, asyncSetter, constant);
	}

	String applyGetOperators(String expr) throws UnableToCompleteException {
		if(operators == null) return expr;
    	for (Iterator<OperatorInfo> i = operators.iterator(); i.hasNext();) {
			OperatorInfo oper = i.next();
			expr = oper.onGetExpr(expr);
		}
    	return expr;
    }
    String applySetOperators(String expr) throws UnableToCompleteException {
		if(operators == null) return expr;
    	for (Iterator<OperatorInfo> i = operators.iterator(); i.hasNext();) {
			OperatorInfo oper = i.next();
			expr = oper.onSetExpr(expr);
		}
    	return expr;
    }
	public String copyStatement(ExpressionInfo src) throws UnableToCompleteException {
        String converted = src.conversionExpr(type);
        if(converted == null) {
            LocalTreeLogger.logger.log(TreeLogger.ERROR, "Unable to convert "+src.type.getName()+" to "+type.getName()+" for "+setter+" = "+(src.getter!=null?src.getter:src.asyncGetter), null);
            throw new UnableToCompleteException();
        }
        return callSetter(setter, applySetOperators(converted)).toString();
    }

    public String conversionExpr(GeneratorTypeInfo targetType) throws UnableToCompleteException {
    	if(getter == null) {
    		LocalTreeLogger.logger.log(TreeLogger.ERROR, "This expression is not async - use asyncCopyStatement() for async support!", new Error());
    		throw new UnableToCompleteException();
    	}
        String converted = converter(getter, type, targetType);
		if(converted == null)
			return null;
		String postOp = applyGetOperators(converted);
		if(postOp == null) {
			LocalTreeLogger.logger.log(TreeLogger.ERROR, "Converted type successfully, but applying operators returned null!", null);
		}
		return postOp;
    }
    
    public String getterExpr() throws UnableToCompleteException {
        if(getter == null) throw new NullPointerException("No synchronous getter for this expression.");
        return applyGetOperators(getter);
    }
    
    /**
     * Asynchronous copy (load) from one expression to another.
     * 
     * @param src Source value to read from
     * @param callback Expression string for the callback to invoke on completion
     * @param maySkipCallback If true, and both this and src are not asynchronous, doesn't call the callback
     * @return A statement to be put into the source which does the copy
     */
    public String asyncCopyStatement(ExpressionInfo src, String callback, boolean maySkipCallback) throws UnableToCompleteException {
    	if(setter != null) {
    		if(src.getter != null) {
    			if(maySkipCallback)
    				return copyStatement(src);
    			else
    				return copyStatement(src)+callback+".onSuccess();";
    		} else if(src.asyncGetter != null) {
    			String converted = converter(src.applyGetOperators(converter("result", null, src.type)), src.type, type);
    			if(converted == null) {
    				LocalTreeLogger.logger.log(TreeLogger.ERROR, "Can't convert "+src.type+" to "+type+" for copy from "+src.asyncGetter+" to "+setter, null);
    				throw new UnableToCompleteException();
    			}
            	return src.callAsyncGetter("new AsyncCallbackProxy<"+src.getType().getParameterizedQualifiedSourceName()+", Void>("+callback+") {" +
    			"public void onSuccess("+src.getType().getParameterizedQualifiedSourceName()+" result) { " +
    				"try { " +
        				callSetter(stripThis(setter), applySetOperators(converted)) +
						"returnSuccess(null); " +
					"} catch(Throwable caught) { " +
						"super.onFailure(caught); " +
					"}" +
				"}" +
			"}")+"; /* async getter and sync setter */";
    		} else throw new NullPointerException("No getter!");
    	} else if(asyncSetter != null) {
    		if(src.getter != null) {
    			String converted = converter(src.getter, src.type, type);
    			if(converted == null) {
    				LocalTreeLogger.logger.log(TreeLogger.ERROR, "Can't convert "+src.type+" to "+type+" for copy from "+src.getter+" to "+asyncSetter, null);
    				throw new UnableToCompleteException();
    			}
            	return callAsyncSetter(asyncSetter, converted, callback).toString();
    		} else if(src.asyncGetter != null) {
    			String converted = converter(src.applyGetOperators(converter("result", null, src.type)), src.type, type);
    			if(converted == null) {
    				LocalTreeLogger.logger.log(TreeLogger.ERROR, "Can't convert "+src.type+" to "+type+" for copy from "+src.asyncGetter+" to "+asyncSetter, null);
    				throw new UnableToCompleteException();
    			}
            	return src.callAsyncGetter("new AsyncCallbackProxy<"+src.getType().getParameterizedQualifiedSourceName()+", Void>("+callback+") {" +
            			"public void onSuccess("+src.getType().getParameterizedQualifiedSourceName()+" result) {" +
            				callSetter(asyncSetter, applySetOperators(converted)+", takeCallback()") +
            			" returnSuccess(null); }}")+";";
    		} else throw new NullPointerException("No getter!");
    	} else throw new NullPointerException("No setter!");
    }

	private String stripThis(String setter) {
		if(setter.startsWith("this."))
			return setter.substring(5);
		return setter;
	}

	public String getGetter() {
		return getter;
	}

	public String getSetter() {
		return setter;
	}

	public String getAsyncGetter() {
		return asyncGetter;
	}

	public String getAsyncSetter() {
		return asyncSetter;
	}

	public boolean isConstant() {
		return constant;
	}

	public GeneratorTypeInfo getType() {
		return type;
	}

	@Override
	public String toString() {
		if(originalExpr != null) {
			return originalExpr;
		}
		return getter!=null?getter:asyncGetter!=null?asyncGetter:setter!=null?setter:asyncSetter!=null?asyncSetter:type.toString();
	}
	public void addOperator(OperatorInfo info) {
		if(operators == null) operators = new ArrayList<OperatorInfo>();
		operators.add(info);
	}

	public static StringBuffer callAsyncSetter(String setter, String value, String callback) throws UnableToCompleteException {
		if(setter.endsWith("="))
			throw new IllegalStateException("Internal Error: Can't call a synchronous assignment setter with an asynchronous callback!");
		StringBuffer sb = new StringBuffer(setter);
		if(!(setter.endsWith("(") || setter.endsWith(",")))
			sb.append('(');
		return sb.append(value).append(", ").append(callback).append(");");
	}

	public static StringBuffer callSetter(String setter, String value) throws UnableToCompleteException {
		if(setter.endsWith("="))
			return new StringBuffer().append(setter).append(value).append(";");
		StringBuffer sb = new StringBuffer(setter);
		if(!(setter.endsWith("(") || setter.endsWith(",")))
			sb.append('(');
		return sb.append(value).append(");");
	}

	public static String callAsyncGetter(String asyncGetter, String callback) {
		StringBuffer buf = new StringBuffer(asyncGetter.length() + callback.length() + 3);
		buf.append(asyncGetter);
		if(asyncGetter.endsWith(",")) buf.append(" ");
		else if(!asyncGetter.endsWith("(")) buf.append("(");
		buf.append(callback);
		buf.append(')');
		return buf.toString();
	}

	/**
	 * Return true if one of the types is a primitive type, and the other is its java.lang.* equivalent
	 */
	public static boolean boxingOrUnboxing(GeneratorTypeInfo srcType, GeneratorTypeInfo destType) {
		if(destType.isPrimitive()) {
			if(destType instanceof PrimitiveTypeInfo)
				return ((PrimitiveTypeInfo)destType).unboxesTo(srcType);
			else
				return PrimitiveTypeInfo.unboxesTo(srcType.getName(), destType.getName());
					
		} else if(srcType.isPrimitive()) {
			if(srcType instanceof PrimitiveTypeInfo)
				return ((PrimitiveTypeInfo)srcType).unboxesTo(destType);
			else
				return PrimitiveTypeInfo.unboxesTo(destType.getName(), srcType.getName());
		}
		return false;
	}

	/**
	 * Return a string expression converting inExpr of type inType to outType, if possible.
	 * 
	 * If inType is null, input type is assumed to be java.lang.Object.
	 * 
	 * @param inExpr
	 * @param inType
	 * @param outType
	 * @return
	 * @throws UnableToCompleteException
	 */
	public static String converter(final String inExpr, final GeneratorTypeInfo inType, final GeneratorTypeInfo outType)
	                throws UnableToCompleteException {
		if(outType == null) throw new NullPointerException("outType is null");
		if(inExpr == null) throw new NullPointerException("inExpr is null");
	    String attributeValueExpr = null;
	    GeneratorMethodInfo valueOfMethod;
		if (directlyAssignable(outType, inExpr, inType)) {
	        attributeValueExpr = inExpr;
	    } else if((valueOfMethod = findValueOf(outType, inType)) != null) {
	    	attributeValueExpr = outType.getParameterizedQualifiedSourceName()+".valueOf("+converter(inExpr, inType, valueOfMethod.getParameterTypes()[0])+")";
	    } else if(stringToArray(inType, outType)) {
    		if(inExpr.startsWith("\"")) {
    			String[] strings = inExpr.substring(1, inExpr.length()-2).split("\\s*,?\\s*");
    			if(strings.length == 1 && strings[0].equals("")) return "new String[] {}";
    			for (int i = 0; i < strings.length; i++) {
					String string = strings[i];
					strings[i] = "\"" + string + "\"";
				}
    			return "new String[] {"+BaseGenerator.joinWithCommas(0, strings)+"}";
    		} else {
    			return inExpr+".split(\"\\\\s*,\\\\s*\")";
    		}
	    } else if(castable(outType, inExpr, inType)) {
            // Downcast
            attributeValueExpr = "((" + outType.getName() + ") " + inExpr + ")";
	    } else if (outType.isPrimitive()) {
			if (outType.getName().equals("boolean")) {
	            if (inType.getName().equals("java.lang.Boolean")) {
	                attributeValueExpr = inExpr + ".booleanValue()";
	            } else if (inType.getName().equals("java.lang.String")) {
	                attributeValueExpr = "Boolean.parseBoolean(" + inExpr + ")";
	            } else if("null".equals(inExpr) || "false".equals(inExpr) || "".equals(inExpr)) {
	            	attributeValueExpr = "false";
	            } else if("true".equals(inExpr)) {
	            	attributeValueExpr = "true";
	            } else if (inType.getName().equals("java.lang.Object")) {
	                attributeValueExpr = inExpr + " != null && (!(" + inExpr
	                + " instanceof Boolean) || ((Boolean)" + inExpr + ").booleanValue())";
	            } else {
	                attributeValueExpr = "(" + inExpr + " != null)";
	            }
	        } else {
				if (outType.getName().equals("int")) {
					if (inType.getName().equals("long") || inType.getName().equals("double") || inType.getName().equals("float")) {
						attributeValueExpr = "((int)"+inExpr+")";
					} else if (inType.getName().equals("java.lang.Integer")) {
				        attributeValueExpr = inExpr + ".intValue()";					        
				    } else if (inType.getName().equals("java.lang.Object")) {
				        attributeValueExpr = "((Integer)" + inExpr + ").intValue()";
				    } else if (inType.getName().equals("java.lang.String")) {
				        attributeValueExpr = "Integer.parseInt(" + inExpr + ")";
				    }
				} else if (outType.getName().equals("long")) {
					if (inType.getName().equals("int") || inType.getName().equals("double") || inType.getName().equals("float")) {
						attributeValueExpr = "((long)"+inExpr+")";
					} else if (inType.getName().equals("java.lang.Long")) {
				        attributeValueExpr = inExpr + ".longValue()";
				    } else if (inType.getName().equals("java.lang.Object")) {
				        attributeValueExpr = "((Long)" + inExpr + ").longValue()";
				    } else if (inType.getName().equals("java.lang.String")) {
				        attributeValueExpr = "Long.parseLong(" + inExpr + ")";
				    }
				} else if (outType.getName().equals("double")) {
					if (inType.getName().equals("int") || inType.getName().equals("long") || inType.getName().equals("float")) {
						attributeValueExpr = "((double)"+inExpr+")";
					} else if (inType.getName().equals("java.lang.Double")) {
				        attributeValueExpr = inExpr + ".doubleValue()";
				    } else if (inType.getName().equals("java.lang.Object")) {
				        attributeValueExpr = "((Double)" + inExpr + ").doubleValue()";
				    } else if (inType.getName().equals("java.lang.String")) {
				        attributeValueExpr = "Double.parseDouble(" + inExpr + ")";
				    }
				} else if (outType.getName().equals("float")) {
					if (inType.getName().equals("int") || inType.getName().equals("long") || inType.getName().equals("double")) {
						attributeValueExpr = "((float)"+inExpr+")";
					} else if (inType.getName().equals("java.lang.Float")) {
				        attributeValueExpr = inExpr + ".floatValue()";
				    } else if (inType.getName().equals("java.lang.Object")) {
				        attributeValueExpr = "((Float)" + inExpr + ").floatValue()";
				    } else if (inType.getName().equals("java.lang.String")) {
				        attributeValueExpr = "Float.parseFloat(" + inExpr + ")";
				    }
				} else {
					logger.log(TreeLogger.ERROR, "Primitive output type not handled yet: "+outType, null);
					return null;
				}
			}
	    } else if(inType.isPrimitive() && outType.getName().equals("java.lang.String")) {
	    	attributeValueExpr = "String.valueOf("+inExpr+")";
	    } else if(inType.getName().equals("java.lang.String") &&
	        	(outType.getName().equals("java.lang.Long"))
        		|| outType.getName().equals("java.lang.Double")
        		|| outType.getName().equals("java.lang.Float")
        		|| outType.getName().equals("java.lang.Integer")) {
        	// TODO Generalize this to more types than just Long; Long was what I needed but others may want something else
        	attributeValueExpr = "("+inExpr+".length()==0?null:new "+ outType.getName() +"("+inExpr+"))";
	    } else if(outType.getName().equals("java.lang.String")) {
        	attributeValueExpr = "("+inExpr+"==null?\"\":String.valueOf("+inExpr+"))";
        } else {
            logger.log(TreeLogger.WARN, "Cannot figure out how to convert from "+inType+" to "+outType+" for '"+inExpr+"'", null);
        	// Couldn't figure out how to convert this; however, we'll try letting the compiler do this itself
            attributeValueExpr = inExpr;
        }
	    return attributeValueExpr;
	}

	/**
	 * Return true if the provided inType can be cast without error to outType.
	 * 
	 * inExpr is used to check for the special case "null" which can be cast to whatever
	 * you want, except primitive types.
	 * 
	 * @param outType Desired result type
	 * @param inExpr Input expression (either "null" or somethign else)
	 * @param inType Provided input type
	 * @return true if the cast should be OK for the compiler
	 */
	public static boolean castable(GeneratorTypeInfo outType, String inExpr, GeneratorTypeInfo inType) {
		if(inType.isArray() && outType.isArray() && 
				(directlyAssignable(outType.getComponentType(), inExpr, inType.getComponentType())
				 || castable(outType.getComponentType(), inExpr, inType.getComponentType())))
			return true;
		// Can downcast a superclass to a subclass
		if(outType.isSubclassOf(inType))
			return true;
		// Can downcast an interface to a type that implements it
		if(outType.implementsInterface(inType))
			return true;		
		if(!outType.isPrimitive() && ("null".equals(inExpr) || "java.lang.Object".equals(inType.getName())))
			return true;
		return false;
	}

	private static boolean stringToArray(final GeneratorTypeInfo inType, final GeneratorTypeInfo outType) {
		return outType.isArray() && inType.equals(RuntimeClassWrapper.STRING) && outType.getComponentType().getName().equals("java.lang.String");
	}

	/**
	 * Check whether a variable of type "outType" will accept the given input type without a compile
	 * error.
	 * 
	 * @param outType Target type being assigned into
	 * @param inExpr Source expression; currently only checked for "null" as a special case
	 * @param inType Input type
	 * @return
	 */
	public static boolean directlyAssignable(final GeneratorTypeInfo outType, final String inExpr, final GeneratorTypeInfo inType) {
		// When inType == null interpret that as "java.lang.Object" I guess - or as if it was the word "null"
		if(inType == null)
			return outType.isPrimitive() == false; // Can't do int x = null, but pretty much anything else goes
		
		return inType.equals(outType) 
	    	|| boxingOrUnboxing(inType, outType) 
	    	|| inType.isSubclassOf(outType)
	    	|| inType.implementsInterface(outType)
	    	|| (inType.isArray() && outType.isArray() 
	    			&& (inType.getComponentType().isSubclassOf(outType.getComponentType())
	    				||inType.getComponentType().implementsInterface(outType.getComponentType())))
	    	|| (!outType.isPrimitive() && "null".equals(inExpr));
	}

	/**
	 * Return true if there's a static method on outType called "valueOf" which
	 * accepts inType as a parameter and returns a type compatible with outType.
	 * 
	 * @param outType Desired result type
	 * @param inType Expected input type
	 * @return true if valueOf can be used to convert
	 */
	public static GeneratorMethodInfo findValueOf(GeneratorTypeInfo outType, GeneratorTypeInfo inType) {
		return outType.findStaticMethodMatching("valueOf", outType, inType);
	}

	/**
	 * Construct a new expression info by concatenating the getter for this expression
	 * and the getter & setter of the given sub-expression.
	 * 
	 * @param subExprInfo Sub-expression to be appended to this expression
	 * @return A new expression info that combines this expression with the other one as a sub-expression
	 * 
	 * @throws UnableToCompleteException 
	 */
	public ExpressionInfo asSubexpression(ExpressionInfo subExprInfo) throws UnableToCompleteException {
		if(getter == null) throw new Error("Can't load subexpression of asynchronous expression "+this);
		String prefix = getterExpr() + ".";
		boolean isAsyncSetter = subExprInfo.asyncSetter != null;
		boolean isAsyncGetter = subExprInfo.asyncGetter != null;
		String subExpressionGetter = prefix + (subExprInfo.getter!=null?subExprInfo.getter:subExprInfo.asyncGetter);
		String subexpressionSetter = prefix + subExprInfo.setterString();
		return new ExpressionInfo(originalExpr+"."+subExprInfo.originalExpr,
				subExpressionGetter, subexpressionSetter, 
				subExprInfo.getType(), isAsyncGetter, 
				isAsyncSetter, subExprInfo.constant);
	}

	/**
	 * Return a call of synchronous setter of this expression with the given value expression
	 */
	public StringBuffer callSetter(String valueExpr) throws UnableToCompleteException {
		return callSetter(setter, valueExpr);
	}

	public boolean hasAsynchronousSetter() {
		return asyncSetter != null;
	}

	/**
	 * Return a call of asynchronous setter of this expression with the given value expression
	 */
	public String callAsyncSetter(String valueExpr, String callbackExpr) {
		return callAsyncSetter(valueExpr, callbackExpr);
	}

	/**
	 * Return a call of the synchronous getter of this expression with the given callback expression
	 */
	public String callAsyncGetter(String callbackExpr) {
		return callAsyncGetter(asyncGetter, callbackExpr);
	}

	public boolean hasAsynchronousGetter() {
		return asyncGetter != null;
	}

	public String getOriginalExpr() {
		return originalExpr;
	}
}