package com.habitsoft.kiyaa.util;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.google.gwt.i18n.client.LocalizableResource;

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
 * myDict = {
 *    someBool: true,
 *    someInt: 7,
 *    someFloat: 3.5f,
 *    someDouble: 31415927
 * };
 * </script>
 * 
 * The generated subclass utilizes GWT's built-in javascript-to-java
 * type conversions and won't parse any strings.
 * 
 * This class could be used for cross-site data loading,
 * whereby you fetch a javascript file that sets the variable loaded
 * by this class and invokes a callback that tells your code that the
 * data is ready.  To do so you would insert a <script> tag into the
 * DOM whose source points to your servlet, and your servlet would
 * return the appropriate javascript to update your GWT data.
 * 
 * This is useful when you want to embed your GWT app on a different site
 * than your server code runs on - due to the "single origin policy"
 * this is the only practical way to load data from another site - by 
 * requesting a javascript file with that data in it.
 */
public interface DictionaryConstants extends LocalizableResource {
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

}
