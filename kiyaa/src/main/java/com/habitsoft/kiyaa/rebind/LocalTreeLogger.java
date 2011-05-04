/**
 * 
 */
package com.habitsoft.kiyaa.rebind;

import java.util.LinkedList;

import com.google.gwt.core.ext.TreeLogger;

public class LocalTreeLogger extends TreeLogger {
	static final ThreadLocal<TreeLogger> delegate = new ThreadLocal<TreeLogger>();
	static final ThreadLocal<LinkedList<TreeLogger>> stack = new ThreadLocal<LinkedList<TreeLogger>>() {
		protected java.util.LinkedList<TreeLogger> initialValue() { return new LinkedList<TreeLogger>(); }
	};

	public TreeLogger branch(Type type, String msg, Throwable caught, HelpInfo helpInfo) {
		return delegate.get().branch(type, msg, caught, helpInfo);
	}

	public boolean isLoggable(Type type) {
		return delegate.get().isLoggable(type);
	}

	public void log(Type type, String msg, Throwable caught, HelpInfo helpInfo) {
		delegate.get().log(type, msg, caught, helpInfo);
	}
	
	public static void pushLogger(TreeLogger logger) {
		stack.get().add(delegate.get());
		delegate.set(logger);
	}
	
	public static void popLogger() {
		TreeLogger previous = stack.get().removeLast();
		if(previous == null) {
			delegate.remove();
			stack.remove();
		} else {
			delegate.set(previous);
		}
	}
	
	public static final LocalTreeLogger logger = new LocalTreeLogger();
}