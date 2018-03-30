package com.bzsoft.oworld.ui.util.event;

public final class CancelableRunnable implements Runnable {

	private final Runnable r;
	private volatile boolean cancelled;

	public CancelableRunnable(Runnable r) {
		this.r = r;
		cancelled = false;
	}

	@Override
	public final void run() {
		if (r != null && !cancelled) {
			r.run();
		}
	}

	public void setCancelled(boolean cancelled) {
		this.cancelled = cancelled;
	}
}
