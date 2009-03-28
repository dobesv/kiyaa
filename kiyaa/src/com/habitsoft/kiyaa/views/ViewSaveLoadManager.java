package com.habitsoft.kiyaa.views;

import java.util.LinkedList;

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 * The purpose of this class is to serialize the save/load cycles
 * of views so that they don't overlap and cause issues - for example,
 * if someone triggers a save/action/load cycle by clicking an button
 * and then clicks another button and triggers another save/action/load
 * cycle there could be a race condition or another invalid state that
 * causes a null pointer exception or some other issue.
 */
public class ViewSaveLoadManager {

    static ViewSaveLoadManager instance;
    public static ViewSaveLoadManager getInstance() {
        if(instance == null) {
            instance = new ViewSaveLoadManager();
        }
        return instance;
    }
    
    class SaveLoadOperation {
        final boolean load;
        final View view;
        final AsyncCallback callback;
        
        public SaveLoadOperation(boolean load, View view, AsyncCallback callback) {
            this.load = load;
            this.view = view;
            this.callback = callback;
        }
        public void perform() {
            assert currentOperation == this;
            AsyncCallback operationCallback = new AsyncCallback() {
                public void onFailure(Throwable caught) {
                    callback.onFailure(caught);
                    operationComplete();
                }
                public void onSuccess(Object result) {
                    callback.onSuccess(result);
                    operationComplete();
                }
            };
            if(load) view.load(operationCallback);
            else view.save(operationCallback);
        }
        public void operationComplete() {
            if(queue.isEmpty()) {
                currentOperation = null;
            } else {
                currentOperation = queue.removeFirst();
                DeferredCommand.addCommand(new Command() {
                    public void execute() {
                       currentOperation.perform();
                    } 
                });
            }
        }
    }
    
    final LinkedList<SaveLoadOperation> queue = new LinkedList<SaveLoadOperation>();
    SaveLoadOperation currentOperation;
    
    /**
     * Call view.load(callback) after any prior load() or save()
     * operations are complete.
     */
    public void load(View view, AsyncCallback callback) {
        enqueue(new SaveLoadOperation(true, view, callback));
    }
    
    /**
     * Call view.save(callback) after any prior load() or save()
     * operations are complete.
     */
    public void save(View view, AsyncCallback callback) {
        enqueue(new SaveLoadOperation(false, view, callback));
    }

    private void enqueue(SaveLoadOperation saveLoadOperation) {
        if(currentOperation == null) {
            currentOperation = saveLoadOperation;
            saveLoadOperation.perform();
        } else {
            queue.add(saveLoadOperation);
        }
    }
}
