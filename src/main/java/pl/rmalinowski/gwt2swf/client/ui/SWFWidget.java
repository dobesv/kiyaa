/*
 *    Copyright 2007 Rafal M.Malinowski
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *   
 */


package pl.rmalinowski.gwt2swf.client.ui;

import java.util.Map;

import pl.rmalinowski.gwt2swf.client.ui.exceptions.UnsupportedFlashPlayerVersionException;

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Widget;

/**
 * 
 * @author Rafal M.Malinowski
 * 
 */
public class SWFWidget extends Widget {

    private final SWFParams swfParams;

    private static int count = 0;

    private final static String divPrefix = "swfDivID_";

    private final String swfDivId;

    private final static String idPrefix = "swfID_";

    private final String swfId;

    private boolean isSWFInjected = false;

    public SWFWidget() {
        this(new SWFParams());
    }

    // var desc = this.@pl.rmalinowski.gwtswf.client.ui::swfParams;
    // alert($doc.getElementById(divId).innerHTML);
    protected native void injectSWF(String divId, String swf, String id,
            String w, String h, String ver, String c,
            Map<String,String> vars) /*-{       	  
                var flashvars = {};
                if(vars!=null){
                   	var iterator=vars.@java.util.Map::keySet()().@java.util.Set::iterator()();
                   	while(iterator.@java.util.Iterator::hasNext()()) {
                   		var key=iterator.@java.util.Iterator::next()();
                   	    var value=vars.@java.util.Map::get(Ljava/lang/Object;)(key);
                   	    flashvars[key] = value;
                   	}
                }
                var params = {
                	allowScriptAccess:"always",
                	swLiveConnect:"true",
                	wmode:"transparent"
                };
                var attributes = {
                  id: id,
                  name: id,
                };
            
                //alert($wnd.document.title);
                $wnd.swfobject.embedSWF(swf, divId, w, h, ver, "expressInstall.swf", flashvars, params, attributes);
                //alert($wnd.document.title);
                //return result;
            }-*/;

    public SWFWidget(String src, Integer width, Integer height) {
        this(new SWFParams(src, width, height));
    }

    public SWFWidget(String src, Integer width, Integer height, String bgcolor) {
        this(new SWFParams(src, width, height, bgcolor));
    }
    
    public SWFWidget(String src, int width, int height) {
        this(new SWFParams(src, width, height));
    }
   
    public SWFWidget(String src, String width, String height) {
        this(new SWFParams(src, width, height));
    }

    public SWFWidget(String src, int width, int height, String bgcolor) {
        this(new SWFParams(src, width, height, bgcolor));
    }

    public SWFWidget(String src, String width, String height, String bgcolor) {
        this(new SWFParams(src, width, height, bgcolor));
    }

    public SWFWidget(SWFParams params) {
        swfParams = params;
        swfId = idPrefix + count;
        swfDivId = divPrefix + count;
        ++count;
        Element element = DOM.createDiv();
        DOM.setElementProperty(element, "id", swfDivId);
        DOM.setInnerText(element, swfParams
                .getInnerTextDivForFlashPlayerNotFound().replaceAll(
                        "\\$flashPlayer.version", params.getVersion().toString()));
        setElement(element);
        // GWT.log("Created with id " + swfId, null);

    }

    /**
     * @throws UnsupportedFlashPlayerVersionException
     */
    @Override
	protected void onLoad() {
        if (!isSWFInjected) {
            //GWT.log("Window title before injection is "+Window.getTitle(), null);
            onBeforeSWFInjection();
            // HACK - For unknown reasons, the window title is being trashed by the SWF we're loading.  Weird!  Affects IE when there's a #whatever on the URL
            // So, try to preserve the title.  This doesn't always work.
            final String title = Window.getTitle();
            injectSWF(getSwfDivId(), swfParams.getSrc(), getSwfId(),
                    swfParams.getWidth(), swfParams.getHeight(), swfParams
                            .getVersion().toString(), swfParams.getBgcolor(),
                    swfParams.getVars());
            isSWFInjected = true;
            DeferredCommand.addCommand(new Command() {
            	public void execute() {
            		Window.setTitle(title);            	
            	}
            });
            onAfterSWFInjection();
            //GWT.log("Window title after injection is "+Window.getTitle(), null);
        }
        super.onLoad();
    }

    /**
     * Override this method to catch information about swf injected.
     * The default implementation does nothing and need not be called by
     * subclasses.
     */
    protected void onAfterSWFInjection() {

    }
    
    /**
     * Override this method to catch information about swf injection.
     * The default implementation does nothing and need not be called by
     * subclasses.
     */
    protected void onBeforeSWFInjection() {

    }

    protected String getSwfDivId() {
        return swfDivId;
    }

    protected String getSwfId() {
        return swfId;
    }

    /**
     * @return the swfParams
     */
    public SWFParams getSwfParams() {
        return swfParams;
    }

}
