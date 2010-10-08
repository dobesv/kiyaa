package com.habitsoft.kiyaa.widgets;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.UIObject;
import com.google.gwt.user.client.ui.Widget;

/**
 * Scroll panel with auto loading feature.
 * 
 * Use this panel instead of regular paging. When the panel is scrolled to the bottom, it invokes a
 * Loader object, which adds more content to the panel.
 * 
 * TODO Allow items to be dropped off the beginning of the list when it gets long TODO Allow a
 * "reverse" list which starts at the end and works upwards, for views where the last entries are
 * the most interesting
 */
public class ScrollAutoLoader extends Widget {
	public static final String STYLE_NAME = "bk-List-Scroller";

	private boolean refreshing = false;
	private final Loader loader;
	private int limit;
	private int offset;
	private int end = 100000;
	private int maxHeight;
	private boolean fillLaterScheduled = false;
	private long lastLoadTime = 0;

	/**
	 * Loader interface. Implementations should add content to the inner panel when load is called.
	 * 
	 * The callback is passed Boolean.TRUE if there might be more data to load (e.g. the server
	 * returned the maximum results requested), Boolean.FALSE otherwise.
	 */
	public interface Loader {
		public void load(int offset, int limit, AsyncCallback<Boolean> completionCallback);
	}

	/**
	 * Create a new scroll panel.
	 * 
	 * @param widget
	 *            The inner panel. Add loaded content to this panel.
	 * @param loader
	 *            The loader which is responsible for adding content to the inner panel.
	 * @param limit
	 *            Number of items to retrieve with each load.
	 */
	public ScrollAutoLoader(final Element element, Loader loader, int limit, int maxHeight, int end) {
		setElement(element);
		this.loader = loader;
		this.limit = limit;
		this.end = end;
		this.maxHeight = maxHeight;
		sinkEvents(Event.ONSCROLL);
		setStyleName(STYLE_NAME);
		if (maxHeight != 0)
			DOM.setStyleAttribute(getElement(), "maxHeight", maxHeight + "px");
	}

	/**
	 * Fill initial content into the scroll panel. Call this on startup (after the widget has been
	 * added to the DOM). Also call this method as the last item in {@link Loader#load(int, int)} to
	 * keep filling until the scrollbar appears.
	 * 
	 * The callback is invoked when the initial load is complete. The callback may be null.
	 */
	public void fill(final AsyncCallback<Void> fillCompleteCallback) {
		refreshing = true;
		final int offsetHeight = getOffsetHeight();
		if (maxHeight == 0 || offsetHeight <= maxHeight) {
			lastLoadTime = System.currentTimeMillis();
			loader.load(offset, limit, new AsyncCallback<Boolean>() {
				public void onSuccess(Boolean maybeMore) {
					if (fillCompleteCallback != null)
						fillCompleteCallback.onSuccess(null);
					if (((Boolean) maybeMore).booleanValue()) {
						fillLater(1);
					} else {
						refreshing = false;
						end = offset;
						// fillLater(10000);
					}
				}

				public void onFailure(Throwable caught) {
					refreshing = false;
					GWT.log("Error filling scrollable area, trying again in a little while...", caught);
					fillLater(60000);
					if (fillCompleteCallback != null)
						fillCompleteCallback.onFailure(caught);
				}

				private void fillLater(int millis) {
					if (isAttached() && !fillLaterScheduled) {// Only keep trying if we're
																// attached to the UI
						fillLaterScheduled = true;
						new com.google.gwt.user.client.Timer() {
							@Override
							public void run() {
								fillLaterScheduled = false;
								fill(null);
							}
						}.schedule(millis);
					}
				}
			});
			offset += limit;
		} else {
			refreshing = false;
			if (fillCompleteCallback != null) {
				fillCompleteCallback.onSuccess(null);
			}
		}
		if (maxHeight != 0 && offsetHeight >= maxHeight) {
			setHeight(maxHeight + "px");
		}
	}

	/**
	 * Reset the offset position. Call this when clearing the inner panel.
	 */
	public void reset() {
		offset = 0;
		refreshing = false;
	}

	@Override
	public void onBrowserEvent(Event e) {
		if (DOM.eventGetType(e) == Event.ONSCROLL) {
			if (offset < end && (System.currentTimeMillis() > lastLoadTime + 250)
				&& getOffsetHeight() - getScrollPosition() - 1 < getOffsetHeight() && !refreshing) {
				refreshing = true;

				GWT.log("Scrolled to bottom - trying to load more ... widget.getOffsetHeight() = "
					+ getOffsetHeight() + " getScrollPosition() = " + getScrollPosition()
					+ " getOffsetHeight() = " + getOffsetHeight(), null);
				lastLoadTime = System.currentTimeMillis();
				loader.load(offset, limit, new AsyncCallback<Boolean>() {
					public void onSuccess(Boolean arg0) {
						offset += limit;
						refreshing = false;
					}

					public void onFailure(Throwable error) {
						refreshing = false;
						GWT.log("ScrollAutoLoader: failed to load more elements", error);
					}
				});

			}
		} else {
			super.onBrowserEvent(e);
		}
	}

	public void waitUntilReady(final AsyncCallback<Void> callback) {
		if (refreshing) {
			new Timer() {
				@Override
				public void run() {
					waitUntilReady(callback);
				}
			}.schedule(250);
		} else {
			callback.onSuccess(null);
		}
	}

	public int getLimit() {
		return limit;
	}

	public void setLimit(int limit) {
		this.limit = limit;
	}

	public int getMaxHeight() {
		return maxHeight;
	}

	public void setMaxHeight(int maxHeight) {
		this.maxHeight = maxHeight;
	}

	public int getEnd() {
		return end;
	}

	public void setEnd(int end) {
		this.end = end;
	}

	/**
	 * Ensures that the specified item is visible, by adjusting the panel's scroll position.
	 * 
	 * @param item
	 *            the item whose visibility is to be ensured
	 */
	public void ensureVisible(UIObject item) {
		Element scroll = getElement();
		Element element = item.getElement();
		ensureVisibleImpl(scroll, element);
	}

	/**
	 * Gets the horizontal scroll position.
	 * 
	 * @return the horizontal scroll position, in pixels
	 */
	public int getHorizontalScrollPosition() {
		return DOM.getElementPropertyInt(getElement(), "scrollLeft");
	}

	/**
	 * Gets the vertical scroll position.
	 * 
	 * @return the vertical scroll position, in pixels
	 */
	public int getScrollPosition() {
		return DOM.getElementPropertyInt(getElement(), "scrollTop");
	}

	/**
	 * Sets whether this panel always shows its scroll bars, or only when necessary.
	 * 
	 * @param alwaysShow
	 *            <code>true</code> to show scroll bars at all times
	 */
	public void setAlwaysShowScrollBars(boolean alwaysShow) {
		DOM.setStyleAttribute(getElement(), "overflow", alwaysShow ? "scroll" : "auto");
	}

	/**
	 * Sets the horizontal scroll position.
	 * 
	 * @param position
	 *            the new horizontal scroll position, in pixels
	 */
	public void setHorizontalScrollPosition(int position) {
		DOM.setElementPropertyInt(getElement(), "scrollLeft", position);
	}

	/**
	 * Sets the vertical scroll position.
	 * 
	 * @param position
	 *            the new vertical scroll position, in pixels
	 */
	public void setScrollPosition(int position) {
		DOM.setElementPropertyInt(getElement(), "scrollTop", position);
	}

	private native void ensureVisibleImpl(Element scroll, Element e) /*-{
	    if (!e)
	      return; 

	    var item = e;
	    var realOffset = 0;
	    while (item && (item != scroll)) {
	      realOffset += item.offsetTop;
	      item = item.offsetParent;
	    }

	    scroll.scrollTop = realOffset - scroll.offsetHeight / 2;
	  }-*/;

}
