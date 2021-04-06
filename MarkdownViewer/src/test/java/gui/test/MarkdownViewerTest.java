package gui.test;

import java.io.File;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import markdown.viewer.MarkdownViewerFrame;
import various.common.light.files.FileVarious;

public class MarkdownViewerTest {

	static final String htmlTest = "<!DOCTYPE html>\r\n" +
			"<html>\r\n" +
			"<body>\r\n" +
			"\r\n" +
			"<h1>My First Heading</h1>\r\n" +
			"<p>My first paragraph.</p>\r\n" +
			"\r\n" +
			"</body>\r\n" +
			"</html>";
	static final String markdownTest = "An h1 header\r\n" +
			"============\r\n" +
			"\r\n" +
			"Paragraphs are separated by a blank line.\r\n" +
			"\r\n" +
			"2nd paragraph. *Italic*, **bold**, and `monospace`. Itemized lists\r\n" +
			"look like:\r\n" +
			"\r\n" +
			"  * this one\r\n" +
			"  * that one\r\n" +
			"  * the other one\r\n" +
			"\r\n" +
			"Note that --- not considering the asterisk --- the actual text\r\n" +
			"content starts at 4-columns in.\r\n" +
			"\r\n" +
			"> Block quotes are\r\n" +
			"> written like so.\r\n" +
			">\r\n" +
			"> They can span multiple paragraphs,\r\n" +
			"> if you like.";

	public static void main(String[] args) {
		SwingUtilities.invokeLater(()->{
			MarkdownViewerFrame.logger.setLoggerAvailable(false);
			MarkdownViewerFrame frame = new MarkdownViewerFrame(htmlTest, markdownTest);
			frame.setVisible(true);
			frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE;
		});
	}

	public static void testFiles(List<File> files, boolean markdownType) {
		String markdown = files!= null && files.size() > 0 ? FileVarious.getCanonicalPathSafe(files.get(0)) : null;
		if(markdownType)
			main(new String[] {null, markdown});
		else
			main(new String[] {markdown, null});

	}

}
