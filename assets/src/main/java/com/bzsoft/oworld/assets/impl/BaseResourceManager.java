package com.bzsoft.oworld.assets.impl;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Toolkit;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bzsoft.oworld.R;
import com.bzsoft.oworld.ap.ResourceProcessable;
import com.bzsoft.oworld.assets.CharacterData;
import com.bzsoft.oworld.assets.CharacterData.Status;
import com.bzsoft.oworld.assets.ResourceException;
import com.bzsoft.oworld.assets.ResourceManager;
import com.bzsoft.oworld.util.tuple.Tuple;
import com.bzsoft.oworld.util.tuple.Tuple.Tuple2;

@ResourceProcessable
public class BaseResourceManager implements ResourceManager {

	private static final Logger LOGGER = LoggerFactory.getLogger(BaseResourceManager.class);
	private static final int CHARACTER_IMG_OFFSET = 10000;

	private static final String STATES = "states";

	protected Component component;
	protected Toolkit toolkit;
	protected Locale locale;
	protected final Map<Integer, Image> imgCache;
	protected Map<String, Map<Status, Map<Integer, CharacterImageDescriptor[]>>> cdMap;
	protected String[] cdUrls;

	public BaseResourceManager(final Component component, int cacheSize) {
		locale = Locale.getDefault();
		toolkit = Toolkit.getDefaultToolkit();
		this.component = component;
		imgCache = new LinkedHashMap<Integer, Image>() {
			private static final long serialVersionUID = 1L;

			@Override
			protected boolean removeEldestEntry(Map.Entry<Integer, Image> eldest) {
				return size() == cacheSize;
			};
		};
	}

	private final static Tuple2<Map<String, Map<Status, Map<Integer, CharacterImageDescriptor[]>>>, String[]> loadCharInfo()
			throws ResourceException {
		try {
			final String imgBasePath = R.CharInfo.get(R.CharInfo.imgbasepath);
			final String characters = R.CharInfo.get(R.CharInfo.characters);
			final String mdFile = R.CharInfo.get(R.CharInfo.characterImgMdFile);
			final String propFile = R.CharInfo.get(R.CharInfo.characterImgPropFile);
			final String pattern = "%s_%s_%d";
			int img = CHARACTER_IMG_OFFSET;
			final List<String> urls = new ArrayList<>();
			final Map<String, Map<Status, Map<Integer, CharacterImageDescriptor[]>>> map = new HashMap<>();
			for (final String character : split(characters)) {
				final Map<Status, Map<Integer, CharacterImageDescriptor[]>> mStatus = new EnumMap<>(Status.class);

				final String mdPath = imgBasePath + "/" + character + "/" + mdFile;
				final String propPath = imgBasePath + "/" + character + "/" + propFile;
				final Properties md = loadProperties(mdPath);
				final Properties prop = loadProperties(propPath);
				final String states = md.getProperty(STATES);
				for (final String st : split(states)) {
					final Status status = parseStatus(st);
					final Map<Integer, CharacterImageDescriptor[]> mDirection = new HashMap<>();

					final String directions = md.getProperty(st);
					for (final String direction : split(directions)) {
						final int dir = parseDirection(direction);
						final int imgCount;
						try {
							imgCount = Integer.parseInt(md.getProperty(st + "." + direction));
						} catch (final NumberFormatException nfe) {
							throw new ResourceException("Bad number at " + st + "." + direction, nfe);
						}
						final CharacterImageDescriptor[] cidArray = new CharacterImageDescriptor[imgCount];
						for (int i = 0; i < imgCount; i++) {
							final String key = String.format(pattern, st, direction, i);
							final String url = prop.getProperty(key);
							final String sOffsets = md.getProperty(key);
							final int[] offsets = parseOffsets(sOffsets);
							cidArray[i] = CharacterImageDescriptor.of(img, offsets[0], offsets[1]);
							img += 1;
							urls.add(url);
						}
						mDirection.put(dir, cidArray);
					}
					mStatus.put(status, mDirection);
				}
				map.put(character, mStatus);
			}
			return Tuple.of(map, urls.toArray(new String[urls.size()]));
		} catch (final IOException e) {
			throw new ResourceException(e.getMessage(), e);
		}
	}

	private static int[] parseOffsets(String val) throws ResourceException {
		if (val == null || val.isEmpty()) {
			throw new ResourceException("Bad Offset format");
		}
		final String[] ss = split(val);
		if (ss.length != 2) {
			throw new ResourceException("Bad Offset format");
		}
		try {
			return new int[] { Integer.parseInt(ss[0]), Integer.parseInt(ss[1]) };
		} catch (final NumberFormatException e) {
			throw new ResourceException("Bad Offset format", e);
		}
	}

	private static final String[] split(String s) {
		return s == null ? new String[0] : s.split(",");
	}

	private final static Properties loadProperties(String url) throws IOException {
		try (InputStream is = BaseResourceManager.class.getClassLoader().getResourceAsStream(url)) {
			final Properties prop = new Properties();
			prop.load(is);
			return prop;
		}
	}

	private static final int parseDirection(String dir) throws ResourceException {
		if (dir == null || dir.isEmpty()) {
			throw new ResourceException("Bad direction format");
		}
		final String c = dir.trim().toUpperCase();
		switch (c) {
		case "N":
			return CharacterData.N;
		case "S":
			return CharacterData.S;
		case "E":
			return CharacterData.E;
		case "W":
			return CharacterData.W;
		case "NE":
			return CharacterData.NE;
		case "NW":
			return CharacterData.NW;
		case "SE":
			return CharacterData.SE;
		case "SW":
			return CharacterData.SW;
		default:
			throw new ResourceException("Bad direction format");
		}
	}

	private static final Status parseStatus(String s) throws ResourceException {
		if (s == null || s.isEmpty()) {
			throw new ResourceException("Bad status format");
		}
		try {
			return Status.valueOf(s.toUpperCase());
		} catch (final Exception e) {
			throw new ResourceException("Bad status " + s, e);
		}
	}

	@Override
	public void loadCharacterInfo() {
		try {
			final Tuple2<Map<String, Map<Status, Map<Integer, CharacterImageDescriptor[]>>>, String[]> t = loadCharInfo();
			cdMap = t.get1();
			cdUrls = t.get2();
		} catch (final Exception e) {
			LOGGER.warn("Error Loading CharacterInfo", e);
		}
	}

	@Override
	public Image getImage(int resource) {
		Image img = imgCache.get(resource);
		if (img == null) {
			String url = null;
			try {
				url = R.Resources.get(resource);
				img = loadImage(component, toolkit, url, false);
				imgCache.put(resource, img);
			} catch (final Exception e) {
				LOGGER.warn("Error Loading Image {}", url, e);
			}
		}
		return img;
	}

	@Override
	public Image getCharacterImage(int key) {
		Image img = imgCache.get(key);
		if (img == null) {
			String url = null;
			try {
				url = cdUrls[key - CHARACTER_IMG_OFFSET];
				img = loadImage(component, toolkit, url, false);
				imgCache.put(key, img);
			} catch (final Exception e) {
				LOGGER.warn("Error Loading Image {}", url, e);
			}
		}
		return img;
	}

	private static final Image loadImage(Component c, Toolkit toolkit, String name, boolean imageio) throws Exception {
		if (imageio) {
			try (InputStream is = BaseResourceManager.class.getClassLoader().getResourceAsStream(name)) {
				return ImageIO.read(is);
			}
		} else {
			try (InputStream is = BaseResourceManager.class.getClassLoader().getResourceAsStream(name)) {
				final ByteArrayOutputStream baos = new ByteArrayOutputStream(1024 * 10);
				final byte[] buff = new byte[1024];
				int l = 0;
				while ((l = is.read(buff)) != -1) {
					baos.write(buff, 0, l);
				}
				final int id = 1;
				final MediaTracker mt = new MediaTracker(c);
				final Image img = toolkit.createImage(baos.toByteArray());
				mt.addImage(img, id);
				try {
					mt.waitForID(id);
				} finally {
					mt.removeImage(img);
				}
				return img;
			}
		}
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

	@Override
	public CharacterData getCharacter(int id, String character) {
		return null;
	}

}
