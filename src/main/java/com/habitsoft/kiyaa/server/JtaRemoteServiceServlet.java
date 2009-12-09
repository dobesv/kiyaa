package com.habitsoft.kiyaa.server;

import javax.annotation.Resource;
import javax.transaction.NotSupportedException;
import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;

import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;

/**
 * Wrap a JTA transaction around processCall
 */

public abstract class JtaRemoteServiceServlet extends RemoteServiceServlet {
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
