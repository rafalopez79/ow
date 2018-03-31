package com.bzsoft.oworld.ap.util;

import java.util.List;
import java.util.Locale;

public final class AppInfo {

	private final String appName;
	private final String version;
	private final List<Locale> locales;
	private final String colorUrl;
	private final String fontUrl;
	private final List<String> i18nUrls;
	private final List<String> resourceUrls;
	private final String charsUrl;

	public AppInfo(String appName, String version, List<Locale> locales, String colorUrl, String fontUrl,
			List<String> i18nUrls, List<String> resourceUrls, final String charsUrl) {
		this.appName = appName;
		this.version = version;
		this.locales = locales;
		this.colorUrl = colorUrl;
		this.fontUrl = fontUrl;
		this.i18nUrls = i18nUrls;
		this.resourceUrls = resourceUrls;
		this.charsUrl = charsUrl;
	}

	public String getAppName() {
		return appName;
	}

	public String getVersion() {
		return version;
	}

	public List<Locale> getLocales() {
		return locales;
	}

	public String getColorUrl() {
		return colorUrl;
	}

	public String getFontUrl() {
		return fontUrl;
	}

	public List<String> getI18nUrls() {
		return i18nUrls;
	}

	public List<String> getResourceUrls() {
		return resourceUrls;
	}

	public String getCharsUrl() {
		return charsUrl;
	}
}
