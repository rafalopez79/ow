package com.bzsoft.oworld.assets;

import java.awt.Image;

public interface CharacterData {

	public static enum Status {
		BEENHIT, GREETING, RUNNING, SHOOTING, STOPPED, THROWING, TIPPINGOVER, WALKING, ATTACK, TALKING, LOOKING, PAUSED, ROARING
	}

	public static final class CharacterImageInfo {
		private final Image image;
		private final int dx;
		private final int dy;

		private CharacterImageInfo(Image image, int dx, int dy) {
			this.image = image;
			this.dx = dx;
			this.dy = dy;
		}

		public static final CharacterImageInfo of(Image image, int dx, int dy) {
			return new CharacterImageInfo(image, dx, dy);
		}

		public Image getImage() {
			return image;
		}

		public int getDx() {
			return dx;
		}

		public int getDy() {
			return dy;
		}

	}

	public static final int N = 0;
	public static final int S = 1;
	public static final int E = 2;
	public static final int W = 3;
	public static final int NE = 4;
	public static final int NW = 5;
	public static final int SE = 6;
	public static final int SW = 7;

	public void setDirection(int d);

	public void setStatus(Status s);

	public CharacterImageInfo getCharacterImageInfo();

}
