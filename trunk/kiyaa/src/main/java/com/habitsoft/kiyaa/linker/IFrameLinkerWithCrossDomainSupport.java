package com.habitsoft.kiyaa.linker;

import com.google.gwt.core.ext.LinkerContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.linker.ArtifactSet;
import com.google.gwt.core.linker.IFrameLinker;

/**
 * GWT linker that prepends the output files so that scripts can be served from a different SUB-domain as the host page.
 * 
 * It does this by setting document.domain to the main domain for your site.  mydomain.com, static.mydomain.com, and www.mydomain.com
 * would all set document.domain = 'mydomain.com'.
 * 
 * Note that if the host page is using SSL, the GWT code must also be served using SSL.
 * 
 * For example:
 * 
 * <table>
 * <thead><tr><th>Host Page</th><th>GWT File</th><th>Should Work?</th></tr></thead>
 * <tbody>
 * <tr><td>http://www.mydomain.com/myapp.html</td><td>http://static.mydomain.com/gwt/com.mydomain.MyApp/MyApp.cache.js</td><td>YES</td></tr>
 * <tr><td>http://mydomain.com/myapp.html</td><td>http://static.mydomain.com/gwt/com.mydomain.MyApp/MyApp.cache.js</td><td>YES</td></tr>
 * <tr><td>http://www.mydomain.com/myapp.html</td><td>http://mydomain.com/gwt/com.mydomain.MyApp/MyApp.cache.js</td><td>YES</td></tr>
 * <tr><td>https://www.mydomain.com/myapp.html</td><td>https://static.mydomain.com/gwt/com.mydomain.MyApp/MyApp.cache.js</td><td>YES</td></tr>
 * <tr><td>http://www.mydomain.com/myapp.html</td><td>http://static.anotherdomain.com/gwt/com.mydomain.MyApp/MyApp.cache.js</td><td>NO</td></tr>
 * <tr><td>http://www.mydomain.com/myapp.html</td><td>https://static.mydomain.com/gwt/com.mydomain.MyApp/MyApp.cache.js</td><td>NO</td></tr>
 * <tr><td>https://www.mydomain.com/myapp.html</td><td>http://static.mydomain.com/gwt/com.mydomain.MyApp/MyApp.cache.js</td><td>NO</td></tr>
 * </tbody>
 * </table>
 * 
 * @author dobes
 *
 */
public class IFrameLinkerWithCrossDomainSupport extends IFrameLinker {
	String FIX_DOMAIN_JS="document.domain=document.domain.replace(/(?:[^.]+\\.)?([^.]+\\.[^.]+)/i, '\\$1')";
	
	@Override
	protected String getModulePrefix(TreeLogger logger, LinkerContext context,
			String strongName) {
		String basePrefix = super.getModulePrefix(logger, context, strongName);
		return basePrefix.replaceFirst("<script>(\\s*)", "<script>$1"+FIX_DOMAIN_JS+"$1");
	}
	
	@Override
	protected String getModulePrefix(TreeLogger logger, LinkerContext context,
			String strongName, int numFragments) {
		String basePrefix = super.getModulePrefix(logger, context, strongName, numFragments);
		return basePrefix.replaceFirst("<script>(\\s*)", "<script>$1"+FIX_DOMAIN_JS+"$1");
	}
	
	@Override
	protected String generateSelectionScript(TreeLogger logger,
			LinkerContext context, ArtifactSet artifacts)
			throws UnableToCompleteException {
		return FIX_DOMAIN_JS+"\n"+
				super.generateSelectionScript(logger, context, artifacts);
	}
}
