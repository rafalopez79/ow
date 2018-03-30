package com.bzsoft.oworld.ui.components.impl;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.image.ImageObserver;

import com.bzsoft.oworld.assets.ResourceManager;
import com.bzsoft.oworld.ui.components.UIComponent;
import com.bzsoft.oworld.ui.util.event.EventManager;

public class UIPanel extends UIComponent {

	private final Rectangle rect;
	private final ResourceManager rm;
	private final ImageObserver o;
	private Image background;

	public UIPanel(EventManager em, ResourceManager rm, Rectangle rect, ImageObserver o) {
		super(em);
		this.rm = rm;
		this.rect = rect;
		this.o = o;
	}

	public void setBackground(Image background) {
		this.background = background;
	}

	@Override
	public void onDraw(Graphics2D g, int w, int h, long now) {
		try {
			if (background != null) {
				g.drawImage(background, 0, 0, o);
			}
		} finally {

		}

	}

}
