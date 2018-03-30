package com.bzsoft.oworld.ui.util.event;

public interface EventManager {

	public <E> void runEvent(Class<E> type, E event);

	public <E> void pushEvent(Class<E> type, E event);

	public <E> void addListener(Class<E> type, Listener<E> listener);

	public <E> void removeListener(Class<E> type, Listener<E> listener);

}
