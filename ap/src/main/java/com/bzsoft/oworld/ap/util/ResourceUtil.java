package com.bzsoft.oworld.ap.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

public final class ResourceUtil {

	private static final String APP = "app";
	private static final String VERSION = "version";
	private static final String LOCALES = "locales";
	private static final String COLOR = "color";
	private static final String FONT = "font";
	private static final String I18N = "i18n";
	private static final String RESOURCES = "resources";

	private ResourceUtil() {
		// empty
	}

	public static AppInfo loadApp(String url) throws IOException {
		try (InputStream is = ResourceUtil.class.getClassLoader().getResourceAsStream(url)) {
			final Properties prop = new Properties();
			prop.load(is);
			final String appName = prop.getProperty(APP);
			final String version = prop.getProperty(VERSION);
			final String locales = prop.getProperty(LOCALES);
			final String color = prop.getProperty(COLOR);
			final String font = prop.getProperty(FONT);
			final String resources = prop.getProperty(RESOURCES);
			final String i18n = prop.getProperty(I18N);
			final List<Locale> localeList = parseLocales(locales);
			final String colorUrl = color;
			final String fontUrl = font;
			final List<String> i18nUrls = parseResource(i18n);
			final List<String> resourceUrls = parseResource(resources);
			return new AppInfo(appName, version, localeList, colorUrl, fontUrl, i18nUrls, resourceUrls);
		}
	}

	private static final List<String> parseResource(String resources) {
		if (resources == null || resources.isEmpty()) {
			return Collections.emptyList();
		}
		return Arrays.asList(resources.split(","));
	}

	private static final List<Locale> parseLocales(String locales) {
		if (locales == null || locales.isEmpty()) {
			return Arrays.asList(Locale.getDefault());
		}
		final String l = locales.trim();
		if (l.isEmpty()) {
			return Arrays.asList(Locale.getDefault());
		}
		final List<Locale> out = new ArrayList<>();
		for (final String ll : l.split(",")) {
			final Locale locale = Locale.forLanguageTag(ll.trim());
			out.add(locale);
		}
		return out;
	}

	public static final Object[][] parseProperties(List<String> urls) throws IOException {
		if (urls == null || urls.size() == 0) {
			return null;
		}
		return parseProperties(urls.toArray(new String[urls.size()]));
	}

	public static final Object[][] parseProperties(String... urls) throws IOException {
		if (urls == null || urls.length == 0) {
			return null;
		}
		final List<Object[]> list = new ArrayList<>(urls.length);
		int i = 0;
		for (final String url : urls) {
			if (url == null || url.isEmpty()) {
				return null;
			}
			try (InputStream is = ResourceUtil.class.getClassLoader().getResourceAsStream(url)) {
				final Properties prop = new Properties();
				prop.load(is);
				for (final String name : prop.stringPropertyNames()) {
					final String val = prop.getProperty(name);
					list.add(new Object[] { name, val, i++ });
				}
			}
		}
		final Object[][] out = new Object[list.size()][];
		for (i = 0; i < list.size(); i++) {
			out[i] = list.get(i);
		}
		return out;
	}
}
