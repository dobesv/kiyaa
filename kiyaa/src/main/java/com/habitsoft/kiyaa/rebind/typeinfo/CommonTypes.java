package com.habitsoft.kiyaa.rebind.typeinfo;

import com.google.gwt.core.ext.typeinfo.TypeOracle;

public class CommonTypes {
	
	public final JClassTypeWrapper asyncCallback;
	public final JClassTypeWrapper action;
	public final JClassTypeWrapper value;
	public final JClassTypeWrapper widget;
	public final JClassTypeWrapper modelView;
	public final JClassTypeWrapper view;
	public final JClassTypeWrapper uiObject;
	public final JClassTypeWrapper viewFactory;
	public final JClassTypeWrapper sourcesChangeEvents; // com.google.gwt.user.client.ui.SourcesChangeEvents
	public final JClassTypeWrapper sourcesFocusEvents; // com.google.gwt.user.client.ui.SourcesFocusEvents
	public final JClassTypeWrapper sourcesClickEvents; // com.google.gwt.user.client.ui.SourcesClickEvents
	public final JClassTypeWrapper object; // java.lang.Object
	public final JClassTypeWrapper constants; // com.google.gwt.i18n.client.Constants
	public final JClassTypeWrapper dictionaryConstants;

	public CommonTypes(TypeOracle oracle) {
		this.object = JClassTypeWrapper.wrap(oracle.getJavaLangObject());
		this.asyncCallback = JClassTypeWrapper.wrap(oracle.findType("com.google.gwt.user.client.rpc.AsyncCallback"));
		this.action = JClassTypeWrapper.wrap(oracle.findType("com.habitsoft.kiyaa.metamodel.Action"));
		this.value = JClassTypeWrapper.wrap(oracle.findType("com.habitsoft.kiyaa.metamodel.Value"));
		this.modelView = JClassTypeWrapper.wrap(oracle.findType("com.habitsoft.kiyaa.views.ModelView"));
		this.widget = JClassTypeWrapper.wrap(oracle.findType("com.google.gwt.user.client.ui.Widget"));
		this.view = JClassTypeWrapper.wrap(oracle.findType("com.habitsoft.kiyaa.views.View"));
		this.viewFactory = JClassTypeWrapper.wrap(oracle.findType("com.habitsoft.kiyaa.views.ViewFactory"));
		this.uiObject = JClassTypeWrapper.wrap(oracle.findType("com.google.gwt.user.client.ui.UIObject"));
		this.sourcesChangeEvents = JClassTypeWrapper.wrap(oracle.findType("com.google.gwt.user.client.ui.SourcesChangeEvents"));
		this.sourcesFocusEvents = JClassTypeWrapper.wrap(oracle.findType("com.google.gwt.user.client.ui.SourcesFocusEvents"));
		this.sourcesClickEvents = JClassTypeWrapper.wrap(oracle.findType("com.google.gwt.user.client.ui.SourcesClickEvents"));
		this.constants = JClassTypeWrapper.wrap(oracle.findType("com.google.gwt.i18n.client.Constants"));
		this.dictionaryConstants = JClassTypeWrapper.wrap(oracle.findType("com.habitsoft.kiyaa.util.DictionaryConstants"));
	}
}