package com.habitsoft.kiyaa.util;

public interface RetryController {

    /**
     * Return true if the service method should be retried.
     * 
     * For example, to retry when we get certain unusual status codes
     * from internet explorer and firefox due to some SSL implementation
     * issues:
     *  
     * return caught instanceof StatusCodeException && (((StatusCodeException)caught).getStatusCode() > 600 || ((StatusCodeException)caught).getStatusCode() < 100)
     */
    public boolean shouldRetry(Throwable caught, int retriesSoFar);

    /**
     * Return the number of milliseconds before the next retry.  If zero,
     * retries immediately. Otherwise, creates a timer that will retry
     * after the given time elapses.
     */
    public int getRetryDelay(Throwable caught, int retriesSoFar);

}