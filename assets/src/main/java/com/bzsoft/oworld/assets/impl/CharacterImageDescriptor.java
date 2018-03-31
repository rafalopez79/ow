package com.bzsoft.oworld.assets.impl;

public final class CharacterImageDescriptor {
	private final int image;
	private final int dx;
	private final int dy;

	private CharacterImageDescriptor(int image, int dx, int dy) {
		this.image = image;
		this.dx = dx;
		this.dy = dy;
	}

	public static final CharacterImageDescriptor of(int image, int dx, int dy) {
		return new CharacterImageDescriptor(image, dx, dy);
	}

	public int getImage() {
		return image;
	}

	public int getDx() {
		return dx;
	}

	public int getDy() {
		return dy;
	}

}