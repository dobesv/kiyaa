package com.habitsoft.kiyaa.util;

import com.google.gwt.debugpanel.common.ExceptionSerializer;
import com.google.gwt.debugpanel.common.GwtExceptionSerializer;
import com.google.gwt.debugpanel.common.GwtStatisticsEventDispatcher;
import com.google.gwt.debugpanel.common.StatisticsEvent;
import com.google.gwt.debugpanel.common.StatisticsEventDispatcher;
import com.google.gwt.debugpanel.common.Utils;
import com.google.gwt.user.client.rpc.AsyncCallback;

public class Stats {
	private static final StatisticsEventDispatcher dispatcher = new GwtStatisticsEventDispatcher();
	private static final ExceptionSerializer exceptionSerializer = new GwtExceptionSerializer();

	public static int nextSequence = 0;

	private static final class StatsCallbackProxy<T> extends
			AsyncCallbackDirectProxy<T> {
		private final int sequence;
		private final String eventTypePrefix;
		private final String method;

		private StatsCallbackProxy(AsyncCallback<T> delegate, int sequence,
				String eventTypePrefix, String method) {
			super(delegate);
			this.sequence = sequence;
			this.eventTypePrefix = eventTypePrefix;
			this.method = method;
		}

		public void onSuccess(T result) {
			sendTimingInfo(method, sequence, eventTypePrefix + "success");
			super.onSuccess(result);
		}

		public void onFailure(Throwable caught) {
			sendTimingInfo(method, sequence, eventTypePrefix + "failure");
			super.onFailure(caught);
		}
	}

	public static boolean enabled() {
		return dispatcher.enabled();
	}

	public static void sendTimingInfo(String method, int sequence, String type) {
		StatisticsEvent event = dispatcher.newEvent("kiyaa", String.valueOf(sequence), Utils.currentTimeMillis(), type);
		dispatcher.setExtraParameter(event, "method", method);
		dispatcher.dispatch(event);
	}
	
	/**
	 * Return a callback proxy which logs success or failure to the stats system
	 */
	public static <T> AsyncCallback<T> callbackProxy(final String method,
			final int sequence, final String eventTypePrefix,
			AsyncCallback<T> delegate) {
		if (!enabled())
			return delegate;
		return new StatsCallbackProxy<T>(delegate, sequence, eventTypePrefix,
				method);
	}

	public static void addException(Throwable t) {
		double now = Utils.currentTimeMillis();
		StatisticsEvent event = dispatcher.newEvent("error", "error", now, "error");
		dispatcher.setExtraParameter(event, "error", exceptionSerializer.serialize(t));
		dispatcher.dispatch(event);
	}
}
