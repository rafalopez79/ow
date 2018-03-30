package com.bzsoft.oworld.ui.components.impl;

import java.awt.Color;
import java.awt.Graphics2D;

import com.bzsoft.oworld.ui.components.UIComponent;
import com.bzsoft.oworld.ui.util.event.EventManager;

public class UIFpsCounter extends UIComponent {

	private int i;
	private long last;
	private final int[] time;

	public UIFpsCounter(EventManager em) {
		super(em);
		i = 0;
		last = System.currentTimeMillis();
		time = new int[50];
	}

	private static final float sum(int[] t) {
		int sum = 0;
		for (final int l : t) {
			sum += l;
		}
		return sum;
	}

	@Override
	public void onDraw(Graphics2D g, int w, int h, long now) {
		if (i == time.length - 1) {
			i = 0;
		}
		time[i++] = (int) (now - last);
		final Color c = g.getColor();
		try {
			g.setColor(Color.GREEN);
			g.drawString("FPS: " + Float.toString(time.length * 1000 / sum(time)), w - 100, h - 100);
		} finally {
			g.setColor(c);
			last = now;
		}
	}

}
