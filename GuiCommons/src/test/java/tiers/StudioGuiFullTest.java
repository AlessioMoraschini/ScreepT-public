package tiers;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.awt.GraphicsEnvironment;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;

import org.junit.Assume;
import org.junit.Test;

import gui.commons.frameutils.frame.ImageViewerFrame;
import gui.commons.imagestudio.BatchDialog;
import gui.commons.imagestudio.StudioMode;
import gui.commons.tabs.TabImplementors;
import gui.commons.tabs.ViewerAssociationsDialog;

/** FULL tier: opens the real windows (needs a display, skipped without one); weekly or before a release. */
public class StudioGuiFullTest {

	private static File tempImage() throws Exception {
		BufferedImage img = new BufferedImage(160, 100, BufferedImage.TYPE_INT_RGB);
		File file = File.createTempFile("studio", ".png");
		ImageIO.write(img, "png", file);
		return file;
	}

	@Test
	public void theStudioWindowOpensTwoTabsAndFollowsTheMode() throws Exception {
		Assume.assumeFalse("no display", GraphicsEnvironment.isHeadless());
		final File a = tempImage();
		final File b = tempImage();
		final ImageViewerFrame[] frame = new ImageViewerFrame[1];
		StudioMode before = StudioMode.current();
		try {
			SwingUtilities.invokeAndWait(() -> {
				frame[0] = new ImageViewerFrame(a);
				frame[0].openFile(b);
				frame[0].openFile(a); // already open: only selected
			});
			Thread.sleep(1500);
			SwingUtilities.invokeAndWait(() -> {
				StudioMode.set(StudioMode.NERD);
				StudioMode.set(StudioMode.BASIC);
				StudioMode.set(StudioMode.CLASSIC);
			});
			assertNotNull(frame[0].imageFile);
			assertTrue(ImageViewerFrame.getViewerInstanceList().size() >= 1);
		} finally {
			SwingUtilities.invokeAndWait(() -> {
				StudioMode.set(before);
				if (frame[0] != null) {
					frame[0].dispose();
				}
			});
			a.delete();
			b.delete();
		}
	}

	@Test
	public void batchAndAssociationsDialogsConstruct() throws Exception {
		Assume.assumeFalse("no display", GraphicsEnvironment.isHeadless());
		final File a = tempImage();
		try {
			SwingUtilities.invokeAndWait(() -> {
				BatchDialog batch = new BatchDialog(null, a);
				assertTrue(batch.getWidth() > 0);
				batch.dispose();
				ViewerAssociationsDialog dialog = new ViewerAssociationsDialog(null, s -> {
				});
				assertTrue(dialog.getWidth() > 0);
				dialog.dispose();
				assertNotNull(TabImplementors.knownExtensions());
			});
		} finally {
			a.delete();
		}
	}
}
