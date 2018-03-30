package com.bzsoft.oworld.ui.components;

import java.awt.Graphics2D;

import com.bzsoft.oworld.ui.util.event.EventManager;

public abstract class UIComponent implements Drawable {

	protected final EventManager em;

	protected UIComponent(EventManager em) {
		this.em = em;
	}

	@Override
	public abstract void onDraw(Graphics2D g, int w, int h, long now);
}
