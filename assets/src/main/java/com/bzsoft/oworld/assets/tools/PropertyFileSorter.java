package com.bzsoft.oworld.assets.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Set;

public final class PropertyFileSorter {

	private PropertyFileSorter() {
		// empty
	}

	public static final void sortPropertyFile(File fInput, File fOutput) {
		final Properties prop = new Properties();
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(fInput);
			prop.load(fis);
		} catch (final Exception e) {
			e.printStackTrace();
		} finally {
			if (fis != null) {
				try {
					fis.close();
				} catch (final Exception e) {
				}
			}
		}
		final Set<Object> sKeys = prop.keySet();
		final List<String> lKeys = new ArrayList<>(sKeys.size());
		for (final Object oKey : sKeys) {
			lKeys.add((String) oKey);
		}
		Collections.sort(lKeys);
		lKeys.remove("States");
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(fOutput);
			final StringBuilder sb = new StringBuilder();
			sb.append("States=").append(prop.getProperty("States")).append('\n');
			for (final String key : lKeys) {
				final String value = prop.getProperty(key);
				sb.append(key).append('=').append(value).append('\n');
			}
			fos.write(sb.toString().getBytes());
		} catch (final Exception e) {
			e.printStackTrace();
		} finally {
			if (fos != null) {
				try {
					fos.close();
				} catch (final Exception e) {
				}
			}
		}
	}

}
