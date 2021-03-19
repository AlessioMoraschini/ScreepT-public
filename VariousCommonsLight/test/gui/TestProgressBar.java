package gui;

import org.junit.Test;
import gui.UpdateProgressPanel;

public class TestProgressBar {
	
	@Test
	public void testAutoResize() throws InterruptedException {
		
		UpdateProgressPanel progressUpdater = new UpdateProgressPanel(false);
		progressUpdater.update("Test number one", 20);
		progressUpdater.adaptWidthToCurrentText();
		Thread.sleep(1000);

		progressUpdater.update("Test number two, let's have a longer string", 40);
		progressUpdater.adaptWidthToCurrentText();
		Thread.sleep(1000);
		
		progressUpdater.update("Test number two, let's have a longer string and something more", 60);
		progressUpdater.adaptWidthToCurrentText();
		Thread.sleep(1000);
		
		progressUpdater.update("Test number two, let's have a longer string and something more, and more again", 80);
		progressUpdater.adaptWidthToCurrentText();
		Thread.sleep(1000);
		
		progressUpdater.update("Test number two, let's have a longer string and something more, and more again. This is the longest one!!!", 99);
		progressUpdater.adaptWidthToCurrentText();
		Thread.sleep(4000);
		
		progressUpdater.forceClose();
	}

}
