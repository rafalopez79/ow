package com.bzsoft.oworld.ui.components.impl;

import java.awt.Color;
import java.awt.Graphics2D;

import com.bzsoft.oworld.ui.components.UIComponent;
import com.bzsoft.oworld.ui.util.event.EventManager;

public class UIFpsCounter extends UIComponent {

	private long last;
	private float fps;

	public UIFpsCounter(EventManager em) {
		super(em);
		fps = 0;
		last = System.currentTimeMillis();
	}

	@Override
	public void onDraw(Graphics2D g, int w, int h, long now) {
		final float time = now - last;
		fps = 1000f / (5f * time) + 4 * fps / 5f;
		final Color c = g.getColor();
		try {
			g.setColor(Color.GREEN);
			g.drawString("FPS: " + Float.toString(fps), w - 100, h - 100);
		} finally {
			g.setColor(c);
			last = now;
		}
	}

}
