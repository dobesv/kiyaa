package com.habitsoft.kiyaa.util;

/**
 * In order to handle history items, the application is represented as
 * a hierarchy of navigable user interface elements.  Each navigable element
 * knows its id and has a list of child navigables.  The current navigation
 * state of the application is represented by a path through the
 * navigables.
 * 
 * As a path is loaded, each navigable along it is selected in the UI
 * and it's contents revealed.  When a new selection is made, the path is saved;
 * when you press the back or forward button, the appropriate path is loaded and the
 * UI is updated to reflect that path.
 * 
 * A path may have "forks" in the sense that two parts of a UI can
 * be updated independently; in this case getNavChildren will
 * return multiple entries.
 * 
 * It's important that for a given "navState" value, the Navigable
 * returns the same number and type of "navChildren", otherwise restoring
 * the history will become too confusing.
 */
public interface Navigable {
	/**
	 * Pass in the set of strings that represents this item's current state.  Using
	 * a list of strings is for the convenience of the Navigable.
	 */
	public void setNavState(String[] names);
	
	/**
	 * Get the current nav state, such that calling setNavState() with those values
	 * will reveal the same content (approximately).
	 */
	public String[] getNavState();
	
	/**
	 * Get the navigable children of this widget.  The children's states are saved
	 * along with this object's, in order to restore their state during navigation
	 * as well.
	 * 
	 * It's important that for a given "navState" value, the Navigable
	 * returns the same number and type of "navChildren", otherwise restoring
	 * the history will become too confusing.
	 */
	public Navigable[] getNavChildren();
	
	/**
	 * Navigate to a state based on a verb and a target object.
	 *
	 * The completion callback is provided so that the caller can wait until
	 * the navigation action is complete before revealing the child.  This avoids
	 * seeing the UI jump around as the data arrives.  The completion callback
	 * most only be invoked if the method returns true; however, it may be 
	 * invoked <b>before</b> the method returns, if setup doesn't require any
	 * network communication.
	 * 
	 * @param verb "edit", "view", TODO
	 * @param target Object to be edited, viewed, or whatever
	 * @param completion Called back when the action is complete; ignored if the method returns false
	 * @return True if a matching state was found, false otherwise
	 */
	public void gotoAction(String verb, Object target, com.google.gwt.user.client.rpc.AsyncCallback<Void> completion);

	/**
	 * Search for an navigable which can perform the given action.
	 * 
	 * The implementation should try to find the "closest" state which
	 * implements the desired functionality; e.g. it should prefer to use
	 * its current state before searching for an alternate state.
	 * 
	 * When this is propagated to a child navigable and the child navigable
	 * returns true, the parents must also return true and select that child.
	 * 
	 * If the navigable returns true, it should be able to respond
	 * to a gotoAction() with the same parameters and doing so should
	 * be the completion of that action.
	 * 
	 * @param verb "edit", "view", TODO
	 * @param target Object to be edited, viewed, or whatever
	 * @return
	 */
	public boolean supportsAction(String verb, Object target);

}
