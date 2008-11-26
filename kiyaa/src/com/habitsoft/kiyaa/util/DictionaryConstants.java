package com.habitsoft.kiyaa.util;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A combination of Dictionary and Constants - this allows us to
 * put some constants in the page, and still use them with statically
 * typed accessors.
 * 
 * Use the @Dictionary annotation to specify the name of the javascript
 * object to load; by default it uses the name of the class with the
 * first letter changed to lower-case.
 * 
 * When generating the host page (or a javascript loaded by the host page)
 * you should specify the value with the correct type, i.e.
 * 
 * <script>
 * var myDict = {
 *    someBool: true,
 *    someInt: 7,
 *    someFloat: 3.5f,
 *    someDouble: 31415927
 * };
 * </script>
 * 
 * The generated subclass utilizes GWT's built-in javascript-to-java
 * type conversions and won't parse any strings.
 */
public interface DictionaryConstants {
    /**
     * Specifies the name of the javascript variable containing the
     * name of the dictionary to use.  If this is not specified,
     * the name of the class is used, with the first letter changed
     * to lower case.  For example:
     * 
     * class MyConstants implements DictionaryConstants // loads myConstants javascript dictionary
     * 
     * @Dictionary("foo")
     * class MyConstnats implements DictionaryConstants // loads foo javascript dictionary
     *
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface Dictionary {
      String value();
    }
    
    /**
     * Default boolean value to be used if no translation is found (and also used as the
     * source for translation).  No quoting (other than normal Java string quoting)
     * is done.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    @Documented
    public @interface DefaultBooleanValue {
      boolean value();
    }

    /**
     * Default double value to be used if no translation is found (and also used as the
     * source for translation).  No quoting (other than normal Java string quoting)
     * is done.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    @Documented
    public @interface DefaultDoubleValue {
      double value();
    }

    /**
     * Default float value to be used if no translation is found (and also used as the
     * source for translation).  No quoting (other than normal Java string quoting)
     * is done.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    @Documented
    public @interface DefaultFloatValue {
      float value();
    }

    /**
     * Default integer value to be used if no translation is found (and also used as the
     * source for translation).  No quoting (other than normal Java string quoting)
     * is done.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    @Documented
    public @interface DefaultIntValue {
      int value();
    }

    /**
     * Default string value to be used if no translation is found (and also used as the
     * source for translation).  No quoting (other than normal Java string quoting)
     * is done.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    @Documented
    public @interface DefaultStringValue {
      String value();
    }

    /**
     * The key used for lookup of translated strings.  If not present, the
     * key will be generated based on the {@code @GenerateKeysUsing} annotation,
     * or the unqualified method name if it is not present.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface Key {
      String value();
    }

}
