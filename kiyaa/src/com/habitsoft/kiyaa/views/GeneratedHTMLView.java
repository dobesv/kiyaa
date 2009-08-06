package com.habitsoft.kiyaa.views;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


public abstract interface GeneratedHTMLView extends View {

    /**
     * Specify the template path for a Generated HTML View relative 
     * to the source file of the class (.java) file.
     */
    @Retention(RetentionPolicy.CLASS)
    @Target(value=ElementType.TYPE)
    public @interface TemplatePath {
        String value();
    }

    /**
     * Annotation for an a method used as an action in a template
     * to specify whether to saveBefore and loadAfter.  By default
     * both are true.
     */
    @Retention(RetentionPolicy.CLASS)
    @Target(value=ElementType.TYPE)
    public @interface ActionMethod {
        boolean saveBefore() default true;
        boolean loadAfter() default true;
    }
    

}
