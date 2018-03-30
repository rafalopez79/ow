package com.bzsoft.oworld.ui.util.event.impl;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.bzsoft.oworld.ui.util.event.EventLoop;
import com.bzsoft.oworld.ui.util.event.EventManager;
import com.bzsoft.oworld.ui.util.event.ExceptionHandler;
import com.bzsoft.oworld.ui.util.event.Listener;

public class EventLoopImpl implements EventLoop, EventManager {

	private final PriorityQueue<TimerItem> queue;
	private final Lock lock;
	private final Condition condition;
	private final Thread edt;
	private final ExceptionHandler eh;
	private final Map<Class<Object>, List<Listener<Object>>> listenerMap;
	private volatile boolean stop;

	public EventLoopImpl(ExceptionHandler eh) {
		queue = new PriorityQueue<>(1024);
		lock = new ReentrantLock(true);
		condition = lock.newCondition();
		edt = Thread.currentThread();
		this.eh = eh;
		this.listenerMap = new IdentityHashMap<>();
		stop = false;
	}

	@Override
	public void readAndDispatch() {
		try {
			lock.lock();
			final TimerItem t = queue.peek();
			final long now = System.currentTimeMillis();
			if (t != null && t.getTime() <= now) {
				execute(t.getRunnable());
				queue.remove(t);
				return;
			}
			sleep(condition, Math.max(0, t == null ? Long.MAX_VALUE : t.getTime() - now));
		} finally {
			lock.unlock();
		}
	}

	private static final void sleep(Condition condition, long time) {
		if (time > 0) {
			try {
				condition.await(time, TimeUnit.MILLISECONDS);
			} catch (final InterruptedException e) {
				// empty
			}
		}
	}

	private final void execute(final Runnable runnable) {
		if (runnable != null) {
			try {
				runnable.run();
			} catch (final Throwable t) {
				if (eh != null) {
					eh.onThrowable(t);
				}
			}
		}
	}

	@Override
	public void submit(Runnable r) {
		submit(r, 0);
	}

	@Override
	public void submit(Runnable r, long time) {
		final TimerItem ti = TimerItem.of(System.currentTimeMillis() + time, r);
		if (Thread.currentThread() == edt) {
			queue.add(ti);
		} else {
			lock.lock();
			try {
				queue.add(ti);
				condition.signal();
			} finally {
				lock.unlock();
			}
		}
	}

	@Override
	public <E> void pushEvent(Class<E> type, E event) {
		submit(() -> {
			executeEvent(listenerMap, type, event);
		});
	}

	@Override
	public <E> void runEvent(Class<E> type, E event) {
		if (Thread.currentThread() == edt) {
			throw new RuntimeException("runEvent must be callrd in EDT");
		}
		executeEvent(listenerMap, type, event);
	}

	protected static <E> void executeEvent(Map<Class<Object>, List<Listener<Object>>> listenerMap, Class<E> type,
			E event) {
		final List<Listener<Object>> list = listenerMap.get(type);
		if (list != null) {
			for (final Listener<Object> l : list) {
				l.onEvent(event);
			}
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public <E> void addListener(Class<E> type, Listener<E> listener) {
		if (Thread.currentThread() == edt) {
			addListener((Class<Object>) type, (Listener<Object>) listener, listenerMap);
		} else {
			lock.lock();
			try {
				addListener((Class<Object>) type, (Listener<Object>) listener, listenerMap);
			} finally {
				lock.unlock();
			}
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public <E> void removeListener(Class<E> type, Listener<E> listener) {
		if (Thread.currentThread() == edt) {
			removeListener((Class<Object>) type, (Listener<Object>) listener, listenerMap);
		} else {
			lock.lock();
			try {
				removeListener((Class<Object>) type, (Listener<Object>) listener, listenerMap);
			} finally {
				lock.unlock();
			}
		}
	}

	private static void addListener(Class<Object> type, Listener<Object> listener,
			Map<Class<Object>, List<Listener<Object>>> listenerMap) {
		List<Listener<Object>> list = listenerMap.get(type);
		if (list == null) {
			list = new ArrayList<>();
			listenerMap.put(type, list);
		}
		list.add(listener);
	}

	private static void removeListener(Class<Object> type, Listener<Object> listener,
			final Map<Class<Object>, List<Listener<Object>>> listenerMap) {
		final List<Listener<Object>> list = listenerMap.get(type);
		if (list != null) {
			list.remove(listener);
		}
	}

	@Override
	public void close() {
		if (!stop) {
			if (Thread.currentThread() == edt) {
				stop = true;
			} else {
				lock.lock();
				try {
					stop = true;
					queue.clear();
					condition.signal();
				} finally {
					lock.unlock();
				}
			}
		}
	}

	@Override
	public boolean isClosed() {
		return stop;
	}

}
