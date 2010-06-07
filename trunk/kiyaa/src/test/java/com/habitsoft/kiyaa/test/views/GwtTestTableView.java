package com.habitsoft.kiyaa.test.views;

import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.habitsoft.kiyaa.views.LabelViewFactory;
import com.habitsoft.kiyaa.views.TableView;
import com.habitsoft.kiyaa.widgets.Label;

public class GwtTestTableView extends GWTTestCase {

	public void testLoadTableView() {
		final TableView<String> tv = new TableView<String>();
		tv.addColumn(new LabelViewFactory<String>());
		final String[] models = new String[] {"one", "two", "three"};
		tv.setModels(models);
		delayTestFinish(1000); // Allow 1000 for the table to load as it may have some deferred command processing
		tv.load(new AsyncCallback<Void>() {
			@Override
			public void onFailure(Throwable caught) {
				throw new Error(caught);
			}
			public void onSuccess(Void result) {
				assertEquals(3, tv.getRowCount());
				for(int i=0; i < models.length; i++) {
					assertEquals(models[i], tv.getModels()[i]);
					assertEquals(models[i], tv.getView(0, i).getModel());
					assertEquals(models[i], ((Label)tv.getView(0, i).getViewWidget()).getText());
				}
				finishTest();
			};
		});
	}
	
	
	@Override
	public String getModuleName() {
		return "com.habitsoft.kiyaa.test.KiyaaTests";
	}

}
