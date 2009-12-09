package com.habitsoft.kiyaa.server;

import javax.annotation.Resource;
import javax.transaction.NotSupportedException;
import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;

import com.google.gwt.user.client.rpc.SerializationException;

/**
 * Allows you to delegate all calls to the service to another object (bean)
 * instead of implementing them as part of the servlet.  Additionally
 * this will wrap each call in a JTA transaction using UserTransaction.
 * 
 * Note that if your bean is not an EJB it should be made thread-safe,
 * since the same servlet instance may be shared by multiple threads
 * in some cases (like when there are a lot of requests coming in).
 * 
 * Failure to use a thread-safe bean would result in bugs that only
 * appear when you have a lot of users, which is the worst time to find 
 * these.
 * 
 * It's worth pointing out that the EntityManager class is *not* 
 * considered thread-safe.
 */
public abstract class GwtDelegatingServletWithJta<T> extends GwtEJBWrapperServlet<T> {
    private static final long serialVersionUID = 1L;

    @Resource
    protected UserTransaction utx;

    @Override
    public String processCall(String payload) throws SerializationException {
        try {
            utx.begin();
        } catch (NotSupportedException caught) {
            throw new Error(caught);
        } catch (SystemException caught) {
            throw new Error(caught);
        }
        try {
            return super.processCall(payload);
        } catch(SerializationException t) {
            try {
                utx.setRollbackOnly();
            } catch(Throwable t2) { getServletContext().log("Failed to mark transaction for rollback", t2); }
            throw t;
        } finally {
            try {
                if(utx.getStatus() == Status.STATUS_ACTIVE)
                    utx.commit(); 
            } catch(Throwable t) {
                getServletContext().log("Failed to commit transaction", t);
            }
        }
    }
}
