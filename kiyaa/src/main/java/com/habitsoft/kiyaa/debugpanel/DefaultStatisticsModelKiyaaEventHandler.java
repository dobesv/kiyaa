package com.habitsoft.kiyaa.debugpanel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.google.gwt.debugpanel.client.AbstractStatisticsModelEventHandler;
import com.google.gwt.debugpanel.common.StatisticsEvent;
import com.google.gwt.debugpanel.models.GwtDebugStatisticsModel;
import com.google.gwt.debugpanel.models.GwtDebugStatisticsValue;
import com.google.gwt.debugpanel.models.GwtDebugStatisticsModel.GwtNode;

public class DefaultStatisticsModelKiyaaEventHandler extends
		AbstractStatisticsModelEventHandler {
	private static final String KIYAA="kiyaa";
	
	  private Map<String, ActivityTree> rpcs;

	  public DefaultStatisticsModelKiyaaEventHandler() {
	    rpcs = new HashMap<String, ActivityTree>();
	  }

	  //@Override
	  public boolean handle(GwtDebugStatisticsModel model, StatisticsEvent event) {
	    if (KIYAA.equals(event.getSubSystem())) {
	      String module = event.getModuleName();
	      String group = event.getEventGroupKey();
	      double millis = event.getMillis();
	      ActivityTree tree = getActivityRoot(model, null, module, group, millis);
	      updateActivityTree(model, event, tree, module, getType(event), millis);
	      return true;
	    }
	    return false;
	  }

	  private void updateActivityTree(GwtDebugStatisticsModel model, StatisticsEvent event,
	      ActivityTree tree, String module, String type, double millis) {
	    if (!tree.root.getValue().hasRpcMethod()) {
	      setActivityMethod(model, tree.root, event);
	    }
	    if (tree.lastEvent == null) {
	    	tree.begin = tree.lastEventTime = millis;
	    }
    	tree.lastEvent = findOrCreateChild(model, tree.root, type, tree.lastEventTime, millis);
    	if(!tree.lastEvent.getValue().hasRpcMethod()) {
    		Object method = event.getExtraParameter("method");
    		if(method != null)
    			tree.lastEvent.getValue().setRpcMethod(String.valueOf(method));
    	}
    	tree.lastEventTime = millis;
	  }

	  private void setActivityMethod(GwtDebugStatisticsModel model, GwtNode node, StatisticsEvent event) {
	    Object method = event.getExtraParameter("method");
	    if (method != null) {
	      GwtDebugStatisticsValue value = node.getValue();
	      value.setRpcMethod(String.valueOf(method));
	    }
	  }

	  public ActivityTree getActivityRoot(
	      GwtDebugStatisticsModel model, GwtNode parent, String module, String group, double millis) {
	    String key = "__" + module + "__" + group;
	    ActivityTree tree = rpcs.get(key);
	    if (tree == null) {
	      GwtNode node = null;
	      if (millis != 0) {
	        node = new GwtNode("action" + group, module, millis, millis);
	        model.addNodeAndUpdateItsParents(parent, node, -1);
	      }
	      tree = new ActivityTree(parent, node);
	      rpcs.put(key, tree);
	    } else if (tree.root == null) {
	      tree.root = new GwtNode("action" + group, module, millis, millis);
	      model.addNodeAndUpdateItsParents(tree.parent, tree.root, -1);
	    }
	    return tree;
	  }

	  /**
	   * A subtree representing a single RPC call.
	   */
	  public static class ActivityTree {
	    public GwtNode parent;
	    public GwtNode root;
	    ArrayList<GwtNode> events = new ArrayList<GwtNode>();
	    public double begin;
	    public GwtNode lastEvent;
	    public double lastEventTime;

	    public ActivityTree(GwtNode parent, GwtNode root) {
	      this.parent = parent;
	      this.root = root;
	    }
	  }
}
