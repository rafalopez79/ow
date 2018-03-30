package com.bzsoft.oworld.ui.util;

import java.util.Locale;

import com.bzsoft.oworld.ui.components.LocaleChangeListener;

public interface I18nManager {

	public String getText(String key);

	public void setLocale(Locale locale);

	public void addLocaleChangeListener(LocaleChangeListener lcl);

	public void removeLocaleChangeListener(LocaleChangeListener lcl);

}
