package plugin.external.root.texteditor;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import org.jmeld.settings.JMeldSettings;
import org.jmeld.ui.AbstractContentPanel;
import org.jmeld.ui.JMeldContentPanelIF;
import org.jmeld.ui.JMeldPanel;
import org.jmeld.ui.util.ImageUtil;
import org.jmeld.ui.util.LookAndFeelManager;
import org.jmeld.util.prefs.WindowPreference;

public class JMeldLauncher implements Runnable {

	private List<String> fileNameList;
	private static JMeldPanel jmeldPanel;

	public JMeldLauncher(String args[]) {
		fileNameList = new ArrayList<>();
		String as[] = args;
		int i = as.length;
		for (int j = 0; j < i; j++) {
			String arg = as[j];
			fileNameList.add(arg);
		}

	}

	public WindowListener getWindowListener() {
		return new WindowAdapter() {

			@Override
			public void windowClosing(WindowEvent we) {
				for (Iterator<AbstractContentPanel> iterator = JMeldPanel.getContentPanelList(getJMeldPanel().getTabbedPane()).iterator(); iterator
						.hasNext();) {
					JMeldContentPanelIF contentPanel = iterator.next();
					if (!contentPanel.checkSave())
						return;
				}
			}
		};
	}

	public static JMeldPanel getJMeldPanel() {
		return jmeldPanel;
	}

	@Override
	public void run() {
		LookAndFeelManager.getInstance().install();
		JFrame frame = new JFrame("JMeld");
		jmeldPanel = new JMeldPanel();
		frame.add(jmeldPanel);
		frame.setIconImage(ImageUtil.getImageIcon("jmeld-small").getImage());
		new WindowPreference(frame.getTitle(), frame);
		frame.addWindowListener(getWindowListener());
		frame.setVisible(true);
		frame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
		frame.toFront();
		jmeldPanel.openComparison(fileNameList);
	}

	public static void main(String args[]) {
		JMeldSettings settings = JMeldSettings.getInstance();
		settings.getEditor().setShowLineNumbers(true);
		settings.setDrawCurves(true);
		settings.setCurveType(1);
		if (settings.getEditor().isAntialiasEnabled())
			System.setProperty("swing.aatext", "true");
		SwingUtilities.invokeLater(new JMeldLauncher(args));
	}
}