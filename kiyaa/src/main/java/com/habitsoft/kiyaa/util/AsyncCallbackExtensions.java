package com.habitsoft.kiyaa.util;


/**
 * Implemented by async callback implementations to help with timeouts and debugging
 */
public interface AsyncCallbackExtensions {

	/**
	 * Notify our callback that we're still "alive".  For example,
	 * if a series of actions is performed and the target callback.
	 * 
	 * Implementations are expected to propagate this onto their
	 * delegate(s) that implement this interface, if any, in 
	 * addition to resetting any of their own timeouts.
	 * 
	 * @param expectedTimeNeeded If non-null, how much time this operation thinks it still needs to complete
	 */
	public void resetTimeout(Integer expectedTimeNeeded);
	
	/**
	 * Check if calling this callback is leading back to a pending view action.
	 * 
	 * If this callback is delaying the currently running view action, this would
	 * return false.
	 * 
	 * It also returns false if there's a callback waiting for this one which
	 * isn't a view action callback.
	 * 
	 * The typical implementation should delegate this to its delegates.  If any
	 * of them return false this can return false.
	 */
	public boolean isOkayToWaitForCurrentAction();
}
