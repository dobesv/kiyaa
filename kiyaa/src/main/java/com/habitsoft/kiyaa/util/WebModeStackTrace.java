package com.habitsoft.kiyaa.util;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JsonUtils;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.ServiceDefTarget;

public class WebModeStackTrace {

	/**
	 * Visible for testing.
	 */
	public static String getStack(Throwable t) {
		StringBuilder sb = new StringBuilder();
		boolean needsComma = false;
		for (StackTraceElement e : t.getStackTrace()) {
			if (needsComma) {
				sb.append("\n");
			} else {
				needsComma = true;
			}

			sb.append(JsonUtils.escapeValue(e.getMethodName()));
		}
		return sb.toString();
	}
	
	public static void log(String serviceUrl, String loggerName, Throwable t) {
		if(GWT.isScript()) {
			StackTraceServiceAsync service = GWT.create(StackTraceService.class);
			((ServiceDefTarget)service).setServiceEntryPoint(serviceUrl);
			service.log(GWT.getModuleName(), GWT.getPermutationStrongName(), loggerName, t.getClass().getName(), t.getMessage(), t.getStackTrace(), new AsyncCallback<Void>() {
				@Override
				public void onFailure(Throwable caught) {
				}
				@Override
				public void onSuccess(Void result) {
				}
			});
		} else {
			t.printStackTrace();
		}
	}
}

