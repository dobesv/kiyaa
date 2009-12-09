package com.habitsoft.kiyaa.widgets;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.TextAreaElement;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.RootPanel;
import com.habitsoft.kiyaa.util.FocusGroup;

public class RichTextArea extends com.google.gwt.user.client.ui.TextArea {
	String oldText;
    FocusGroup focusGroup;
    boolean xhtml = false;
    String toolbar = "Default";
    
    static int nextUniqueId=1;
    final Timer richTextSetupTimer = new Timer() {
        @Override
        public void run() {
            initRichText();
        }
    };
    FCKeditor fckEditor;
    
    @Override
    protected void onAttach() {
        super.onAttach();
        
        // Connect to the FCKeditor
        FCKeditor.init();
        richTextSetupTimer.schedule(500);
    }
    
    // onDetach ..
    
    @Override
    protected void onDetach() {
        fckEditor = null;
        super.onDetach();
    }
    
    static final class FCKeditor extends JavaScriptObject {
        static boolean fckInjected=false;
        protected FCKeditor() {}
        /**
         * Wrap the given textarea
         */
        public native void useTextarea(Element textarea) /*-{
            this.UseTextarea(textarea);
        }-*/;
        
        public native String getName() /*-{
            return this.InstanceName;
        }-*/;
        
        /**
         * Wrap a textarea whose id or name matches the name given when the
         * FCKEditor was created.
         */
        public native void replaceTextarea() /*-{
            this.ReplaceTextarea();
        }-*/;
        
        public static native FCKeditor create(String name, String width, String height, String toolbar, String value) /*-{
            return new $wnd.FCKeditor(name, width, height, toolbar, value);
        }-*/;
        
        public FCKeditorAPI getApi() {
            return FCKeditorAPI.get(getName());
        }
        public void setHtml(String html) {
            getApi().setHtml(html);
        }
        public void focus() {
            getApi().focus();
        }
        public String getHtml() {
            return getApi().getHtml();
        }
        public String getXhtml() {
            return getApi().getXhtml();
        }
        static void init() {
            if(!fckInjected) {
                fckInjected = true;
                GWT.log("Injecting fckeditor script into DOM...", null);
                Element e = DOM.createElement("script"); 
                e.setAttribute("language", "JavaScript");
                e.setAttribute("src", "/fckeditor/fckeditor.js"); 
                RootPanel.get().getElement().appendChild(e); 
            }
        }
        public static native void installCallbacks() /*-{
            $wnd.FCKeditor_OnComplete = function(editor) {
                @com.habitsoft.kiyaa.widgets.RichTextArea.FCKeditor::callback(Ljava/lang/String;Lcom/habitsoft/kiyaa/widgets/RichTextArea$FCKeditor;)('OnComplete', editor);
            }
            $wnd.FCKeditor_OnFocus = function(editor) {
                @com.habitsoft.kiyaa.widgets.RichTextArea.FCKeditor::callback(Ljava/lang/String;Lcom/habitsoft/kiyaa/widgets/RichTextArea$FCKeditor;)('OnFocus', editor);
            }
        }-*/;
        public static void callback(String event, FCKeditor editor) {
            GWT.log("FCKeditor Callback "+event+" editor = "+editor, null);
        }
        static native boolean isScriptLoaded() /*-{
            return $wnd.FCKeditor != undefined;
        }-*/;
    }
    
    static final class FCKeditorAPI extends JavaScriptObject {
        protected FCKeditorAPI() {}
        public static native FCKeditorAPI get(String fckName) /*-{
            return $wnd.FCKeditorAPI.GetInstance(fckName);
        }-*/;
        public native String getName() /*-{
            return this.Name;
        }-*/;
        public native String getStatus() /*-{
            return this.Status;
        }-*/;
        public native Document getEditorDocument() /*-{
            return this.EditorDocument;
        }-*/;
        public native void focus() /*-{
            this.Focus();
        }-*/;
        public native void setHtml(String html) /*-{
            this.SetHTML(html);
        }-*/;
        public native String getHtml() /*-{
            return this.GetHTML();
        }-*/;
        public native String getXhtml() /*-{
            return this.GetXHTML();
        }-*/;
        public native void updateLinkedField() /*-{
            this.UpdateLinkedField();
        }-*/;
    }
    
    private void initRichText() {
        if(fckEditor != null || !isAttached())
            return;
        FCKeditor.init();
        if(!FCKeditor.isScriptLoaded()) {
            GWT.log("Waiting for FCK editor script to load.", null);
            richTextSetupTimer.schedule(1000);
        } else {
            String name = "fck"+(nextUniqueId++);
            setName(name);
            TextAreaElement element = getElement().cast();
            element.setId(name);
            GWT.log("Attaching FCK editor with name "+name+" attached? "+isAttached(), null);
            fckEditor = FCKeditor.create(name, getOffsetWidth()+"px", getOffsetHeight()+"px", toolbar, getText());
            //fckEditor.useTextarea(getElement());
            fckEditor.replaceTextarea();
        }
    }
	@Override
	public void setText(String text) {
		if(oldText != null && text != null && oldText.equals(text))
			return;
		super.setText(text);
		if(fckEditor != null) {
		    fckEditor.setHtml(text==null?"":text);
		}
		oldText = text;
	}
	@Override
	public String getText() {
	    if(fckEditor != null) {
	        fckEditor.getApi().updateLinkedField();
	        GWT.log("Reading fck editor "+fckEditor.getName()+": html='"+fckEditor.getHtml()+"' xhtml='"+fckEditor.getXhtml()+"' my text='"+super.getText()+"'", null);
	        //return xhtml?fckEditor.getXhtml():fckEditor.getHtml();
	    }
	    return (oldText = super.getText());
	}
    public void setFocusGroup(FocusGroup group) {
        if(this.focusGroup != null)
            this.focusGroup.remove(this);
        this.focusGroup = group;
        if(group != null)
            group.add(this);
    }
    
    @Override
    public void setFocus(boolean focused) {
        if(focused && fckEditor != null)
            fckEditor.focus();
        else
            super.setFocus(focused);
    }

    public boolean isXhtml() {
        return xhtml;
    }

    public void setXhtml(boolean xhtml) {
        this.xhtml = xhtml;
    }

    public String getToolbar() {
        return toolbar;
    }

    public void setToolbar(String toolbar) {
        this.toolbar = toolbar;
    }
}
