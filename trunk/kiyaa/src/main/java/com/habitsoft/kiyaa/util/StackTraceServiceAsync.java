package com.habitsoft.kiyaa.util;

import com.google.gwt.user.client.rpc.AsyncCallback;

public interface StackTraceServiceAsync {

	/**
	 * Log an exception in the server log with an un-obfuscated stack trace.
	 * 
	 * @param moduleName Return value of GWT.getModuleName() on the client side
	 * @param strongName Return value of GWT.getPermutationStrongName() on the client side
	 * @param exceptionClassName Exception class name
	 * @param message Exception message (getMessage() return value)
	 * @param obfuscatedTrace Obfuscated stack trace, getStackTrace() on the client side
	 */
	void log(String moduleName, String strongName, String loggerName,
			String exceptionClass, String message,
			StackTraceElement[] obfuscatedTrace, AsyncCallback<Void> callback);

	/**
	 * Return a new strack trace, replacing stack information where missing, if available.
	 * 
	 * Afterwards, the stack trace will have been improved as best we could.
	 * 
	 * @param moduleName Return value of GWT.getModuleName() on the client side
	 * @param strongName Return value of GWT.getPermutationStrongName() on the client side
	 * @param obfuscatedTrace Obfuscated stack trace, returned by getStackTrace() on the client side
	 * @return A new array with some or all stack trace elements replaced
	 */
	void deobfuscateStackTrace(String moduleName, String strongName,
			StackTraceElement[] obfuscatedTrace,
			AsyncCallback<StackTraceElement[]> callback);

}
