package com.bzsoft.oworld.ui.components;

import java.awt.Graphics2D;

public class DrawEvent {

	private final Graphics2D graphics;
	private final int width;
	private final int height;

	public DrawEvent(Graphics2D graphics, int width, int height) {
		this.graphics = graphics;
		this.width = width;
		this.height = height;
	}

	public Graphics2D getGraphics() {
		return graphics;
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

}
