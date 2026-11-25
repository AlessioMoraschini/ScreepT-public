package markdown.viewer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.SwingUtilities;
import javax.swing.event.HyperlinkEvent;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * The extra features inside the real window (never shown): what the preview really paints (alerts, code colors, in dark and
 * light, at zoom 100 and 150), the clickable task boxes, find / replace, table sorting, spreadsheet and image paste, rich copy.
 */
public class MarkdownViewerExtrasGuiTest {

	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();

	/** The tests write the settings (zoom, view mode...) in their own node, never in the one of the user */
	@org.junit.BeforeClass
	public static void isolatePreferences() {
		MarkdownViewerFrame.prefNode = "am-design-development/screept/markdownviewer-test";
	}

	@org.junit.AfterClass
	public static void removeTestPreferences() throws Exception {
		java.util.prefs.Preferences.userRoot().node(MarkdownViewerFrame.prefNode).removeNode();
		MarkdownViewerFrame.prefNode = "am-design-development/screept/markdownviewer";
	}


	private MarkdownViewerFrame frame;
	private File mdFile;
	/** The clipboard of the tests: the real one (the user's) is never touched. */
	private static final Clipboard CLIP = new Clipboard("markdown-viewer-test");

	@Before
	public void build() throws Exception {
		Assume.assumeFalse("needs a display server (windows are never shown)", GraphicsEnvironment.isHeadless());
		MarkdownViewerFrame.logger.setLoggerAvailable(false);
		mdFile = new File(tmp.getRoot(), "doc.md");
		Files.write(mdFile.toPath(), "# Start\n".getBytes(StandardCharsets.UTF_8));
		frame = edt(new Callable<MarkdownViewerFrame>() {
			@Override
			public MarkdownViewerFrame call() {
				return new MarkdownViewerFrame(null, mdFile);
			}
		});
		flush();
		ImagePaste.testClipboard = CLIP;
		CLIP.setContents(new StringSelection(""), null);
	}

	@After
	public void close() throws Exception {
		ImagePaste.testClipboard = null;
		if(frame == null)
			return;
		edt(new Callable<Void>() {
			@Override
			public Void call() {
				frame.releaseResources();
				frame.dispose();
				return null;
			}
		});
	}

	// ------------------------------------------------------------------ plumbing

	private static <T> T edt(final Callable<T> c) throws Exception {
		if(SwingUtilities.isEventDispatchThread())
			return c.call();
		final Object[] box = new Object[2];
		SwingUtilities.invokeAndWait(new Runnable() {
			@Override
			public void run() {
				try {
					box[0] = c.call();
				} catch (Throwable t) {
					box[1] = t;
				}
			}
		});
		if(box[1] != null)
			throw new AssertionError("EDT: " + box[1], (Throwable) box[1]);
		@SuppressWarnings("unchecked")
		T r = (T) box[0];
		return r;
	}

	private static void flush() throws Exception {
		for(int i = 0; i < 3; i++)
			SwingUtilities.invokeAndWait(new Runnable() {
				@Override
				public void run() {
				}
			});
	}

	@SuppressWarnings("unchecked")
	private <T> T field(String name) throws Exception {
		Field f = MarkdownViewerFrame.class.getDeclaredField(name);
		f.setAccessible(true);
		return (T) f.get(frame);
	}

	private void setField(String name, Object value) throws Exception {
		Field f = MarkdownViewerFrame.class.getDeclaredField(name);
		f.setAccessible(true);
		f.set(frame, value);
	}

	private Object call(final String name, final Object... args) throws Exception {
		for(final Method m : MarkdownViewerFrame.class.getDeclaredMethods()) {
			if(m.getName().equals(name) && m.getParameterTypes().length == args.length) {
				m.setAccessible(true);
				return edt(new Callable<Object>() {
					@Override
					public Object call() throws Exception {
						return m.invoke(frame, args);
					}
				});
			}
		}
		throw new AssertionError("no method " + name);
	}

	private RSyntaxTextArea editor() throws Exception {
		return field("textAreaMarkDown");
	}

	private JEditorPane preview() throws Exception {
		return field("textAreaPreview");
	}

	private String text() throws Exception {
		return editor().getText().replace("\r\n", "\n");
	}

	private void setText(final String s, final int caret) throws Exception {
		edt(new Callable<Void>() {
			@Override
			public Void call() throws Exception {
				editor().setText(s);
				editor().setCaretPosition(Math.min(caret, s.length()));
				return null;
			}
		});
		call("renderNow");
		flush();
	}

	private void theme(boolean dark, int zoom) throws Exception {
		setField("previewDark", dark);
		setField("zoomPercent", zoom);
		call("applyPreviewStyle");
		call("renderNow");
		flush();
	}

	/** What the preview paints, as pixels. */
	private BufferedImage paint() throws Exception {
		return edt(new Callable<BufferedImage>() {
			@Override
			public BufferedImage call() throws Exception {
				JEditorPane p = preview();
				p.setSize(760, 1400);
				BufferedImage img = new BufferedImage(760, 1400, BufferedImage.TYPE_INT_RGB);
				p.paint(img.getGraphics());
				return img;
			}
		});
	}

	private static int count(BufferedImage img, Color c) {
		int n = 0, rgb = c.getRGB() & 0xFFFFFF;
		for(int y = 0; y < img.getHeight(); y++)
			for(int x = 0; x < img.getWidth(); x++)
				if((img.getRGB(x, y) & 0xFFFFFF) == rgb)
					n++;
		return n;
	}

	private void press(String tooltipStart) throws Exception {
		final List<AbstractButton> found = new ArrayList<AbstractButton>();
		collect(frame.getContentPane(), tooltipStart, found);
		assertFalse("no button with tooltip \"" + tooltipStart + "\"", found.isEmpty());
		edt(new Callable<Void>() {
			@Override
			public Void call() {
				found.get(0).doClick();
				return null;
			}
		});
	}

	private static void collect(Container c, String tooltipStart, List<AbstractButton> out) {
		for(Component k : c.getComponents()) {
			if(k instanceof AbstractButton && ((JComponent) k).getToolTipText() != null && ((JComponent) k).getToolTipText().startsWith(tooltipStart))
				out.add((AbstractButton) k);
			if(k instanceof Container)
				collect((Container) k, tooltipStart, out);
		}
	}

	private static boolean next(final FindBar bar, final boolean forward) throws Exception {
		return edt(new Callable<Boolean>() {
			@Override
			public Boolean call() {
				return bar.findNext(forward);
			}
		});
	}

	private static void setClipboard(Transferable t, java.awt.datatransfer.ClipboardOwner owner) {
		CLIP.setContents(t, owner);
	}

	private static Clipboard clipboard() {
		return CLIP;
	}

	// ------------------------------------------------------------------ what is painted

	@Test
	public void alertIsPaintedWithItsBorderAndBackgroundInDarkAndLight() throws Exception {
		setText("para\n\n> [!NOTE]\n> The note text\n\n> [!CAUTION]\n> Careful\n\nafter\n", 0);
		for(int zoom : new int[] {100, 150}) {
			theme(true, zoom);
			BufferedImage dark = paint();
			assertTrue("dark note border at zoom " + zoom, count(dark, Color.decode("#4493f8")) > 20);
			assertTrue("dark note background", count(dark, Color.decode("#1b2a3d")) > 500);
			assertTrue("dark caution border", count(dark, Color.decode("#f85149")) > 20);
			theme(false, zoom);
			BufferedImage light = paint();
			assertTrue("light note border at zoom " + zoom, count(light, Color.decode("#0969da")) > 20);
			assertTrue("light note background", count(light, Color.decode("#eaf3ff")) > 500);
			assertTrue("light caution background", count(light, Color.decode("#ffebe9")) > 500);
		}
	}

	@Test
	public void codeColorsArePainted() throws Exception {
		setText("```java\npublic class A { int x = 42; String s = \"hi\"; // c\n}\n```\n", 0);
		theme(true, 100);
		BufferedImage dark = paint();
		assertTrue("keyword", count(dark, Color.decode("#ff7b72")) > 10);
		assertTrue("number", count(dark, Color.decode("#79c0ff")) > 3);
		assertTrue("string", count(dark, Color.decode("#a5d6a7")) > 5);
		assertTrue("comment", count(dark, Color.decode("#7f8c98")) > 3);
		theme(false, 150);
		BufferedImage light = paint();
		assertTrue("keyword light", count(light, Color.decode("#cf222e")) > 10);
		assertTrue("string light", count(light, Color.decode("#0a7d3c")) > 5);
	}

	@Test
	public void frontMatterAndEmojiAreInThePreview() throws Exception {
		setText("---\ntitle: Hello\nauthor: Me\n---\n# Heading\n\nGo :rocket: :tada:\n", 0);
		JEditorPane p = preview();
		String shown = p.getDocument().getText(0, p.getDocument().getLength());
		assertTrue(shown, shown.contains("title") && shown.contains("Hello") && shown.contains("author"));
		assertTrue(shown, shown.contains(new String(Character.toChars(0x1F680))));
		assertFalse(shown, shown.contains(":rocket:"));
		assertFalse("the --- must not show", shown.contains("---"));
		// the outline jumps to the right place although the front matter is blanked
		javax.swing.DefaultListModel<?> outline = field("outlineModel");
		assertEquals(1, outline.size());
		call("jumpTo", editor().getText().indexOf("# Heading"));
		assertEquals(editor().getText().indexOf("# Heading"), editor().getCaretPosition());
	}

	// ------------------------------------------------------------------ task boxes

	@Test
	public void clickingABoxInThePreviewChangesTheMarkdown() throws Exception {
		setText("# T\n\n- [ ] first\n- [x] second\n- [ ] third\n", 0);
		assertTrue(((javax.swing.JLabel) field("lblStats")).getText().contains("tasks 1/3"));
		final JEditorPane p = preview();
		final String shown = p.getDocument().getText(0, p.getDocument().getLength());
		assertTrue(shown, shown.indexOf('☐') > 0 && shown.indexOf('☑') > 0);

		// a real mouse click on the third box
		final int offset = shown.lastIndexOf('☐');
		edt(new Callable<Void>() {
			@Override
			public Void call() throws Exception {
				p.setSize(760, 600);
				p.doLayout();
				Rectangle r = p.modelToView(offset);
				assertNotNull(r);
				int x = r.x + Math.max(1, r.width / 2), y = r.y + r.height / 2;
				long now = System.currentTimeMillis();
				for(int id : new int[] {MouseEvent.MOUSE_PRESSED, MouseEvent.MOUSE_RELEASED, MouseEvent.MOUSE_CLICKED})
					p.dispatchEvent(new MouseEvent(p, id, now, 0, x, y, 1, false, MouseEvent.BUTTON1));
				return null;
			}
		});
		flush();
		assertEquals("# T\n\n- [ ] first\n- [x] second\n- [x] third\n", text());
		call("renderNow");
		assertTrue(((javax.swing.JLabel) field("lblStats")).getText().contains("tasks 2/3"));

		// the link event itself, for the first and the second box (uncheck)
		call("toggleTaskInSource", "0");
		call("toggleTaskInSource", "1");
		assertEquals("# T\n\n- [x] first\n- [ ] second\n- [x] third\n", text());
		call("toggleTaskInSource", "99");
		call("toggleTaskInSource", "abc");
		assertEquals("# T\n\n- [x] first\n- [ ] second\n- [x] third\n", text());
	}

	@Test
	public void followLinkRoutesTaskLinks() throws Exception {
		setText("- [ ] only\n", 0);
		final JEditorPane p = preview();
		edt(new Callable<Void>() {
			@Override
			public Void call() {
				p.fireHyperlinkUpdate(new HyperlinkEvent(p, HyperlinkEvent.EventType.ACTIVATED, null, "mdv-task:0"));
				return null;
			}
		});
		assertEquals("- [x] only\n", text());
	}

	// ------------------------------------------------------------------ find and replace

	@Test
	public void findBarCountsFindsAndReplaces() throws Exception {
		setText("Cat cat CAT concat\ncat-1 cat-22\n", 0);
		press("Find in the document");
		final FindBar bar = field("findBar");
		assertTrue(bar.isVisible());

		edt(new Callable<Void>() {
			@Override
			public Void call() {
				bar.setQuery("cat", false, false, false);
				return null;
			}
		});
		assertEquals("6 matches", bar.countText());
		edt(new Callable<Void>() {
			@Override
			public Void call() {
				bar.setQuery("cat", true, false, false);
				return null;
			}
		});
		assertEquals("4 matches", bar.countText());
		edt(new Callable<Void>() {
			@Override
			public Void call() {
				bar.setQuery("cat", false, true, false);
				return null;
			}
		});
		assertEquals("whole words: not the one in concat", "5 matches", bar.countText());
	}

	@Test
	public void findNextMovesAndWraps() throws Exception {
		setText("one two one two one", 0);
		call("openFind", false);
		final FindBar bar = field("findBar");
		edt(new Callable<Void>() {
			@Override
			public Void call() {
				bar.setQuery("one", true, false, false);
				return null;
			}
		});
		int[] starts = new int[4];
		for(int i = 0; i < 4; i++) {
			assertTrue(next(bar, true));
			starts[i] = editor().getSelectionStart();
		}
		assertEquals(0, starts[0]);
		assertEquals(8, starts[1]);
		assertEquals(16, starts[2]);
		assertEquals("wraps to the first match", 0, starts[3]);
		assertTrue(next(bar, false));
		assertEquals("backwards from the first wraps to the last", 16, editor().getSelectionStart());
		edt(new Callable<Void>() {
			@Override
			public Void call() {
				bar.setQuery("zzz", true, false, false);
				return null;
			}
		});
		assertEquals("no match", bar.countText());
		assertFalse(next(bar, true));
	}

	@Test
	public void replaceAndRegexReplace() throws Exception {
		setText("a1 b22 c333\n", 0);
		call("openFind", true);
		final FindBar bar = field("findBar");
		edt(new Callable<Void>() {
			@Override
			public Void call() throws Exception {
				bar.setQuery("([a-z])(\\d+)", true, false, true);
				bar.setReplacement("$2$1");
				Method m = FindBar.class.getDeclaredMethod("replaceEverything");
				m.setAccessible(true);
				m.invoke(bar);
				return null;
			}
		});
		assertEquals("1a 22b 333c\n", text());
		assertEquals("3 replaced", bar.countText());

		edt(new Callable<Void>() {
			@Override
			public Void call() throws Exception {
				bar.setQuery("(unclosed", true, false, true);
				return null;
			}
		});
		assertEquals("invalid expression", bar.countText());
		assertFalse(next(bar, true));
		assertEquals("1a 22b 333c\n", text());
	}

	@Test
	public void findWorksInThePreviewOnly() throws Exception {
		setText("# Title\n\nalpha beta alpha\n\n- gamma alpha\n", 0);
		for(Object mode : Class.forName("markdown.viewer.MarkdownViewerFrame$ViewMode").getEnumConstants()) {
			if(mode.toString().equals("PREVIEW"))
				setField("viewMode", mode);
		}
		call("openFind", false);
		final FindBar bar = field("findBar");
		edt(new Callable<Void>() {
			@Override
			public Void call() {
				bar.setQuery("alpha", true, false, false);
				return null;
			}
		});
		assertEquals("3 matches", bar.countText());
		JEditorPane p = preview();
		int[] at = new int[4];
		for(int i = 0; i < 4; i++) {
			assertTrue(next(bar, true));
			assertEquals("alpha", p.getSelectedText());
			at[i] = p.getSelectionStart();
		}
		assertTrue(at[0] < at[1] && at[1] < at[2]);
		assertEquals("wraps", at[0], at[3]);
		assertTrue(next(bar, false));
		assertEquals(at[2], p.getSelectionStart());
	}

	// ------------------------------------------------------------------ tables

	@Test
	public void sortButtonsOrderTheTableByTheCaretColumn() throws Exception {
		String table = "intro\n\n| Name | Qty |\n| --- | ---: |\n| pear | 10 |\n| apple | 9 |\n| fig | 100 |\n\nafter\n";
		int caretInQty = table.indexOf("| 9 |") + 3;
		setText(table, caretInQty);
		press("Sort the table at the caret by the column of the caret, ascending");
		String sorted = text();
		assertTrue(sorted, sorted.indexOf("| apple") < sorted.indexOf("| pear") && sorted.indexOf("| pear") < sorted.indexOf("| fig"));
		assertTrue(sorted.startsWith("intro\n\n| Name") && sorted.endsWith("\nafter\n"));
		press("Sort the table at the caret by the column of the caret, descending");
		sorted = text();
		assertTrue(sorted, sorted.indexOf("| fig") < sorted.indexOf("| pear") && sorted.indexOf("| pear") < sorted.indexOf("| apple"));

		// by the first column
		int caretInName = text().indexOf("| pear") + 3;
		edt(new Callable<Void>() {
			@Override
			public Void call() throws Exception {
				editor().setCaretPosition(text().indexOf("| pear") + 3);
				return null;
			}
		});
		press("Sort the table at the caret by the column of the caret, ascending");
		sorted = text();
		assertTrue(sorted, sorted.indexOf("| apple") < sorted.indexOf("| fig") && sorted.indexOf("| fig") < sorted.indexOf("| pear"));
		assertTrue(caretInName > 0);

		// not in a table: nothing changes
		String before = text();
		edt(new Callable<Void>() {
			@Override
			public Void call() throws Exception {
				editor().setCaretPosition(2);
				return null;
			}
		});
		press("Sort the table at the caret by the column of the caret, ascending");
		assertEquals(before, text());
	}

	@Test
	public void sortingIsOneUndoStep() throws Exception {
		String table = "| a |\n| --- |\n| 3 |\n| 1 |\n| 2 |\n";
		setText(table, table.indexOf("| 3") + 2);
		press("Sort the table at the caret by the column of the caret, ascending");
		assertTrue(text(), text().indexOf("| 1") < text().indexOf("| 2") && text().indexOf("| 2") < text().indexOf("| 3"));
		edt(new Callable<Void>() {
			@Override
			public Void call() throws Exception {
				editor().undoLastAction();
				return null;
			}
		});
		assertEquals(table, text());
	}

	private void paste() throws Exception {
		final SmartMarkdownEditor smart = field("smartEditor");
		edt(new Callable<Void>() {
			@Override
			public Void call() throws Exception {
				Method m = SmartMarkdownEditor.class.getDeclaredMethod("smartPaste");
				m.setAccessible(true);
				m.invoke(smart);
				return null;
			}
		});
	}

	@Test
	public void pastingSpreadsheetCellsMakesATable() throws Exception {
		setText("before\n", 7);
		setClipboard(new StringSelection("Name\tQty\nApple\t3\nPear\t10"), null);
		paste();
		String t = text();
		assertTrue(t, t.startsWith("before\n| Name") || t.startsWith("before\n\n| Name"));
		assertTrue(t, t.contains("| Apple") && t.contains("| Pear") && t.contains("| --"));
		assertTrue(t, MarkdownLint.check(t, null).isEmpty());
		// the table is a real table in the preview
		assertTrue(((String) call("markdownToHtml", t)).contains("<table>"));
	}

	@Test
	public void cellsPastedInACodeFenceStayText() throws Exception {
		String start = "```\n";
		setText(start, start.length());
		setClipboard(new StringSelection("a\tb\nc\td"), null);
		paste();
		assertEquals("```\na\tb\nc\td", text());
	}

	@Test
	public void urlOnSelectionAndPlainTextStillPasteAsBefore() throws Exception {
		setText("see docs here", 0);
		edt(new Callable<Void>() {
			@Override
			public Void call() throws Exception {
				editor().select(4, 8);
				return null;
			}
		});
		setClipboard(new StringSelection("https://example.com/d"), null);
		paste();
		assertEquals("see [docs](https://example.com/d) here", text());
		setText("x", 1);
		setClipboard(new StringSelection("one\ttwo"), null);
		paste();
		assertEquals("xone\ttwo", text());
	}

	@Test
	public void imageButtonWithoutClipboardImageChangesNothing() throws Exception {
		setText("keep", 4);
		setClipboard(new StringSelection("only text"), null);
		press("Saves the image of the clipboard");
		assertEquals("keep", text());
		assertFalse(new File(tmp.getRoot(), "images").exists());
	}

	// ------------------------------------------------------------------ clipboard

	@Test
	public void imageOnTheClipboardIsSavedAndLinked() throws Exception {
		setText("text ", 5);
		BufferedImage image = new BufferedImage(6, 6, BufferedImage.TYPE_INT_ARGB);
		image.setRGB(2, 2, 0xFFAA5500);
		setClipboard(new Transferable() {
			@Override
			public DataFlavor[] getTransferDataFlavors() {
				return new DataFlavor[] {DataFlavor.imageFlavor};
			}

			@Override
			public boolean isDataFlavorSupported(DataFlavor f) {
				return DataFlavor.imageFlavor.equals(f);
			}

			@Override
			public Object getTransferData(DataFlavor f) throws UnsupportedFlavorException {
				if(!isDataFlavorSupported(f))
					throw new UnsupportedFlavorException(f);
				return image;
			}
		}, null);
		press("Saves the image of the clipboard");
		File[] saved = new File(tmp.getRoot(), "images").listFiles();
		assertNotNull("images folder created", saved);
		assertEquals(1, saved.length);
		assertTrue(text(), text().matches("text !\\[image\\]\\(images/image-\\d{8}-\\d{6}\\.png\\)\\n?"));
		assertEquals(0xFFAA5500, javax.imageio.ImageIO.read(saved[0]).getRGB(2, 2));
		assertTrue(MarkdownLint.check(text(), tmp.getRoot()).isEmpty());
	}

	@Test
	public void copyRichPutsHtmlAndMarkdownOnTheClipboard() throws Exception {
		setText("# Title\n\n> [!TIP]\n> use **it**\n\n```js\nvar a = 1;\n```\n", 0);
		press("Copies the rendered document with its formatting");
		Clipboard c = clipboard();
		DataFlavor html = new DataFlavor("text/html;class=java.lang.String");
		assertTrue(c.isDataFlavorAvailable(html));
		String h = (String) c.getData(html);
		assertTrue(h, h.contains("<h1") && h.contains("<strong>it</strong>") && h.contains("<font color=\"#cf222e\">var</font>") && h.contains("<table"));
		assertFalse("rich editors ignore classes: none left", h.contains("alert-tip") || h.contains("tk-"));
		assertTrue(((String) c.getData(DataFlavor.stringFlavor)).startsWith("# Title"));
	}

	// ------------------------------------------------------------------ export

	@Test
	public void exportedPageHasTheNewStyles() throws Exception {
		setText("> [!WARNING]\n> careful :tada:\n\n```java\nint x;\n```\n\n- [x] done\n", 0);
		String page = (String) call("buildStandaloneHtml", (File) null);
		assertTrue(page.contains(".alert-warning{--c:") && page.contains(".tk-k{color:") && page.contains("prefers-color-scheme:dark"));
		assertTrue(page.contains("<div class=\"alert-warning\">") && page.contains("<span class=\"tk-k\">int</span>"));
		assertTrue("real checkbox in the export", page.contains("<input") && !page.contains("mdv-task"));
		assertTrue(page.contains(new String(Character.toChars(0x1F389))));
	}

	@Test
	public void documentBetweenRulesIsNotSwallowed() throws Exception {
		setText("---\n# Slide 1\n\ntext\n\n---\n# Slide 2\n", 0);
		JEditorPane p = preview();
		String shown = p.getDocument().getText(0, p.getDocument().getLength());
		assertTrue(shown, shown.contains("Slide 1") && shown.contains("Slide 2") && shown.contains("text"));
		javax.swing.DefaultListModel<?> outline = field("outlineModel");
		assertEquals(2, outline.size());
	}

	@Test
	public void boxesStayPlainWhenTheTaskCountsDisagree() throws Exception {
		// an indented code block with a task-like line: the html has 1 checkbox, the source 2 task lines
		setText("- [ ] real\n\n\n    - [ ] inside indented code\n", 0);
		JEditorPane p = preview();
		String shown = p.getDocument().getText(0, p.getDocument().getLength());
		int boxes = 0;
		for(char c : shown.toCharArray())
			if(c == '\u2610' || c == '\u2611')
				boxes++;
		int sourceTasks = MarkdownExtras.taskLines(text()).size();
		int htmlBoxes = ((String) call("markdownToHtml", text())).split("task-list-item-checkbox", -1).length - 1;
		if(htmlBoxes != sourceTasks)
			assertEquals("no clickable boxes when the numbers disagree", 0, boxes);
		else
			assertEquals(sourceTasks, boxes);
	}

	// ------------------------------------------------------------------ heading combo, caret, emoji button

	@SuppressWarnings("unchecked")
	private javax.swing.JComboBox<String> headingCombo() throws Exception {
		return (javax.swing.JComboBox<String>) field("comboHeading");
	}

	private void selectHeading(final int index) throws Exception {
		edt(new Callable<Void>() {
			@Override
			public Void call() throws Exception {
				headingCombo().setSelectedIndex(index);
				return null;
			}
		});
	}

	@Test
	public void headingComboSetsTheLevelOfTheLine() throws Exception {
		setText("Title\n\nsecond line\n", 2);
		selectHeading(1);
		assertEquals("# Title\n\nsecond line\n", text());
		selectHeading(3);
		assertEquals("### Title\n\nsecond line\n", text());
		selectHeading(6);
		assertEquals("###### Title\n\nsecond line\n", text());
		selectHeading(2);
		assertEquals("## Title\n\nsecond line\n", text());
		selectHeading(0);
		assertEquals("Title\n\nsecond line\n", text());
		selectHeading(0);
		assertEquals("Title\n\nsecond line\n", text());
	}

	@Test
	public void headingComboWorksOnSelectedLinesAndSkipsCodeFences() throws Exception {
		final String md = "one\n## two\nthree\n\n```\n# not a heading\n```\n";
		setText(md, 0);
		edt(new Callable<Void>() {
			@Override
			public Void call() throws Exception {
				editor().select(0, md.indexOf("three") + 3);
				return null;
			}
		});
		selectHeading(2);
		assertEquals("## one\n## two\n## three\n\n```\n# not a heading\n```\n", text());
		// across the fence: the fenced line is left alone
		edt(new Callable<Void>() {
			@Override
			public Void call() throws Exception {
				editor().select(0, editor().getText().length() - 1);
				return null;
			}
		});
		selectHeading(1);
		assertTrue(text(), text().startsWith("# one\n# two\n# three\n") && text().contains("```\n# not a heading\n```"));
	}

	@Test
	public void headingComboFollowsTheCaret() throws Exception {
		setText("plain\n### third\n", 0);
		assertEquals(0, headingCombo().getSelectedIndex());
		edt(new Callable<Void>() {
			@Override
			public Void call() throws Exception {
				editor().setCaretPosition(editor().getText().indexOf("third"));
				return null;
			}
		});
		flush();
		assertEquals(3, headingCombo().getSelectedIndex());
		assertEquals("moving the caret never edits the text", "plain\n### third\n", text());
		edt(new Callable<Void>() {
			@Override
			public Void call() throws Exception {
				editor().setCaretPosition(1);
				return null;
			}
		});
		flush();
		assertEquals(0, headingCombo().getSelectedIndex());
	}

	@Test
	public void caretHeightFollowsTheZoom() throws Exception {
		setText("hello\nworld\n", 3);
		edt(new Callable<Void>() {
			@Override
			public Void call() throws Exception {
				frame.setBounds(-4000, 0, 900, 600); // shown far away from the screens: the caret exists only in a displayed window
				frame.setVisible(true);
				editor().requestFocusInWindow();
				editor().getCaret().setVisible(true);
				return null;
			}
		});
		Thread.sleep(300);
		flush();
		int before = ((Rectangle) editor().getCaret()).height;
		assertTrue("caret at 100%: " + before, before > 5);
		call("setZoom", 250);
		flush();
		int after = ((Rectangle) editor().getCaret()).height;
		assertEquals("the caret as high as a line at 250%", editor().getLineHeight(), after);
		assertTrue(after > before * 2);
		call("setZoom", 60);
		flush();
		assertEquals(editor().getLineHeight(), ((Rectangle) editor().getCaret()).height);
		// a selection survives the refresh
		edt(new Callable<Void>() {
			@Override
			public Void call() throws Exception {
				editor().select(1, 4);
				return null;
			}
		});
		call("setZoom", 120);
		flush();
		assertEquals("ell", editor().getSelectedText());
	}

	@Test
	public void emojiButtonAndShortcutExist() throws Exception {
		List<AbstractButton> found = new ArrayList<AbstractButton>();
		collect(frame.getContentPane(), "Choose an emoji to insert", found);
		assertEquals(1, found.size());
		assertEquals("Emoji", found.get(0).getText());
	}

	@Test
	public void checkButtonExists() throws Exception {
		List<AbstractButton> found = new ArrayList<AbstractButton>();
		collect(frame.getContentPane(), "Check the document", found);
		assertEquals(1, found.size());
		collect(frame.getContentPane(), "Copies the rendered document", found);
		collect(frame.getContentPane(), "Find and replace", found);
		assertEquals(3, found.size());
	}
}
