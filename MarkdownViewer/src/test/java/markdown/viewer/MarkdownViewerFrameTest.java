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
package markdown.viewer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import javax.swing.AbstractButton;
import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * The markdown editor window: the toolbar buttons edit the text as their tooltips say, the view modes, zoom and outline
 * work, files are saved and loaded, and the standalone html page is built (images embedded). The window is never shown;
 * the buttons that open choosers, dialogs or the browser are not pressed.
 */
public class MarkdownViewerFrameTest {

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

	@Before
	public void build() throws Exception {
		Assume.assumeFalse("needs a display server (windows are never shown)", GraphicsEnvironment.isHeadless());
		MarkdownViewerFrame.logger.setLoggerAvailable(false);
		mdFile = new File(tmp.getRoot(), "notes.md");
		Files.write(mdFile.toPath(), "# Start\n\ntext".getBytes(StandardCharsets.UTF_8));
		frame = edt(new Callable<MarkdownViewerFrame>() {
			@Override
			public MarkdownViewerFrame call() {
				return new MarkdownViewerFrame(null, mdFile);
			}
		});
		flush();
	}

	@After
	public void close() throws Exception {
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
		final Object[] box = new Object[2];
		if (SwingUtilities.isEventDispatchThread()) {
			return c.call();
		}
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
		if (box[1] != null)
			throw new AssertionError("EDT: " + box[1], (Throwable) box[1]);
		@SuppressWarnings("unchecked")
		T r = (T) box[0];
		return r;
	}

	private static void edt(final Runnable r) throws Exception {
		edt(new Callable<Void>() {
			@Override
			public Void call() {
				r.run();
				return null;
			}
		});
		flush();
	}

	private static void flush() throws Exception {
		for (int i = 0; i < 3; i++)
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

	private Object call(final String name, final Object... args) throws Exception {
		for (final Method m : MarkdownViewerFrame.class.getDeclaredMethods()) {
			if (m.getName().equals(name) && m.getParameterTypes().length == args.length) {
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

	private String text() throws Exception {
		return editor().getText().replace("\r\n", "\n");
	}

	private void setText(final String s, final int selStart, final int selEnd) throws Exception {
		edt(new Runnable() {
			@Override
			public void run() {
				try {
					editor().setText(s);
					editor().setSelectionStart(selStart);
					editor().setSelectionEnd(selEnd);
				} catch (Exception e) {
					throw new AssertionError(e);
				}
			}
		});
	}

	private AbstractButton button(String tooltipStart) {
		List<AbstractButton> found = new ArrayList<AbstractButton>();
		collect(frame.getContentPane(), tooltipStart, found);
		assertFalse("no button with tooltip \"" + tooltipStart + "\"", found.isEmpty());
		return found.get(0);
	}

	private static void collect(Container c, String tooltipStart, List<AbstractButton> out) {
		for (Component k : c.getComponents()) {
			if (k instanceof AbstractButton && ((JComponent) k).getToolTipText() != null
					&& ((JComponent) k).getToolTipText().startsWith(tooltipStart))
				out.add((AbstractButton) k);
			if (k instanceof Container)
				collect((Container) k, tooltipStart, out);
		}
	}

	private void press(String tooltipStart) throws Exception {
		final AbstractButton b = button(tooltipStart);
		edt(new Runnable() {
			@Override
			public void run() {
				b.doClick();
			}
		});
	}

	// ------------------------------------------------------------------ the text tools

	@Test
	public void inlineFormattingWrapsAndUnwrapsTheSelection() throws Exception {
		setText("hello world", 0, 11);
		press("Bold");
		assertEquals("**hello world**", text());
		press("Bold");
		assertEquals("a second click removes it", "hello world", text());
		setText("word", 2, 2);
		press("Italic");
		assertEquals("*word*", text());
		setText("old", 0, 3);
		press("Strikethrough");
		assertEquals("~~old~~", text());
		setText("call()", 0, 6);
		press("Inline code");
		assertEquals("`call()`", text());
	}

	@Test
	public void headingButtonCyclesTheLevels() throws Exception {
		setText("title", 0, 0);
		press("Heading");
		assertEquals("# title", text());
		press("Heading");
		assertEquals("## title", text());
		press("Heading");
		assertEquals("### title", text());
		press("Heading");
		assertEquals("none after the third level", "title", text());
	}

	@Test
	public void listQuoteAndTaskButtonsWorkOnTheSelectedLines() throws Exception {
		setText("a\nb", 0, 3);
		press("Bulleted list");
		assertEquals("- a\n- b", text());
		press("Bulleted list");
		assertEquals("a second click removes the bullets", "a\nb", text());
		setText("a\nb", 0, 3);
		press("Numbered list");
		assertEquals("1. a\n2. b", text());
		setText("a\nb", 0, 3);
		press("Quote");
		assertEquals("> a\n> b", text());
		setText("a", 0, 1);
		press("Task list");
		assertEquals("- [ ] a", text());
		press("Task list");
		assertEquals("a second click checks the box", "- [x] a", text());
	}

	@Test
	public void blocksAreInserted() throws Exception {
		setText("", 0, 0);
		press("Insert a horizontal rule");
		assertTrue(text(), text().contains("---"));
		setText("", 0, 0);
		press("Insert a table");
		assertTrue(text(), text().contains("|"));
		setText("code", 0, 4);
		press("Fenced code block");
		assertTrue(text(), text().contains("```") && text().contains("code"));
		setText("\n", 0, 0);
		press("Math");
		setText("# One\n\n## Two\n", 0, 0);
		press("Table of contents");
		assertTrue("toc between markers: " + text(), text().contains("<!-- TOC -->") && text().contains("Two"));
		setText("|a|b|\n|-|-|\n|long cell|x|\n", 1, 1);
		press("Format the table");
		assertTrue(text(), text().contains("| long cell |"));
	}

	// ------------------------------------------------------------------ views, outline, zoom

	@Test
	public void viewModesAndTogglesChangeTheWindow() throws Exception {
		press("Editor only");
		assertEquals("EDITOR", String.valueOf((Object) field("viewMode")));
		press("Preview only");
		assertEquals("PREVIEW", String.valueOf((Object) field("viewMode")));
		press("Split view");
		assertEquals("SPLIT", String.valueOf((Object) field("viewMode")));
		press("Show or hide the list of headings");
		press("Show or hide the list of headings");
		boolean wrapped = editor().getLineWrap();
		press("Wrap long lines");
		assertEquals(!wrapped, editor().getLineWrap());
		press("Wrap long lines");
		assertEquals(wrapped, editor().getLineWrap());
		press("Scroll the preview together");
		press("Scroll the preview together");
		press("Invert the preview colors");
		press("Invert the preview colors");
		press("Render the document again");
	}

	@Test
	public void zoomStaysInsideItsLimits() throws Exception {
		for (int i = 0; i < 40; i++)
			press("Smaller text");
		int min = (Integer) field("zoomPercent");
		assertTrue("lower limit: " + min, min >= 50 && min <= 70);
		for (int i = 0; i < 60; i++)
			press("Larger text");
		int max = (Integer) field("zoomPercent");
		assertTrue("upper limit: " + max, max >= 250 && max <= 320);
	}

	@Test
	public void headingsFeedTheOutlineAndSelectingOneJumpsThere() throws Exception {
		frame.loadMarkdown("# Alpha\n\ntext\n\n## Beta\n\nmore\n\n### Gamma\n", true);
		flush();
		Thread.sleep(300);
		flush();
		DefaultListModel<?> model = field("outlineModel");
		assertEquals("three headings: " + model, 3, model.size());
		final javax.swing.JList<?> list = field("outlineList");
		edt(new Runnable() {
			@Override
			public void run() {
				list.setSelectedIndex(1);
			}
		});
		Thread.sleep(200);
		flush();
		int caretLine = editor().getCaretLineNumber();
		assertEquals("the caret is on the second heading", 4, caretLine);
	}

	// ------------------------------------------------------------------ files and conversion

	@Test
	public void saveWritesTheTextAndClearsTheModifiedFlag() throws Exception {
		assertFalse(frame.isModified());
		setText("# Changed\n\nbody", 0, 0);
		assertTrue("the edit is detected", frame.isModified());
		assertTrue(frame.saveMarkdown(true));
		assertEquals("# Changed\n\nbody", new String(Files.readAllBytes(mdFile.toPath()), StandardCharsets.UTF_8).replace("\r\n", "\n"));
		assertFalse(frame.isModified());
		assertEquals(mdFile, frame.getMarkdownFile());
		assertTrue(frame.getHostTitle().contains("notes"));
	}

	@Test
	public void loadingMarkdownAndHtmlFiles() throws Exception {
		File other = new File(tmp.getRoot(), "other.md");
		Files.write(other.toPath(), "## Other file\n\n- x\n- y".getBytes(StandardCharsets.UTF_8));
		frame.loadFile(other);
		flush();
		assertTrue(text(), text().startsWith("## Other file"));
		assertFalse(frame.isModified());
		File html = new File(tmp.getRoot(), "page.html");
		Files.write(html.toPath(), "<h1>From html</h1><p>some <b>bold</b> text</p>".getBytes(StandardCharsets.UTF_8));
		frame.loadFile(html);
		flush();
		assertTrue("html converted to markdown: " + text(), text().contains("From html") && text().contains("**bold**"));
		assertTrue("converted text is unsaved", frame.isModified());
	}

	@Test
	public void conversionsBetweenMarkdownAndHtml() throws Exception {
		String html = frame.markdownToHtml("# T\n\n~~gone~~ and **bold**\n\n| a | b |\n|---|---|\n| 1 | 2 |\n\n- [x] done\n");
		assertTrue(html, html.contains("<h1") && html.contains("<del>gone</del>") && html.contains("<table>") && html.contains("checkbox"));
		String md = frame.htmlToMarkdown("<h2>Sub</h2><ul><li>one</li><li>two</li></ul><a href=\"http://x.y\">link</a>");
		assertTrue(md, md.contains("Sub") && md.contains("one") && md.contains("[link](http://x.y)"));
		assertNotNull(frame.getDefaultDimension());
	}

	@Test
	public void standaloneHtmlEmbedsLocalImages() throws Exception {
		File img = new File(tmp.getRoot(), "pic.png");
		java.awt.image.BufferedImage bi = new java.awt.image.BufferedImage(4, 4, java.awt.image.BufferedImage.TYPE_INT_RGB);
		javax.imageio.ImageIO.write(bi, "png", img);
		setText("# Page\n\n![pic](pic.png)\n\n[next](other.md)\n\n$x^2$\n", 0, 0);
		String page = (String) call("buildStandaloneHtml", new File(tmp.getRoot(), "export.html"));
		assertTrue(page, page.startsWith("<!DOCTYPE html>"));
		assertTrue("the image is inside the page: " + page.substring(0, Math.min(400, page.length())), page.contains("data:image/png;base64,"));
		assertTrue(page.contains("<title>notes</title>"));
		assertTrue("formulas bring KaTeX", page.contains("katex"));
		assertTrue(page.contains("other.md"));
	}

	@Test
	public void helpersOfTheEditor() throws Exception {
		assertEquals("a&lt;b&amp;c", call("escape", "a<b&c"));
		assertEquals("notes", call("baseName", mdFile));
		assertEquals("image/png", call("imageMime", "x.PNG"));
		call("setStatus", "hello");
	}
}
