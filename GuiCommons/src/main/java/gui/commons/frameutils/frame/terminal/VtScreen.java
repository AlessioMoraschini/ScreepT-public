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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

/**
 * Screen of a terminal: a grid of cells with colors and attributes, a cursor, a scroll region and a scrollback, driven
 * by the VT100 / xterm sequences that programs write (cursor movements, erase, scroll, SGR colors, alternate screen).
 * Not thread safe by itself: callers synchronize on the instance.
 */
final class VtScreen {

	static final int DEFAULT = -1;
	static final int BOLD = 1;
	static final int UNDERLINE = 2;
	static final int REVERSE = 4;
	static final int ITALIC = 8;
	static final int DIM = 16;
	private static final int MAX_SCROLLBACK = 10000;
	/** The scrollback is trimmed in blocks, so a long output does not shift the whole list for every line */
	private static final int TRIM_BLOCK = 500;

	private static final int[] ANSI = { 0x0C0C0C, 0xC50F1F, 0x13A10E, 0xC19C00, 0x0037DA, 0x881798, 0x3A96DD, 0xCCCCCC,
			0x767676, 0xE74856, 0x16C60C, 0xF9F1A5, 0x3B78FF, 0xB4009E, 0x61D6D6, 0xF2F2F2 };

	/** One line of cells */
	static final class Row {
		final char[] ch;
		final int[] fg;
		final int[] bg;
		final byte[] at;

		Row(int cols) {
			ch = new char[cols];
			fg = new int[cols];
			bg = new int[cols];
			at = new byte[cols];
			Arrays.fill(ch, ' ');
			Arrays.fill(fg, DEFAULT);
			Arrays.fill(bg, DEFAULT);
		}

		void clear(int from, int to, int background) {
			for (int i = Math.max(0, from); i < Math.min(ch.length, to); i++) {
				ch[i] = ' ';
				fg[i] = DEFAULT;
				bg[i] = background;
				at[i] = 0;
			}
		}

		String text() {
			int end = ch.length;
			while (end > 0 && ch[end - 1] == ' ') {
				end--;
			}
			return new String(ch, 0, end);
		}
	}

	private enum State { GROUND, ESC, CSI, OSC, OSC_ESC, SKIP_ONE }

	int cols;
	int rows;
	Row[] grid;
	private Row[] savedMainGrid;
	final List<Row> scrollback = new ArrayList<Row>();
	int curX;
	int curY;
	boolean cursorVisible = true;
	boolean appCursorKeys;
	boolean bracketedPaste;
	boolean altScreen;
	/** Mouse reporting asked by the program: 0 none, else 1000 (clicks), 1002 (drag), 1003 (any motion) */
	int mouseMode;
	/** SGR encoding of the mouse reports (mode 1006) */
	boolean mouseSgr;
	/** Lines removed from the beginning of the scrollback so far: gives stable numbers to the lines (dropped + index) */
	long dropped;

	private boolean wrapPending;
	private int top;
	private int bottom;
	private int curFg = DEFAULT;
	private int curBg = DEFAULT;
	private int curAt;
	private int savedX;
	private int savedY;
	private State state = State.GROUND;
	private final StringBuilder params = new StringBuilder();
	private final Consumer<String> responder;

	VtScreen(int cols, int rows, Consumer<String> responder) {
		this.responder = responder;
		this.cols = Math.max(2, cols);
		this.rows = Math.max(1, rows);
		this.grid = newGrid(this.cols, this.rows);
		this.bottom = this.rows - 1;
	}

	private static Row[] newGrid(int cols, int rows) {
		Row[] result = new Row[rows];
		for (int i = 0; i < rows; i++) {
			result[i] = new Row(cols);
		}
		return result;
	}

	////////// INPUT ////////////////

	void feed(CharSequence text) {
		for (int i = 0; i < text.length(); i++) {
			feed(text.charAt(i));
		}
	}

	private void feed(char c) {
		switch (state) {
		case GROUND:
			ground(c);
			break;
		case ESC:
			escape(c);
			break;
		case CSI:
			if (c >= 0x40 && c <= 0x7e) {
				state = State.GROUND;
				csi(c, params.toString());
			} else if (c == 0x1b) {
				state = State.ESC;
			} else if (c >= 0x20) {
				params.append(c);
			}
			break;
		case OSC:
			if (c == 0x07) {
				state = State.GROUND;
			} else if (c == 0x1b) {
				state = State.OSC_ESC;
			}
			break;
		case OSC_ESC:
			state = c == '\\' ? State.GROUND : State.ESC;
			if (state == State.ESC) {
				escape(c);
			}
			break;
		case SKIP_ONE:
			state = State.GROUND;
			break;
		default:
			state = State.GROUND;
		}
	}

	private void ground(char c) {
		switch (c) {
		case 0x1b:
			state = State.ESC;
			break;
		case '\n':
		case 0x0b:
		case 0x0c:
			lineFeed();
			break;
		case '\r':
			curX = 0;
			wrapPending = false;
			break;
		case '\b':
			if (curX > 0) {
				curX--;
			}
			wrapPending = false;
			break;
		case '\t':
			curX = Math.min(cols - 1, (curX / 8 + 1) * 8);
			wrapPending = false;
			break;
		default:
			if (c >= 0x20 && c != 0x7f) {
				print(c);
			}
		}
	}

	private void escape(char c) {
		state = State.GROUND;
		switch (c) {
		case '[':
			params.setLength(0);
			state = State.CSI;
			break;
		case ']':
		case 'P':
		case 'X':
		case '^':
		case '_':
			state = State.OSC;
			break;
		case '(':
		case ')':
		case '*':
		case '+':
		case '#':
		case ' ':
			state = State.SKIP_ONE;
			break;
		case '7':
			savedX = curX;
			savedY = curY;
			break;
		case '8':
			curX = Math.min(savedX, cols - 1);
			curY = Math.min(savedY, rows - 1);
			wrapPending = false;
			break;
		case 'M':
			if (curY == top) {
				scrollDown(1);
			} else if (curY > 0) {
				curY--;
			}
			break;
		case 'D':
			lineFeed();
			break;
		case 'E':
			curX = 0;
			lineFeed();
			break;
		case 'c':
			reset();
			break;
		default:
			break;
		}
	}

	private void reset() {
		curFg = DEFAULT;
		curBg = DEFAULT;
		curAt = 0;
		top = 0;
		bottom = rows - 1;
		curX = 0;
		curY = 0;
		wrapPending = false;
		cursorVisible = true;
		appCursorKeys = false;
		bracketedPaste = false;
		mouseMode = 0;
		mouseSgr = false;
		eraseDisplay(2);
	}

	private void print(char c) {
		if (wrapPending) {
			curX = 0;
			lineFeed();
			wrapPending = false;
		}
		Row row = grid[curY];
		row.ch[curX] = c;
		row.fg[curX] = curFg;
		row.bg[curX] = curBg;
		row.at[curX] = (byte) curAt;
		if (curX == cols - 1) {
			wrapPending = true;
		} else {
			curX++;
		}
	}

	private void lineFeed() {
		if (curY == bottom) {
			scrollUp(1);
		} else if (curY < rows - 1) {
			curY++;
		}
	}

	private void scrollUp(int n) {
		for (int k = 0; k < n; k++) {
			Row removed = grid[top];
			if (!altScreen && top == 0 && bottom == rows - 1) {
				scrollback.add(removed);
				if (scrollback.size() > MAX_SCROLLBACK + TRIM_BLOCK) {
					scrollback.subList(0, TRIM_BLOCK).clear();
					dropped += TRIM_BLOCK;
				}
			}
			System.arraycopy(grid, top + 1, grid, top, bottom - top);
			grid[bottom] = blankRow();
		}
	}

	private void scrollDown(int n) {
		for (int k = 0; k < n; k++) {
			System.arraycopy(grid, top, grid, top + 1, bottom - top);
			grid[top] = blankRow();
		}
	}

	private Row blankRow() {
		Row row = new Row(cols);
		if (curBg != DEFAULT) {
			row.clear(0, cols, curBg);
		}
		return row;
	}

	////////// CSI ////////////////

	private static int[] numbers(String raw) {
		String text = raw.replaceAll("[^0-9;:]", "");
		if (text.isEmpty()) {
			return new int[0];
		}
		String[] parts = text.split("[;:]", -1);
		int[] values = new int[parts.length];
		for (int i = 0; i < parts.length; i++) {
			try {
				values[i] = parts[i].isEmpty() ? 0 : Integer.parseInt(parts[i]);
			} catch (NumberFormatException e) {
				values[i] = 0;
			}
		}
		return values;
	}

	private static int param(int[] values, int index, int fallback) {
		return index < values.length && values[index] > 0 ? values[index] : fallback;
	}

	private void csi(char finalChar, String raw) {
		boolean priv = raw.startsWith("?");
		// sequences with a private prefix (queries and modes of newer terminals, e.g. ESC[?u, ESC[>4;2m) are not the plain ones
		if (!raw.isEmpty() && "<=>?".indexOf(raw.charAt(0)) >= 0 && !(priv && (finalChar == 'h' || finalChar == 'l'))) {
			return;
		}
		int[] p = numbers(raw);
		wrapPending = finalChar == 'm' ? wrapPending : false;

		switch (finalChar) {
		case 'A':
			curY = Math.max(curY >= top ? top : 0, curY - param(p, 0, 1));
			break;
		case 'B':
			curY = Math.min(curY <= bottom ? bottom : rows - 1, curY + param(p, 0, 1));
			break;
		case 'C':
			curX = Math.min(cols - 1, curX + param(p, 0, 1));
			break;
		case 'D':
			curX = Math.max(0, curX - param(p, 0, 1));
			break;
		case 'E':
			curX = 0;
			curY = Math.min(rows - 1, curY + param(p, 0, 1));
			break;
		case 'F':
			curX = 0;
			curY = Math.max(0, curY - param(p, 0, 1));
			break;
		case 'G':
			curX = clamp(param(p, 0, 1) - 1, cols);
			break;
		case 'H':
		case 'f':
			curY = clamp(param(p, 0, 1) - 1, rows);
			curX = clamp(param(p, 1, 1) - 1, cols);
			break;
		case 'd':
			curY = clamp(param(p, 0, 1) - 1, rows);
			break;
		case 'J':
			eraseDisplay(p.length > 0 ? p[0] : 0);
			break;
		case 'K':
			eraseLine(p.length > 0 ? p[0] : 0);
			break;
		case 'L':
			if (curY >= top && curY <= bottom) {
				int save = top;
				top = curY;
				scrollDown(Math.min(param(p, 0, 1), bottom - curY + 1));
				top = save;
			}
			break;
		case 'M':
			if (curY >= top && curY <= bottom) {
				int save = top;
				top = curY;
				scrollUp(Math.min(param(p, 0, 1), bottom - curY + 1));
				top = save;
			}
			break;
		case 'P':
			deleteChars(param(p, 0, 1));
			break;
		case '@':
			insertChars(param(p, 0, 1));
			break;
		case 'X':
			grid[curY].clear(curX, curX + param(p, 0, 1), curBg);
			break;
		case 'S':
			scrollUp(param(p, 0, 1));
			break;
		case 'T':
			scrollDown(param(p, 0, 1));
			break;
		case 'm':
			sgr(p);
			break;
		case 'r':
			top = clamp(param(p, 0, 1) - 1, rows);
			bottom = clamp(param(p, 1, rows) - 1, rows);
			if (top >= bottom) {
				top = 0;
				bottom = rows - 1;
			}
			curX = 0;
			curY = 0;
			break;
		case 's':
			savedX = curX;
			savedY = curY;
			break;
		case 'u':
			curX = Math.min(savedX, cols - 1);
			curY = Math.min(savedY, rows - 1);
			break;
		case 'h':
		case 'l':
			if (priv) {
				for (int mode : p) {
					setMode(mode, finalChar == 'h');
				}
			}
			break;
		case 'n':
			if (p.length > 0 && p[0] == 6) {
				respond("\u001b[" + (curY + 1) + ";" + (curX + 1) + "R");
			}
			break;
		case 'c':
			if (!priv && !raw.startsWith(">")) {
				respond("\u001b[?1;0c");
			}
			break;
		default:
			break;
		}
	}

	private void respond(String text) {
		if (responder != null) {
			responder.accept(text);
		}
	}

	private static int clamp(int value, int size) {
		return Math.max(0, Math.min(size - 1, value));
	}

	private void setMode(int mode, boolean on) {
		switch (mode) {
		case 1:
			appCursorKeys = on;
			break;
		case 25:
			cursorVisible = on;
			break;
		case 2004:
			bracketedPaste = on;
			break;
		case 1000:
		case 1002:
		case 1003:
			if (on) {
				mouseMode = mode;
			} else if (mouseMode == mode) {
				mouseMode = 0;
			}
			break;
		case 1006:
			mouseSgr = on;
			break;
		case 47:
		case 1047:
		case 1049:
			switchAltScreen(on, mode == 1049);
			break;
		default:
			break;
		}
	}

	private void switchAltScreen(boolean on, boolean saveCursor) {
		if (on == altScreen) {
			return;
		}
		if (on) {
			if (saveCursor) {
				savedX = curX;
				savedY = curY;
			}
			savedMainGrid = grid;
			grid = newGrid(cols, rows);
			altScreen = true;
		} else {
			grid = savedMainGrid != null && savedMainGrid.length == rows ? savedMainGrid : newGrid(cols, rows);
			savedMainGrid = null;
			altScreen = false;
			if (saveCursor) {
				curX = Math.min(savedX, cols - 1);
				curY = Math.min(savedY, rows - 1);
			}
		}
		top = 0;
		bottom = rows - 1;
	}

	private void eraseDisplay(int mode) {
		switch (mode) {
		case 0:
			grid[curY].clear(curX, cols, curBg);
			for (int y = curY + 1; y < rows; y++) {
				grid[y].clear(0, cols, curBg);
			}
			break;
		case 1:
			for (int y = 0; y < curY; y++) {
				grid[y].clear(0, cols, curBg);
			}
			grid[curY].clear(0, curX + 1, curBg);
			break;
		case 2:
			for (int y = 0; y < rows; y++) {
				grid[y].clear(0, cols, curBg);
			}
			break;
		case 3:
			dropped += scrollback.size();
			scrollback.clear();
			break;
		default:
			break;
		}
	}

	private void eraseLine(int mode) {
		switch (mode) {
		case 0:
			grid[curY].clear(curX, cols, curBg);
			break;
		case 1:
			grid[curY].clear(0, curX + 1, curBg);
			break;
		default:
			grid[curY].clear(0, cols, curBg);
		}
	}

	private void deleteChars(int n) {
		Row row = grid[curY];
		n = Math.min(n, cols - curX);
		shift(row, curX + n, curX, cols - curX - n);
		row.clear(cols - n, cols, curBg);
	}

	private void insertChars(int n) {
		Row row = grid[curY];
		n = Math.min(n, cols - curX);
		shift(row, curX, curX + n, cols - curX - n);
		row.clear(curX, curX + n, curBg);
	}

	private static void shift(Row row, int from, int to, int length) {
		if (length <= 0) {
			return;
		}
		System.arraycopy(row.ch, from, row.ch, to, length);
		System.arraycopy(row.fg, from, row.fg, to, length);
		System.arraycopy(row.bg, from, row.bg, to, length);
		System.arraycopy(row.at, from, row.at, to, length);
	}

	private void sgr(int[] p) {
		if (p.length == 0) {
			p = new int[] { 0 };
		}
		for (int i = 0; i < p.length; i++) {
			int v = p[i];
			if (v == 0) {
				curFg = DEFAULT;
				curBg = DEFAULT;
				curAt = 0;
			} else if (v == 1) {
				curAt |= BOLD;
			} else if (v == 2) {
				curAt |= DIM;
			} else if (v == 3) {
				curAt |= ITALIC;
			} else if (v == 4) {
				curAt |= UNDERLINE;
			} else if (v == 7) {
				curAt |= REVERSE;
			} else if (v == 22) {
				curAt &= ~(BOLD | DIM);
			} else if (v == 23) {
				curAt &= ~ITALIC;
			} else if (v == 24) {
				curAt &= ~UNDERLINE;
			} else if (v == 27) {
				curAt &= ~REVERSE;
			} else if (v >= 30 && v <= 37) {
				curFg = ANSI[v - 30];
			} else if (v >= 90 && v <= 97) {
				curFg = ANSI[v - 90 + 8];
			} else if (v >= 40 && v <= 47) {
				curBg = ANSI[v - 40];
			} else if (v >= 100 && v <= 107) {
				curBg = ANSI[v - 100 + 8];
			} else if (v == 39) {
				curFg = DEFAULT;
			} else if (v == 49) {
				curBg = DEFAULT;
			} else if (v == 38 || v == 48) {
				int color = DEFAULT;
				if (i + 2 < p.length && p[i + 1] == 5) {
					color = palette256(p[i + 2]);
					i += 2;
				} else if (i + 4 < p.length && p[i + 1] == 2) {
					color = (clamp255(p[i + 2]) << 16) | (clamp255(p[i + 3]) << 8) | clamp255(p[i + 4]);
					i += 4;
				}
				if (v == 38) {
					curFg = color;
				} else {
					curBg = color;
				}
			}
		}
	}

	private static int clamp255(int v) {
		return Math.max(0, Math.min(255, v));
	}

	static int palette256(int n) {
		if (n < 0) {
			return DEFAULT;
		}
		if (n < 16) {
			return ANSI[n];
		}
		if (n < 232) {
			int[] levels = { 0, 95, 135, 175, 215, 255 };
			int c = n - 16;
			return (levels[c / 36] << 16) | (levels[(c / 6) % 6] << 8) | levels[c % 6];
		}
		int gray = Math.min(255, 8 + (n - 232) * 10);
		return (gray << 16) | (gray << 8) | gray;
	}

	////////// RESIZE ////////////////

	void resize(int newCols, int newRows) {
		newCols = Math.max(2, newCols);
		newRows = Math.max(1, newRows);
		if (newCols == cols && newRows == rows) {
			return;
		}
		Row[] resized = newGrid(newCols, newRows);
		// when it gets shorter the lines above the cursor go to the scrollback, as a normal terminal does
		int drop = (!altScreen && curY >= newRows) ? curY - newRows + 1 : 0;
		for (int y = 0; y < drop; y++) {
			scrollback.add(grid[y]);
		}
		// when it gets taller and the text filled the screen, the last lines of the scrollback come back at the top
		int pull = 0;
		if (!altScreen && newRows > rows && !scrollback.isEmpty() && lastUsedRow() + 1 >= rows) {
			pull = Math.min(scrollback.size(), newRows - rows);
		}
		List<Row> sources = new ArrayList<Row>();
		for (int i = scrollback.size() - pull; i < scrollback.size(); i++) {
			sources.add(scrollback.get(i));
		}
		for (int i = 0; i < pull; i++) {
			scrollback.remove(scrollback.size() - 1);
		}
		for (int y = drop; y < rows; y++) {
			sources.add(grid[y]);
		}
		for (int y = 0; y < Math.min(newRows, sources.size()); y++) {
			Row source = sources.get(y);
			Row target = resized[y];
			int width = Math.min(newCols, source.ch.length);
			System.arraycopy(source.ch, 0, target.ch, 0, width);
			System.arraycopy(source.fg, 0, target.fg, 0, width);
			System.arraycopy(source.bg, 0, target.bg, 0, width);
			System.arraycopy(source.at, 0, target.at, 0, width);
		}
		grid = resized;
		savedMainGrid = null;
		cols = newCols;
		rows = newRows;
		curY = Math.max(0, Math.min(newRows - 1, curY - drop + pull));
		curX = Math.min(curX, newCols - 1);
		top = 0;
		bottom = newRows - 1;
		wrapPending = false;
	}

	/** Last row of the screen with some text, or the cursor row */
	private int lastUsedRow() {
		int last = curY;
		for (int y = rows - 1; y > last; y--) {
			if (!grid[y].text().isEmpty()) {
				return y;
			}
		}
		return last;
	}

	/** Line of the whole buffer: scrollback first, then the screen */
	Row lineAt(int absolute) {
		if (absolute < 0) {
			return null;
		}
		if (absolute < scrollback.size()) {
			return scrollback.get(absolute);
		}
		int y = absolute - scrollback.size();
		return y < rows ? grid[y] : null;
	}

	int totalLines() {
		return scrollback.size() + rows;
	}

	/** Plain text of the screen (tests) */
	String screenText() {
		StringBuilder text = new StringBuilder();
		for (Row row : grid) {
			text.append(row.text()).append('\n');
		}
		return text.toString();
	}
}
