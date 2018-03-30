package com.bzsoft.oworld.assets;

import java.awt.Color;
import java.awt.Font;
import java.awt.Image;
import java.util.Locale;

public interface ResourceManager {

	public String getText(int key);

	public Font getFont(int key);

	public Color getColor(int key);

	public Image getImage(int key);

	public void setLocale(Locale locale);

}
