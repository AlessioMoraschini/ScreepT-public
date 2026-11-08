/**
 *
 * =========================================================================================
 *  Copyright (C) 2019-2026
 *
 *  AM-Design-Development - (Alessio Moraschini) - All Rights Reserved
 * =========================================================================================
 *
 * You should have received a copy of the license with this file.
 * If not, please write to: info@am-design-development.com, or visit : https://www.am-design-development.com
 */
package various.common.light.easteregg.cats;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.Random;

/**
 * Feo and Nano, the two pixel-art cats of the easter egg, playing on a floor: they pop in with a puff, walk, trot,
 * crouch and pounce, chase and kick a ball of yarn, steal it from each other, hop, say something and vanish. At the end
 * Nano the fluffy one follows Feo and they leave the scene together.
 * <p>
 * The look is pixel art, the motion is not: bodies accelerate and brake, squash when they crouch or land and stretch when
 * they jump (a damped spring), the ball bounces with weight, shadows follow the height, and the floor has a depth so the
 * cats walk "towards" and "away" from the viewer.
 * <p>
 * Usage: {@link #setArea} when the size is known, {@link #tick} about 30 times a second, {@link #paint} from the
 * component. No threads and no Swing: it lives in any panel or transparent window.
 */
public final class CatsPlayground {

	private static final Color OUTLINE = new Color(0x1A, 0x1A, 0x1E);
	private static final Color PINK = new Color(0xF2, 0xA0, 0xB0);
	private static final Color EYE = new Color(0xC9, 0xE2, 0x65);
	private static final Color YARN = new Color(0xD9, 0x4A, 0x5A);
	private static final Color YARN_LINE = new Color(0xF5, 0xB8, 0xC0);

	// side view, facing right. O outline, F fur, S stripe, W light fur, P pink, E eye
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

	private static final String[] FEO_WORDS = { "Meow!", "Mew?", "Prrr", "Meooow" };
	private static final String[] NANO_WORDS = { "Mrrp!", "Mrow!", "Prrr", "Mip!" };

	private static final double GRAVITY = 0.5;
	private static final double BALL_GRAVITY = 0.42;

	private enum State { HIDDEN, APPEAR, IDLE, WALK, CHASE, CROUCH, AIR, VANISH, LEAVE, TRAIL }

	private final Random random = new Random();
	private final int scale;
	private final Cat feo;
	private final Cat nano;
	private final Ball ball = new Ball();
	private final BufferedImage[] yarnFrames;
	private final Font font = new Font(Font.MONOSPACED, Font.BOLD, 11);
	private int left;
	private int width = 400;
	private int depthPx;
	private long clock;
	private long finaleAt = Long.MAX_VALUE;
	private boolean leaving;
	private boolean finished;
	private boolean initialized;

	/** @param scale screen pixels for each pixel of art (2 or 3 are good) */
	public CatsPlayground(int scale) {
		this.scale = Math.max(1, scale);
		feo = new Cat("Feo", FEO_WORDS, FEO_BODY, FEO_LEGS_STRIDE, FEO_LEGS_TOGETHER,
				new Color(0xD0, 0x6A, 0x2C), new Color(0xA8, 0x50, 0x20), new Color(0xF2, 0xF2, 0xF0), 1.0, 10);
		nano = new Cat("Nano", NANO_WORDS, NANO_BODY, NANO_LEGS_STRIDE, NANO_LEGS_TOGETHER,
				new Color(0xE0, 0x7B, 0x2C), new Color(0xB4, 0x56, 0x1C), new Color(0xF2, 0xA7, 0x66), 0.82, 30);
		yarnFrames = new BufferedImage[] { sprite(YARN_BALL[0], null), sprite(YARN_BALL[1], null) };
	}

	/**
	 * Floor of the playground
	 *
	 * @param left  left edge in screen pixels
	 * @param width width of the floor
	 * @param depth height of the floor band: the cats can walk up and down inside it (0: a single line)
	 */
	public void setArea(int left, int width, int depth) {
		this.left = left;
		this.width = Math.max(160, width);
		this.depthPx = Math.max(0, depth);
		if (!initialized) {
			initialized = true;
			ball.x = left + this.width * 0.5;
			ball.d = 0.6;
			feo.x = left + this.width * 0.25;
			nano.x = left + this.width * 0.7;
			feo.d = 0.4;
			nano.d = 0.7;
		}
	}

	/** Same as the three arguments version with a flat floor */
	public void setArea(int left, int width) {
		setArea(left, width, 0);
	}

	/** The cats play for this many ticks, then Nano follows Feo and they walk out of the area (default: never) */
	public void leaveAfter(int ticks) {
		finaleAt = clock + Math.max(1, ticks);
	}

	/** True once both cats are out of the area after the finale */
	public boolean isFinished() {
		return finished;
	}

	// ------------------------------------------------------------------ simulation

	/** One step of the animation: call it about 30 times a second */
	public void tick() {
		clock++;
		if (!leaving && clock >= finaleAt) {
			startLeaving();
		}
		ball.tick();
		feo.tick();
		nano.tick();
		if (leaving && !finished && feo.isOut() && nano.isOut()) {
			finished = true;
		}
	}

	private void startLeaving() {
		leaving = true;
		double middle = left + width / 2.0;
		feo.leaveDirection = feo.x < middle ? -1 : 1;
		feo.enter(State.LEAVE, 0);
		nano.enter(State.TRAIL, 0);
	}

	/** Draws the playground; {@code ground} is the y of the floor line at the front (the nearest cats touch it) */
	public void paint(Graphics2D g, int ground) {
		Object previous = g.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
		boolean feoFirst = feo.d <= nano.d;
		double ballDepth = ball.d;
		Cat back = feoFirst ? feo : nano;
		Cat front = feoFirst ? nano : feo;
		back.paintShadow(g, ground);
		front.paintShadow(g, ground);
		ball.paintShadow(g, ground);
		if (ballDepth < back.d) {
			ball.paint(g, ground);
		}
		back.paint(g, ground);
		if (ballDepth >= back.d && ballDepth < front.d) {
			ball.paint(g, ground);
		}
		front.paint(g, ground);
		if (ballDepth >= front.d) {
			ball.paint(g, ground);
		}
		g.setFont(font);
		feo.paintBubble(g, ground);
		nano.paintBubble(g, ground);
		if (previous != null) {
			g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, previous);
		}
	}

	private int contactY(int ground, double depth) {
		return ground - (int) Math.round((1 - depth) * depthPx);
	}

	// ------------------------------------------------------------------ sprites

	private BufferedImage sprite(String[] rows, Cat cat) {
		int columns = 0;
		for (String row : rows) {
			columns = Math.max(columns, row.length());
		}
		BufferedImage image = new BufferedImage(columns * scale, rows.length * scale, BufferedImage.TYPE_INT_ARGB);
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
			case 'F': return cat == null ? null : cat.fur;
			case 'S': return cat == null ? null : cat.stripe;
			case 'W': return cat == null ? null : cat.light;
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

	private static String[] closedEye(String[] body) {
		String[] copy = body.clone();
		for (int i = 0; i < copy.length; i++) {
			copy[i] = copy[i].replace('E', 'O');
		}
		return copy;
	}

	private static double clamp(double value, double min, double max) {
		return Math.max(min, Math.min(max, value));
	}

	// ------------------------------------------------------------------ the ball of yarn

	private final class Ball {
		double x;
		double d = 0.5;
		double z;
		double vx;
		double vd;
		double vz;
		double roll;

		void tick() {
			vz -= BALL_GRAVITY;
			z += vz;
			if (z <= 0) {
				z = 0;
				if (vz < -1.2) {
					vz = -vz * 0.62; // elastic bounce, losing energy
					vx *= 0.92;
				} else {
					vz = 0;
				}
			}
			boolean onGround = z <= 0.01;
			x += vx;
			d += depthPx > 0 ? vd / depthPx : 0;
			double rolling = onGround ? 0.978 : 0.998;
			vx *= rolling;
			vd *= onGround ? 0.96 : 0.995;
			if (Math.abs(vx) < 0.04 && onGround) {
				vx = 0;
			}
			if (Math.abs(vd) < 0.03) {
				vd = 0;
			}
			roll += Math.abs(vx);
			double half = yarnFrames[0].getWidth() / 2.0;
			if (x < left + half) {
				x = left + half;
				vx = Math.abs(vx) * 0.78;
			} else if (x > left + width - half) {
				x = left + width - half;
				vx = -Math.abs(vx) * 0.78;
			}
			if (d < 0.02) {
				d = 0.02;
				vd = Math.abs(vd) * 0.7;
			} else if (d > 1) {
				d = 1;
				vd = -Math.abs(vd) * 0.7;
			}
		}

		void paintShadow(Graphics2D g, int ground) {
			int y = contactY(ground, d);
			int w = (int) Math.round(yarnFrames[0].getWidth() * (0.95 - Math.min(z, 60) / 150));
			shadow(g, (int) Math.round(x), y, w);
		}

		void paint(Graphics2D g, int ground) {
			BufferedImage frame = yarnFrames[((int) (roll / 5)) % 2];
			g.drawImage(frame, (int) Math.round(x - frame.getWidth() / 2.0), contactY(ground, d) - frame.getHeight() - (int) Math.round(z), null);
		}
	}

	private void shadow(Graphics2D g, int cx, int y, int w) {
		java.awt.Composite before = g.getComposite();
		g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.32f));
		g.setColor(Color.BLACK);
		int h = Math.max(2, scale * 2);
		g.fillRect(cx - w / 2 + h / 2, y - h / 2, Math.max(1, w - h), h);
		g.fillRect(cx - w / 2, y - h / 4, Math.max(1, w), Math.max(1, h / 2));
		g.setComposite(before);
	}

	// ------------------------------------------------------------------ a cat

	private final class Cat {
		private final String name;
		private final String[] words;
		private final Color fur;
		private final Color stripe;
		private final Color light;
		private final BufferedImage[] walk;
		private final BufferedImage stand;
		private final BufferedImage blink;
		/** relative running ability */
		private final double agility;

		double x;
		double d = 0.5;
		double vx;
		double vd;
		double z;
		double vz;
		/** vertical squash and stretch (1 = normal) with its spring */
		double sq = 1;
		double sqv;
		int facing = 1;
		int leaveDirection = 1;
		double gait;

		private State state = State.HIDDEN;
		private int stateTicks;
		private int stateLength;
		private double targetX;
		private double targetD;
		private int kickCooldown;
		private int blinkTicks;
		private String bubble;
		private int bubbleTicks;

		Cat(String name, String[] words, String[] body, String[] legsStride, String[] legsTogether,
				Color fur, Color stripe, Color light, double agility, int appearDelay) {
			this.name = name;
			this.words = words;
			this.fur = fur;
			this.stripe = stripe;
			this.light = light;
			this.agility = agility;
			walk = new BufferedImage[] { sprite(join(body, legsStride), this), sprite(join(body, legsTogether), this) };
			stand = walk[1];
			blink = sprite(join(closedEye(body), legsTogether), this);
			enter(State.HIDDEN, appearDelay);
		}

		void enter(State next, int length) {
			state = next;
			stateTicks = 0;
			stateLength = length;
		}

		private double spriteWidth() {
			return walk[0].getWidth();
		}

		boolean isOut() {
			return x < left - spriteWidth() || x > left + width + spriteWidth();
		}

		private Cat other() {
			return this == feo ? nano : feo;
		}

		void tick() {
			stateTicks++;
			if (kickCooldown > 0) {
				kickCooldown--;
			}
			if (bubbleTicks > 0 && --bubbleTicks == 0) {
				bubble = null;
			}
			if (blinkTicks > 0) {
				blinkTicks--;
			} else if (random.nextInt(80) == 0) {
				blinkTicks = 4;
			}
			// squash and stretch: a damped spring around 1
			sqv += (1.0 - sq) * 0.32;
			sqv *= 0.68;
			sq = clamp(sq + sqv, 0.62, 1.32);
			// height
			if (z > 0 || vz != 0) {
				z += vz;
				vz -= GRAVITY;
				sq = clamp(sq + 0.012 * vz, 0.7, 1.3); // stretches while it goes up, shortens while it falls
				if (z <= 0) {
					z = 0;
					if (vz < -2) {
						sq = 0.76 + Math.min(0.1, (-vz - 2) * 0.02); // landing squash
						sqv = 0;
					}
					vz = 0;
					if (state == State.AIR) {
						enter(State.IDLE, 6 + random.nextInt(14));
					}
				}
			}
			switch (state) {
			case HIDDEN:
				if (stateTicks >= stateLength) {
					x = left + spriteWidth() / 2 + random.nextDouble() * Math.max(1, width - spriteWidth());
					d = depthPx > 0 ? 0.15 + random.nextDouble() * 0.8 : 0.5;
					vx = 0;
					vd = 0;
					facing = x < left + width / 2.0 ? 1 : -1;
					enter(State.APPEAR, 16);
				}
				break;
			case APPEAR:
				if (stateTicks >= stateLength) {
					decide();
				}
				break;
			case IDLE:
				brake();
				if (stateTicks >= stateLength) {
					decide();
				}
				break;
			case WALK:
				steer(targetX, targetD, 1.5 * agility, 0.14);
				if ((Math.abs(x - targetX) < 5 && Math.abs((d - targetD) * depthPx) < 4) || stateTicks > 420) {
					enter(State.IDLE, 20 + random.nextInt(60));
				}
				break;
			case CHASE:
				tickChase();
				break;
			case CROUCH:
				brake();
				sq += (0.78 - sq) * 0.45; // loads the spring
				if (stateTicks >= stateLength) {
					vz = 5.4 + random.nextDouble() * 1.2;
					z = 0.1;
					vx = clamp((ball.x - x) * 0.07, -4.6, 4.6);
					facing = vx >= 0 ? 1 : -1;
					sq = 1.12;
					enter(State.AIR, 0);
				}
				break;
			case AIR:
				x += vx;
				vx *= 0.985;
				break;
			case VANISH:
				if (stateTicks >= stateLength) {
					enter(State.HIDDEN, 25 + random.nextInt(60));
				}
				break;
			case LEAVE:
				steer(leaveDirection < 0 ? left - spriteWidth() - 80 : left + width + spriteWidth() + 80, d, 2.9 * agility, 0.16);
				break;
			case TRAIL:
				tickTrail();
				break;
			default:
				break;
			}
			x = leaving ? x : clamp(x, left + spriteWidth() / 2, left + width - spriteWidth() / 2);
			if (depthPx > 0) {
				d = clamp(d, 0.02, 1);
			}
		}

		/** Heads for (tx, td) with acceleration and an arrival that slows down: bodies have weight */
		private void steer(double tx, double td, double maxSpeed, double accel) {
			double desiredVx = clamp((tx - x) * 0.09, -maxSpeed, maxSpeed);
			vx += clamp(desiredVx - vx, -accel, accel);
			x += vx;
			if (depthPx > 0) {
				double desiredVd = clamp((td - d) * depthPx * 0.09, -maxSpeed * 0.55, maxSpeed * 0.55);
				vd += clamp(desiredVd - vd, -accel, accel);
				d += vd / depthPx;
			}
			if (Math.abs(vx) > 0.25) {
				facing = vx > 0 ? 1 : -1;
			}
			gait += (Math.abs(vx) + Math.abs(vd)) * 0.11;
		}

		private void brake() {
			vx *= 0.82;
			vd *= 0.82;
			x += vx;
			if (depthPx > 0) {
				d += vd / depthPx;
			}
			gait += (Math.abs(vx) + Math.abs(vd)) * 0.11;
		}

		private void tickChase() {
			double ahead = ball.x + ball.vx * 7;
			boolean rival = other().kickCooldown > 0;
			steer(ahead, ball.d, (rival ? 3.9 : 3.1) * agility, 0.3);
			double reach = spriteWidth() * 0.5 + yarnFrames[0].getWidth() * 0.5 + 4;
			boolean closeInDepth = depthPx == 0 || Math.abs(ball.d - d) * depthPx < 16;
			if (Math.abs(ball.x - x) < reach && closeInDepth && ball.z < 26 && kickCooldown == 0) {
				kick();
			} else if (stateTicks > 280) {
				enter(State.IDLE, 20);
			}
		}

		/** Bats the ball: sometimes towards the other cat, sometimes away, with a little hop */
		private void kick() {
			Cat rival = other();
			double towardRival = rival.state != State.HIDDEN ? Math.signum(rival.x - x) : facing;
			double dir = random.nextInt(100) < 45 ? towardRival : (random.nextBoolean() ? facing : -facing);
			if (dir == 0) {
				dir = facing;
			}
			double power = 3.4 + random.nextDouble() * 4.2;
			ball.vx = dir * power + vx * 0.5;
			ball.vz = 2.2 + random.nextDouble() * 3.4;
			ball.vd = depthPx > 0 ? (random.nextDouble() - 0.5) * 2.4 : 0;
			facing = dir > 0 ? 1 : -1;
			kickCooldown = 30;
			vz = 2.6;
			z = 0.1;
			sq = 0.86;
			if (random.nextInt(3) == 0) {
				say();
			}
			enter(State.IDLE, 6 + random.nextInt(14));
		}

		private void tickTrail() {
			Cat leader = feo;
			double behind = leader.x - leader.facing * (spriteWidth() * 1.15);
			double gap = Math.abs(behind - x);
			steer(behind, leader.d, Math.min(4.6, 1.4 + gap * 0.035) * agility * 1.3, 0.22);
			if (leader.state == State.LEAVE && (leader.x < left - leader.spriteWidth() * 0.5 || leader.x > left + width + leader.spriteWidth() * 0.5)) {
				// the leader is gone: go through the same exit
				steer(leader.leaveDirection < 0 ? left - spriteWidth() - 80 : left + width + spriteWidth() + 80, d, 3.4, 0.2);
			}
		}

		/** What to do next, at random: the ball is the favourite */
		private void decide() {
			double distance = Math.abs(ball.x - x);
			double r = random.nextDouble();
			if (r < 0.50) {
				enter(State.CHASE, 0);
			} else if (r < 0.62 && distance > 45 && distance < 170) {
				enter(State.CROUCH, 7);
			} else if (r < 0.77) {
				double step = 60 + random.nextDouble() * width * 0.45;
				targetX = clamp(x + (random.nextBoolean() ? step : -step), left + 40, left + width - 40);
				targetD = depthPx > 0 ? 0.1 + random.nextDouble() * 0.9 : 0.5;
				enter(State.WALK, 0);
			} else if (r < 0.90) {
				enter(State.IDLE, 40 + random.nextInt(100));
			} else if (r < 0.96) {
				enter(State.CROUCH, 6);
			} else {
				enter(State.VANISH, 14);
			}
			if (random.nextInt(9) == 0) {
				say();
			}
		}

		private void say() {
			bubble = words[random.nextInt(words.length)];
			bubbleTicks = 55;
		}

		// ---- drawing

		private BufferedImage currentFrame() {
			boolean moving = (state == State.WALK || state == State.CHASE || state == State.LEAVE || state == State.TRAIL) && Math.abs(vx) + Math.abs(vd) > 0.3;
			if (moving) {
				return walk[((int) gait) % 2];
			}
			return blinkTicks > 0 && state == State.IDLE ? blink : stand;
		}

		void paintShadow(Graphics2D g, int ground) {
			if (state == State.HIDDEN) {
				return;
			}
			int w = (int) Math.round(spriteWidth() * 0.78 * (1 - Math.min(z, 70) / 190));
			shadow(g, (int) Math.round(x), contactY(ground, d), w);
		}

		void paint(Graphics2D g, int ground) {
			if (state == State.HIDDEN) {
				return;
			}
			BufferedImage frame = currentFrame();
			float alpha = 1f;
			if (state == State.APPEAR) {
				alpha = Math.min(1f, stateTicks / (float) stateLength);
			} else if (state == State.VANISH) {
				alpha = 1f - Math.min(1f, stateTicks / (float) stateLength);
			}
			double breathing = state == State.IDLE ? 1 + 0.018 * Math.sin(clock * 0.12) : 1;
			double scaleY = sq * breathing;
			double scaleX = 1 + (1 - sq) * 0.7; // keeps the volume: flatter means wider
			int w = (int) Math.round(frame.getWidth() * scaleX);
			int h = (int) Math.round(frame.getHeight() * scaleY);
			// the body bobs one art pixel at every step
			int bob = (state == State.WALK || state == State.CHASE || state == State.LEAVE || state == State.TRAIL) && Math.abs(vx) > 1.2
					? (((int) (gait * 2)) % 2 == 0 ? 0 : scale) : 0;
			int cx = (int) Math.round(x);
			int bottom = contactY(ground, d) - (int) Math.round(z) - bob;
			int top = bottom - h;
			java.awt.Composite before = g.getComposite();
			if (alpha < 1f) {
				g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0f, alpha)));
			}
			if (facing >= 0) {
				g.drawImage(frame, cx - w / 2, top, cx + w - w / 2, bottom, 0, 0, frame.getWidth(), frame.getHeight(), null);
			} else {
				g.drawImage(frame, cx + w - w / 2, top, cx - w / 2, bottom, 0, 0, frame.getWidth(), frame.getHeight(), null);
			}
			g.setComposite(before);
			if (state == State.APPEAR || state == State.VANISH) {
				paintPuff(g, cx, bottom - h / 2, state == State.APPEAR ? stateTicks : stateLength - stateTicks);
			}
		}

		/** Little pixel stars flying out from the cat when it shows up or disappears */
		private void paintPuff(Graphics2D g, int cx, int cy, int step) {
			int distance = (16 - Math.min(16, step)) * scale + 2;
			int size = Math.max(2, scale);
			java.awt.Composite before = g.getComposite();
			g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0.1f, Math.min(1f, step / 16f))));
			int[][] dirs = { { 1, 0 }, { -1, 0 }, { 0, -1 }, { 1, -1 }, { -1, -1 }, { 0, 1 } };
			for (int i = 0; i < dirs.length; i++) {
				g.setColor(i % 2 == 0 ? EYE : new Color(0xF2, 0xF2, 0xF0));
				g.fillRect(cx + dirs[i][0] * distance - size / 2, cy + dirs[i][1] * distance - size / 2, size, size);
			}
			g.setComposite(before);
		}

		void paintBubble(Graphics2D g, int ground) {
			if (bubble == null || state == State.HIDDEN || state == State.APPEAR || state == State.LEAVE || state == State.TRAIL) {
				return;
			}
			FontMetrics metrics = g.getFontMetrics();
			int textWidth = metrics.stringWidth(bubble);
			int bw = textWidth + 14;
			int bh = metrics.getHeight() + 6;
			int cx = (int) Math.round(x);
			int bx = (int) clamp(cx - bw / 2.0, left, left + width - bw);
			int by = contactY(ground, d) - stand.getHeight() - (int) Math.round(z) - bh - 8;
			g.setColor(new Color(0xF2, 0xF2, 0xF0));
			g.fillRect(bx + 1, by, bw - 2, bh);
			g.fillRect(bx, by + 1, bw, bh - 2);
			g.setColor(OUTLINE);
			g.drawRect(bx, by + 1, bw - 1, bh - 3);
			g.fillRect(cx - 2, by + bh - 1, 5, 2);
			g.drawString(bubble, bx + 7, by + bh - 4 - (bh - metrics.getHeight()) / 2);
		}
	}
}
