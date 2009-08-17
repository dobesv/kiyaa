package com.habitsoft.kiyaa.widgets;

import com.google.gwt.user.client.ui.FocusWidget;

public class EditableLabelTextArea extends EditableLabel {

	@Override
	protected FocusWidget createEditor() {
		return new TextArea();
	}
}
