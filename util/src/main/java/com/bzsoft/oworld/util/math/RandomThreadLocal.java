package com.bzsoft.oworld.util.math;

import java.util.Random;

public final class RandomThreadLocal {

	private static final ThreadLocal<Random> THL = new ThreadLocal<Random>() {
		@Override
		protected Random initialValue() {
			return new RandomXS128();
		};
	};

	private RandomThreadLocal() {
		// empty
	}

	public static final Random get() {
		return THL.get();
	}

}
