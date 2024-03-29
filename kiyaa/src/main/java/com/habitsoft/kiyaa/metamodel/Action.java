package com.habitsoft.kiyaa.metamodel;

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.habitsoft.kiyaa.util.AsyncCallbackProxy;

/**
 * Action metadata wraps an action which can be performed on
 * a particular object, and allows you to call it.  A sort of
 * replacement for calling methods using reflection.
 */
public abstract class Action {
	private final class DeferredActionCommand implements Command {
		private final AsyncCallback<Void> callback;

		private DeferredActionCommand(AsyncCallback<Void> callback) {
			this.callback = callback;
		}

		public void execute() {
			perform(callback);
		}
	}

	String label;
	
	public Action() {
		
	}
	public Action(String label) {
		super();
		this.label = label;
	}
	public Action(String label, String ignored) {
		super();
		this.label = label;
	}
	
	/**
	 * Perform the action, as defined by the concrete subclass
	 * @param callback Informs the caller when the action is complete, and whether it succeeded or failed
	 */
	public abstract void perform(AsyncCallback<Void> callback);
	
	
	/**
	 * Create an AsyncCallback proxy which can be passed to another
	 * operation.  If that operation is successful, the action is
	 * performed and its result is passed onto the given callback.
	 */
	public <T> AsyncCallback<T> performOnSuccess(final AsyncCallback<Void> callback) {
		return new AsyncCallbackProxy<T,Void>(callback) {
			@Override
			public void onSuccess(Object result) {
				perform(callback);
			}
		};
	}
	
	/**
	 * Perform the action after the current event handlers have finished running.
	 * 
	 * Uses DeferredCommand.addCommand().
	 */
	public void performDeferred(final AsyncCallback<Void> callback) {
		DeferredCommand.addCommand(new DeferredActionCommand(callback));
	}
	public String getLabel() {
		return label;
	}
	public void setLabel(String label) {
		this.label = label;
	}
	
	@Override
	public String toString() {
		if(label == null) return super.toString();
		return "Action("+label+")";
	}
}
