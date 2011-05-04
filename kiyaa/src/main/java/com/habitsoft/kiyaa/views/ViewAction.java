package com.habitsoft.kiyaa.views;

import java.util.LinkedList;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.impl.RemoteServiceProxy;
import com.habitsoft.kiyaa.metamodel.Action;
import com.habitsoft.kiyaa.util.AsyncCallbackExtensions;
import com.habitsoft.kiyaa.util.AsyncCallbackProxy;
import com.habitsoft.kiyaa.util.AsyncCallbackShared;
import com.habitsoft.kiyaa.util.AsyncCallbackWithTimeout;
import com.habitsoft.kiyaa.util.Stats;

/**
 * Action which can load/save a view before/after being performed.
 */
public class ViewAction extends Action {

	View view;
	boolean saveBefore;
	boolean loadAfter;
	Action action;
	int timeout;
	
	public ViewAction(String label, Action action, View view, boolean saveBefore, boolean loadAfter, int timeout) {
		super(label);
		this.view = view;
		this.saveBefore = saveBefore;
		this.loadAfter = loadAfter;
		this.action = action;
		this.timeout = timeout;
	}
	public ViewAction(String label, Action action, View view, boolean saveBefore, boolean loadAfter) {
		this(label, action, view, saveBefore, loadAfter, 0);
	}
	public ViewAction(Action action, View view, boolean saveBefore, boolean loadAfter, int timeout) {
		this(action==null?null:action.getLabel(), action, view, saveBefore, loadAfter, timeout);
	}
	public ViewAction(Action action, View view, boolean saveBefore, boolean loadAfter) {
		this(action, view, saveBefore, loadAfter, 0);
	}
	public ViewAction(Action action, View view) {
		this(action, view, true, true);
	}

	@Override
	public void perform(AsyncCallback<Void> callback) {
		performOnView(action, view, saveBefore, loadAfter, timeout, callback);
	}

	protected static final class ViewLoadingCallback<T> extends
			AsyncCallbackProxy<T, Void> {
		private final View view;

		protected ViewLoadingCallback(AsyncCallback<Void> delegate, View view) {
			super(delegate);
			this.view = view;
		}

		@Override
		public void onSuccess(T result) {
		    // ViewSaveLoadManager.getInstance().load(view, callback);
		    view.load(takeCallback());
		}
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
		
		@Override
		public String toString() {
			return "Save "+view;
		}
	}
	static class ViewActionExecutor {
		public static final int ACTION_TIMEOUT = 2000; // If an individual action takes longer than this we'll run the next one "concurrently", if there is one
		public static final int OVERALL_TIMEOUT=120000; // any list of actions on a view shouldn't take longer than this ...
		long overallTimeoutTime = System.currentTimeMillis() + OVERALL_TIMEOUT;
		public final int sequence = ++Stats.nextSequence;
		
		  class ActionQueueItem<T> implements AsyncCallback<T>, AsyncCallbackExtensions {
			final Action action;
			final boolean loadAfter;
			final AsyncCallback<Void> callback;
			final Exception location;
			boolean timedOut;
			
			public ActionQueueItem(Action action, AsyncCallback<Void> callback, boolean loadAfter,
					Exception location) {
				super();
				this.action = action;
				this.loadAfter = loadAfter;
				this.callback = callback;
				this.location = location;
			}
			
			@Override
			public void onSuccess(T result) {
				if(Stats.enabled())
					Stats.sendTimingInfo(view.getClass().getName(), sequence, String.valueOf(action)+" success");
				if(timedOut) {
					Log.error("Callback timed out but onSuccess got called anyway.", new Exception());
					return; // Already timed out
				}
				if(callback != null) {
					if(!loadAfter) {
						callback.onSuccess(null);
					} else if(loadPending) {
						// Load pending, add ourselves to the list if we're not there already
						if(!waitingForLoad.getCallbacks().contains(callback))
							waitingForLoad.addCallback(callback);
					} else {
						ViewAction.performOnView(null, view, false, true, callback);
					}
				}
				done();
			}

			/**
			 * Pass the torch onto the next job, if we haven't already.
			 * 
			 * Otherwise, take us off the "slow" list.
			 */
			private void done() {
				if(currentAction == this)
					performNext();
				else
					slowActions.remove(this);
			}
			
			/**
			 * Failure case is the tough one to decide on.
			 * 
			 * Probably best idea is actually to continue executing since ViewActions are generally triggered
			 * by different user actions.
			 */
			@Override
			public void onFailure(Throwable caught) {
				if(Stats.enabled())
					Stats.sendTimingInfo(view.getClass().getName(), sequence, String.valueOf(action)+" failure");
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
				done();
			}
			
			/**
			 * Action is still active, we should reset our timeout so we don't time it out
			 */
			@Override
			public void resetTimeout(Integer expectedTimeNeeded) {
				setTimeout(expectedTimeNeeded == null ? ACTION_TIMEOUT : expectedTimeNeeded.intValue());
				if(callback instanceof AsyncCallbackExtensions)
					((AsyncCallbackExtensions) callback).resetTimeout(expectedTimeNeeded);
			}
			
			@Override
			public boolean isOkayToWaitForCurrentAction() {
				return currentAction != this || !isCurrentExecutor();
			}
		}
		final LinkedList<ActionQueueItem<Void>> actions = new LinkedList<ActionQueueItem<Void>>();
		final LinkedList<ActionQueueItem<Void>> slowActions = new LinkedList<ActionQueueItem<Void>>();
		View view;
		boolean loadPending;
		boolean saved;
		boolean finished; // If true, we've popped ourselves off the queue and should NOT get any more callbacks
		AsyncCallbackShared<Void> waitingForLoad;
		
		ActionQueueItem<Void> currentAction;
		
		Timer timeout = new Timer() {
			@Override
			public void run() {
				if(currentAction != null) {
					slowActions.add(currentAction);
				} else {
					Log.error("View load() timed out.  Moving on.");
					performNext();
				}
			}
		};
		public ViewActionExecutor(View view, int timeout) {
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
		 * @param timeout Minimum timeout to allow for this callback
		 * @param callback Callback to invoke after the action has been run.  When loadAfter is true, the callback is invoked after the view is loaded
		 * @return true if this is the first action added to this executor
		 */
		public void add(final Action action, final View view, boolean saveBefore, boolean loadAfter, Exception location, int timeout, AsyncCallback<Void> callback) {
			if(saveBefore && !saved) {
				// Should save before continuing
				actions.add(new ActionQueueItem<Void>(new SaveAction(view), null, false, null));
				saved = true;
			}
			
			actions.add(new ActionQueueItem<Void>(action, callback, loadAfter, location));
			
			// If a load is requested, add this callback to the list of callbacks waiting for a load
			if(loadAfter) {
				loadPending = true;
				if(callback != null)
					waitForLoad(callback);
			}
			
			if(currentAction != null)
				setTimeout(timeout);
			else
				updateTimeoutTime(timeout);
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
					loadPending = false;
					setTimeout();
					if(Stats.enabled())
						Stats.sendTimingInfo(view.getClass().getName(), sequence, "Load "+String.valueOf(view)+" begin");
					view.load(new AsyncCallback<Void>() {
						@Override
						public void onSuccess(Void result) {
							if(Stats.enabled())
								Stats.sendTimingInfo(view.getClass().getName(), sequence, "Load "+String.valueOf(view)+" success");
							performNext();
						}
						
						@Override
						public void onFailure(Throwable caught) {
							if(Stats.enabled())
								Stats.sendTimingInfo(view.getClass().getName(), sequence, "Load "+String.valueOf(view)+" failure");
							if(waitingForLoad != null) {
								waitingForLoad.onFailure(caught);
								waitingForLoad = null;
							}
							performNext();
						}
					});
				} else {
					if(waitingForLoad != null) {
						waitingForLoad.onSuccess(null);
						waitingForLoad = null;
					}
					
					// Pass the torch onto the next set of actions
					if(isCurrentExecutor()) {
						finished = true;
						
						// Kill all the "slow" actions
						for(ActionQueueItem<Void> slowAction : slowActions) {
							Log.error("Action "+slowAction.action+" timed out.  Stack trace from where the the action was enqueued.", slowAction.location);
							slowAction.onFailure(new AsyncCallbackWithTimeout.TimeOutException());
							slowAction.timedOut = true;
						}

						
						actionExecutorQueue.removeFirst();
						if(!actionExecutorQueue.isEmpty()) {
							actionExecutorQueue.getFirst().performNext();
						}
					} else {
						Log.error("ViewActionExecutor isn't at the head of the queue, but (still) running.  finished = "+finished);
					}
				}
			} else {
				currentAction = actions.removeFirst();
				if(Stats.enabled() && currentAction.action != null)
					Stats.sendTimingInfo(view.getClass().getName(), sequence, String.valueOf(currentAction.action)+" begin");
					
				if(currentAction.action != null) {
					setTimeout();
					currentAction.action.performDeferred(currentAction);
				} else {
					// call the callback if any and move onto the next action 
					currentAction.onSuccess(null);
				}
			}
		}
		private boolean isCurrentExecutor() {
			return !actionExecutorQueue.isEmpty() && actionExecutorQueue.getFirst() == this;
		}
		private void setTimeout() {
			setTimeout(ACTION_TIMEOUT);
		}
		private void setTimeout(int minimum) {
			int timeoutMillis = updateTimeoutTime(minimum);
			this.timeout.schedule(timeoutMillis);
		}
		private int updateTimeoutTime(int minimum) {
			if(minimum==0) minimum = ACTION_TIMEOUT;
			int timeoutMillis = Math.max(minimum, actions.size()>0?minimum:(int)(overallTimeoutTime-System.currentTimeMillis()));
			overallTimeoutTime = Math.max(System.currentTimeMillis()+timeoutMillis, overallTimeoutTime);
			return timeoutMillis;
		}
		
		public int remaining() {
			return actions.size();
		}
	}
	
	static final LinkedList<ViewActionExecutor> actionExecutorQueue = new LinkedList<ViewActionExecutor>();
	
	/**
	 * Tell the view to save, then perform the action, then load.
	 */
	public static void performOnView(final Action action, final View view, boolean saveBefore, boolean loadAfter, AsyncCallback<Void> callback) {
		performOnView(action, view, saveBefore, loadAfter, 0, callback);
	}
	
	/**
	 * Tell the view to save, then perform the action, then load.
	 */
	public static void performOnView(final Action action, final View view, boolean saveBefore, boolean loadAfter, int timeout, AsyncCallback<Void> callback) {
		performOnView(action, view, saveBefore, loadAfter, new Exception(), timeout, callback);
	}
	
	/**
	 * Tell the view to save, then perform the action, then load.
	 */
	public static void performOnView(final Action action, final View view, boolean saveBefore, boolean loadAfter, Exception location, int timeout, AsyncCallback<Void> callback) {
		boolean onlyExecutor = actionExecutorQueue.isEmpty();
		if(onlyExecutor || (callback instanceof AsyncCallbackExtensions && ((AsyncCallbackExtensions)callback).isOkayToWaitForCurrentAction())) {
			ViewActionExecutor executor;
			boolean newExecutor = onlyExecutor || actionExecutorQueue.getLast().getView() != view;
			if(newExecutor) {
				// Add new executor
				actionExecutorQueue.add(executor = new ViewActionExecutor(view, timeout));
			} else {
				executor = actionExecutorQueue.getLast();
			}
			
			executor.add(action, view, saveBefore, loadAfter, location, timeout, callback);
			
			//Log.info("ViewAction.performOnView("+action+", "+view+", save="+saveBefore+", load="+loadAfter+" callback="+callback+") actionExecutorQueue().size() == "+actionExecutorQueue.size()+" executor.remaining() == "+executor.remaining(), new Exception());
			
			// If this is a new executor and it is the first one in the queue then we need to start running its actions right away.
			if(onlyExecutor)
				executor.performNext();
		} else {
		
			int sequence=0;
			boolean statsAvailable = Stats.enabled();
			if(statsAvailable)
				sequence = ++Stats.nextSequence;
			if(loadAfter) {
				if(statsAvailable)
					callback = Stats.callbackProxy(String.valueOf(view), sequence, "load: ", callback);
				callback = loadViewOnSuccess(view, callback);
			}
			if(timeout > 0)
				callback = new AsyncCallbackWithTimeout<Void>(callback, timeout, null);
			if(statsAvailable)
				callback = Stats.callbackProxy(String.valueOf(view), sequence, String.valueOf(action)+": ", callback);
			if(saveBefore) {
				if(action != null)
					callback = action.performOnSuccess(callback);
				if(statsAvailable)
					callback = Stats.callbackProxy(String.valueOf(view), sequence, "save: ", callback);
				view.save(callback);
			} else if(action !=  null) {
				action.perform(callback);
			} else {
				callback.onSuccess(null);
			}
		}
	}
	
    public static <T> AsyncCallback<T> loadViewOnSuccess(final View view, AsyncCallback<Void> callback) {
        return new ViewLoadingCallback<T>(callback, view);
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
