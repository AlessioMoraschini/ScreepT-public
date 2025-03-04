package various;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import gui.commons.frameutils.frame.ShellFrame;
import gui.commons.frameutils.frame.panels.ShellReadWritePanel;
import gui.commons.frameutils.frame.panels.arch.ParentPanel;
import resources.IconsPathConfigurator;
import various.common.light.gui.GuiUtils;

public class ShellFrameTest {

	public static void main(String[] args) {
		
		IconsPathConfigurator.ICON_PAUSE = "./test_resources/icons/pause.png";
		IconsPathConfigurator.ICON_PLAY = "./test_resources/icons/play.png";
		IconsPathConfigurator.ICON_STOP = "./test_resources/icons/Stop.png";
		IconsPathConfigurator.ICON_FILE_SAVE = "./test_resources/icons/saveFile.png";
		IconsPathConfigurator.ICON_FILE_SAVE_AS = "./test_resources/icons/saveFileAs.png";
		IconsPathConfigurator.ICON_FILE_LOAD = "./test_resources/icons/LoadFile.png";
		IconsPathConfigurator.ICON_FILE_NEW = "./test_resources/icons/NewFile.png";
		IconsPathConfigurator.ICON_PIN_RED = "./test_resources/icons/pinRed.png";
		IconsPathConfigurator.ICON_PIN_GREEN = "./test_resources/icons/pin.png";
		IconsPathConfigurator.ICON_TEXT_CLEAR = "./test_resources/icons/Clear.png";
		
		ParentPanel.logger.setLoggerAvailable(false);
		
		ShellFrame shellFrame = ShellFrame.getInstance(null, true);
		
		shellFrame.setMinimumSize(shellFrame.getMinimumDimension());
		shellFrame.setPreferredSize(shellFrame.getDefaultDimension());
		
		shellFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		
//		for(File curr : new File("./test_resources").listFiles()) {
//			System.out.println(curr);
//		}
		
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
//				}
				GuiUtils.centerComponent(shellFrame, shellFrame.getDefaultDimension());
				shellFrame.setLocationRelativeTo(null);
				((ShellReadWritePanel) shellFrame.payloadPanel).darker();
				((ShellReadWritePanel) shellFrame.payloadPanel).splitPane.setDividerLocation(150);
				shellFrame.setVisible(true);
				
				System.out.print("Hello this is a test text!");
			}
		});
	}
}
