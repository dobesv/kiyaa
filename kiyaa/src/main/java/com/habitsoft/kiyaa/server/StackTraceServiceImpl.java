package com.habitsoft.kiyaa.server;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;

import javax.servlet.ServletContext;

import org.apache.commons.io.IOUtils;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import com.habitsoft.kiyaa.util.StackTraceService;

public class StackTraceServiceImpl extends RemoteServiceServlet implements StackTraceService {
	private static final long serialVersionUID = 1L;
	
	@Override
	public void log(String moduleName, String strongName, String loggerName, String message, String exceptionClassName, StackTraceElement[] obfuscatedTrace) {
		Throwable exception = createException(moduleName, strongName, exceptionClassName, message, obfuscatedTrace);
		log(loggerName, message, exception);
	}


	private void log(String loggerName, String message, Throwable exception) {
		java.util.logging.Logger.getLogger(loggerName).log(
				Level.SEVERE, message,
				exception);
	}


	/**
	 * Create a Throwable instance with the stack trace filled in with the data available (if any).
	 * 
	 * If we can't find a symbol map, this'll dump out the obfuscation function names anyway, just for the hell of it.
	 * 
	 * @param moduleName Return value of GWT.getModuleName() on the client side
	 * @param strongName Return value of GWT.getPermutationStrongName() on the client side
	 * @param exceptionClassName Exception class name
	 * @param message Exception message (getMessage() return value)
	 * @param obfuscatedTrace Obfuscated stack trace, returned by getStackTrace() on the client side
	 * @return A Throwable instance that can be logged
	 */
	public Throwable createException(String moduleName, String strongName,
			String exceptionClassName, String message, StackTraceElement[] obfuscatedTrace) {
		StackTraceElement[] stackTrace = deobfuscateStackTrace(strongName,
				obfuscatedTrace);
		
		return createException(exceptionClassName, message, stackTrace);
		
	}


	private Throwable createException(String exceptionClassName,
			String message, StackTraceElement[] stackTrace) {
		Throwable exceptionInstance;
		// Let's create an exception object
		try {
			Class<?> exceptionClass = Class.forName(exceptionClassName);
			if(message != null) {
				// Find a new Exception(String message) ctor
				exceptionInstance = (Throwable) exceptionClass.getConstructor(String.class).newInstance(message);
			} else {
				exceptionInstance = (Throwable) exceptionClass.newInstance();
			}
		} catch (Exception e) {
			// Class or constructor not found, just fake it with an Error instance
			exceptionInstance = new Error(exceptionClassName+(message==null?"":": "+message));
		}
		exceptionInstance.setStackTrace(stackTrace);
		return exceptionInstance;
	}

	/**
	 * Return a new stack trace, replacing stack information where missing, if available.
	 * 
	 * Afterwards, the stack trace will have been improved as best we could.
	 * @param strongName Return value of GWT.getPermutationStrongName() on the client side
	 * @param obfuscatedTrace Obfuscated stack trace, returned by getStackTrace() on the client side
	 * 
	 * @return A new array with some or all stack trace elements replaced
	 */
	@Override
	public StackTraceElement[] deobfuscateStackTrace(String strongName,
			StackTraceElement[] obfuscatedTrace) {
		HashSet<String> functionsToLookFor = new HashSet<String>(obfuscatedTrace.length);
		for(StackTraceElement frame : obfuscatedTrace) {
			if(isMissingData(frame))
				functionsToLookFor.add(frame.getMethodName());
		}
		if(functionsToLookFor.isEmpty())
			return obfuscatedTrace; // No missing methods
		HashMap<String,StackTraceElement> symbolMap = new HashMap<String, StackTraceElement>(obfuscatedTrace.length);
		
		loadSymbolMap(strongName, functionsToLookFor, symbolMap);
		
		// Now make that into a stack trace
		StackTraceElement[] stackTrace = new StackTraceElement[obfuscatedTrace.length];
		for(int i=0; i < obfuscatedTrace.length; i++) {
			StackTraceElement obframe = obfuscatedTrace[i];
			if(isMissingData(obframe)) {
				StackTraceElement replacement = symbolMap.get(obframe.getMethodName());
				if(replacement != null)
					stackTrace[i] = replacement;
				else
					stackTrace[i] = obframe;
			} else {
				stackTrace[i] = obframe;
			}
		}
		return stackTrace;
	}

	@Override
	public StackTraceElement[] logAndDeobfuscate(String moduleName,
			String strongName, String loggerName, String exceptionClassName,
			String message, StackTraceElement[] obfuscatedTrace) {
		StackTraceElement[] trace = deobfuscateStackTrace(strongName, obfuscatedTrace);
		log(loggerName, message, createException(exceptionClassName, message, trace));
		return trace;
	}

	private boolean isMissingData(StackTraceElement obframe) {
		return obframe.getLineNumber() <= 0 || obframe.getFileName() == null || obframe.getFileName().startsWith("Unknown") || obframe.getClassName().equals("Unknown");
	}


	protected void loadSymbolMap(String strongName,
			HashSet<String> functionsToLookFor,
			HashMap<String, StackTraceElement> symbolMap) throws Error {
		InputStream symbolMapStream = findSymbolMapFile(strongName);
		if(symbolMapStream != null)
			try {
				BufferedReader reader = new BufferedReader(new InputStreamReader(symbolMapStream, "utf8"));
				
				String line;
				while((line = reader.readLine()) != null) {
					int idx = line.indexOf(',');
					if(idx == -1)
						continue;
					String jsName = line.substring(0, idx);
					if(functionsToLookFor.remove(jsName)) {
						String[] parts = line.split(",");
					    String className = parts[2];
					    String memberName = parts[3];
					    String fileName = parts[4];
					    String sourceLine = parts[5];
	
					    // The sourceUri contains the actual file name.
					    String sourceFileName = fileName.substring(fileName.lastIndexOf('/') + 1,
					        fileName.length());
					    
						symbolMap.put(jsName, new StackTraceElement(className, memberName, sourceFileName, Integer.parseInt(sourceLine)));
					}
					
				}
			} catch (UnsupportedEncodingException e) {
				throw new Error(e);
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				IOUtils.closeQuietly(symbolMapStream);
			}
	}
	
	/**
	 * Override this with your own implementation if you know where your symbolMap files are
	 * 
	 * @return An input stream reading the appropriate symbolMap, or null if none was found.
	 */
	protected InputStream findSymbolMapFile(String strongName) {
		// Try build/gwt/extra/<moduleName>/symbolMaps
		String basename = strongName+".symbolMap";
		
		// Try war/WEB-INF/deploy/*/symbolMaps/<strongName>
		File deployDir = new File("war/WEB-INF/deploy");
		for(File moduleDir : deployDir.listFiles()) {
			if(!moduleDir.isDirectory())
				continue;
			File symbolMapFile = new File(new File(deployDir, "symbolMaps"), basename);
			if(symbolMapFile.canRead()) {
				try {
					return new FileInputStream(symbolMapFile);
				} catch (FileNotFoundException e) {
					// ??
				}
			}
		}
		
		// Try WEB-INF/deploy/<module>/symbolMaps in our servlet context
		ServletContext servletContext = getServletContext();
		@SuppressWarnings("unchecked")
		Set<String> resourcePaths = servletContext.getResourcePaths("/WEB-INF/deploy/");
		for(String moduleDeployPath : resourcePaths) {
			String resourcePath = moduleDeployPath+"symbolMaps/"+basename;
			InputStream webInfFile = servletContext.getResourceAsStream(resourcePath);
			if(webInfFile != null) return webInfFile;
		}
		// Oh well, give up
		return null;
	}
}
