package com.habitsoft.kiyaa.server;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import com.google.gwt.user.client.rpc.IncompatibleRemoteServiceException;
import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.server.rpc.RPC;
import com.google.gwt.user.server.rpc.RPCRequest;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import com.google.gwt.user.server.rpc.SerializationPolicy;
import com.google.gwt.user.server.rpc.SerializationPolicyLoader;

/**
 *
 * Wrap an EJB into a GWT RPC servlet, providing access from GWT to a
 * stateless bean.
 * 
 * To use this, create a subclass like:
 * 
 * <code>
 * class MyBeanServlet extends GwtEJBWrapperServlet&lt;MyBean&gt; {
 *    @EJB
 *    MyBean bean;
 *    
 *    protected MyBean getBean() {
 *    	return bean;
 *    }
 * }
 * </code>
 * 
 * The bean must implement the GWT RemoteService interface that is being used
 * by the client code.
 * 
 */
public abstract class GwtEJBWrapperServlet<T> extends RemoteServiceServlet {
	private static final long serialVersionUID = 1L;

	public GwtEJBWrapperServlet() {
		super();
	}

	protected abstract T getBean();

	/**
	 * Gets the {@link SerializationPolicy} for given module base URL and strong
	 * name if there is one.
	 * 
	 * Override this method to provide a {@link SerializationPolicy} using an
	 * alternative approach.
	 * 
	 * @param request the HTTP request being serviced
	 * @param moduleBaseURL as specified in the incoming payload
	 * @param strongName a strong name that uniquely identifies a serialization
	 *          policy file
	 * @return a {@link SerializationPolicy} for the given module base URL and
	 *         strong name, or <code>null</code> if there is none
	 */
	@Override
	protected SerializationPolicy doGetSerializationPolicy(HttpServletRequest request, String moduleBaseURL, String strongName) {
		// The request can tell you the path of the web app relative to the
		// container root.
		String contextPath = request.getContextPath();
	
		String modulePath = null;
		if (moduleBaseURL != null) {
			try {
				modulePath = new URL(moduleBaseURL).getPath();
			} catch (MalformedURLException ex) {
				// log the information, we will default
				getServletContext().log(
						"Malformed moduleBaseURL: " + moduleBaseURL, ex);
			}
		}
	
		SerializationPolicy serializationPolicy = null;
	
		// Strip off the context path from the module base URL. It should be a
		// strict prefix.
		String contextRelativePath = modulePath.startsWith(contextPath)?modulePath.substring(contextPath.length()):"";
	
		String serializationPolicyFilePath = SerializationPolicyLoader
				.getSerializationPolicyFileName(contextRelativePath
						+ strongName);
	
		// Open the RPC resource file read its contents.
		InputStream is = getServletContext().getResourceAsStream(
				serializationPolicyFilePath);
		try {
			if (is != null) {
				try {
					List<ClassNotFoundException> classNotFoundExceptions = new ArrayList<ClassNotFoundException>();
					serializationPolicy = SerializationPolicyLoader
							.loadFromStream(is, classNotFoundExceptions);
					for(ClassNotFoundException cnfe : classNotFoundExceptions) {
						getServletContext()
						.log(
								"ERROR: Could not find class '"
										+ cnfe.getMessage()
										+ "' listed in the serialization policy file '"
										+ serializationPolicyFilePath
										+ "'"
										+ "; your server's classpath may be misconfigured",
										cnfe);
					}
				} catch (ParseException e) {
					getServletContext().log(
							"ERROR: Failed to parse the policy file '"
									+ serializationPolicyFilePath + "'", e);
				} catch (IOException e) {
					getServletContext().log(
							"ERROR: Could not read the policy file '"
									+ serializationPolicyFilePath + "'", e);
				}
			} else {
				String message = "ERROR: The serialization policy file '"
						+ serializationPolicyFilePath
						+ "' was not found; did you forget to include it in this deployment?";
				getServletContext().log(message);
			}
			return serializationPolicy;
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (IOException e) {
					// Ignore this error
				}
			}
		}
	}

	@Override
	public String processCall(String payload) throws SerializationException {
		try {
			Object bean = getBean();
			RPCRequest rpcRequest = RPC.decodeRequest(payload, bean
					.getClass(), this);
			return RPC.invokeAndEncodeResponse(bean,
					rpcRequest.getMethod(), rpcRequest.getParameters(),
					rpcRequest.getSerializationPolicy());
		} catch (IncompatibleRemoteServiceException ex) {
			getServletContext().log("An IncompatibleRemoteServiceException was thrown while processing this call.", ex);
			return RPC.encodeResponseForFailure(null, ex);
		}
	}

}