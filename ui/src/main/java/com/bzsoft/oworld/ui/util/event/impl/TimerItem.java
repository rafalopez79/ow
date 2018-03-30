package com.bzsoft.oworld.ui.util.event.impl;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public final class TimerItem implements Comparable<TimerItem> {

	private final long time;
	private final Runnable runnable;

	private TimerItem(final long time, final Runnable runnable) {
		this.time = time;
		this.runnable = runnable;
	}

	public static final TimerItem of(final long time, final Runnable runnable) {
		return new TimerItem(time, runnable);
	}

	public long getTime() {
		return time;
	}

	public Runnable getRunnable() {
		return runnable;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((runnable == null) ? 0 : runnable.hashCode());
		result = prime * result + (int) (time ^ (time >>> 32));
		return result;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final TimerItem other = (TimerItem) obj;
		// pointer comparison
		if (runnable != other.runnable) {
			return false;
		}
		if (time != other.time) {
			return false;
		}
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		final Date date = new Date(time);
		final DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss SSS");
		final StringBuilder builder = new StringBuilder("TimerItem [time=");
		builder.append(df.format(date)).append(", runnable=").append(runnable).append("]");
		return builder.toString();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int compareTo(final TimerItem o) {
		return this.time > o.time ? 1 : (this.time == o.time ? 0 : -1);
	}
}
