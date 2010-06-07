package com.habitsoft.kiyaa.views;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Widget;
import com.habitsoft.kiyaa.util.NameValueAdapter;
import com.habitsoft.kiyaa.widgets.Label;

/**
 * This class makes it easier to define your own ViewFactory that just
 * creates a ModelView with Label in it with a string of your choice
 * based on the model given.  Very handy for custom subclasses of
 * TableView or ListView that define their own columns.
 */
public class LabelViewFactory<T> implements ViewFactory<ModelView<T>> {
    
    protected final class LabelView implements ModelView<T> {
        Label label = new Label();
        T model;

        public T getModel() {
            return model;
        }

        public void setModel(T model, AsyncCallback<Void> callback) {
            this.model = model;
            callback.onSuccess(null);
        }

        public void clearFields() {
            this.model = null;
        }

        public Widget getViewWidget() {
            return label;
        }

        public void load(AsyncCallback<Void> callback) {
            label.setText(getText(this.model));
            callback.onSuccess(null);
        }

        public void save(AsyncCallback<Void> callback) {
            // Labels are not used for input, so do nothing
            callback.onSuccess(null);
        }
    }

    /**
     * Subclasses implement this to determine the text to show
     * for the given model.
     * 
     * The method should handle null values gracefully, returning
     * "" or some appropriate string.
     */
    public String getText(T model) {
    	return String.valueOf(model);
    }

    public ModelView<T> createView() {
        return new LabelView();
    }
    
    public static <T> ViewFactory<ModelView<T>> createWithNameValueAdapter(final NameValueAdapter<T> nameValueAdapter) {
        return new LabelViewFactory<T> () {
            @Override
            public String getText(T model) {
                return nameValueAdapter.getName(model);
            };
        };
    }
}
