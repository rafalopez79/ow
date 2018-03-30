package com.bzsoft.oworld.ui.util.event;

import java.io.Closeable;

public interface EventLoop extends Closeable, EventManager {

	public void readAndDispatch();

	public void submit(Runnable r);

	public void submit(Runnable r, long time);

	@Override
	public void close();

	public boolean isClosed();

}
