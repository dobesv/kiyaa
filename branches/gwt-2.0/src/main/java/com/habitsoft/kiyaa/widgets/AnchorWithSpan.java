package com.habitsoft.kiyaa.widgets;

import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.user.client.DOM;

/**
 * Sometimes, for CSS hackery, we need a span inside our anchor tag.
 */
public class AnchorWithSpan extends Anchor {

	SpanElement span;

	public AnchorWithSpan() {
		super();
		span = SpanElement.as(DOM.createSpan());
		anchor.appendChild(span);
	}

	@Override
	public final String getHtml() {
		return span.getInnerHTML();
	}

	@Override
	public final String getText() {
		return span.getInnerText();
	}

	@Override
	public final void setHtml(String html) {
		span.setInnerHTML(html);
	}

	@Override
	public final void setText(String text) {
		span.setInnerText(text);
	}
	
	
}
