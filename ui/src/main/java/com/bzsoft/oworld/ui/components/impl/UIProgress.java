package com.bzsoft.oworld.ui.components.impl;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;

import com.bzsoft.oworld.ui.components.UIComponent;
import com.bzsoft.oworld.ui.util.event.EventManager;

public class UIProgress extends UIComponent {

	private int progress;
	private final int max;
	private final Rectangle rect;

	public UIProgress(EventManager em, Rectangle rect, int max) {
		super(em);
		this.rect = rect;
		this.max = max;
	}

	public void setProgress(int progress) {
		this.progress = progress;
	}

	public void incrProgress(int progress) {
		this.progress += progress;
	}

	@Override
	public void onDraw(Graphics2D g, int w, int h, long now) {
		final Color c = g.getColor();
		try {
			g.setColor(Color.white);
			g.drawRect(rect.x, rect.y, rect.width, rect.height);
			final int level = (int) (((double) progress * rect.width) / max);
			g.fillRect(rect.x, rect.y, Math.min(level, rect.width), rect.height);
		} finally {
			g.setColor(c);
		}
	}

}
