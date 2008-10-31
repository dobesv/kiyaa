package com.habitsoft.kiyaa.widgets;

import java.util.ArrayList;
import java.util.Collection;

import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Widget;
import com.habitsoft.kiyaa.metamodel.Action;
import com.habitsoft.kiyaa.util.AsyncCallbackFactory;

public class Feedback extends FlowPanel {

	boolean positive;
	boolean negative;
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
	
	public void setText(String text, Collection<Action> actions, Collection<Widget> widgets, int timeout) {
		clear();
		if(text == null || text.length() == 0) {
			setVisible(false);
		} else {
			setVisible(true);
			add(new Label(text));
			if(actions != null) {
    			for(final Action action: actions) {
    				Anchor actionLink = new Anchor();
    				actionLink.setText(action.getLabel());
    				actionLink.setStyleName("action");
    				actionLink.addClickListener(new ClickListener() {
    					public void onClick(Widget sender) {
    						action.perform(AsyncCallbackFactory.defaultNewInstance());
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
			if(timeout == 0) clearTimer.cancel();
			else clearTimer.schedule(timeout);
		}
	}

	public boolean isPositive() {
		return positive;
	}

	public void setPositive(boolean positive) {
		if(positive != this.positive) {
			this.positive = positive;
			if(positive) addStyleDependentName("positive");
			else removeStyleDependentName("positive");
		}
	}

	public boolean isNegative() {
		return negative;
	}

	public void setNegative(boolean negative) {
		if(negative != this.negative) {
			this.negative = negative;
			if(negative) addStyleDependentName("negative");
			else removeStyleDependentName("negative");
		}
	}
	
	/**
	 * 
	 * @param timeout Message disappears automatically after this many millisenconds
	 */
	public void positiveFeedback(String text, Collection<Action> actions, int timeout) {
		setPositive(true);
		setNegative(false);
		setText(text, actions, null, timeout);
	}
	
	/**
	 * 
	 * @param timeout Message disappears automatically after this many millisenconds
	 */
	public void positiveFeedback(String text, Collection<Action> actions, Collection<Widget> widgets, int timeout) {
		setPositive(true);
		setNegative(false);
		setText(text, actions, widgets, timeout);
	}
	
	/**
	 * 
	 * @param timeout Message disappears automatically after this many millisenconds
	 */
	public void negativeFeedback(String text, Collection<Action> actions, int timeout) {
		setPositive(false);
		setNegative(true);
		setText(text, actions, null, timeout);
	}
	
	/**
	 * 
	 * @param timeout Message disappears automatically after this many millisenconds
	 */
	public void neutralFeedback(String text, Collection<Action> actions, int timeout) {
		setPositive(false);
		setNegative(false);
		setText(text, actions, null, timeout);
	}

	/**
	 * Remove any message currently showing
	 */
	public void clearFeedback() {
		setPositive(false);
		setNegative(false);
		setText(null, null, null, 0);
	}
}
