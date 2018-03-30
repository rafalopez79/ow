package com.bzsoft.oworld.ui.util.impl;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import javax.swing.event.EventListenerList;

import com.bzsoft.oworld.ui.components.LocaleChangeListener;
import com.bzsoft.oworld.ui.util.I18nManager;

public final class I18nManagerImpl implements I18nManager {

	private static final String RBBASENAME = "i18n/ui";

	private final EventListenerList eventListenerList;
	private final ResourceBundle rb;

	public I18nManagerImpl() {
		eventListenerList = new EventListenerList();
		final ResourceBundle.Control rbcontrol = ResourceBundle.Control
				.getControl(ResourceBundle.Control.FORMAT_PROPERTIES);
		rb = ResourceBundle.getBundle(RBBASENAME, rbcontrol);
	}

	@Override
	public void setLocale(Locale locale) {
		Locale.setDefault(locale);
		ResourceBundle.clearCache();
		for (final LocaleChangeListener lcl : eventListenerList.getListeners(LocaleChangeListener.class)) {
			lcl.onLocaleChange();
		}
	}

	@Override
	public void addLocaleChangeListener(LocaleChangeListener l) {
		eventListenerList.add(LocaleChangeListener.class, l);
	}

	@Override
	public void removeLocaleChangeListener(LocaleChangeListener l) {
		eventListenerList.remove(LocaleChangeListener.class, l);
	}

	@Override
	public String getText(String key) {
		if (key == null || key.isEmpty()) {
			return key;
		}
		try {
			return rb.getString(key);
		} catch (final MissingResourceException e) {
			return key;
		}
	}
}
