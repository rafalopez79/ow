package com.bzsoft.oworld.assets.impl;

import java.awt.Color;
import java.awt.Font;
import java.awt.Image;
import java.awt.Toolkit;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import javax.imageio.ImageIO;

import com.bzsoft.oworld.R;
import com.bzsoft.oworld.ap.ResourceProcessable;
import com.bzsoft.oworld.assets.ResourceManager;

@ResourceProcessable
public class BaseResourceManager implements ResourceManager {

	protected static final int IMGCACHESIZE = 1024;
	protected Toolkit toolkit;
	protected Locale locale;
	protected final Map<Integer, Image> imgCache = new LinkedHashMap<Integer, Image>() {
		private static final long serialVersionUID = 1L;

		@Override
		protected boolean removeEldestEntry(Map.Entry<Integer, Image> eldest) {
			return size() == IMGCACHESIZE;
		};
	};

	public BaseResourceManager() {
		locale = Locale.getDefault();
		toolkit = Toolkit.getDefaultToolkit();
	}

	@Override
	public Image getImage(int resource) {
		Image img = imgCache.get(resource);
		if (img == null) {
			try (InputStream is = getClass().getClassLoader().getResourceAsStream(R.Resources.get(resource))) {
				img = ImageIO.read(is);
				imgCache.put(resource, img);
			} catch (final Exception e) {
				// empty
			}
		}
		return img;
	}

	@Override
	public String getText(int key) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Font getFont(int key) {
		return R.Fonts.get(key);
	}

	@Override
	public Color getColor(int key) {
		return R.Colors.get(key);
	}

	@Override
	public void setLocale(Locale locale) {
		this.locale = locale;
	}

}
