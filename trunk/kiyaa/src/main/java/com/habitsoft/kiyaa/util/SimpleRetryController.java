package com.habitsoft.kiyaa.util;

import com.google.gwt.user.client.rpc.StatusCodeException;

/**
 * Implementation of RetryController containing some general rules
 * that may be useful in various GWT applications.
 * 
 * With the default of 16 retries, and each retry waits for
 * the number of previous retries in seconds, the
 * wait time before the failure is passed through, assuming it
 * persists, is 120 seconds, or two minutes.  It seems like a long
 * time, but it's sometimes just enough for an EAR deployment to
 * complete.
 * 
 */
public class SimpleRetryController implements RetryController {
    int maxRetries=16;
    
    public SimpleRetryController() {
    }
    public SimpleRetryController(int maxRetries) {
        this.maxRetries = maxRetries;
    }
    public int getMaxRetries() {
        return maxRetries;
    }
    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }
    
    public int getRetryDelay(Throwable caught, int retriesSoFar) {
        return retriesSoFar*1000;
    }
    
    public boolean shouldRetry(Throwable caught, int retriesSoFar) {
        if(caught instanceof StatusCodeException) {
            StatusCodeException sce = (StatusCodeException)caught;
            int code = sce.getStatusCode();
            // Treat any service temporarily unavailable or "special" IE/Firefox status codes like 0 or 12029
            // as something worth retrying
            // code 0 - Firefox returns this if there's an unexpected connection issue
            // code 12027 and other 12xxx codes are returned by IE when it's being buggy
            // 404 Not Found - Assume the application is being restarted or redeployed, why would be do RPC calls to the wrong URL?
            // 408 Timed Out - probably a temporary issue
            // 502, 503, 504 - Various errors returned by apache if mod_proxy fails to connect or loses connection during a session
            //
            return (code == 0 || code > 1000 || code == 404 || code == 408 || code == 502 || code == 503 || code == 504);
        }
        return false;
    }

}
