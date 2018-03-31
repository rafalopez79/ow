package com.bzsoft.oworld.ui;

import java.awt.Color;
import java.awt.DisplayMode;
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.Graphics2D;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferStrategy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bzsoft.oworld.R;
import com.bzsoft.oworld.assets.ResourceManager;
import com.bzsoft.oworld.assets.impl.BaseResourceManager;
import com.bzsoft.oworld.ui.components.DrawEvent;
import com.bzsoft.oworld.ui.components.Drawable;
import com.bzsoft.oworld.ui.components.impl.UIFpsCounter;
import com.bzsoft.oworld.ui.components.impl.UIPanel;
import com.bzsoft.oworld.ui.components.impl.UIProgress;
import com.bzsoft.oworld.ui.util.I18nManager;
import com.bzsoft.oworld.ui.util.I18nNaming;
import com.bzsoft.oworld.ui.util.event.EventLoop;
import com.bzsoft.oworld.ui.util.event.impl.EventLoopImpl;
import com.bzsoft.oworld.ui.util.impl.I18nManagerImpl;

public final class Launcher {

	protected static final Logger LOGGER = LoggerFactory.getLogger(Launcher.class);
	protected static final long MILLIS = 1000 / 12;
	protected static final int IMGCACHESIZE = 2048;

	protected final Frame frame;
	protected final I18nManager i18nManager;
	protected final Thread animThread;
	protected final Map<RenderingHints.Key, Object> renderingHints;
	protected final EventLoop el;
	protected final SortedMap<Integer, List<Drawable>> drawables;
	protected final ResourceManager resourceManager;
	protected volatile boolean running;

	protected Launcher() {
		frame = new Frame();
		i18nManager = new I18nManagerImpl();
		i18nManager.setLocale(Locale.ENGLISH);
		renderingHints = createRenderingHints();
		drawables = new TreeMap<>();
		el = new EventLoopImpl(null);
		resourceManager = new BaseResourceManager(frame, IMGCACHESIZE);
		animThread = new Thread(createGameLoop(frame), "AnimationThread");
	}

	protected final void initUI() {
		frame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				exit();
			}
		});
		frame.setIconImage(resourceManager.getImage(R.Resources.icon));
		frame.setIgnoreRepaint(true);
		frame.setUndecorated(true);
		i18nManager.addLocaleChangeListener(() -> {
			frame.setTitle(i18nManager.getText(I18nNaming.APP_TITLE));
		});
		frame.setBackground(Color.BLACK);
		frame.setResizable(false);
		final GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		final Rectangle rec = ge.getMaximumWindowBounds();
		frame.setSize((int) rec.getWidth(), (int) rec.getHeight());
		frame.setLocationRelativeTo(null);
		final boolean fullScreen = tryFullScreen(frame);
		LOGGER.info("Trying fullscreen: {}", fullScreen);
		frame.setVisible(true);
		EventQueue.invokeLater(() -> {
			running = true;
			animThread.start();
		});
	}

	private static boolean tryFullScreen(Frame frame) {
		final GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		final GraphicsDevice gs = ge.getDefaultScreenDevice();
		if (gs.isFullScreenSupported()) {
			final DisplayMode dm = gs.getDisplayMode();
			try {
				gs.setFullScreenWindow(frame);
				final DisplayMode sdm = getPreferredDisplayMode(gs.getDisplayModes(), 1280, 800, 8, 60);
				gs.setDisplayMode(sdm != null ? sdm : dm);
				return true;
			} catch (final Exception e) {
				gs.setFullScreenWindow(null);
				gs.setDisplayMode(dm);
				return false;
			}
		}
		return false;
	}

	private static final DisplayMode getPreferredDisplayMode(DisplayMode[] dms, int w, int h, int bitDepth, int hz) {
		DisplayMode selected = null;
		for (final DisplayMode dm : dms) {
			if (dm.getHeight() == h && dm.getWidth() == w && dm.getBitDepth() == bitDepth
					&& dm.getRefreshRate() == hz) {
				selected = dm;
				break;
			}
		}
		return selected;
	}

	protected void exit() {
		LOGGER.info("Exiting ...");
		running = false;
		System.exit(0);
	}

	protected void startGameLoop() {
		animThread.start();
	}

	protected final Runnable createGameLoop(final Frame f) {
		return () -> {
			f.createBufferStrategy(3);
			final BufferStrategy bstg = f.getBufferStrategy();
			createGame(f.getWidth(), f.getHeight());
			el.addListener(DrawEvent.class, e -> {
				final long now = System.currentTimeMillis();
				for (final List<Drawable> list : drawables.values()) {
					for (final Drawable d : list) {
						d.onDraw(e.getGraphics(), e.getWidth(), e.getHeight(), now);
					}
				}
			});
			el.addListener(BufferStrategy.class, e -> {
				do {
					do {
						final Graphics2D g2d = (Graphics2D) e.getDrawGraphics();
						g2d.setRenderingHints(renderingHints);
						final int w = f.getWidth();
						final int h = f.getHeight();
						g2d.clearRect(0, 0, w, h);
						el.runEvent(DrawEvent.class, new DrawEvent(g2d, w, h));
						Toolkit.getDefaultToolkit().sync();
						g2d.dispose();
					} while (bstg.contentsRestored());
					bstg.show();
				} while (bstg.contentsLost());
			});
			final Runnable anim = new Runnable() {
				@Override
				public void run() {
					el.pushEvent(BufferStrategy.class, bstg);
					el.submit(this, MILLIS);
				}
			};
			el.submit(anim, MILLIS);
			while (!el.isClosed()) {
				el.readAndDispatch();
			}
		};
	}

	protected final void addDrawable(int zeta, Drawable d) {
		synchronized (drawables) {
			List<Drawable> l = drawables.get(zeta);
			if (l == null) {
				l = new ArrayList<>();
				drawables.put(zeta, l);
			}
			l.add(d);
		}
	}

	protected final void removeDrawable(Drawable d) {
		synchronized (drawables) {
			for (final List<Drawable> list : drawables.values()) {
				list.remove(d);
			}
		}
	}

	protected final void createGame(int w, int h) {
		final int maxProgress = 100;
		final Rectangle rect = new Rectangle(w / 4, h / 2 - h / 32, w / 2, h / 16);
		final UIProgress progress = new UIProgress(el, rect, maxProgress);
		addDrawable(0, progress);
		final UIPanel panel = new UIPanel(el, resourceManager, rect, frame);
		addDrawable(100, new UIFpsCounter(el));
		final Thread backgroundLoader = new Thread(() -> {
			try {
				// load images
				final Image background = resourceManager.getImage(R.Resources.background);
				panel.setBackground(background);
				//
				progress.incrProgress(10);
				resourceManager.loadCharacterInfo();
				progress.incrProgress(90);
				removeDrawable(progress);
				addDrawable(0, panel);
			} catch (final Exception e) {
				// TODO: error loading game
			}
		});
		backgroundLoader.start();
	}

	protected static final Map<RenderingHints.Key, Object> createRenderingHints() {
		final Map<RenderingHints.Key, Object> hints = new HashMap<>();
		hints.put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		hints.put(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
		hints.put(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
		hints.put(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		hints.put(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		hints.put(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		hints.put(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
		return hints;
	}

	public static void main(String[] args) {
		if (GraphicsEnvironment.isHeadless()) {
			LOGGER.error("Headless graphics environment.");
			return;
		}
		EventQueue.invokeLater(() -> {
			LOGGER.info("Starting ...");
			final Launcher l = new Launcher();
			l.initUI();
		});
	}

}
