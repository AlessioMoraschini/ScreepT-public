package gui.commons.frameutils.frame.terminal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.junit.Test;

/**
 * The terminal emulator behind the interactive shell: bytes in, a screen of cells out. Expected values are what a
 * VT100/xterm terminal does with the same bytes.
 */
public class VtScreenTest {

	private static final String ESC = "\u001b";

	private final List<String> answers = new ArrayList<String>();

	private VtScreen screen(int cols, int rows) {
		return new VtScreen(cols, rows, new Consumer<String>() {
			@Override
			public void accept(String s) {
				answers.add(s);
			}
		});
	}

	private static String line(VtScreen s, int y) {
		String text = s.grid[y].text();
		int end = text.length();
		while (end > 0 && text.charAt(end - 1) == ' ') {
			end--;
		}
		return text.substring(0, end);
	}

	@Test
	public void plainTextAndNewLinesLandOnTheRightRows() {
		VtScreen s = screen(20, 5);
		s.feed("hello\r\nworld");
		assertEquals("hello", line(s, 0));
		assertEquals("world", line(s, 1));
		assertEquals(5, s.curX);
		assertEquals(1, s.curY);
	}

	@Test
	public void longLinesWrapAtTheLastColumn() {
		VtScreen s = screen(5, 4);
		s.feed("1234567890AB");
		assertEquals("12345", line(s, 0));
		assertEquals("67890", line(s, 1));
		assertEquals("AB", line(s, 2));
	}

	@Test
	public void cursorMovementAndAbsolutePositioning() {
		VtScreen s = screen(20, 6);
		s.feed(ESC + "[3;4HX");
		assertEquals('X', s.grid[2].ch[3]);
		s.feed(ESC + "[HO");
		assertEquals('O', s.grid[0].ch[0]);
		s.feed(ESC + "[4;10H" + ESC + "[2A" + ESC + "[3DY");
		assertEquals("2 rows up, 3 columns left of (row 4, col 10)", 'Y', s.grid[1].ch[6]);
		s.feed(ESC + "[2;1H" + ESC + "[5CZ");
		assertEquals('Z', s.grid[1].ch[5]);
		s.feed(ESC + "[99;99H");
		assertEquals("the cursor never leaves the screen", 19, s.curX);
		assertEquals(5, s.curY);
	}

	@Test
	public void backspaceTabAndCarriageReturn() {
		VtScreen s = screen(30, 3);
		s.feed("ab\bX");
		assertEquals("aX", line(s, 0));
		s.feed("\r\n" + "a\tb");
		assertEquals("tab stops every 8 columns", 'b', s.grid[1].ch[8]);
		s.feed("\rZ");
		assertEquals("Z", line(s, 1).substring(0, 1));
	}

	@Test
	public void eraseInDisplayAndInLine() {
		VtScreen s = screen(10, 3);
		s.feed("aaaaaaaaaa\r\nbbbbbbbbbb\r\ncccccccccc");
		s.feed(ESC + "[2;4H" + ESC + "[K");
		assertEquals("bbb", line(s, 1));
		s.feed(ESC + "[1;4H" + ESC + "[1K");
		assertEquals("erase to the cursor included", "    aaaaaa", line(s, 0));
		s.feed(ESC + "[2J");
		assertEquals("", line(s, 0));
		assertEquals("", line(s, 1));
		assertEquals("", line(s, 2));
	}

	@Test
	public void insertAndDeleteCharactersAndLines() {
		VtScreen s = screen(10, 4);
		s.feed("abcdef");
		s.feed("\r" + ESC + "[2P");
		assertEquals("cdef", line(s, 0));
		s.feed(ESC + "[2@");
		assertEquals("  cdef", line(s, 0));
		s.feed("\r\nline2\r\nline3");
		s.feed(ESC + "[2;1H" + ESC + "[L");
		assertEquals("", line(s, 1));
		assertEquals("line2", line(s, 2));
		s.feed(ESC + "[2;1H" + ESC + "[M");
		assertEquals("line2", line(s, 1));
		assertEquals("line3", line(s, 2));
	}

	@Test
	public void colorsAndAttributesAreStoredPerCellAndResetByZero() {
		VtScreen s = screen(20, 3);
		s.feed(ESC + "[1;31mR" + ESC + "[0mN");
		assertNotEquals(VtScreen.DEFAULT, s.grid[0].fg[0]);
		assertTrue((s.grid[0].at[0] & VtScreen.BOLD) != 0);
		assertEquals(VtScreen.DEFAULT, s.grid[0].fg[1]);
		assertEquals(0, s.grid[0].at[1]);

		s.feed(ESC + "[4;7;44mU" + ESC + "[mV");
		assertTrue((s.grid[0].at[2] & VtScreen.UNDERLINE) != 0);
		assertTrue((s.grid[0].at[2] & VtScreen.REVERSE) != 0);
		assertNotEquals(VtScreen.DEFAULT, s.grid[0].bg[2]);
		assertEquals(0, s.grid[0].at[3]);
		assertEquals(VtScreen.DEFAULT, s.grid[0].bg[3]);

		s.feed(ESC + "[38;5;196mA" + ESC + "[38;2;10;20;30mB" + ESC + "[39mC");
		assertEquals("256 palette index 196", VtScreen.palette256(196), s.grid[0].fg[4]);
		assertEquals("true color keeps the 24 bit value", 0x0A141E, s.grid[0].fg[5] & 0xFFFFFF);
		assertEquals("39 gives the default colour back", VtScreen.DEFAULT, s.grid[0].fg[6]);
	}

	@Test
	public void paletteCoversAnsiCubeAndGrayRamp() {
		assertEquals(0x000000, VtScreen.palette256(16) & 0xFFFFFF);
		assertEquals(0xFFFFFF, VtScreen.palette256(231) & 0xFFFFFF);
		int gray = VtScreen.palette256(244) & 0xFFFFFF;
		assertEquals("gray ramp has equal channels", (gray >> 16) & 0xFF, gray & 0xFF);
		assertNotEquals(VtScreen.palette256(1), VtScreen.palette256(9));
	}

	@Test
	public void outputPastTheLastRowScrollsAndFeedsTheScrollback() {
		VtScreen s = screen(10, 3);
		s.feed("one\r\ntwo\r\nthree\r\nfour\r\nfive");
		assertEquals("three", line(s, 0));
		assertEquals("four", line(s, 1));
		assertEquals("five", line(s, 2));
		assertEquals(2, s.scrollback.size());
		assertEquals(5, s.totalLines());
		assertEquals("one", s.lineAt(0).text().trim());
		assertEquals("five", s.lineAt(4).text().trim());
	}

	@Test
	public void scrollRegionKeepsTheLinesOutsideInPlace() {
		VtScreen s = screen(10, 5);
		s.feed("header\r\nA\r\nB\r\nC\r\nfooter");
		s.feed(ESC + "[2;4r");
		s.feed(ESC + "[4;1H\n");
		assertEquals("header", line(s, 0));
		assertEquals("footer", line(s, 4));
		assertEquals("B", line(s, 1));
		assertEquals("C", line(s, 2));
		assertEquals("", line(s, 3));
	}

	@Test
	public void alternateScreenIsolatesAFullScreenProgramAndRestoresTheShell() {
		VtScreen s = screen(12, 4);
		s.feed("prompt$ ls");
		s.feed(ESC + "[?1049h");
		assertTrue(s.altScreen);
		assertEquals("alt screen starts blank", "", line(s, 0));
		s.feed(ESC + "[HVIM");
		assertEquals("VIM", line(s, 0));
		s.feed(ESC + "[?1049l");
		assertFalse(s.altScreen);
		assertEquals("prompt$ ls", line(s, 0));
	}

	@Test
	public void modesRequestedByProgramsAreTracked() {
		VtScreen s = screen(20, 4);
		assertTrue(s.cursorVisible);
		s.feed(ESC + "[?25l");
		assertFalse(s.cursorVisible);
		s.feed(ESC + "[?25h");
		assertTrue(s.cursorVisible);
		s.feed(ESC + "[?1h");
		assertTrue(s.appCursorKeys);
		s.feed(ESC + "[?1l");
		assertFalse(s.appCursorKeys);
		s.feed(ESC + "[?2004h");
		assertTrue(s.bracketedPaste);
		s.feed(ESC + "[?2004l");
		assertFalse(s.bracketedPaste);
		s.feed(ESC + "[?1000h" + ESC + "[?1006h");
		assertNotEquals(0, s.mouseMode);
		assertTrue(s.mouseSgr);
		s.feed(ESC + "[?1000l" + ESC + "[?1006l");
		assertEquals(0, s.mouseMode);
		assertFalse(s.mouseSgr);
	}

	@Test
	public void deviceQueriesAreAnsweredToTheProgram() {
		VtScreen s = screen(20, 6);
		s.feed(ESC + "[3;5H" + ESC + "[6n");
		assertEquals(ESC + "[3;5R", answers.get(answers.size() - 1));
		s.feed(ESC + "[c");
		assertTrue(answers.get(answers.size() - 1).startsWith(ESC + "[?"));
	}

	@Test
	public void saveRestoreCursorAndReverseIndex() {
		VtScreen s = screen(20, 5);
		s.feed(ESC + "[2;3H" + ESC + "7" + ESC + "[5;9H" + ESC + "8X");
		assertEquals('X', s.grid[1].ch[2]);
		s.feed(ESC + "[1;1H" + ESC + "M");
		assertEquals("reverse index at the top scrolls the content down", 'X', s.grid[2].ch[2]);
	}

	@Test
	public void sequencesSplitAcrossReadsAndIgnoredStringsDoNotLeakIntoTheScreen() {
		VtScreen s = screen(20, 3);
		s.feed(ESC + "[3");
		s.feed("1mR");
		assertEquals('R', s.grid[0].ch[0]);
		assertNotEquals(VtScreen.DEFAULT, s.grid[0].fg[0]);
		s.feed(ESC + "]0;window title\u0007after");
		assertEquals("Rafter", line(s, 0));
	}

	@Test
	public void fullResetClearsScreenAndModes() {
		VtScreen s = screen(20, 3);
		s.feed("junk" + ESC + "[?25l" + ESC + "[1;31m");
		s.feed(ESC + "c");
		assertEquals("", line(s, 0));
		assertTrue(s.cursorVisible);
		assertEquals(0, s.curX);
		assertEquals(0, s.curY);
		s.feed("Z");
		assertEquals(VtScreen.DEFAULT, s.grid[0].fg[0]);
	}

	@Test
	public void resizeKeepsTextAndMovesOverflowToTheScrollback() {
		VtScreen s = screen(10, 4);
		s.feed("l1\r\nl2\r\nl3\r\nl4");
		s.resize(10, 2);
		assertEquals(2, s.rows);
		assertEquals("l3", line(s, 0));
		assertEquals("l4", line(s, 1));
		assertEquals("lines above the new top are not lost", 2, s.scrollback.size());
		s.resize(10, 4);
		assertEquals("a taller screen takes the lines back from the scrollback", "l1", line(s, 0));
		s.resize(30, 4);
		assertEquals(30, s.cols);
		assertEquals("l4", line(s, 3));
	}

	@Test
	public void screenTextListsEveryRow() {
		VtScreen s = screen(6, 2);
		s.feed("ab\r\ncd");
		String[] rows = s.screenText().split("\n");
		assertEquals(2, rows.length);
		assertTrue(rows[0].startsWith("ab"));
		assertTrue(rows[1].startsWith("cd"));
	}

	@Test
	public void veryLongOutputIsBoundedAndCountsWhatItDropped() {
		VtScreen s = screen(20, 5);
		StringBuilder big = new StringBuilder();
		for (int i = 0; i < 12000; i++) {
			big.append("line ").append(i).append("\r\n");
		}
		s.feed(big);
		assertTrue("scrollback is capped (10000 lines, trimmed in blocks of 500)", s.scrollback.size() <= 10500);
		assertTrue("what was trimmed is counted", s.dropped > 0 && s.dropped % 500 == 0);
		assertEquals("no line is lost without being counted: kept + dropped = scrolled out", 12000 - 4, s.scrollback.size() + s.dropped);
		assertTrue("the newest line is still there", s.screenText().contains("line 11999"));
	}
}
