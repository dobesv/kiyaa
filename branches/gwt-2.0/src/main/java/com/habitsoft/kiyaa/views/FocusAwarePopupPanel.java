/**
 * 
 */
package com.habitsoft.kiyaa.views;

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Event;

/**
 * Subclass of PopupPanel which automatically hides if the user
 * transfers focus to an input not inside the popup's area.
 *
 * Since this is only useful when autoHide == true, autoHide is
 * always true for this class.  To use a non-autoHide popup, use
 * the original class.
 */
class FocusAwarePopupPanel extends com.google.gwt.user.client.ui.PopupPanel {

    public FocusAwarePopupPanel() {
        super(true);
    }

    public FocusAwarePopupPanel(boolean modal) {
        super(true, modal);
    }
    
    @Override
    public boolean onEventPreview(Event event) {
        if(event.getTypeInt() == Event.ONFOCUS) {
            Element target = DOM.eventGetTarget(event);

            boolean eventTargetsPopup = (target != null)
                && DOM.isOrHasChild(getElement(), target);
            if(!eventTargetsPopup) {
                
                // Focused somewhere outside the popup?  In that case, close the popup!
                hide(true);
            }
        }
        return super.onEventPreview(event);
    }
    
}