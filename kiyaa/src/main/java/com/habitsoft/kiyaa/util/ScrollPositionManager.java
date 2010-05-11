package com.habitsoft.kiyaa.util;

import java.util.HashMap;

import com.google.gwt.user.client.History;
import com.google.gwt.user.client.HistoryListener;
import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 * Remember the vertical scroll position for each history token, and restore
 * the previous position when navigating.  By default this will jump to the
 * top for anything it doesn't already have a position saved for.
 */
public class ScrollPositionManager implements HistoryListener {

    HashMap<String,Integer> memory = new HashMap<String, Integer>();
    String currentToken;
    
    static ScrollPositionManager instance;
    public static void addHistoryListener() {
        History.addHistoryListener(getInstance());
    }
    
    public void onHistoryChanged(String historyToken) {
        save();
        load(historyToken);
    }

    public void load() {
        load(History.getToken());
    }
    
    private void load(String historyToken) {
        Integer scrollTop = memory.get(historyToken);
        if(scrollTop == null) {
            scrollTop = 0;
            //GWT.log("Resetting scroll position for: "+historyToken, null);
        } else {
            //GWT.log("Restoring scroll position for: "+historyToken+" - "+scrollTop, null);
        }
        scrollTo(getScrollLeft(), scrollTop);
        currentToken = historyToken;
    }

    public void save() {
        if(currentToken != null) {
            int scrollTop = getScrollTop();
            memory.put(currentToken, scrollTop);
            //GWT.log("Saving scroll position for: "+currentToken+" - "+scrollTop, null);
        }
    }
    
    public <T> AsyncCallback<T> doAsyncTransition(AsyncCallback<T> callback, final String token) {
        save();
        if(currentToken == null || !currentToken.equals(token)) {
            return new AsyncCallbackDirectProxy<T>(callback) {
                @Override
                public void onSuccess(T result) {
                    // TODO Maybe we could animate this?
                    load(token);
                    super.onSuccess(result);
                }
            };
        } else {
            return callback;
        }
    }

    public static ScrollPositionManager getInstance() {
        if(instance == null) {
            instance = new ScrollPositionManager();
        }
        return instance;
    }
    
    /** 
     * Gets the left scroll position. 
     * 
     * @return The left scroll position. 
     */ 
    public static native int getScrollLeft() /*-{ 
            var scrollLeft; 
            if ($wnd.innerHeight) 
            { 
                    scrollLeft = $wnd.pageXOffset; 
            } 
            else if ($doc.documentElement && $doc.documentElement.scrollLeft) 
            { 
                    scrollLeft = $doc.documentElement.scrollLeft; 
            } 
            else if ($doc.body) 
            { 
                    scrollLeft = $doc.body.scrollLeft; 
            } 
            return scrollLeft || 0; 
    }-*/; 

    /** 
     * Gets the top scroll position. 
     * 
     * @return The top scroll position. 
     */ 
    public static native int getScrollTop() /*-{ 
            var scrollTop; 
            if ($wnd.innerHeight) 
            { 
                    scrollTop = $wnd.pageYOffset; 
            } 
            else if ($doc.documentElement && $doc.documentElement.scrollTop) 
            { 
                    scrollTop = $doc.documentElement.scrollTop; 
            } 
            else if ($doc.body) 
            { 
                    scrollTop = $doc.body.scrollTop; 
            } 
            return scrollTop || 0; 
    }-*/;
    
    public static native void scrollTo(int scrollLeft, int scrollTop) /*-{
        $wnd.scrollTo(scrollLeft,scrollTop);
    }-*/;

    
}
