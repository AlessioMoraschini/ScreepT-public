package markdown.viewer;

import java.awt.Image;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.imageio.ImageIO;

/**
 * Images from the clipboard (a screenshot, a copied picture) saved as png in the "images" folder next to the markdown file,
 * so that pasting them in the editor gives a working {@code ![image](images/...)} link.
 */
public final class ImagePaste {

	public static final String FOLDER = "images";

	/** Tests put their own clipboard here, so that they never touch the one of the user. */
	static java.awt.datatransfer.Clipboard testClipboard;

	private ImagePaste() {
	}

	static Clipboard clipboard() {
		return testClipboard != null ? testClipboard : java.awt.Toolkit.getDefaultToolkit().getSystemClipboard();
	}

	/** The image on the clipboard, null when there is none (or it cannot be read). */
	public static BufferedImage fromClipboard(Clipboard clipboard) {
		try {
			if(!clipboard.isDataFlavorAvailable(DataFlavor.imageFlavor))
				return null;
			return toBuffered(clipboard.getData(DataFlavor.imageFlavor));
		} catch (UnsupportedFlavorException | IOException | IllegalStateException e) {
			return null;
		}
	}

	static BufferedImage toBuffered(Object data) {
		if(!(data instanceof Image))
			return null;
		if(data instanceof BufferedImage)
			return (BufferedImage) data;
		Image image = (Image) data;
		int w = image.getWidth(null), h = image.getHeight(null);
		if(w <= 0 || h <= 0)
			return null;
		BufferedImage copy = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		copy.getGraphics().drawImage(image, 0, 0, null);
		return copy;
	}

	/** Saves the image as images/image-yyyyMMdd-HHmmss[-n].png (never overwrites) and returns the file. */
	public static File save(BufferedImage image, File baseDir) throws IOException {
		File folder = new File(baseDir, FOLDER);
		if(!folder.isDirectory() && !folder.mkdirs())
			throw new IOException("Cannot create " + folder);
		String stamp = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
		File file = new File(folder, "image-" + stamp + ".png");
		for(int n = 2; file.exists(); n++)
			file = new File(folder, "image-" + stamp + "-" + n + ".png");
		if(!ImageIO.write(image, "png", file))
			throw new IOException("No png writer for this image");
		return file;
	}

	/** Markdown of the saved image, relative to the markdown folder (spaces escaped). */
	public static String link(File baseDir, File image) {
		String relative = baseDir.getAbsoluteFile().toPath().relativize(image.getAbsoluteFile().toPath()).toString().replace('\\', '/');
		return "![image](" + relative.replace(" ", "%20") + ")";
	}

	/** Text and html at once: rich text editors (Word, Outlook, Gmail) take the html, plain text fields the markdown / text. */
	public static final class RichSelection implements Transferable {
		private static final DataFlavor HTML = html();
		private final String html;
		private final String text;

		public RichSelection(String html, String text) {
			this.html = html;
			this.text = text;
		}

		private static DataFlavor html() {
			try {
				return new DataFlavor("text/html;class=java.lang.String");
			} catch (ClassNotFoundException e) {
				return DataFlavor.stringFlavor;
			}
		}

		@Override
		public DataFlavor[] getTransferDataFlavors() {
			return new DataFlavor[] {HTML, DataFlavor.stringFlavor};
		}

		@Override
		public boolean isDataFlavorSupported(DataFlavor flavor) {
			return HTML.equals(flavor) || DataFlavor.stringFlavor.equals(flavor);
		}

		@Override
		public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
			if(HTML.equals(flavor))
				return html;
			if(DataFlavor.stringFlavor.equals(flavor))
				return text;
			throw new UnsupportedFlavorException(flavor);
		}
	}
}
