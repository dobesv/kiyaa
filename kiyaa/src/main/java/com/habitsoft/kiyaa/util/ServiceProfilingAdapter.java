package com.habitsoft.kiyaa.util;

/**
 * Marker class for a generator that allows you to
 * output the ms taken to run each async call to the
 * given RemoteService.
 * 
 * Example use:
 * 
 * class MyServiceProfilingAdapter implements ServiceProfilingAdapter<MyServiceAsync> { }
 * 
 * public void startProfiling() {
 *      MyServiceProfilingAdapter profiling = GWT.create(MyServiceProfilingAdapter.class);
 *      MyServiceAsync service = GWT.create(MyService.class);
 *      service = profiling.getProxy(service);
 * }
 */
public interface ServiceProfilingAdapter<T> {
    /**
     * This method wraps the given instance in a profiling
     * implementation.
     */
    public T getProxy(T delegate);
}
