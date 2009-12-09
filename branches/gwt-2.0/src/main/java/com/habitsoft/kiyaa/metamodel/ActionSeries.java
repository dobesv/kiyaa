package com.habitsoft.kiyaa.metamodel;

import java.util.ArrayList;

import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 * This Action subclass runs a series of other actions.  If any of them fails,
 * it returns failure without running the ones that follow.
 */
public class ActionSeries extends Action {

	public ArrayList actions = new ArrayList();
	
	public ActionSeries() {
	}

	public ActionSeries(Action... actions) {
	    for(Action a : actions) {
	        this.actions.add(a);
	    }
	}
	
	public void add(Action action) {
		if(action == null) throw new NullPointerException("Don't add null actions to an ActionSeries!");
		actions.add(action);
	}
	
	public void performImpl(int i) {
		
	}
	
	@Override
	public void perform(AsyncCallback callback) {
		if(actions.size() == 0) {
			callback.onSuccess(null);
			return;
		}
		for(int i=actions.size()-1; i > 0; i--) {
			Action action = (Action) actions.get(i);
			callback = action.performOnSuccess(callback);
		}
		((Action)actions.get(0)).perform(callback);
	}
}
