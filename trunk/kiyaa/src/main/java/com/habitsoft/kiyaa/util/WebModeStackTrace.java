package com.habitsoft.kiyaa.util;


public class WebModeStackTrace {

	public static native String getStack(Throwable e) /*-{
		// Firefox
		if(e.stack)
			return e.stack;
			
		// Opera
		if(window.opera && e.message)
			return e.message;
			
		// Else IE or Safari
	    var currentFunction = arguments.callee.caller;
	    var callstack = 'stack trace:';
	    while (currentFunction && callstack.length < 99) {
	      var fn = currentFunction.toString();
	      var fname = fn.substring(fn.indexOf("function") + 8, fn.indexOf("(")) || "anonymous";
	      if(callstack.indexOf(fname) != -1) {
	         callstack += '\n'+fname+' (mutually recursive calls - remaining stack trace unavailable)';
	      }
	      callstack += '\n'+fname;
	      currentFunction = currentFunction.caller;
	    }
	    return callstack;
	}-*/;
}
