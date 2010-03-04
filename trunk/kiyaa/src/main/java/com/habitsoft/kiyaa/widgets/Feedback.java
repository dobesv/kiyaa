package com.habitsoft.kiyaa.widgets;

import java.util.ArrayList;
import java.util.Collection;

import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Widget;
import com.habitsoft.kiyaa.metamodel.Action;
import com.habitsoft.kiyaa.util.AsyncCallbackDirectProxy;
import com.habitsoft.kiyaa.util.AsyncCallbackFactory;

public class Feedback extends FlowPanel {
    public enum FeedbackType {
        POSITIVE,
        NEGATIVE,
        WARNING,
        NEUTRAL,
        BUSY;
    }
    FeedbackType feedbackType;
	ArrayList<Hyperlink> actionLinks = new ArrayList<Hyperlink>();
	Action[] actions;
	Timer clearTimer = new Timer() {
		@Override
		public void run() {
			clearFeedback();
		}
	};
	
	public Feedback() {
		setStylePrimaryName("ui-feedback");
		clearFeedback();
	}
	
	public void setText(FeedbackType feedbackType, String text, Collection<Action> actions, Collection<Widget> widgets, int timeout) {
		clear();
		if((text == null || text.length() == 0) && (widgets == null || widgets.isEmpty()) && (actions == null || actions.isEmpty())) {
			setVisible(false);
		} else {
            setFeedbackType(feedbackType);
            if(text != null && !text.isEmpty())
                add(new Label(text));
			if(actions != null) {
    			for(final Action action: actions) {
    				Anchor actionLink = new Anchor();
    				actionLink.setText(action.getLabel());
    				actionLink.setStyleName("action");
    				actionLink.addClickListener(new ClickListener() {
    					public void onClick(Widget sender) {
    						final AsyncCallback<Void> defaultNewInstance = AsyncCallbackFactory.defaultNewInstance();
                            action.perform(defaultNewInstance);
    					}
    				});
    				add(actionLink);
    			}
			}
			if(widgets != null) {
				for(Widget widget: widgets) {
					add(widget);
				}
			}
            setVisible(true);
			if(timeout == 0) clearTimer.cancel();
			else clearTimer.schedule(timeout);
		}
	}

	public boolean isPositive() {
		return feedbackType == FeedbackType.POSITIVE;
	}

	public void setPositive(boolean positive) {
	    if(positive) setFeedbackType(FeedbackType.POSITIVE);
	}

	public boolean isNegative() {
		return feedbackType == FeedbackType.NEGATIVE;
	}

	public void setNegative(boolean negative) {
	    if(negative) setFeedbackType(FeedbackType.NEGATIVE);
	}
	
	/**
	 * 
	 * @param timeout Message disappears automatically after this many millisenconds
	 */
	public void positiveFeedback(String text, Collection<Action> actions, int timeout) {
		setText(FeedbackType.POSITIVE, text, actions, null, timeout);
	}
	
	/**
	 * 
	 * @param timeout Message disappears automatically after this many millisenconds
	 */
	public void positiveFeedback(String text, Collection<Action> actions, Collection<Widget> widgets, int timeout) {
		setText(FeedbackType.POSITIVE, text, actions, widgets, timeout);
	}
	
	/**
	 * 
	 * @param timeout Message disappears automatically after this many millisenconds
	 */
	public void negativeFeedback(String text, Collection<Action> actions, int timeout) {
		setText(FeedbackType.NEGATIVE, text, actions, null, timeout);
	}
	
	public void showError(Throwable caught) {
		setText(FeedbackType.NEGATIVE, caught.getLocalizedMessage()==null?caught.toString():caught.getLocalizedMessage(), null, null, 0);
	}
	
    public void showError(String message) {
        setText(FeedbackType.NEGATIVE, message, null, null, 0);
    }
	
	/**
	 * 
	 * @param timeout Message disappears automatically after this many milliseconds
	 */
	public void neutralFeedback(String text, Collection<Action> actions, int timeout) {
		setText(FeedbackType.NEUTRAL, text, actions, null, timeout);
	}

	/**
	 * Remove any message currently showing
	 */
	public void clearFeedback() {
		setText(null, null, null, null, 0);
	}

    public boolean isBusy() {
        return feedbackType == FeedbackType.BUSY;
    }

    public void setBusy(boolean working) {
        if(working) setFeedbackType(FeedbackType.BUSY);
    }
    
    public void busy(String text) {
        setText(FeedbackType.BUSY, text, null, null, 0);
    }
    
    public FeedbackType getFeedbackType() {
        return feedbackType;
    }

    public void setFeedbackType(FeedbackType feedbackType) {
        if(feedbackType != this.feedbackType) {
            if(this.feedbackType != null)
                this.removeStyleDependentName(this.feedbackType.name().toLowerCase());
            this.feedbackType = feedbackType;
            if(feedbackType != null)
                this.addStyleDependentName(feedbackType.name().toLowerCase());
        }
    }

    public <T> AsyncCallback<T> busyCallback(final String busyText, final String successText, AsyncCallback<T> callback) {
    	return busyCallback(busyText, successText, callback);
    }
    
    public <T> AsyncCallback<T> busyCallback(final String busyText, final String successText, final Collection<Action> followupActions, AsyncCallback<T> callback) {
        busy(busyText);
        callback = new AsyncCallbackDirectProxy<T>(callback) {
            @Override
            public void onSuccess(T result) {
                positiveFeedback(successText, followupActions, 0);
                super.onSuccess(result);
            }
            
            @Override
            public void onFailure(Throwable caught) {
                showError(caught);
                super.onFailure(caught);
            }
        };
        return callback;
    }

}
