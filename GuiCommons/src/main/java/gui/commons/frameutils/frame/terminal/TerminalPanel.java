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
package gui.commons.frameutils.frame.terminal;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import various.common.light.gui.ShortcutKeys;

/**
 * Interactive terminal: a real console (Windows pseudo console) shown with colors, cursor and scrollback. Programs
 * that need a terminal (prompts, editors, colored tools, progress bars) work as in any terminal emulator.
 * Keys go straight to the process; Ctrl+C copies the selection when there is one, else interrupts; Ctrl+V pastes.
 * History: mouse wheel, scroll bar and Shift+PageUp/PageDown/Home/End scroll the scrollback; full screen programs (alternate
 * screen) get the wheel as they ask (mouse reports) or as arrow keys. Ctrl+Shift+H (Cmd+Shift+H on macOS) opens the search bar.
 * With the focus here the terminal owns the keyboard: no shortcut of the other panels or of the menus runs ({@link ShortcutKeys#CAPTURE_ALL_KEYS}),
 * and its own shortcuts are not used by any other function of the application.
 */
public class TerminalPanel extends JPanel {

	private static final long serialVersionUID = 1L;

	private static final Color DEFAULT_FG = new Color(0xCC, 0xCC, 0xCC);
	private static final Color DEFAULT_BG = new Color(0x0C, 0x0C, 0x0C);
	private static final Color SELECTION = new Color(0x26, 0x4F, 0x78);
	private static final Color MATCH_BG = new Color(0x6B, 0x5B, 0x1A);
	private static final Color CURRENT_MATCH_BG = new Color(0xF2, 0xA3, 0x3A);
	private static final int MAX_MATCHES = 5000;

	private final Canvas canvas = new Canvas();
	private final JScrollBar scrollBar = new JScrollBar(JScrollBar.VERTICAL);
	private final Timer refreshTimer;
	private final TerminalSearchBar searchBar;
	/** Matches of the search: {line number (dropped + index), first column, length}, ordered by position */
	private final List<long[]> matches = new ArrayList<long[]>();
	private int currentMatch = -1;
	private boolean searchStale;
	private long lastSearchTime;
	private long lastAbsTotal;
	private long lastDropped;
	private double wheelAccumulator;

	private VtScreen screen;
	private PtySession session;
	private volatile boolean gotOutput;
	/** Windows: ConPTY gives the best fidelity; where it does not attach (no output at all) winpty is used for the rest of the run */
	private static volatile boolean conPtyBroken;
	private Runnable onExit;
	private float fontSize = 14f;
	private volatile boolean dirty;
	private boolean adjustingScrollBar;

	/** Lines scrolled back from the bottom (0 = following the output) */
	private int scrollOffset;
	private int selAnchorLine = -1;
	private int selAnchorCol;
	private int selEndLine = -1;
	private int selEndCol;

	/** Best monospaced font of this system, so the terminal looks like the native one */
	private static String monospacedFamily() {
		java.util.Set<String> available = new java.util.HashSet<String>(java.util.Arrays.asList(
				java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()));
		for (String family : new String[] { "Cascadia Mono", "Consolas", "Menlo", "DejaVu Sans Mono", "Liberation Mono", "Courier New" }) {
			if (available.contains(family)) {
				return family;
			}
		}
		return Font.MONOSPACED;
	}

	/** True when the interactive terminal can run on this system */
	public static boolean isSupported() {
		return PtySession.isSupported();
	}

	public TerminalPanel() {
		super(new BorderLayout());
		searchBar = new TerminalSearchBar(new TerminalSearchBar.Host() {
			@Override
			public void queryChanged() {
				runSearch(true);
			}

			@Override
			public void step(boolean older) {
				stepMatch(older);
			}

			@Override
			public void closeRequested() {
				hideSearch();
			}
		});
		searchBar.setVisible(false);
		add(searchBar, BorderLayout.NORTH);
		add(canvas, BorderLayout.CENTER);
		add(scrollBar, BorderLayout.EAST);
		scrollBar.addMouseWheelListener(e -> scrollByWheel(e.getPreciseWheelRotation()));
		screen = new VtScreen(80, 24, this::sendString);
		// the canvas is built before the font size is set: measure again now
		canvas.fontChanged();
		scrollBar.addAdjustmentListener(e -> {
			if (!adjustingScrollBar) {
				scrollOffset = Math.max(0, scrollBar.getMaximum() - scrollBar.getVisibleAmount() - scrollBar.getValue());
				canvas.repaint();
			}
		});
		refreshTimer = new Timer(30, e -> {
			if (dirty) {
				dirty = false;
				updateScrollBar();
				searchStale = searchStale || searchBar.isVisible();
				canvas.repaint();
			}
			// the output goes on while the search is open: matches are refreshed a few times per second
			if (searchStale && System.currentTimeMillis() - lastSearchTime > 250) {
				runSearch(false);
			}
		});
		refreshTimer.start();
	}

	public void setOnExit(Runnable onExit) {
		this.onExit = onExit;
	}

	public void setTerminalFontSize(float size) {
		fontSize = size;
		canvas.fontChanged();
	}

	public boolean isAlive() {
		return session != null && session.isAlive();
	}

	/** Starts the default shell of this system in a new terminal (the previous one, if any, is closed) */
	public void start(String directory) throws IOException {
		stop();
		synchronized (screen) {
			screen = new VtScreen(screen.cols, screen.rows, this::sendString);
		}
		scrollOffset = 0;
		lastAbsTotal = 0;
		lastDropped = 0;
		clearSelection();
		matches.clear();
		currentMatch = -1;
		launch(directory, PtySession.isWindows() && !conPtyBroken);
		canvas.requestFocusInWindow();
	}

	private void launch(String directory, boolean useConPty) throws IOException {
		final PtySession pty = new PtySession(ShellLauncher.command(), directory, ShellLauncher.environment(), screen.cols, screen.rows, useConPty);
		session = pty;
		gotOutput = false;

		Thread reader = new Thread(() -> readLoop(pty), "Terminal-reader");
		reader.setDaemon(true);
		reader.start();

		if (useConPty) {
			// a ConPTY that does not attach prints nothing: after a few seconds the shell is restarted on winpty
			Timer check = new Timer(3000, e -> {
				if (session == pty && !gotOutput) {
					conPtyBroken = true;
					pty.destroy();
					try {
						launch(directory, false);
					} catch (IOException ex) {
						synchronized (screen) {
							screen.feed("\r\n\u001b[31mCannot start the terminal: " + ex.getMessage() + "\u001b[0m\r\n");
						}
						dirty = true;
					}
				}
			});
			check.setRepeats(false);
			check.start();
		}

		Thread waiter = new Thread(() -> {
			pty.waitForExit();
			// let the reader drain what the process wrote before it ended
			try {
				Thread.sleep(150);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			pty.destroy();
			SwingUtilities.invokeLater(() -> {
				if (session == pty && (gotOutput || !useConPty)) {
					synchronized (screen) {
						screen.feed("\r\n\u001b[90m[process ended - press Enter or Play to start a new one]\u001b[0m\r\n");
					}
					dirty = true;
					if (onExit != null) {
						onExit.run();
					}
				}
			});
		}, "Terminal-waiter");
		waiter.setDaemon(true);
		waiter.start();
	}

	public void stop() {
		PtySession current = session;
		session = null;
		if (current != null) {
			current.destroy();
		}
	}

	/** Text typed by the user, or a command sent by the owner (add "\r" to press Enter) */
	public void sendString(String text) {
		PtySession current = session;
		if (current != null && text != null && !text.isEmpty()) {
			current.write(text.getBytes(StandardCharsets.UTF_8));
		}
	}

	public void focusTerminal() {
		canvas.requestFocusInWindow();
	}

	public void dispose() {
		refreshTimer.stop();
		stop();
	}

	private void readLoop(PtySession pty) {
		byte[] buffer = new byte[8192];
		CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPLACE)
				.onUnmappableCharacter(CodingErrorAction.REPLACE);
		ByteBuffer pending = ByteBuffer.allocate(buffer.length + 8);
		CharBuffer chars = CharBuffer.allocate(buffer.length + 8);
		int read;
		while ((read = pty.read(buffer)) > 0) {
			gotOutput = true;
			pending.put(buffer, 0, read);
			pending.flip();
			chars.clear();
			decoder.decode(pending, chars, false);
			pending.compact();
			chars.flip();
			synchronized (screen) {
				if (session == pty) {
					screen.feed(chars);
				}
			}
			dirty = true;
		}
	}

	private void updateScrollBar() {
		int total;
		int rows;
		boolean alt;
		long absTotal;
		long droppedNow;
		synchronized (screen) {
			total = screen.totalLines();
			rows = screen.rows;
			alt = screen.altScreen;
			droppedNow = screen.dropped;
			absTotal = droppedNow + screen.scrollback.size();
		}
		// who is reading the history keeps looking at the same lines while new output arrives
		long growth = absTotal - lastAbsTotal;
		long trimmed = droppedNow - lastDropped;
		lastAbsTotal = absTotal;
		lastDropped = droppedNow;
		if (trimmed > 0 && selAnchorLine >= 0) {
			selAnchorLine -= (int) trimmed;
			selEndLine -= (int) trimmed;
			if (selAnchorLine < 0 || selEndLine < 0) {
				clearSelection();
			}
		}
		if (growth > 0 && scrollOffset > 0) {
			scrollOffset += (int) Math.min(growth, Integer.MAX_VALUE / 2);
		}
		if (alt) {
			// a full screen program owns the whole window: no history under it
			scrollOffset = 0;
		}
		scrollOffset = Math.max(0, Math.min(scrollOffset, total - rows));
		adjustingScrollBar = true;
		scrollBar.setValues(total - rows - scrollOffset, rows, 0, total);
		adjustingScrollBar = false;
	}

	/** Scrolls the history by lines (positive: towards older output) */
	private void scrollLines(int lines) {
		scrollOffset = (int) Math.max(0L, Math.min((long) scrollOffset + lines, Integer.MAX_VALUE / 2));
		updateScrollBar();
		canvas.repaint();
	}

	private void scrollByWheel(double rotation) {
		wheelAccumulator += rotation;
		int steps = (int) wheelAccumulator;
		wheelAccumulator -= steps;
		if (steps != 0) {
			scrollLines(-steps * 3);
		}
	}

	/** Mouse wheel: full screen programs get it as they asked (mouse reports) or as arrow keys, the rest scrolls the history */
	private void onWheel(MouseWheelEvent e) {
		double rotation = e.getPreciseWheelRotation();
		int mode;
		boolean sgr;
		boolean alt;
		int cols;
		int rows;
		synchronized (screen) {
			mode = screen.mouseMode;
			sgr = screen.mouseSgr;
			alt = screen.altScreen;
			cols = screen.cols;
			rows = screen.rows;
		}
		boolean forward = !e.isShiftDown() && session != null && session.isAlive();
		if (forward && (mode != 0 || alt)) {
			wheelAccumulator += rotation;
			int steps = (int) wheelAccumulator;
			wheelAccumulator -= steps;
			if (mode != 0) {
				int col = Math.max(1, Math.min(cols, e.getX() / canvas.charWidth + 1));
				int row = Math.max(1, Math.min(rows, e.getY() / canvas.charHeight + 1));
				int button = steps < 0 ? 64 : 65;
				for (int i = 0; i < Math.abs(steps); i++) {
					if (sgr) {
						sendString("\u001b[<" + button + ";" + col + ";" + row + "M");
					} else if (col <= 222 && row <= 222) {
						sendString("\u001b[M" + (char) (32 + button) + (char) (32 + col) + (char) (32 + row));
					}
				}
			} else {
				for (int i = 0; i < Math.abs(steps) * 3; i++) {
					sendString(canvas.cursorKey(null, steps < 0 ? 'A' : 'B'));
				}
			}
		} else {
			scrollByWheel(rotation);
		}
	}

	////////// SEARCH ////////////////

	public void showSearch() {
		String initial = hasSelection() ? selectedText() : null;
		searchBar.setVisible(true);
		if (initial != null && !initial.isEmpty() && initial.indexOf('\n') < 0) {
			searchBar.setQuery(initial);
		}
		revalidate();
		searchBar.focusField();
		runSearch(true);
	}

	public void hideSearch() {
		searchBar.setVisible(false);
		matches.clear();
		currentMatch = -1;
		searchStale = false;
		revalidate();
		canvas.repaint();
		canvas.requestFocusInWindow();
	}

	public void toggleSearch() {
		if (searchBar.isVisible()) {
			hideSearch();
		} else {
			showSearch();
		}
	}

	public boolean isSearchVisible() {
		return searchBar.isVisible();
	}

	/** Looks for the query in the whole history. jump: go to a match (the latest one), else the current one is kept */
	private void runSearch(boolean jump) {
		searchStale = false;
		lastSearchTime = System.currentTimeMillis();
		String query = searchBar.getQuery();
		long anchorLine = -1;
		long anchorColumn = 0;
		if (!jump && currentMatch >= 0 && currentMatch < matches.size()) {
			anchorLine = matches.get(currentMatch)[0];
			anchorColumn = matches.get(currentMatch)[1];
		}

		List<long[]> found = new ArrayList<long[]>();
		boolean capped = false;
		boolean invalid = false;
		if (!query.isEmpty()) {
			Pattern pattern = null;
			try {
				String expression = searchBar.isRegex() ? query : Pattern.quote(query);
				if (searchBar.isWholeWord()) {
					expression = "\\b(?:" + expression + ")\\b";
				}
				pattern = Pattern.compile(expression, searchBar.isMatchCase() ? 0 : Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
			} catch (PatternSyntaxException e) {
				invalid = true;
			}
			if (pattern != null) {
				synchronized (screen) {
					int from = screen.altScreen ? screen.scrollback.size() : 0;
					int total = screen.totalLines();
					scan: for (int i = from; i < total; i++) {
						VtScreen.Row row = screen.lineAt(i);
						if (row == null) {
							continue;
						}
						String text = row.text();
						if (text.isEmpty()) {
							continue;
						}
						Matcher matcher = pattern.matcher(text);
						while (matcher.find()) {
							if (matcher.end() > matcher.start()) {
								found.add(new long[] { screen.dropped + i, matcher.start(), matcher.end() - matcher.start() });
								if (found.size() >= MAX_MATCHES) {
									capped = true;
									break scan;
								}
							}
						}
					}
				}
			}
		}

		matches.clear();
		matches.addAll(found);
		if (matches.isEmpty()) {
			currentMatch = -1;
		} else {
			currentMatch = matches.size() - 1;
			if (anchorLine >= 0) {
				for (int i = 0; i < matches.size(); i++) {
					long[] m = matches.get(i);
					if (m[0] > anchorLine || (m[0] == anchorLine && m[1] >= anchorColumn)) {
						currentMatch = i;
						break;
					}
				}
			}
		}
		searchBar.setResult(currentMatch, matches.size(), capped, invalid);
		if (jump && currentMatch >= 0) {
			revealCurrentMatch();
		}
		canvas.repaint();
	}

	private void stepMatch(boolean older) {
		if (matches.isEmpty()) {
			return;
		}
		int count = matches.size();
		currentMatch = currentMatch < 0 ? count - 1 : (currentMatch + (older ? -1 : 1) + count) % count;
		searchBar.setResult(currentMatch, count, count >= MAX_MATCHES, false);
		revealCurrentMatch();
		canvas.repaint();
	}

	/** Scrolls the history so that the current match is in the middle of the screen (nothing if already visible) */
	private void revealCurrentMatch() {
		if (currentMatch < 0 || currentMatch >= matches.size()) {
			return;
		}
		long line = matches.get(currentMatch)[0];
		synchronized (screen) {
			int index = (int) (line - screen.dropped);
			int total = screen.totalLines();
			int rows = screen.rows;
			int first = total - rows - scrollOffset;
			if (index < first || index >= first + rows) {
				int newFirst = Math.max(0, Math.min(total - rows, index - rows / 2));
				scrollOffset = total - rows - newFirst;
			}
		}
		updateScrollBar();
	}

	private void clearSelection() {
		selAnchorLine = -1;
		selEndLine = -1;
	}

	private boolean hasSelection() {
		return selAnchorLine >= 0 && selEndLine >= 0 && (selAnchorLine != selEndLine || selAnchorCol != selEndCol);
	}

	private String selectedText() {
		int startLine = selAnchorLine;
		int startCol = selAnchorCol;
		int endLine = selEndLine;
		int endCol = selEndCol;
		if (startLine > endLine || (startLine == endLine && startCol > endCol)) {
			int tl = startLine;
			int tc = startCol;
			startLine = endLine;
			startCol = endCol;
			endLine = tl;
			endCol = tc;
		}
		StringBuilder text = new StringBuilder();
		synchronized (screen) {
			for (int line = startLine; line <= endLine; line++) {
				VtScreen.Row row = screen.lineAt(line);
				if (row == null) {
					continue;
				}
				String content = row.text();
				int from = line == startLine ? Math.min(startCol, content.length()) : 0;
				int to = line == endLine ? Math.min(endCol + 1, content.length()) : content.length();
				text.append(from < to ? content.substring(from, to) : "");
				if (line < endLine) {
					text.append('\n');
				}
			}
		}
		return text.toString();
	}

	private void copySelection() {
		if (hasSelection()) {
			Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(selectedText()), null);
		}
	}

	private void paste() {
		try {
			Object data = Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor);
			String text = String.valueOf(data).replace("\r\n", "\r").replace('\n', '\r');
			if (screen.bracketedPaste) {
				text = "\u001b[200~" + text + "\u001b[201~";
			}
			sendString(text);
		} catch (Exception e) {
			// clipboard without text: nothing to paste
		}
	}

	////////// CANVAS ////////////////

	private final class Canvas extends JComponent {
		private static final long serialVersionUID = 1L;

		private Font font = new Font("Consolas", Font.PLAIN, 14);
		private Font boldFont = font.deriveFont(Font.BOLD);
		private Font symbolFont = font;
		private Font symbolBold = boldFont;
		private int charWidth = 8;
		private int charHeight = 16;
		private int ascent = 12;
		private char expectedSkip;
		private long skipUntil;

		Canvas() {
			setOpaque(true);
			setFocusable(true);
			putClientProperty(ShortcutKeys.CAPTURE_ALL_KEYS, Boolean.TRUE);
			setFocusTraversalKeysEnabled(false);
			setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.TEXT_CURSOR));
			fontChanged();

			addComponentListener(new ComponentAdapter() {
				@Override
				public void componentResized(ComponentEvent e) {
					fitToSize();
				}
			});
			addFocusListener(new FocusAdapter() {
				@Override
				public void focusGained(FocusEvent e) {
					repaint();
				}

				@Override
				public void focusLost(FocusEvent e) {
					repaint();
				}
			});
			addKeyListener(new KeyAdapter() {
				@Override
				public void keyPressed(KeyEvent e) {
					onKeyPressed(e);
					// whatever it was, no menu accelerator or panel binding may see it
					e.consume();
				}

				@Override
				public void keyReleased(KeyEvent e) {
					e.consume();
				}

				@Override
				public void keyTyped(KeyEvent e) {
					onKeyTyped(e);
				}
			});
			MouseAdapter mouse = new MouseAdapter() {
				@Override
				public void mousePressed(MouseEvent e) {
					requestFocusInWindow();
					if (SwingUtilities.isRightMouseButton(e)) {
						if (hasSelection()) {
							copySelection();
							clearSelection();
						} else {
							paste();
						}
						repaint();
						return;
					}
					selAnchorLine = lineAt(e.getY());
					selAnchorCol = colAt(e.getX());
					selEndLine = selAnchorLine;
					selEndCol = selAnchorCol;
					repaint();
				}

				@Override
				public void mouseDragged(MouseEvent e) {
					if (SwingUtilities.isLeftMouseButton(e)) {
						selEndLine = lineAt(e.getY());
						selEndCol = colAt(e.getX());
						repaint();
					}
				}

				@Override
				public void mouseWheelMoved(MouseWheelEvent e) {
					onWheel(e);
				}
			};
			addMouseListener(mouse);
			addMouseMotionListener(mouse);
			addMouseWheelListener(mouse);
		}

		void fontChanged() {
			font = new Font(monospacedFamily(), Font.PLAIN, Math.round(fontSize));
			boldFont = font.deriveFont(Font.BOLD);
			symbolFont = new Font("Segoe UI Symbol", Font.PLAIN, Math.round(fontSize));
			symbolBold = symbolFont.deriveFont(Font.BOLD);
			// measured on an image: the component may not be displayable yet (it is built before being shown)
			java.awt.Graphics2D probe = new java.awt.image.BufferedImage(1, 1, java.awt.image.BufferedImage.TYPE_INT_RGB).createGraphics();
			FontMetrics metrics = probe.getFontMetrics(font);
			probe.dispose();
			charWidth = Math.max(1, metrics.charWidth('M'));
			charHeight = Math.max(1, metrics.getHeight());
			ascent = metrics.getAscent();
			fitToSize();
			repaint();
		}

		/** The terminal font, or a symbol font for the glyphs it does not have (arrows, dingbats, box symbols) */
		private Font fontFor(char ch, boolean bold) {
			Font base = bold ? boldFont : font;
			return ch < 0x80 || base.canDisplay(ch) ? base : (bold ? symbolBold : symbolFont);
		}

		private void fitToSize() {
			if (screen == null || getWidth() <= 0 || getHeight() <= 0) {
				return;
			}
			int cols = Math.max(2, getWidth() / charWidth);
			int rows = Math.max(1, getHeight() / charHeight);
			boolean changed;
			synchronized (screen) {
				changed = cols != screen.cols || rows != screen.rows;
				if (changed) {
					screen.resize(cols, rows);
				}
			}
			if (changed) {
				if (session != null) {
					session.resize(cols, rows);
				}
				dirty = true;
			}
		}

		private int firstVisibleLine() {
			synchronized (screen) {
				return Math.max(0, screen.totalLines() - screen.rows - scrollOffset);
			}
		}

		private int lineAt(int y) {
			return firstVisibleLine() + Math.max(0, Math.min(screen.rows - 1, y / charHeight));
		}

		private int colAt(int x) {
			return Math.max(0, Math.min(screen.cols - 1, x / charWidth));
		}

		@Override
		public Dimension getPreferredSize() {
			return new Dimension(charWidth * 80, charHeight * 24);
		}

		////////// KEYS ////////////////

		private void skipTyped(char c) {
			expectedSkip = c;
			skipUntil = System.currentTimeMillis() + 150;
		}

		private int modifierCode(KeyEvent e) {
			int code = 1 + (e.isShiftDown() ? 1 : 0) + (e.isAltDown() ? 2 : 0) + (e.isControlDown() ? 4 : 0);
			return code;
		}

		private String cursorKey(KeyEvent e, char letter) {
			int mod = e == null ? 1 : modifierCode(e);
			if (mod > 1) {
				return "\u001b[1;" + mod + letter;
			}
			return screen.appCursorKeys ? "\u001bO" + letter : "\u001b[" + letter;
		}

		private String tilde(KeyEvent e, int number) {
			int mod = modifierCode(e);
			return "\u001b[" + number + (mod > 1 ? ";" + mod : "") + "~";
		}

		/** Keys the terminal keeps for itself: search and history scrolling. They work also when the process is over */
		private boolean handleLocalShortcut(KeyEvent e) {
			int code = e.getKeyCode();
			// Ctrl+Shift+H (Cmd+Shift+H on macOS): free in the whole application (Ctrl+F, Ctrl+Shift+F, F3... belong to other functions)
			if (code == KeyEvent.VK_H && ShortcutKeys.isCtrlDown(e) && e.isShiftDown() && !e.isAltDown()) {
				skipTyped((char) 8);
				e.consume();
				showSearch();
				return true;
			}
			boolean alt;
			int rows;
			synchronized (screen) {
				alt = screen.altScreen;
				rows = screen.rows;
			}
			if (alt || e.isAltDown()) {
				return false;
			}
			boolean shiftOnly = e.isShiftDown() && !e.isControlDown() && !e.isMetaDown();
			if (shiftOnly && code == KeyEvent.VK_PAGE_UP) {
				scrollLines(Math.max(1, rows - 1));
			} else if (shiftOnly && code == KeyEvent.VK_PAGE_DOWN) {
				scrollLines(-Math.max(1, rows - 1));
			} else if (shiftOnly && code == KeyEvent.VK_HOME) {
				scrollLines(Integer.MAX_VALUE / 4);
			} else if (shiftOnly && code == KeyEvent.VK_END) {
				scrollLines(-Integer.MAX_VALUE / 4);
			} else {
				return false;
			}
			e.consume();
			return true;
		}

		private void onKeyPressed(KeyEvent e) {
			int code = e.getKeyCode();
			if (code == KeyEvent.VK_SHIFT || code == KeyEvent.VK_CONTROL || code == KeyEvent.VK_ALT || code == KeyEvent.VK_META) {
				return;
			}
			if (handleLocalShortcut(e)) {
				return;
			}
			if (session == null || !session.isAlive()) {
				if (code == KeyEvent.VK_ENTER) {
					e.consume();
					restartRequested();
				}
				return;
			}

			boolean ctrl = e.isControlDown() && !e.isAltDown();
			if (ctrl && code == KeyEvent.VK_C && (hasSelection() || e.isShiftDown())) {
				copySelection();
				clearSelection();
				skipTyped((char) 3);
				e.consume();
				repaint();
				return;
			}
			if ((ctrl && code == KeyEvent.VK_V) || (e.isShiftDown() && code == KeyEvent.VK_INSERT)) {
				paste();
				skipTyped((char) 22);
				e.consume();
				return;
			}
			if (ctrl && code == KeyEvent.VK_INSERT) {
				copySelection();
				e.consume();
				return;
			}
			if (scrollOffset != 0 && !e.isShiftDown()) {
				scrollOffset = 0;
				updateScrollBar();
			}

			String sequence = null;
			char skip = 0;
			switch (code) {
			case KeyEvent.VK_UP:
				sequence = cursorKey(e, 'A');
				break;
			case KeyEvent.VK_DOWN:
				sequence = cursorKey(e, 'B');
				break;
			case KeyEvent.VK_RIGHT:
				sequence = cursorKey(e, 'C');
				break;
			case KeyEvent.VK_LEFT:
				sequence = cursorKey(e, 'D');
				break;
			case KeyEvent.VK_HOME:
				sequence = cursorKey(e, 'H');
				break;
			case KeyEvent.VK_END:
				sequence = cursorKey(e, 'F');
				break;
			case KeyEvent.VK_INSERT:
				sequence = tilde(e, 2);
				break;
			case KeyEvent.VK_DELETE:
				sequence = tilde(e, 3);
				skip = 0x7f;
				break;
			case KeyEvent.VK_PAGE_UP:
				sequence = tilde(e, 5);
				break;
			case KeyEvent.VK_PAGE_DOWN:
				sequence = tilde(e, 6);
				break;
			case KeyEvent.VK_F1:
				sequence = "\u001bOP";
				break;
			case KeyEvent.VK_F2:
				sequence = "\u001bOQ";
				break;
			case KeyEvent.VK_F3:
				sequence = "\u001bOR";
				break;
			case KeyEvent.VK_F4:
				sequence = "\u001bOS";
				break;
			case KeyEvent.VK_F5:
				sequence = tilde(e, 15);
				break;
			case KeyEvent.VK_F6:
				sequence = tilde(e, 17);
				break;
			case KeyEvent.VK_F7:
				sequence = tilde(e, 18);
				break;
			case KeyEvent.VK_F8:
				sequence = tilde(e, 19);
				break;
			case KeyEvent.VK_F9:
				sequence = tilde(e, 20);
				break;
			case KeyEvent.VK_F10:
				sequence = tilde(e, 21);
				break;
			case KeyEvent.VK_F11:
				sequence = tilde(e, 23);
				break;
			case KeyEvent.VK_F12:
				sequence = tilde(e, 24);
				break;
			case KeyEvent.VK_ESCAPE:
				sequence = "\u001b";
				skip = 0x1b;
				break;
			case KeyEvent.VK_ENTER:
				sequence = "\r";
				skip = '\n';
				break;
			case KeyEvent.VK_BACK_SPACE:
				sequence = e.isControlDown() ? "\b" : "\u007f";
				skip = '\b';
				break;
			case KeyEvent.VK_TAB:
				sequence = e.isShiftDown() ? "\u001b[Z" : "\t";
				skip = '\t';
				break;
			default:
				return;
			}
			sendString(sequence);
			if (skip != 0) {
				skipTyped(skip);
			}
			e.consume();
		}

		private void onKeyTyped(KeyEvent e) {
			char c = e.getKeyChar();
			if (c == KeyEvent.CHAR_UNDEFINED || session == null) {
				return;
			}
			if (expectedSkip != 0 && c == expectedSkip && System.currentTimeMillis() < skipUntil) {
				expectedSkip = 0;
				e.consume();
				return;
			}
			if (c == '\n') {
				c = '\r';
			}
			if (c == 0x7f || c == 0x08) {
				return;
			}
			String text = String.valueOf(c);
			if (e.isAltDown() && !e.isControlDown()) {
				text = "\u001b" + text;
			}
			if (hasSelection()) {
				clearSelection();
				repaint();
			}
			if (scrollOffset != 0) {
				scrollOffset = 0;
				updateScrollBar();
			}
			sendString(text);
			e.consume();
		}

		////////// PAINT ////////////////

		/** Block elements (half blocks, quadrants, shades) drawn as rectangles, so they join exactly as in a native terminal */
		private void drawBlock(Graphics2D g, char ch, int px, int py, Color fg) {
			int w = charWidth;
			int h = charHeight;
			g.setColor(fg);
			if (ch == 0x2580) {
				g.fillRect(px, py, w, h / 2);
			} else if (ch >= 0x2581 && ch <= 0x2587) {
				int part = h * (ch - 0x2580) / 8;
				g.fillRect(px, py + h - part, w, part);
			} else if (ch == 0x2588) {
				g.fillRect(px, py, w, h);
			} else if (ch >= 0x2589 && ch <= 0x258F) {
				g.fillRect(px, py, w * (0x2590 - ch) / 8, h);
			} else if (ch == 0x2590) {
				g.fillRect(px + w / 2, py, w - w / 2, h);
			} else if (ch >= 0x2591 && ch <= 0x2593) {
				g.setColor(new Color(fg.getRed(), fg.getGreen(), fg.getBlue(), 64 * (ch - 0x2590)));
				g.fillRect(px, py, w, h);
			} else if (ch == 0x2594) {
				g.fillRect(px, py, w, Math.max(1, h / 8));
			} else if (ch == 0x2595) {
				g.fillRect(px + w - Math.max(1, w / 8), py, Math.max(1, w / 8), h);
			} else {
				// quadrants: bit 1 upper left, 2 upper right, 4 lower left, 8 lower right
				int[] masks = { 4, 8, 1, 1 | 4 | 8, 1 | 8, 1 | 2 | 4, 1 | 2 | 8, 2, 2 | 4, 2 | 4 | 8 };
				int mask = masks[ch - 0x2596];
				int halfW = w / 2;
				int halfH = h / 2;
				if ((mask & 1) != 0) {
					g.fillRect(px, py, halfW, halfH);
				}
				if ((mask & 2) != 0) {
					g.fillRect(px + halfW, py, w - halfW, halfH);
				}
				if ((mask & 4) != 0) {
					g.fillRect(px, py + halfH, halfW, h - halfH);
				}
				if ((mask & 8) != 0) {
					g.fillRect(px + halfW, py + halfH, w - halfW, h - halfH);
				}
			}
		}

		private Color blend(Color a, Color b, double amount) {
			return new Color((int) (a.getRed() + (b.getRed() - a.getRed()) * amount), (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * amount),
					(int) (a.getBlue() + (b.getBlue() - a.getBlue()) * amount));
		}

		private Color resolve(int value, Color fallback) {
			return value == VtScreen.DEFAULT ? fallback : new Color(value);
		}

		@Override
		protected void paintComponent(Graphics graphics) {
			Graphics2D g = (Graphics2D) graphics.create();
			g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			g.setColor(DEFAULT_BG);
			g.fillRect(0, 0, getWidth(), getHeight());

			synchronized (screen) {
				int first = Math.max(0, screen.totalLines() - screen.rows - scrollOffset);
				int cols = screen.cols;
				int matchCount = searchBar.isVisible() ? matches.size() : 0;
				int matchPos = matchCount > 0 ? firstMatchFrom(screen.dropped + first) : 0;
				int selStartLine = Math.min(selAnchorLine, selEndLine);
				int selFinalLine = Math.max(selAnchorLine, selEndLine);
				boolean forward = selAnchorLine < selEndLine || (selAnchorLine == selEndLine && selAnchorCol <= selEndCol);
				int selStartCol = forward ? selAnchorCol : selEndCol;
				int selFinalCol = forward ? selEndCol : selAnchorCol;
				boolean selecting = hasSelection();

				for (int y = 0; y < screen.rows; y++) {
					int line = first + y;
					VtScreen.Row row = screen.lineAt(line);
					if (row == null) {
						continue;
					}
					int py = y * charHeight;
					byte[] highlight = null;
					if (matchCount > 0) {
						long absolute = screen.dropped + line;
						while (matchPos < matchCount && matches.get(matchPos)[0] < absolute) {
							matchPos++;
						}
						for (int k = matchPos; k < matchCount && matches.get(k)[0] == absolute; k++) {
							long[] m = matches.get(k);
							if (highlight == null) {
								highlight = new byte[row.ch.length];
							}
							for (int c = (int) m[1]; c < Math.min(highlight.length, m[1] + m[2]); c++) {
								highlight[c] = (byte) (k == currentMatch ? 2 : 1);
							}
						}
					}
					for (int x = 0; x < Math.min(cols, row.ch.length); x++) {
						int attr = row.at[x];
						boolean reverse = (attr & VtScreen.REVERSE) != 0;
						Color fg = resolve(row.fg[x], DEFAULT_FG);
						Color bg = row.bg[x] == VtScreen.DEFAULT ? null : new Color(row.bg[x]);
						if (reverse) {
							Color swap = fg;
							fg = bg != null ? bg : DEFAULT_BG;
							bg = swap;
						}
						if ((attr & VtScreen.DIM) != 0) {
							fg = blend(fg, bg != null ? bg : DEFAULT_BG, 0.45);
						}
						boolean selected = selecting && line >= selStartLine && line <= selFinalLine
								&& (line != selStartLine || x >= selStartCol) && (line != selFinalLine || x <= selFinalCol);
						if (selected) {
							bg = SELECTION;
							fg = Color.WHITE;
						} else if (highlight != null && highlight[x] != 0) {
							bg = highlight[x] == 2 ? CURRENT_MATCH_BG : MATCH_BG;
							fg = highlight[x] == 2 ? Color.BLACK : Color.WHITE;
						}
						int px = x * charWidth;
						if (bg != null) {
							g.setColor(bg);
							g.fillRect(px, py, charWidth, charHeight);
						}
						char ch = row.ch[x];
						if (ch >= 0x2580 && ch <= 0x259F) {
							drawBlock(g, ch, px, py, fg);
						} else if (ch != ' ') {
							g.setFont(fontFor(ch, (attr & VtScreen.BOLD) != 0));
							g.setColor(fg);
							g.drawString(String.valueOf(ch), px, py + ascent);
						}
						if ((attr & VtScreen.UNDERLINE) != 0) {
							g.setColor(fg);
							g.drawLine(px, py + charHeight - 2, px + charWidth - 1, py + charHeight - 2);
						}
					}
				}

				if (screen.cursorVisible && scrollOffset == 0 && session != null) {
					int cx = screen.curX * charWidth;
					int cy = screen.curY * charHeight;
					g.setColor(new Color(0xF2, 0xF2, 0xF2, 200));
					if (isFocusOwner()) {
						g.fillRect(cx, cy, charWidth, charHeight);
						VtScreen.Row row = screen.grid[screen.curY];
						if (screen.curX < row.ch.length && row.ch[screen.curX] != ' ') {
							g.setFont(font);
							g.setColor(DEFAULT_BG);
							g.drawString(String.valueOf(row.ch[screen.curX]), cx, cy + ascent);
						}
					} else {
						g.drawRect(cx, cy, charWidth - 1, charHeight - 1);
					}
				}
			}
			g.dispose();
		}
	}

	/** Index of the first match not before the given line (matches are ordered) */
	private int firstMatchFrom(long line) {
		int low = 0;
		int high = matches.size();
		while (low < high) {
			int mid = (low + high) >>> 1;
			if (matches.get(mid)[0] < line) {
				low = mid + 1;
			} else {
				high = mid;
			}
		}
		return low;
	}

	private Runnable restartAction;

	/** Called when the user presses Enter in a terminal whose process ended */
	public void setRestartAction(Runnable restartAction) {
		this.restartAction = restartAction;
	}

	private void restartRequested() {
		if (restartAction != null) {
			restartAction.run();
		}
	}
}
