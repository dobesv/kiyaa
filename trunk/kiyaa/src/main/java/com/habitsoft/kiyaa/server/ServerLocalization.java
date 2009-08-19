package com.habitsoft.kiyaa.server;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.TreeMap;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtField;
import javassist.CtMethod;
import javassist.Modifier;
import javassist.NotFoundException;
import javassist.CtField.Initializer;

import com.google.gwt.i18n.client.Constants.DefaultBooleanValue;
import com.google.gwt.i18n.client.Constants.DefaultDoubleValue;
import com.google.gwt.i18n.client.Constants.DefaultFloatValue;
import com.google.gwt.i18n.client.Constants.DefaultIntValue;
import com.google.gwt.i18n.client.Constants.DefaultStringValue;
import com.google.gwt.i18n.client.LocalizableResource.GenerateKeys;
import com.google.gwt.i18n.client.LocalizableResource.Key;
import com.google.gwt.i18n.client.LocalizableResource.Meaning;
import com.google.gwt.i18n.client.Messages.DefaultMessage;
import com.google.gwt.i18n.rebind.keygen.KeyGenerator;
import com.google.gwt.i18n.rebind.keygen.MethodNameKeyGenerator;

/**
 * Use javassist to mimic the static localization features of GWT.
 * 
 * Code on the server can instantiate the same Messages and Constants
 * subclasses as on the client, and this code will load the appropriate
 * properties files and use the @DefaultStringValue annotations of the
 * class (if present).
 *
 */
public class ServerLocalization {
    private static ClassPool classPool;
    
    static class ImplKey {
        String className;
        Locale locale;
        public ImplKey(String className, Locale locale) {
            this.className = className;
            this.locale = locale;
        }
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((className == null) ? 0 : className.hashCode());
            result = prime * result + ((locale == null) ? 0 : locale.hashCode());
            return result;
        }
        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (!(obj instanceof ImplKey))
                return false;
            ImplKey other = (ImplKey) obj;
            if (className == null) {
                if (other.className != null)
                    return false;
            } else if (!className.equals(other.className))
                return false;
            if (locale == null) {
                if (other.locale != null)
                    return false;
            } else if (!locale.equals(other.locale))
                return false;
            return true;
        }
        
    }
    static Map<ImplKey,Object> cache = new HashMap<ImplKey, Object>();
    
    private static ClassPool getClassPool() {
        if(classPool == null) {
            classPool = ClassPool.getDefault(); //new ClassPool();
        }
        return classPool;
    }
    /**
     * Create a class like:
     * 
     * <code>
     * class SomeMessagesImpl extends TreeMap<String,MessageFormat> implements SomeMessages { 
     *     ResourceBundle res;
     *     public SomeMessagesImpl(ResourceBundle res) {
     *         this.res = res;
     *     } 
     * 
     *     public String someMessage(int a, Object b, String c) {
     *         get("someMessage").format(new Object[]{a, b, c}, new StringBuffer(), null).toString();
     *     }
     * }
     * </code>
     * 
     * And return an instance of it populated with the appropriate MessageFormat objects.
     * @throws ClassNotFoundException 
     * @throws IllegalAccessException 
     * @throws InstantiationException 
     */
    public static <T> T createMessagesInstance(Class<T> messagesClass, Locale locale) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        ImplKey cacheKey = new ImplKey(messagesClass.getName(), locale);
        T result = (T) cache.get(cacheKey);
        if(result != null) {
            return result;
        }
        String ifaceName = messagesClass.getName();
        final ClassPool classPool = getClassPool();
        try {
            CtClass iface = classPool.get(messagesClass.getName());
            CtClass messageFormat = classPool.get(MessageFormat.class.getName());
            CtClass stringClass = classPool.get(String.class.getName());
            CtClass localeClass = classPool.get(Locale.class.getName());
            CtClass impl = classPool.makeClass(ifaceName+"Impl_"+locale.toString());
            impl.addInterface(iface);
            
            KeyGenerator keyGenerator = new MethodNameKeyGenerator();
            for(Object ann : iface.getAnnotations()) {
                if(ann instanceof GenerateKeys) {
                    String keyGeneratorClassName = ((GenerateKeys)ann).value();
                    Class<KeyGenerator> keyGeneratorClass;
                    try {
                        keyGeneratorClass = (Class<KeyGenerator>) Class.forName(keyGeneratorClassName);
                    } catch(ClassNotFoundException cnfe) {
                        keyGeneratorClass = (Class<KeyGenerator>) Class.forName(KeyGenerator.class.getName()+"."+keyGeneratorClassName);
                    }
                    keyGenerator = keyGeneratorClass.newInstance();
                }
            }
            
            ResourceBundle resBundle = ResourceBundle.getBundle(ifaceName, locale);
            CtConstructor ctorMethod = new CtConstructor(new CtClass[] {localeClass}, impl);
            impl.addConstructor(ctorMethod);
            ctorMethod.setModifiers(Modifier.PUBLIC);
            ctorMethod.setBody("super();");
            CtField localeField = new CtField(localeClass, "_locale", impl);
            impl.addField(localeField, CtField.Initializer.byParameter(0));
            TreeMap<String,String> keys = new TreeMap<String,String>();
            for(CtMethod method : iface.getMethods()) {
                // Only abstract methods
                if((method.getModifiers() & Modifier.ABSTRACT) == 0)
                    continue;
                CtMethod methodImpl = new CtMethod(method, impl, null);
                String methodKey = null;
                String meaning = null;
                String defaultValue = null;
                for(Object ann : method.getAnnotations()) {
                    if(ann instanceof Key) {
                        methodKey = ((Key)ann).value();
                    } else if(ann instanceof Meaning) {
                        meaning = ((Meaning)ann).value();
                    } else if(ann instanceof DefaultStringValue) {
                        defaultValue = ((DefaultStringValue)ann).value();
                    } else if(ann instanceof DefaultBooleanValue) {
                        defaultValue = String.valueOf(((DefaultBooleanValue)ann).value());
                    } else if(ann instanceof DefaultDoubleValue) {
                        defaultValue = String.valueOf(((DefaultDoubleValue)ann).value());
                    } else if(ann instanceof DefaultMessage) {
                        defaultValue = ((DefaultMessage)ann).value();
                    }
                }
                if(methodKey == null) {
                    methodKey = keyGenerator.generateKey(ifaceName, method.getName(), defaultValue, meaning);
                }
                String value;
                try {
                    value = resBundle.getString(methodKey);
                } catch (java.util.MissingResourceException mre) {
                    if(defaultValue != null) {
                        value = defaultValue;
                    } else throw mre;
                }
                CtField patternField = new CtField(stringClass, method.getName()+"Pattern", impl);
                impl.addField(patternField, CtField.Initializer.constant(value));
                CtField formatField = new CtField(messageFormat, method.getName()+"MessageFormat", impl);
                impl.addField(formatField, CtField.Initializer.byExpr("new java.text.MessageFormat("+patternField.getName()+", _locale)"));
                methodImpl.setModifiers(Modifier.PUBLIC);
                methodImpl.setBody("return "+formatField.getName()+".format($args, new StringBuffer(), null).toString();");
                impl.addMethod(methodImpl);
                keys.put(methodKey, defaultValue);
            }
            final Class implClazz = impl.toClass();
            final Constructor ctor = implClazz.getConstructor(Locale.class);
            Object instance = ctor.newInstance(locale);
            cache.put(cacheKey, instance);
            return messagesClass.cast(instance);
            
        } catch (NotFoundException e) {
            throw new Error(e);
        } catch (RuntimeException e) {
            throw new Error(e);
        } catch (CannotCompileException e) {
            throw new Error(e);
        } catch (NoSuchMethodException e) {
            throw new Error(e);
        } catch (InvocationTargetException e) {
            throw new Error(e);
        }
        
    }
    
    public static Map<String, String> getMap(ResourceBundle res, String methodName) throws MissingResourceException {
        String[] keys = getStringArray(res, methodName);
        Map<String,String> result = new TreeMap<String, String>();
        for(String key : keys) {
            result.put(key, res.getString(key));
        }
        return result;
    }

    public static String[] getStringArray(ResourceBundle res, String methodName) throws MissingResourceException {
        String s = res.getString(methodName);
        String[] parts = s.split("(?<!\\\\),");
        for(int i=0; i < parts.length; i++) {
            parts[i] = parts[i].replaceAll("\\\\(.)", "$1").trim();
        }
        return parts;
    }
    
    /**
     * Create a class like:
     * 
     * <code>
     * class SomeConstantsImpl extends BaseConstantsImpl implements SomeConstants {
     *     String _someConstant;
     *     int _intConstant;
     *     double _doubleConstant;
     *     SomeConstantsImpl(ResourceBundle res) {
     *        super(res);
     *        _someConstant = getString("someConstant");
     *        _intConstant = getInt("intConstant");
     *        _doubleConstant = getDouble("doubleConstant");
     *     }
     *     
     *     public String someConstant() {
     *        return _someConstant;
     *     }
     *     public int intConstant() {
     *        return _intConstant;
     *     }
     *     public double doubleConstant() {
     *        return _doubleConstant;
     *     }
     * }
     * 
     * Then lookup the resource bundle for the given locale and construct
     * and instance of this new class with that resource bundle.
     */
    public static <T> T createConstantsInstance(Class<T> constantsClass, Locale locale) {
        ImplKey key = new ImplKey(constantsClass.getName(), locale);
        T result = (T) cache.get(key);
        if(result != null) {
            return result;
        }
        String ifaceName = constantsClass.getName();
        final ClassPool classPool = getClassPool();
        try {
            CtClass iface = classPool.get(constantsClass.getName());
            CtClass baseConstantsImpl = classPool.get(BaseConstantsImpl.class.getName());
            CtClass stringClass = classPool.get(String.class.getName());
            CtClass impl = classPool.makeClass(ifaceName+"Impl_"+locale.toString(), baseConstantsImpl);
            impl.addInterface(iface);
            final CtClass resourceBundle = classPool.get(ResourceBundle.class.getName());
            CtField resField = new CtField(resourceBundle, "res", impl);
            impl.addField(resField);
            
            KeyGenerator keyGenerator = new MethodNameKeyGenerator();
            for(Object ann : iface.getAnnotations()) {
                if(ann instanceof GenerateKeys) {
                    String keyGeneratorClassName = ((GenerateKeys)ann).value();
                    Class<KeyGenerator> keyGeneratorClass;
                    try {
                        keyGeneratorClass = (Class<KeyGenerator>) Class.forName(keyGeneratorClassName);
                    } catch(ClassNotFoundException cnfe) {
                        keyGeneratorClass = (Class<KeyGenerator>) Class.forName(KeyGenerator.class.getName()+"."+keyGeneratorClassName);
                    }
                    keyGenerator = keyGeneratorClass.newInstance();
                }
            }
            
            ResourceBundle resBundle = ResourceBundle.getBundle(ifaceName, locale);
            for(CtMethod method : iface.getMethods()) {
                // Only abstract methods
                if((method.getModifiers() & Modifier.ABSTRACT) == 0)
                    continue;
                String methodKey = null;
                String meaning = null;
                Object defaultValue = null;
                for(Object ann : method.getAnnotations()) {
                    if(ann instanceof Key) {
                        methodKey = ((Key)ann).value();
                    } else if(ann instanceof Meaning) {
                        meaning = ((Meaning)ann).value();
                    } else if(ann instanceof DefaultStringValue) {
                        defaultValue = ((DefaultStringValue)ann).value();
                    } else if(ann instanceof DefaultBooleanValue) {
                        defaultValue = ((DefaultBooleanValue)ann).value();
                    } else if(ann instanceof DefaultDoubleValue) {
                        defaultValue = ((DefaultDoubleValue)ann).value();
                    } else if(ann instanceof DefaultIntValue) {
                        defaultValue = ((DefaultIntValue)ann).value();
                    } else if(ann instanceof DefaultFloatValue) {
                        defaultValue = ((DefaultFloatValue)ann).value();
                    } else if(ann instanceof DefaultMessage) {
                        defaultValue = ((DefaultMessage)ann).value();
                    }
                }
                if(methodKey == null) {
                    methodKey = keyGenerator.generateKey(ifaceName, method.getName(), String.valueOf(defaultValue), meaning);
                }
                CtMethod methodImpl = new CtMethod(method, impl, null);
                CtClass returnType = methodImpl.getReturnType();
                Initializer initializer;
                String valueString;
                try {
                    valueString = resBundle.getString(methodKey);
                } catch(MissingResourceException mre) {
                    if(defaultValue != null)
                        valueString = String.valueOf(defaultValue);
                    else
                        throw mre;
                }
                if(returnType.isPrimitive()) {
                    if(returnType.getSimpleName().equals("int"))
                        initializer = CtField.Initializer.constant(Integer.parseInt(valueString));
                    else if(returnType.getSimpleName().equals("float"))
                        initializer = CtField.Initializer.byExpr(Float.parseFloat(valueString)+"f");
                    else if(returnType.getSimpleName().equals("double"))
                        initializer = CtField.Initializer.constant(Double.parseDouble(valueString));
                    else if(returnType.getSimpleName().equals("boolean"))
                        initializer = CtField.Initializer.byExpr(String.valueOf(Boolean.parseBoolean(valueString)));
                    else throw new IllegalStateException(returnType+" is not a supported primitive return type of a constant in "+iface.getName());
                } else if(returnType.equals(stringClass)) {
                    initializer = CtField.Initializer.constant(valueString);
                } else if(returnType.getSimpleName().equals("Map")){
                    CtField keyField = new CtField(stringClass, method.getName().toUpperCase(), impl);
                    keyField.setModifiers(Modifier.FINAL|Modifier.STATIC);
                    impl.addField(keyField, CtField.Initializer.constant(methodKey));
                    initializer = CtField.Initializer.byExpr("getMap("+keyField.getName()+")");
                } else if(returnType.getSimpleName().equals("String[]")){
                    CtField keyField = new CtField(stringClass, method.getName().toUpperCase(), impl);
                    keyField.setModifiers(Modifier.FINAL|Modifier.STATIC);
                    impl.addField(keyField, CtField.Initializer.constant(methodKey));
                    initializer = CtField.Initializer.byExpr("getStringArray("+keyField.getName()+")");
                    returnType = classPool.get(String[].class.getName());
                } else throw new IllegalStateException(returnType+" is not a supported return type of a constant in "+iface.getName());
                
                CtField valueField = new CtField(returnType, method.getName(), impl);
                impl.addField(valueField, initializer);
                //sb.append("this."+valueField.getName()+" = get"+getter+"(\""+methodKey+"\"));\n");
                methodImpl.setModifiers(Modifier.PUBLIC);
                methodImpl.setBody("return this."+valueField.getName()+";");
                impl.addMethod(methodImpl);
            }
            CtConstructor ctor = new CtConstructor(new CtClass[]{resourceBundle}, impl);
            ctor.setModifiers(Modifier.PUBLIC);
            ctor.setExceptionTypes(new CtClass[] {classPool.get(MissingResourceException.class.getName())});
            impl.addConstructor(ctor);
            ctor.setBody("super($1);");
            
            //System.out.println("impl name is "+impl.getName()+" iface name is "+ifaceName);
            Class<T> implClass = impl.toClass();
            T instance = implClass.getConstructor(ResourceBundle.class).newInstance(resBundle);
            
            cache.put(key, instance);
            return instance;
            
        } catch (NotFoundException e) {
            throw new Error(e);
        } catch (RuntimeException e) {
            throw new Error(e);
        } catch (CannotCompileException e) {
            throw new Error(e);
        } catch (InvocationTargetException e) {
            throw new Error(e);
        } catch (NoSuchMethodException e) {
            throw new Error(e);
        } catch (ClassNotFoundException e) {
            throw new Error(e);
        } catch (InstantiationException e) {
            throw new Error(e);
        } catch (IllegalAccessException e) {
            throw new Error(e);
        }
    }
}
