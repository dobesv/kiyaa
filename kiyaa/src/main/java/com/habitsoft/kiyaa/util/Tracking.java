package com.habitsoft.kiyaa.util;

public class Tracking {

	static boolean googleAnalytics=haveGoogle();
	static boolean clicky=haveClicky();
	
	static native void clickyLog(String href, String title) /*-{
		$wnd.clicky.log(href, title);
	}-*/;
	
	static native void clickyLog(String href, String title, String type) /*-{
    	$wnd.clicky.log(href, title, type);
    }-*/;
	
	static native void googleTrackPageView(String href) /*-{
		$wnd.pageTracker._trackPageview(href);
	}-*/;
	
	static native boolean haveGoogle() /*-{
		return $wnd.pageTracker != undefined;
	}-*/;

	static native boolean haveClicky() /*-{
    	return $wnd.clicky != undefined;
    }-*/;
	
	public static void logNavigation(String token) {
		if(googleAnalytics) {
			try {
                String href = "/app/_"+token;
				googleTrackPageView(href);
			} catch(Throwable t) {
				// oh well ...
			}
		}
		if(clicky) {
			try {
		        String href = "/app/#"+token;
				clickyLog(href, href);
			} catch(Throwable t) {
				// oh well ...
			}
		}
	}
	
	public static void logDownload(String href, String title) {
		if(googleAnalytics) {
			try {
				googleTrackPageView(href);
			} catch(Throwable t) {
				// oh well ...
			}
		}
		if(clicky) {
			try {
				clickyLog(href, title, "download");
			} catch(Throwable t) {
				// oh well ...
			}
		}
		
	}

	public static void logError(String message) {
		if(googleAnalytics) {
			try {
				googleTrackPageView("/app/_error/"+message);
			} catch(Throwable t) {
				// oh well ...
			}
		}
		if(clicky) {
			try {
				clickyLog("/app/#error", message, "error");
			} catch(Throwable t) {
				// oh well ...
			}
		}
	}

	public static void logUrl(String newUrl) {
		if(googleAnalytics) {
			try {
				googleTrackPageView(newUrl);
			} catch(Throwable t) {
				// oh well ...
			}
		}
		if(clicky) {
			try {
				clickyLog(newUrl, newUrl, "click");
			} catch(Throwable t) {
				// oh well ...
			}
		}
	}

	/**
	 * Track some kind of special conversion-related event.
	 */
	public static void logConversion(String event) {
		if(googleAnalytics) {
			try {
		        String href = "/app/_conversion/"+event;
				googleTrackPageView(href);
			} catch(Throwable t) {
				// oh well ...
			}
		}
		if(clicky) {
			try {
		        String href = "/app/_conversion/"+event;
				clickyLog(href, event, "conversion");
			} catch(Throwable t) {
				// oh well ...
			}
		}
	}
}
