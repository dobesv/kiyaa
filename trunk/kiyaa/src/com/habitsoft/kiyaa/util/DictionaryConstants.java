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
     * Default string array value to be used if no translation is found (and also
     * used as the source for translation). No quoting (other than normal Java
     * string quoting) is done.
     * 
     * Note that in the corresponding properties/etc file, commas are used to separate
     * elements of the array unless they are preceded with a backslash.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    @Documented
    public @interface DefaultStringArrayValue {
      String[] value();
    }

    /**
     * Default string map value to be used if no translation is found (and also
     * used as the source for translation). No quoting (other than normal Java
     * string quoting) is done.  The strings for the map are supplied in key/value
     * pairs.
     * 
     * Note that in the corresponding properties/etc file, new keys can be supplied
     * with the name of the method (or its corresponding key) listing the set of keys
     * for the map separated by commas (commas can be part of the keys by preceding
     * them with a backslash).  In either case, further entries have keys matching
     * the key in this map.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    @Documented
    public @interface DefaultStringMapValue {
      /**
       * Must be key-value pairs.
       */
      String[] value();
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
