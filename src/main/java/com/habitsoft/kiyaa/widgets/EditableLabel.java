package com.habitsoft.kiyaa.widgets;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.FocusListener;
import com.google.gwt.user.client.ui.FocusPanel;
import com.google.gwt.user.client.ui.FocusWidget;
import com.google.gwt.user.client.ui.HasText;
import com.google.gwt.user.client.ui.KeyboardListener;
import com.google.gwt.user.client.ui.TextBoxBase;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.client.ui.HasHorizontalAlignment.HorizontalAlignmentConstant;

public class EditableLabel extends FocusPanel {
	protected FocusWidget editor;
	protected Label label = new Label();
	protected boolean editorShowing;
	protected boolean empty;
	protected String emptyHtml = "<i>&nbsp;</i>";
	protected FlowPanel panel = new FlowPanel();
	protected boolean editorGotFocus=false;
	protected boolean editorLostFocus=true;
	public EditableLabel() {
		addFocusListener(new FocusListener() {
			public void onLostFocus(Widget sender) {
			}
		
			public void onFocus(Widget sender) {
				GWT.log("I got focus... ", null);
				showEditor();
			}
		});
		addClickListener(new ClickListener() {
			public void onClick(Widget sender) {
				showEditor();
			}
		});
		setWidget(panel);
		DOM.setStyleAttribute(getElement(), "cursor", "text");
		panel.add(label);
		label.setTitle("Click to edit");
	}
	
	protected FocusWidget createEditor() {
		return new TextBox();
	}
	public void showEditor() {
		if(editor == null) {
			editor = createEditor();
			editor.setStylePrimaryName(label.getStylePrimaryName());
			editor.addFocusListener(new FocusListener() {
			
				public void onLostFocus(Widget sender) {
					new Timer(){
						@Override
						public void run() {
							updateFromEditor();
							GWT.log("Editor lost focus, hiding the editor ...", null);
							hideEditor();
							editorLostFocus = true;
						}
					}.schedule(100);
				}
			
				public void onFocus(Widget sender) {
					editorGotFocus = true;
					editorLostFocus = false;
					GWT.log("Editor got focus... ", null);
				}
			});
			editor.addKeyboardListener(new KeyboardListener() {
				public void onKeyUp(Widget sender, char keyCode, int modifiers) {
				}
			
				public void onKeyPress(Widget sender, char keyCode, int modifiers) {
				}
				public void onKeyDown(Widget sender, char keyCode, int modifiers) {
					if(keyCode == KEY_ESCAPE) {
						updateEditor();
						hideEditor();
					}
					if(keyCode == KEY_ENTER) {
						hideEditor();
						updateFromEditor();
					}
				}
			});
			DOM.setStyleAttribute(editor.getElement(), "position", "absolute");
			DOM.setStyleAttribute(editor.getElement(), "border", "0");
			DOM.setStyleAttribute(editor.getElement(), "padding", "0");
			DOM.setStyleAttribute(editor.getElement(), "margin", "0");
			DOM.setStyleAttribute(editor.getElement(), "background", "transparent");
			DOM.setStyleAttribute(editor.getElement(), "font-size", "1em");
			String[] copyAttrs = new String[] { 
			    "font-family", "font-weight", "color", 
			    "font-size", "background-color", 
			    "border-color", "border-size", "border-style",
			    "margin-left", "margin-top", "margin-right", "margin-bottom",
			    "padding-left", "padding-top", "padding-right", "padding-bottom"
			};
			editor.setHeight("1em");
			for (int i = 0; i < copyAttrs.length; i++) {
				String prop = copyAttrs[i];
				DOM.setStyleAttribute(editor.getElement(), prop, DOM.getStyleAttribute(label.getElement(), prop));
			}
			//DOM.setStyleAttribute(editor.getElement(), "vertical-align", "middle");
			panel.add(editor);
		}
		if(!editorShowing) {
			editorShowing = true;
			updateEditor();
			DOM.setStyleAttribute(editor.getElement(), "top", getAbsoluteTop()+"px");
			DOM.setStyleAttribute(editor.getElement(), "left", getAbsoluteLeft()+"px");
			editor.setWidth(getOffsetWidth()+"px");
			label.setVisible(false);
			editor.setVisible(true);
			giveFocusToEditor();
		}
	}

	protected void activateEditor() {
		if(editor instanceof TextBoxBase) {
			TextBoxBase textbox = (TextBoxBase)editor;
			textbox.setSelectionRange(0, textbox.getText().length());
		}
	}
	protected void giveFocusToEditor() {
		editorLostFocus = false; // if it gets AND loses focus then don't keep trying
		new Timer() {
			@Override
			public void run() {
				if(!editorGotFocus && !editorLostFocus) {
					//activateEditor();
					editor.setFocus(true);
					giveFocusToEditor(); // Keep trying until it works
				}
			}
		}.schedule(50);
	}
	protected void updateEditor() {
		((HasText)editor).setText(getText());
	}
	public void hideEditorLater() {
		new Timer() {
			@Override
			public void run() {
				hideEditor();
			}
		}.schedule(500);
	}
	public void hideEditor() {
		GWT.log("Hiding the editor ...", null);
		if(editorShowing) {
			editorShowing = false;
			label.setVisible(true);
			editor.setVisible(false);
		}
	}

	@Override
	public void addClickListener(ClickListener listener) {
		label.addClickListener(listener);
	}

	public String getText() {
		if(empty) return "";
		return label.getText();
	}

	@Override
	public String getTitle() {
		return label.getTitle();
	}

	public boolean getWordWrap() {
		return label.getWordWrap();
	}

	public void setHorizontalAlignment(HorizontalAlignmentConstant align) {
		label.setHorizontalAlignment(align);
	}

	@Override
	public void setSize(String width, String height) {
		label.setSize(width, height);
	}

	public void setText(String text) {
		empty = text == null || text.length()==0;
		if(empty) {
			label.setHTML(getEmptyHtml());
		} else {
			label.setText(text);
		}
	}

	@Override
	public void setTitle(String title) {
		label.setTitle(title);
	}

	public void setWordWrap(boolean wrap) {
		label.setWordWrap(wrap);
	}

	protected void updateFromEditor() {
		setText(((HasText)editor).getText());
	}

	public String getEmptyHtml() {
		return emptyHtml;
	}

	public void setEmptyHtml(String emptyHtml) {
		this.emptyHtml = emptyHtml;
	}
	
	
}
