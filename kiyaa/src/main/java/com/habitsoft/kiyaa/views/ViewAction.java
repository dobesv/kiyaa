package com.habitsoft.kiyaa.views;

import java.util.LinkedList;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.habitsoft.kiyaa.metamodel.Action;
import com.habitsoft.kiyaa.util.AsyncCallbackDirectProxy;
import com.habitsoft.kiyaa.util.AsyncCallbackProxy;
import com.habitsoft.kiyaa.util.AsyncCallbackShared;
import com.habitsoft.kiyaa.util.AsyncCallbackWithTimeout;

/**
 * Action which can load/save a view before/after being performed.
 */
public class ViewAction extends Action {

	View view;
	boolean saveBefore;
	boolean loadAfter;
	Action action;
	
	public ViewAction(String label, Action action, View view, boolean saveBefore, boolean loadAfter) {
		super(label);
		this.view = view;
		this.saveBefore = saveBefore;
		this.loadAfter = loadAfter;
		this.action = action;
	}
	public ViewAction(Action action, View view, boolean saveBefore, boolean loadAfter) {
		this(action==null?null:action.getLabel(), action, view, saveBefore, loadAfter);
	}
	public ViewAction(Action action, View view) {
		this(action, view, true, true);
	}

	@Override
	public void perform(AsyncCallback<Void> callback) {
		performOnView(action, view, saveBefore, loadAfter, callback);
	}

	static class SaveAction extends Action {
		final View view;

		@Override
		public void perform(AsyncCallback<Void> callback) {
			view.save(callback);
		}

		public SaveAction(View view) {
			super();
			this.view = view;
		}
		
	}
	static class ViewActionExecutor {
		public static final int DEFAULT_TIMEOUT = AsyncCallbackWithTimeout.DEFAULT_TIMEOUT;
		class ActionQueueItem implements AsyncCallback<Void> {
			final Action action;
			final AsyncCallback<Void> callback;
			final Exception location;
			boolean timedOut;
			
			public ActionQueueItem(Action action, AsyncCallback<Void> callback,
					Exception location) {
				super();
				this.action = action;
				this.callback = callback;
				this.location = location;
			}
			
			@Override
			public void onSuccess(Void result) {
				if(timedOut) {
					Log.error("Callback timed out but onSuccess got called anyway.", new Exception());
					return; // Already timed out
				}
				if(callback != null && !(loadPending && waitingForLoad.getCallbacks().contains(callback)))
					currentAction.callback.onSuccess(null);
				performNext();
			}
			
			/**
			 * Failure case is the tough one to decide on.
			 * 
			 * Probably best idea is actually to continue executing since ViewActions are generally triggered
			 * by different user actions.
			 */
			@Override
			public void onFailure(Throwable caught) {
				if(timedOut) {
					Log.error("Callback timed out but onFailure got called anyway.", caught);
					return; // Already timed out
				}
				if(callback != null) {
					callback.onFailure(caught);
					// Cancel notification of the successful load if there is one coming
					if(waitingForLoad != null)
						waitingForLoad.getCallbacks().remove(callback);
				}
				performNext();
			}
			
			
		}
		final LinkedList<ActionQueueItem> actions = new LinkedList<ActionQueueItem>();
		View view;
		boolean loadPending;
		boolean saved;
		boolean finished; // If true, we've popped ourselves off the queue and should NOT get any more callbacks
		AsyncCallbackShared<Void> waitingForLoad;
		
		ActionQueueItem currentAction;
		
		Timer timeout = new Timer() {
			@Override
			public void run() {
				if(currentAction != null) {
					Log.error("Action "+currentAction.action+" timed out.  Stack trace from where the the action was enqueued.", currentAction.location);
					currentAction.onFailure(new AsyncCallbackWithTimeout.TimeOutException());
					currentAction.timedOut = true;
				} else {
					Log.error("View load() timed out.  Moving on.");
					performNext();
				}
			}
		};
		public ViewActionExecutor(View view) {
			super();
			this.view = view;
		}
		public View getView() {
			return view;
		}
		public void setView(View view) {
			this.view = view;
		}
		
		/**
		 * Add the action to the list and return true if this is the first action added
		 * to this executor.
		 * 
		 * @param action Action to run
		 * @param view View to save/load depending on other parameters
		 * @param saveBefore If true, save the view before running this action, if not already saved while running an earlier action
		 * @param loadAfter If true, load the view after running this action and any subsequent ones on the same view
		 * @param location Stack trace where the view action was enqueued
		 * @param callback Callback to invoke after the action has been run.  When loadAfter is true, the callback is invoked after the view is loaded
		 * @return true if this is the first action added to this executor
		 */
		public void add(final Action action, final View view, boolean saveBefore, boolean loadAfter, Exception location, AsyncCallback<Void> callback) {
			if(saveBefore && !saved) {
				// Should save before continuing
				actions.add(new ActionQueueItem(new SaveAction(view), null, null));
				saved = true;
			}
			
			actions.add(new ActionQueueItem(action, callback, location));
			
			// If a load is requested, add this callback to the list of callbacks waiting for a load
			if(loadAfter) {
				loadPending = true;
				if(callback != null)
					waitForLoad(callback);
			}
		}
		private void waitForLoad(AsyncCallback<Void> callback) {
			if(waitingForLoad == null) waitingForLoad = new AsyncCallbackShared<Void>(callback);
			else waitingForLoad.addCallback(callback);
		}
		
		public void performNext() {
			this.timeout.cancel();
			//if(currentAction != null) Log.info("Finished running "+currentAction.action+(currentAction.timedOut?" (time out)":"")+"; "+remaining()+" actions remaining on "+view);
			if(actions.isEmpty()) {
				currentAction = null;
				if(loadPending) {
					view.load(new AsyncCallback<Void>() {
						@Override
						public void onSuccess(Void result) {
							loadPending = false;
							waitingForLoad.onSuccess(null);
							waitingForLoad = null;
							performNext();
						}
						
						@Override
						public void onFailure(Throwable caught) {
							loadPending = false;
							waitingForLoad.onFailure(caught);
							waitingForLoad = null;
							performNext();
						}
					});
				} else {
					if(waitingForLoad != null) {
						waitingForLoad.onSuccess(null);
						waitingForLoad = null;
					}
					
					// Pass the torch onto the next set of actions
					if(!actionQueue.isEmpty() && actionQueue.getFirst() == this) {
						finished = true;
						actionQueue.removeFirst();
						if(!actionQueue.isEmpty()) {
							actionQueue.getFirst().performNext();
						}
					} else {
						Log.error("ViewActionExecutor isn't at the head of the queue, but (still) running.  finished = "+finished);
					}
				}
			} else {
				currentAction = actions.removeFirst();
				if(currentAction.action != null) {
					setTimeout();
					currentAction.action.performDeferred(currentAction);
				} else {
					// call the callback if any and move onto the next action 
					currentAction.onSuccess(null);
				}
			}
		}
		private void setTimeout() {
			this.timeout.schedule(DEFAULT_TIMEOUT);
		}
		
		public int remaining() {
			return actions.size();
		}
	}
	
	static final LinkedList<ViewActionExecutor> actionQueue = new LinkedList<ViewActionExecutor>();
	
	/**
	 * Tell the view to save, then perform the action, then load.
	 */
	public static void performOnView(final Action action, final View view, boolean saveBefore, boolean loadAfter, AsyncCallback<Void> callback) {
		performOnView(action, view, saveBefore, loadAfter, new Exception(), callback);
	}
	
	
	public static void performOnView(final Action action, final View view, boolean saveBefore, boolean loadAfter, Exception location, AsyncCallback<Void> callback) {
		ViewActionExecutor executor;
		boolean onlyExecutor = actionQueue.isEmpty();
		boolean newExecutor = onlyExecutor || actionQueue.getLast().getView() != view;
		if(newExecutor) {
			// Add new executor
			actionQueue.add(executor = new ViewActionExecutor(view));
		} else {
			executor = actionQueue.getLast();
		}
		
		executor.add(action, view, saveBefore, loadAfter, location, callback);
		
		//Log.info("ViewAction.performOnView("+action+", "+view+", save="+saveBefore+", load="+loadAfter+" callback="+callback+") actionQueue.size() == "+actionQueue.size()+" executor.remaining() == "+executor.remaining(), new Exception());
		
		// If this is a new executor and it is the first one in the queue then we need to start running its actions right away.
		if(onlyExecutor)
			executor.performNext();
	}
	
    public static <T> AsyncCallback<T> loadViewOnSuccess(final View view, AsyncCallback<Void> callback) {
        return new AsyncCallbackProxy<T,Void>(callback) {
        	@Override
        	public void onSuccess(T result) {
        	    // ViewSaveLoadManager.getInstance().load(view, callback);
        	    view.load(takeCallback());
        	}
        };
    }
	
	/**
	 * Wrapper for perform on view that always saves and loads the view, since that is the most common case.
	 */
	public static void performOnView(Action action, View view, AsyncCallback<Void> callback) {
		performOnView(action, view, true, true, callback);
	}
	
	protected View getView() {
		return view;
	}

	protected void setView(View view) {
		this.view = view;
	}

	protected boolean isSaveBefore() {
		return saveBefore;
	}

	protected void setSaveBefore(boolean saveBefore) {
		this.saveBefore = saveBefore;
	}

	protected boolean isLoadAfter() {
		return loadAfter;
	}

	protected void setLoadAfter(boolean loadAfter) {
		this.loadAfter = loadAfter;
	}
	
}
