package com.habitsoft.kiyaa.util;

/**
 * Marker interface to indicate that a concrete subclass should be 
 * generated which implements methods of the interface T with
 * retry capability.
 * 
 * The class should implement the methods of retrying; the generated
 * subclass provides an implementation of the interface which is
 * the type parameter.
 */
public interface ServiceRetryingAdapter<T> {
    /**
     * By default, the generated implementation uses an instance
     * of SimpleRetryController.  Use this method to supply
     * your own retry logic.
     */
    public void setController(RetryController controller);
    
    /**
     * This method wraps the given instance in a retrying
     * implementation.
     */
    public T getProxy(T delegate);
}
