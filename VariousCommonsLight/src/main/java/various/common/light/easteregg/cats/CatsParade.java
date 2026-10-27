/**
 *
 * =========================================================================================
 *  Copyright (C) 2019-2020
 *
 *  AM-Design-Development - (Alessio Moraschini) - All Rights Reserved
 * =========================================================================================
 *
 * You should have received a copy of the license with this file.
 * If not, please write to: info@am-design-development.com, or visit : https://www.am-design-development.com
 */
package various.common.light.easteregg.cats;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsDevice;
import java.awt.GraphicsDevice.WindowTranslucency;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JWindow;
import javax.swing.Timer;

import various.common.light.utility.log.SafeLogger;

/**
 * Easter egg: Feo and Nano, two pixel-art cats, walk across a window. Nano chases a ball of yarn, Feo follows calmly,
 * both leave fading paw prints and say something on the way.
 * Transparent, non focusable, always on top overlay: transparent pixels let the mouse through, it closes by itself.
 */
public final class CatsParade extends JWindow {

	private static final long serialVersionUID = 4127783391162581930L;

	private static SafeLogger logger = new SafeLogger(CatsParade.class);

	private static final int TICK_MS = 40;
	private static final int CROSSING_MS = 7500;
	private static final int WALK_FRAME_TICKS = 4;
	private static final int BOTTOM_MARGIN = 56;
	private static final int PRINT_STEP = 22;
	private static final int PRINT_LIFE_TICKS = 55;

	private static final Color OUTLINE = new Color(0x1A, 0x1A, 0x1E);
	private static final Color WHITE = new Color(0xF2, 0xF2, 0xF0);
	private static final Color PINK = new Color(0xF2, 0xA0, 0xB0);
	private static final Color EYE = new Color(0xC9, 0xE2, 0x65);
	private static final Color YARN = new Color(0xD9, 0x4A, 0x5A);
	private static final Color YARN_LINE = new Color(0xF5, 0xB8, 0xC0);
	private static final Color TEXT = new Color(0xDF, 0xE1, 0xE5);

	// side view, facing right. O = outline, F = fur, S = shading, W = light fur, P = pink, E = eye
	// Feo: slim, red on top and white below, thin muzzle, long white legs
	private static final String[] FEO_BODY = {
			"..............O...O..",
			"..O..........OPO.OPO.",
			".OFO.........OFFFFFO.",
			".OFO.........OFFFEFO.",
			"..OFO........OFFFFWWP",
			"...OFOOOOOOOOFFFWWO..",
			"...OFFFFFFFFFFFWO....",
			"...OWWWWWWWWWWWO.....",
			"....OOOOOOOOOOO......" };
	private static final String[] FEO_LEGS_STRIDE = {
			"....OWO.OWO.OWO.OWO..",
			"...OWO.OWO..OWO.OWO..",
			"..OWO...OWO.OWO...OWO",
			"..OO....OO..OO....OO." };
	private static final String[] FEO_LEGS_TOGETHER = {
			"....OWO.OWO.OWO.OWO..",
			"....OWO.OWO.OWO.OWO..",
			"....OWO.OWO.OWO.OWO..",
			"....OO..OO..OO..OO..." };
	// Nano: chubby and fluffy, all red: thick tail, tufts on the back, round body, short legs
	private static final String[] NANO_BODY = {
			"..............O...O..",
			".OO..........OPO.OPO.",
			"OFFO........OFFFFFFFO",
			"OFFO.......OFFFFFFEFO",
			".OFFO..O.O.OFFFFFFWWP",
			"..OFFOOOOOOOFFFFFFWWO",
			"..OFFSFFFSFFFSFFFWWO.",
			"..OFFSFFFSFFFSFFFWO..",
			"..OFFFFFFFFFFFFFWO...",
			"..OFWWWWWWWWWWWFO....",
			"...OOOOOOOOOOOOO....." };
	private static final String[] NANO_LEGS_STRIDE = {
			"...OFO.OFO..OFO.OFO..",
			"..OOO...OO..OO...OOO." };
	private static final String[] NANO_LEGS_TOGETHER = {
			"....OFO.OFO.OFO.OFO..",
			"....OO..OO..OO..OO..." };
	private static final String[][] YARN_BALL = {
			{ "..OOO..", ".ORRRO.", "ORRLRRO", "ORLRRRO", "ORRRLRO", ".ORRRO.", "..OOO.." },
			{ "..OOO..", ".ORRRO.", "ORLRRRO", "ORRRLRO", "ORRLRRO", ".ORRRO.", "..OOO.." } };

	private static CatsParade instance;

	private final Cat feo = new Cat("Feo", "Meow!", FEO_BODY, FEO_LEGS_STRIDE, FEO_LEGS_TOGETHER,
			new Color(0xD0, 0x6A, 0x2C), new Color(0xA8, 0x50, 0x20), WHITE, 4, 0.42f);
	private final Cat nano = new Cat("Nano", "Mrrp!", NANO_BODY, NANO_LEGS_STRIDE, NANO_LEGS_TOGETHER,
			new Color(0xE0, 0x7B, 0x2C), new Color(0xB4, 0x56, 0x1C), new Color(0xF2, 0xA7, 0x66), 5, 0.64f);
	private final BufferedImage[] yarnFrames = { sprite(YARN_BALL[0], null, 4), sprite(YARN_BALL[1], null, 4) };
	private final List<PawPrint> pawPrints = new ArrayList<>();
	private final Font font = new Font(Font.MONOSPACED, Font.BOLD, 15);
	private final Timer timer;
	private final double speed;
	private int tick;
	/** x of the group's left edge (Feo) */
	private double groupX;

	/**
	 * Starts the parade over the given window (whole screen if null or not showing); ignored if one is already running
	 * or the system does not support transparent windows.
	 */
	public static synchronized void start(Window over) {
		if (instance != null) {
			return;
		}
		GraphicsDevice device = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
		if (!device.isWindowTranslucencySupported(WindowTranslucency.PERPIXEL_TRANSLUCENT)) {
			logger.info("Cats parade skipped: transparent windows not supported");
			return;
		}
		Rectangle area = over != null && over.isShowing() ? over.getBounds() : device.getDefaultConfiguration().getBounds();
		instance = new CatsParade(area);
		instance.setVisible(true);
		instance.timer.start();
	}

	private CatsParade(Rectangle area) {
		setBounds(area);
		setBackground(new Color(0, 0, 0, 0));
		setFocusableWindowState(false);
		setAlwaysOnTop(true);

		JComponent scene = new JComponent() {
			private static final long serialVersionUID = 1L;
			@Override
			protected void paintComponent(Graphics graphics) {
				paintScene((Graphics2D) graphics);
			}
		};
		setContentPane(scene);

		groupX = -groupWidth() - 20;
		speed = (area.width + groupWidth() + 40) / (double) (CROSSING_MS / TICK_MS);
		timer = new Timer(TICK_MS, e -> step());
	}

	private int groupWidth() {
		return feo.width() + 40 + nano.width() + 50 + yarnFrames[0].getWidth();
	}

	private void step() {
		tick++;
		groupX += speed;

		feo.moveTo(groupX, 0);
		// Nano is younger: hops a bit while chasing the yarn
		nano.moveTo(groupX + feo.width() + 40, Math.abs(Math.sin(tick * 0.25)) * 6);
		feo.leavePrints();
		nano.leavePrints();

		for (Iterator<PawPrint> iterator = pawPrints.iterator(); iterator.hasNext();) {
			if (++iterator.next().age > PRINT_LIFE_TICKS) {
				iterator.remove();
			}
		}

		if (groupX > getWidth() + 20) {
			stop();
		}
		repaint();
	}

	private void stop() {
		timer.stop();
		dispose();
		synchronized (CatsParade.class) {
			instance = null;
		}
	}

	private void paintScene(Graphics2D g) {
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
		g.setFont(font);
		int ground = getHeight() - BOTTOM_MARGIN;

		for (PawPrint print : pawPrints) {
			int alpha = Math.max(0, 170 - print.age * 170 / PRINT_LIFE_TICKS);
			g.setColor(new Color(print.color.getRed(), print.color.getGreen(), print.color.getBlue(), alpha));
			g.fillRect(print.x, ground + 2, 4, 4);
			g.fillRect(print.x + 6, ground + 4, 4, 4);
		}

		BufferedImage yarn = yarnFrames[(tick / WALK_FRAME_TICKS) % 2];
		int yarnBounce = (int) Math.round(Math.abs(Math.sin(tick * 0.3)) * 14);
		int yarnX = (int) Math.round(nano.x + nano.width() + 50);
		g.drawImage(yarn, yarnX, ground - yarn.getHeight() - yarnBounce, null);

		feo.paint(g, ground);
		nano.paint(g, ground);
	}

	private static BufferedImage sprite(String[] rows, Cat cat, int scale) {
		BufferedImage image = new BufferedImage(rows[0].length() * scale, rows.length * scale, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = image.createGraphics();
		for (int row = 0; row < rows.length; row++) {
			for (int column = 0; column < rows[row].length(); column++) {
				Color color = colorOf(rows[row].charAt(column), cat);
				if (color != null) {
					g.setColor(color);
					g.fillRect(column * scale, row * scale, scale, scale);
				}
			}
		}
		g.dispose();
		return image;
	}

	private static Color colorOf(char pixel, Cat cat) {
		switch (pixel) {
			case 'O': return OUTLINE;
			case 'F': return cat.fur;
			case 'S': return cat.stripe;
			case 'W': return cat.light;
			case 'P': return PINK;
			case 'E': return EYE;
			case 'R': return YARN;
			case 'L': return YARN_LINE;
			default: return null;
		}
	}

	private static String[] join(String[] top, String[] bottom) {
		String[] rows = new String[top.length + bottom.length];
		System.arraycopy(top, 0, rows, 0, top.length);
		System.arraycopy(bottom, 0, rows, top.length, bottom.length);
		return rows;
	}

	/** Pixel speech bubble with stepped corners and a small tail */
	private static void paintBubble(Graphics2D g, String text, int centerX, int bottom) {
		FontMetrics metrics = g.getFontMetrics();
		int width = metrics.stringWidth(text) + 20;
		int height = metrics.getHeight() + 8;
		int x = centerX - width / 2;
		int y = bottom - height - 8;

		g.setColor(OUTLINE);
		g.fillRect(x + 4, y, width - 8, height);
		g.fillRect(x, y + 4, width, height - 8);
		g.fillRect(centerX - 4, y + height, 8, 4);
		g.fillRect(centerX - 2, y + height + 4, 4, 4);
		g.setColor(WHITE);
		g.fillRect(x + 4, y + 2, width - 8, height - 4);
		g.fillRect(x + 2, y + 4, width - 4, height - 8);
		g.fillRect(centerX - 2, y + height - 2, 4, 4);
		g.setColor(OUTLINE);
		g.drawString(text, x + 10, y + 4 + metrics.getAscent());
	}

	private final class Cat {
		private final String name;
		private final String says;
		private final Color fur;
		private final Color stripe;
		private final Color light;
		/** where the cat talks, as a fraction of the window width */
		private final float talkAt;
		private final BufferedImage[] frames;
		private double x;
		private double hop;
		private double lastPrintX = Double.NEGATIVE_INFINITY;

		Cat(String name, String says, String[] body, String[] legsStride, String[] legsTogether,
				Color fur, Color stripe, Color light, int scale, float talkAt) {
			this.name = name;
			this.says = says;
			this.fur = fur;
			this.stripe = stripe;
			this.light = light;
			this.talkAt = talkAt;
			frames = new BufferedImage[] { sprite(join(body, legsStride), this, scale), sprite(join(body, legsTogether), this, scale) };
		}

		int width() {
			return frames[0].getWidth();
		}

		void moveTo(double newX, double newHop) {
			x = newX;
			hop = newHop;
		}

		void leavePrints() {
			if (x - lastPrintX >= PRINT_STEP) {
				lastPrintX = x;
				pawPrints.add(new PawPrint((int) Math.round(x + width() * 0.2), stripe));
			}
		}

		void paint(Graphics2D g, int ground) {
			BufferedImage frame = frames[(tick / WALK_FRAME_TICKS) % 2];
			int left = (int) Math.round(x);
			int top = ground - frame.getHeight() - (int) Math.round(hop);
			g.drawImage(frame, left, top, null);

			int centerX = left + frame.getWidth() / 2;
			FontMetrics metrics = g.getFontMetrics();
			int nameY = top - 6;
			g.setColor(new Color(0, 0, 0, 160));
			g.drawString(name, centerX - metrics.stringWidth(name) / 2 + 2, nameY + 2);
			g.setColor(TEXT);
			g.drawString(name, centerX - metrics.stringWidth(name) / 2, nameY);

			float position = centerX / (float) Math.max(1, getWidth());
			if (position > talkAt && position < talkAt + 0.14f) {
				paintBubble(g, says, centerX, nameY - metrics.getAscent());
			}
		}
	}

	private static final class PawPrint {
		private final int x;
		private final Color color;
		private int age;

		PawPrint(int x, Color color) {
			this.x = x;
			this.color = color;
		}
	}
}
