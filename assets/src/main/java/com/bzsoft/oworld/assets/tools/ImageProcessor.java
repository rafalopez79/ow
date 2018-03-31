package com.bzsoft.oworld.assets.tools;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.imageio.ImageIO;

import com.bzsoft.oworld.util.tuple.Tuple;
import com.bzsoft.oworld.util.tuple.Tuple.Tuple2;

public final class ImageProcessor {

	private static final String MDPROPFILE = "md.properties";
	private static final String IMGPROPFILE = "img.properties";

	protected static final class SuffixFileFilter implements FileFilter {

		private final String suffix;

		public SuffixFileFilter(String suffix) {
			this.suffix = suffix;
		}

		@Override
		public boolean accept(File pathname) {
			final String name = pathname.getName();
			return name.endsWith(suffix);
		}
	}

	public ImageProcessor() {
	}

	public void processZipFiles(File findir, File foutdir) {
		final File[] flist = findir.listFiles(new SuffixFileFilter("zip"));
		for (final File f : flist) {
			try {
				process(f, foutdir);
			} catch (final Exception e) {
				System.err.println(f.getName());
				e.printStackTrace();
			}
		}
	}

	private String getRealName(String name) {
		name = name.replace("T", "");
		name = name.replace("_", "");
		name = name.replace(" ", "");
		name = name.replace(".zip", "");
		return name;
	}

	private void process(File f, File foutdir) throws Exception {
		final File fout = new File(foutdir, getRealName(f.getName()));
		fout.mkdir();
		try (ZipFile zf = new ZipFile(f)) {
			final Enumeration<? extends ZipEntry> eze = zf.entries();
			while (eze.hasMoreElements()) {
				final ZipEntry ze = eze.nextElement();
				if (ze.isDirectory()) {
					continue;
				}
				String name = ze.getName();
				if (!name.endsWith(".bmp")) {
					continue;
				}
				final int indexof = name.indexOf('/');
				if (indexof > -1) {
					name = name.substring(indexof + 1);
				}
				System.out.println("Extracting " + name + " ...");
				InputStream is = null;
				BufferedOutputStream bos = null;
				try {
					is = zf.getInputStream(ze);
					final File file = new File(fout, name);
					bos = new BufferedOutputStream(new FileOutputStream(file));
					final byte[] barray = new byte[(int) ze.getSize()];
					is.read(barray);
					bos.write(barray);
				} catch (final Exception e) {
					throw e;
				} finally {
					if (is != null) {
						try {
							is.close();
						} catch (final Exception e) {
						}
					}
					if (bos != null) {
						try {
							bos.close();
						} catch (final Exception e) {
						}
					}
				}
			}
		}
		processImageDir(fout);
	}

	private void processImageDir(File dir) {
		final File[] flist = dir.listFiles(new SuffixFileFilter("bmp"));
		for (final File f : flist) {
			try {
				processImageFile(f);
			} catch (final Exception e) {
				System.err.println(f.getName());
				e.printStackTrace();
			}
		}
	}

	private void processImageFile(File fimg) throws Exception {
		System.out.println("Processing " + fimg.getName() + " ...");
		try {
			final BufferedImage bimg = ImageIO.read(fimg);
			final BufferedImage bout = processImage(bimg);
			final File fout = new File(fimg.getParentFile(), fimg.getName().replace(".bmp", ".png"));
			System.out.println("Writing " + fout.getPath() + " ...");
			ImageIO.write(bout, "PNG", fout);
			fimg.delete();
		} catch (final Exception e) {
			throw e;
		}
	}

	private BufferedImage processImage(BufferedImage input) {
		final int width = input.getWidth();
		final int height = input.getHeight();
		final BufferedImage output = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR);
		final int p = getTransparentPixel(input, width, height);
		final WritableRaster wra = output.getAlphaRaster();
		for (int i = 0; i < width; i++) {
			for (int j = 0; j < height; j++) {
				final int po = input.getRGB(i, j);
				if (po == p) {
					wra.setSample(i, j, 0, 0);
				}
			}
		}
		for (int i = 0; i < width; i++) {
			for (int j = 0; j < height; j++) {
				final int po = input.getRGB(i, j);
				if (po != p) {
					output.setRGB(i, j, po);
				} else {
					output.setRGB(i, j, 0);
				}
			}
		}
		return output;
	}

	private int getTransparentPixel(BufferedImage input, int width, int height) {
		final HashMap<Integer, Integer> mHisto = new HashMap<>();
		for (int i = 0; i < width; i++) {
			for (int j = 0; j < height; j++) {
				final int po = input.getRGB(i, j);
				Integer count = mHisto.get(po);
				if (count == null) {
					mHisto.put(po, 0);
				} else {
					count++;
					mHisto.put(po, count);
				}
			}
		}
		final List<Integer> l = new ArrayList<>(mHisto.values());
		Collections.sort(l);
		final int count = l.get(l.size() - 1);
		final Set<Integer> sKeys = mHisto.keySet();
		int pkey = 0;
		for (final int key : sKeys) {
			final int value = mHisto.get(key);
			if (value == count) {
				pkey = key;
				break;
			}
		}
		return pkey;
	}

	public Tuple2<BufferedImage, Point> processSize(BufferedImage input) {
		final int width = input.getWidth();
		final int height = input.getHeight();
		int x = 0, y = 0, x2 = width - 1, y2 = height - 1;
		boolean all = true;
		for (int i = 0; i < width; i++) {
			for (int j = 0; j < height; j++) {
				final int pixel = input.getRGB(i, j);
				if (pixel != 0) {
					all = false;
					break;
				}
			}
			if (all) {
				x++;
			} else {
				break;
			}
		}
		all = true;
		for (int j = 0; j < height; j++) {
			for (int i = 0; i < width; i++) {
				final int pixel = input.getRGB(i, j);
				if (pixel != 0) {
					all = false;
					break;
				}
			}
			if (all) {
				y++;
			} else {
				break;
			}
		}
		all = true;
		for (int i = width - 1; i >= 0; i--) {
			for (int j = 0; j < height; j++) {
				final int pixel = input.getRGB(i, j);
				if (pixel != 0) {
					all = false;
					break;
				}
			}
			if (all) {
				x2--;
			} else {
				break;
			}
		}
		all = true;
		for (int j = height - 1; j >= 0; j--) {
			for (int i = 0; i < width; i++) {
				final int pixel = input.getRGB(i, j);
				if (pixel != 0) {
					all = false;
					break;
				}
			}
			if (all) {
				y2--;
			} else {
				break;
			}
		}
		final int w = x2 - x + 1;
		final int h = y2 - y + 1;
		final BufferedImage output = input.getSubimage(x, y, w, h);
		final Point p = new Point(x, y);
		return Tuple.of(output, p);
	}

	public void processPNGFiles(File fDirInput, File fDirOutput) {
		if (!fDirOutput.exists()) {
			fDirOutput.mkdirs();
		}
		final File[] flist = fDirInput.listFiles(new SuffixFileFilter("png"));
		for (final File fInput : flist) {
			try {
				final BufferedImage imginput = ImageIO.read(fInput);
				final IndexColorModelProcessor imgp = new IndexColorModelProcessor(imginput);
				final BufferedImage imgoutput = imgp.convertToByte(255);
				final File foutput = new File(fDirOutput, fInput.getName());
				ImageIO.write(imgoutput, "PNG", foutput);
			} catch (final Exception e) {
				e.printStackTrace();
			}
		}
	}

	public void processSizeFiles(File fDirInput, File fDirOutput, String prefix, String fileprefix) {
		if (!fDirOutput.exists()) {
			fDirOutput.mkdirs();
		}
		final Properties mdprop = new Properties();
		final Properties imgprop = new Properties();
		final File[] flist = fDirInput.listFiles(new SuffixFileFilter("png"));
		final Pattern pattern = Pattern.compile("(.+)\\s(n|s|e|w|ne|nw|se|sw)(\\d\\d\\d\\d)\\.png");
		final Map<String, Map<String, Integer>> stateDirectionCountMap = new TreeMap<>();
		for (final File fInput : flist) {
			try {
				final BufferedImage imginput = ImageIO.read(fInput);
				final Tuple2<BufferedImage, Point> poutput = processSize(imginput);
				final File foutput = new File(fDirOutput, fInput.getName());
				ImageIO.write(poutput.get1(), "PNG", foutput);
				final Point p = poutput.get2();
				final String name = fInput.getName();
				final Matcher m = pattern.matcher(name);
				final String state;
				final String direction;
				final String count;
				int c;
				if (m.matches()) {
					state = m.group(1).replaceAll("\\s", "");
					direction = m.group(2);
					count = m.group(3);
					c = Integer.parseInt(count);
					Map<String, Integer> mDir = stateDirectionCountMap.get(state);
					if (mDir == null) {
						mDir = new TreeMap<>();
						stateDirectionCountMap.put(state, mDir);
					}
					final Integer icount = mDir.get(direction);
					if (icount == null) {
						mDir.put(direction, c + 1);
					} else {
						mDir.put(direction, Math.max(icount, c + 1));
					}
				} else {
					continue;
				}
				final String spritename = state + "_" + direction + "_" + c;
				mdprop.setProperty(spritename, p.x + "," + p.y);
				imgprop.setProperty(spritename, fileprefix + "/" + prefix + "/" + name);
			} catch (final Exception e) {
				e.printStackTrace();
			}
		}
		mdprop.put("states", stateDirectionCountMap.keySet().stream().collect(Collectors.joining(",")));
		for (final Entry<String, Map<String, Integer>> e : stateDirectionCountMap.entrySet()) {
			final String state = e.getKey();
			mdprop.put(state, e.getValue().keySet().stream().collect(Collectors.joining(",")));
			for (final Entry<String, Integer> e2 : e.getValue().entrySet()) {
				mdprop.put(state + "." + e2.getKey(), e2.getValue().toString());
			}
		}
		FileOutputStream fos = null;
		try {
			final File f = new File(fDirOutput, MDPROPFILE);
			fos = new FileOutputStream(f);
			mdprop.store(fos, "Displacements");
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
		try {
			final File f = new File(fDirOutput, IMGPROPFILE);
			fos = new FileOutputStream(f);
			imgprop.store(fos, "Images");
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

	public void generatePropertyFiles(File fDirInput, File fDirOutput) {
		final Properties propPlayer = new Properties();
		final Properties propImg = new Properties();
		FileInputStream fis = null;
		try {
			final File f = new File(fDirInput, null); // TODO:!!
			fis = new FileInputStream(f);
			propImg.load(fis);
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
		final File[] flist = fDirInput.listFiles(new SuffixFileFilter("png"));
		Arrays.sort(flist);
		final EnumSet<PlayerState> addedPS = EnumSet.noneOf(PlayerState.class);
		int num = 0;
		for (final File file : flist) {
			final String name = file.getName();
			String shortName = name.trim().replaceAll(" ", "").toUpperCase();
			final int numIndex = shortName.indexOf('0');
			if (numIndex >= 0) {
				shortName = shortName.substring(0, numIndex);
				PlayerState ps = null;
				try {
					ps = PlayerState.valueOf(shortName);
				} catch (final Exception e) {
					e.printStackTrace();
					num = 0;
					continue;
				}
				if (ps != null) {
					if (addedPS.contains(ps)) {
						num++;
					} else {
						addedPS.add(ps);
						num = 0;
					}
					propPlayer.setProperty(ps.name() + "." + num, name);
					propPlayer.setProperty(ps.name() + "." + num + ".disp", propImg.getProperty(name));
				}
			}
		}
		final StringBuilder sb = new StringBuilder();
		for (final PlayerState p : addedPS) {
			sb.append(p).append(';');
		}
		propPlayer.setProperty("States", sb.toString());
		FileOutputStream fos = null;
		try {
			final File f = new File(fDirOutput, "Player.properties");
			fos = new FileOutputStream(f);
			propPlayer.store(fos, "Player");
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

	public static void main(String[] args) {
		// File fdir = new File ("d:/animals/");
		// File fout = new File ("d:/animalsout/");

		// File fdir = new File ("d:/dinored-out2/");
		// File fout = new File ("d:/dinored-out3/");
		//// final File fdir = new File("d:/crocy/");
		// File fout = new File ("d:/crocy/");
		// File fout2 = new File ("d:/crocy-out/");
		//// final File fout = fdir;
		final File fout = new File("d:/test/animalsout/dinogreen/");
		final File fout2 = new File("d:/test/animalsout2/dinogreen/");
		final File fout3 = new File("d:/test/animalsout3/dinogreen/");
		final File fdir = new File("d:/test/animals/dinogreen/");
		fout.delete();
		fout2.delete();
		fout3.delete();
		if (!fout.exists()) {
			fout.mkdirs();
		}
		if (!fout2.exists()) {
			fout2.mkdirs();
		}
		if (!fout3.exists()) {
			fout3.mkdirs();
		}
		final ImageProcessor imp = new ImageProcessor();
		// imp.processZipFiles(fdir, fout);
		imp.processPNGFiles(fout, fout2);
		imp.processSizeFiles(fout2, fout3, "dinogreen", "img/chars");
		// imp.generatePropertyFiles(fout3, fout3);
		// PropertyFileSorter.sortPropertyFile(new File(fout, "Player.properties"), new
		// File(fout, "Player2.properties"));
	}
}
