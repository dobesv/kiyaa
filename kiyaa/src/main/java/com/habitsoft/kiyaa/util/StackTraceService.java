package com.habitsoft.kiyaa.util;

import com.google.gwt.user.client.rpc.RemoteService;

public interface StackTraceService extends RemoteService {

	/**
	 * Log an exception in the server log with an un-obfuscated stack trace.
	 * 
	 * @param moduleName Return value of GWT.getModuleName() on the client side
	 * @param strongName Return value of GWT.getPermutationStrongName() on the client side
	 * @param exceptionClassName Exception class name
	 * @param message Exception message (getMessage() return value)
	 * @param obfuscatedTrace Obfuscated stack trace, the method names returned by getStackTrace() on the client side
	 */
	public void log(String moduleName, String strongName, String loggerName, String exceptionClass, String message, StackTraceElement[] obfuscatedTrace);

	/**
	 * Return a new strack trace, replacing stack information where missing, if available.
	 * 
	 * Afterwards, the stack trace will have been improved as best we could.
	 * @param strongName Return value of GWT.getPermutationStrongName() on the client side
	 * @param obfuscatedTrace Obfuscated stack trace, returned by getStackTrace() on the client side
	 * 
	 * @return A new array with some or all stack trace elements replaced
	 */
	public StackTraceElement[] deobfuscateStackTrace(String strongName,
			StackTraceElement[] obfuscatedTrace);

	/**
	 * Log the exception and also return the de-obfuscated stack trace.  This is useful
	 * for displaying the stack trace client-side.
	 */
	public StackTraceElement[] logAndDeobfuscate(String moduleName, String strongName,
			String loggerName, String exceptionClass, String message,
			StackTraceElement[] obfuscatedTrace);
	
}
