package markdown.viewer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

import javax.imageio.ImageIO;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/** The document check (links, anchors, structure) and the image saved from the clipboard. */
public class MarkdownLintTest {

	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();

	private static String all(List<MarkdownLint.Issue> issues) {
		StringBuilder sb = new StringBuilder();
		for(MarkdownLint.Issue i : issues)
			sb.append(i).append('\n');
		return sb.toString();
	}

	private static boolean has(List<MarkdownLint.Issue> issues, int line, MarkdownLint.Level level, String text) {
		for(MarkdownLint.Issue i : issues) {
			if(i.line == line && i.level == level && i.message.contains(text))
				return true;
		}
		return false;
	}

	@Test
	public void cleanDocumentHasNoFindings() throws Exception {
		File dir = tmp.getRoot();
		Files.write(new File(dir, "other.md").toPath(), "# Other\n\n## Part two\n".getBytes(StandardCharsets.UTF_8));
		Files.write(new File(dir, "pic.png").toPath(), new byte[] {1});
		String md = "# Title\n\n## Intro\n\n[self](#intro) [file](other.md) [part](other.md#part-two) ![pic](pic.png \"t\") [web](https://example.com/x)\n\n"
				+ "[ref][r] and [r][] done\n\n[r]: https://example.com\n\n| a | b |\n| --- | --- |\n| 1 | 2 |\n";
		assertEquals(all(MarkdownLint.check(md, dir)), 0, MarkdownLint.check(md, dir).size());
	}

	@Test
	public void brokenLinksImagesAndAnchorsAreFound() throws Exception {
		File dir = tmp.getRoot();
		Files.write(new File(dir, "other.md").toPath(), "# Other\n".getBytes(StandardCharsets.UTF_8));
		String md = "# Title\n[a](missing.md)\n![x](nope.png)\n[b](#no-such)\n[c](other.md#ghost)\n[d]()\n![](other.md)\n[e](My%20File.md)\n";
		List<MarkdownLint.Issue> issues = MarkdownLint.check(md, dir);
		String text = all(issues);
		assertTrue(text, has(issues, 2, MarkdownLint.Level.ERROR, "File not found: missing.md"));
		assertTrue(text, has(issues, 3, MarkdownLint.Level.ERROR, "File not found: nope.png"));
		assertTrue(text, has(issues, 4, MarkdownLint.Level.ERROR, "#no-such"));
		assertTrue(text, has(issues, 5, MarkdownLint.Level.ERROR, "No heading #ghost"));
		assertTrue(text, has(issues, 6, MarkdownLint.Level.ERROR, "empty target"));
		assertTrue(text, has(issues, 7, MarkdownLint.Level.INFO, "without alternative text"));
		assertTrue(text, has(issues, 8, MarkdownLint.Level.ERROR, "My%20File.md"));
	}

	@Test
	public void unsavedFileChecksOnlyTheAnchors() {
		List<MarkdownLint.Issue> issues = MarkdownLint.check("# T\n[a](missing.md) [b](#t) [c](#x)\n", null);
		assertEquals(all(issues), 1, issues.size());
		assertTrue(all(issues), issues.get(0).message.contains("#x"));
	}

	@Test
	public void codeAndWebAndFrontMatterAreIgnored() {
		String md = "---\ntitle: [x](broken.md)\n---\n# T\n\n`[a](missing.md)` and ``[b](missing2.md)``\n\n```\n[c](missing3.md)\n```\n\n[w](https://x.y/z) [m](mailto:a@b.c) [d](data:text/plain,hi) [r](/abs/path)\n";
		List<MarkdownLint.Issue> issues = MarkdownLint.check(md, new File("."));
		assertEquals(all(issues), 0, issues.size());
	}

	@Test
	public void anchorsFollowTheGithubRules() {
		String md = "# Hello, World!\n\n## Same\n\n## Same\n\n<a name=\"manual\"></a>\n\n[1](#hello-world) [2](#same) [3](#same-1) [4](#manual) [5](#Hello-World)\n";
		List<MarkdownLint.Issue> issues = MarkdownLint.check(md, null);
		assertTrue(all(issues), !all(issues).contains("ERROR"));
	}

	@Test
	public void undefinedReferencesAndStructure() {
		String md = "# One\n\n### Skipped\n\n# Second title\n\n## Dup\n\n## Dup\n\n[x][nodef]\n";
		List<MarkdownLint.Issue> issues = MarkdownLint.check(md, null);
		String text = all(issues);
		assertTrue(text, has(issues, 3, MarkdownLint.Level.WARNING, "jumps from 1 to 3"));
		assertTrue(text, has(issues, 5, MarkdownLint.Level.INFO, "Second level 1"));
		assertTrue(text, has(issues, 9, MarkdownLint.Level.INFO, "Duplicate heading (same as line 7)"));
		assertTrue(text, has(issues, 11, MarkdownLint.Level.WARNING, "[nodef] has no definition"));
	}

	@Test
	public void unclosedFenceAndUnevenTables() {
		String md = "# T\n\n| a | b |\n| --- | --- |\n| 1 |\n| 1 | 2 | 3 |\n\n```java\nnever closed\n";
		List<MarkdownLint.Issue> issues = MarkdownLint.check(md, null);
		String text = all(issues);
		assertTrue(text, has(issues, 5, MarkdownLint.Level.WARNING, "1 cells, the header has 2"));
		assertTrue(text, has(issues, 6, MarkdownLint.Level.WARNING, "3 cells, the header has 2"));
		assertTrue(text, has(issues, 8, MarkdownLint.Level.ERROR, "never closed"));
		// sorted by line
		for(int i = 1; i < issues.size(); i++)
			assertTrue(issues.get(i - 1).line <= issues.get(i).line);
	}

	@Test
	public void linksInsideTablesAndCrLfLines() {
		String md = "# T\r\n\r\n| a |\r\n| --- |\r\n| [x](#gone) |\r\n";
		List<MarkdownLint.Issue> issues = MarkdownLint.check(md, null);
		assertTrue(all(issues), has(issues, 5, MarkdownLint.Level.ERROR, "#gone"));
	}

	// ------------------------------------------------------------------ images from the clipboard

	@Test
	public void imageIsSavedUnderImagesWithAWorkingLink() throws Exception {
		File dir = new File(tmp.getRoot(), "my notes");
		assertTrue(dir.mkdirs());
		BufferedImage image = new BufferedImage(4, 3, BufferedImage.TYPE_INT_ARGB);
		image.setRGB(1, 1, 0xFF336699);
		File first = ImagePaste.save(image, dir);
		File second = ImagePaste.save(image, dir);
		assertTrue(first.isFile() && second.isFile());
		assertTrue("never overwrites: " + first + " " + second, !first.equals(second));
		assertEquals(new File(dir, "images"), first.getParentFile());
		BufferedImage back = ImageIO.read(first);
		assertEquals(4, back.getWidth());
		assertEquals(0xFF336699, back.getRGB(1, 1));
		String link = ImagePaste.link(dir, first);
		assertTrue(link, link.matches("!\\[image\\]\\(images/image-\\d{8}-\\d{6}\\.png\\)"));
		// the link resolves, and the document check agrees
		assertEquals(0, MarkdownLint.check("# T\n\n" + link + "\n", dir).size());
		String spaced = ImagePaste.link(tmp.getRoot(), first);
		assertTrue(spaced, spaced.startsWith("![image](my%20notes/images/"));
		assertEquals(0, MarkdownLint.check("# T\n\n" + spaced + "\n", tmp.getRoot()).size());
	}

	@Test
	public void toBufferedConvertsPlainImages() {
		assertEquals(null, ImagePaste.toBuffered("text"));
		java.awt.Image plain = new BufferedImage(5, 2, BufferedImage.TYPE_INT_RGB).getScaledInstance(5, 2, java.awt.Image.SCALE_DEFAULT);
		BufferedImage converted = ImagePaste.toBuffered(plain);
		assertNotNull(converted);
		assertEquals(5, converted.getWidth());
	}

	@Test
	public void richSelectionOffersHtmlAndText() throws Exception {
		ImagePaste.RichSelection rich = new ImagePaste.RichSelection("<html><body><b>x</b></body></html>", "**x**");
		DataFlavor html = new DataFlavor("text/html;class=java.lang.String");
		Transferable t = rich;
		assertTrue(t.isDataFlavorSupported(html));
		assertTrue(t.isDataFlavorSupported(DataFlavor.stringFlavor));
		assertEquals("<html><body><b>x</b></body></html>", t.getTransferData(html));
		assertEquals("**x**", t.getTransferData(DataFlavor.stringFlavor));
		assertTrue(!t.isDataFlavorSupported(DataFlavor.imageFlavor));
	}
}
