package com.habitsoft.kiyaa.util;

import com.google.gwt.user.client.ui.MouseListener;
import com.google.gwt.user.client.ui.Widget;

public class HoverStyleHandler implements MouseListener {
	
	final Widget target;
	final Group group;
	public static class Group {
		HoverStyleHandler active;
		
		public Group() {
		}
		
		public HoverStyleHandler getActive() {
			return active;
		}

		public void setActive(HoverStyleHandler active) {
			if(this.active != null && this.active != active) {
				try {
					this.active.onMouseLeave(null);
				} catch(Exception e) {
				    e.printStackTrace();
				}
			}
			this.active = active;
		}
		
		public void setInActive(HoverStyleHandler active) {
			if(this.active == active) {
				this.active = null;
			}
		}

        public void clear() {
            setActive(null);
        }
	}
	public HoverStyleHandler(Widget target, Group group) {
		super();
		this.target = target;
		this.group = group;
	}

	public void onMouseDown(Widget widget, int arg1, int arg2) {
		try {
			target.addStyleDependentName("active");
		} catch(Throwable t) {
			// oh well ...
			t.printStackTrace();
		}
		if(group != null) group.setActive(this);
	}

	public void onMouseEnter(Widget widget) {
		try {
			target.addStyleDependentName("hover");
		} catch(Throwable t) {
			// oh well ...
			t.printStackTrace();
		}
		//GWT.log("hovering "+target.getStyleName(), null);
		if(group != null) group.setActive(this);
	}

	public void onMouseLeave(Widget widget) {
		try {
			target.removeStyleDependentName("hover");
			target.removeStyleDependentName("active");
		} catch(Throwable t) {
			// oh well ...
			t.printStackTrace();
		}
		//GWT.log("not hovering "+target.getStyleName(), null);
		if(group != null) group.setInActive(this);
	}

	public void onMouseMove(Widget widget, int arg1, int arg2) {
	}

	public void onMouseUp(Widget widget, int arg1, int arg2) {
		target.removeStyleDependentName("active");
	}

}
