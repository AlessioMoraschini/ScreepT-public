package plugin.external.root.texteditor;

import java.awt.Component;
import java.awt.KeyboardFocusManager;
import java.awt.KeyEventDispatcher;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.LookAndFeel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.text.JTextComponent;

import org.jmeld.settings.JMeldSettings;
import org.jmeld.ui.AbstractContentPanel;
import org.jmeld.ui.JMeldPanel;
import org.jmeld.ui.util.ImageUtil;
import org.jmeld.ui.util.LookAndFeelManager;
import org.jmeld.util.prefs.WindowPreference;

import various.common.light.files.FileWorker;

public class JMeldLauncher implements Runnable {

	/** Kept for compatibility: closing is now handled by the launcher (DO_NOTHING_ON_CLOSE + confirmation, then dispose). */
	public static int DEFAULT_CLOSE_OPTION = JFrame.DISPOSE_ON_CLOSE;

	private List<String> fileNameList;
	private static JMeldPanel jmeldPanel;

	/** Windows currently open: the temporary files are removed when the last one is closed. */
	private static int openWindows = 0;

	public JMeldLauncher(String args[]) {
		fileNameList = new ArrayList<>();
		String as[] = args;
		int i = as.length;
		for (int j = 0; j < i; j++) {
			String arg = as[j];
			fileNameList.add(arg);
		}

	}

	public static JMeldPanel getJMeldPanel() {
		return jmeldPanel;
	}

	@Override
	public void run() {
		// The look and feel is inherited from the main ScreepT window: JMeld must never redefine it.
		// (LookAndFeelManager is replaced by a version that does not switch it; the check below is a safety net)
		LookAndFeel hostLookAndFeel = UIManager.getLookAndFeel();
		LookAndFeelManager.getInstance().install();
		if (UIManager.getLookAndFeel() != hostLookAndFeel) {
			try {
				UIManager.setLookAndFeel(hostLookAndFeel);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		JFrame frame = new JFrame("JMeld");
		jmeldPanel = new JMeldPanel();
		frame.add(jmeldPanel);
		JMeldZoomSupport.install(frame, jmeldPanel);
		frame.setIconImage(ImageUtil.getImageIcon("jmeld-small").getImage());
		new WindowPreference(frame.getTitle(), frame);
		installClosing(frame, jmeldPanel);
		frame.setVisible(true);
		frame.toFront();
		jmeldPanel.openComparison(fileNameList);

	}

	// ---------------------------------------------------------------- closing

	/**
	 * Closing (window X or ESC): every changed document asks "Save selected / Don't save / Cancel"; Cancel
	 * keeps the window open. When the last window is closed the temporary files are deleted.
	 */
	static void installClosing(final JFrame frame, final JMeldPanel panel) {
		openWindows++;
		frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

		final KeyEventDispatcher escapeDispatcher = new KeyEventDispatcher() {
			@Override
			public boolean dispatchKeyEvent(KeyEvent e) {
				if (isCloseRequest(e, frame)) {
					e.consume();
					SwingUtilities.invokeLater(new Runnable() {
						@Override
						public void run() {
							requestClose(frame, panel);
						}
					});
					return true;
				}
				return false;
			}
		};
		KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(escapeDispatcher);

		frame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent we) {
				requestClose(frame, panel);
			}

			@Override
			public void windowClosed(WindowEvent we) {
				KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(escapeDispatcher);
				openWindows--;
				if (openWindows <= 0) {
					openWindows = 0;
					cleanupTempFiles();
				}
			}
		});
	}

	/**
	 * ESC closes the window, unless it is typed into a text field or a combo box (JMeld uses ESC to close
	 * its search bars) or into a dialog opened by the window.
	 */
	static boolean isCloseRequest(KeyEvent e, Window frame) {
		if (e.getID() != KeyEvent.KEY_PRESSED || e.getKeyCode() != KeyEvent.VK_ESCAPE || e.getModifiersEx() != 0 || e.isConsumed()) {
			return false;
		}
		Component source = e.getComponent();
		if (source == null) {
			return false;
		}
		Window window = source instanceof Window ? (Window) source : SwingUtilities.getWindowAncestor(source);
		if (window != frame) {
			return false;
		}
		if ((source instanceof JTextComponent && !isMultiLineEditor(source)) || source instanceof JComboBox
				|| SwingUtilities.getAncestorOfClass(JComboBox.class, source) != null) {
			return false;
		}
		return true;
	}

	private static boolean isMultiLineEditor(Component component) {
		return component instanceof javax.swing.JTextArea || component instanceof javax.swing.JEditorPane;
	}

	static void requestClose(JFrame frame, JMeldPanel panel) {
		for (AbstractContentPanel contentPanel : JMeldPanel.getContentPanelList(panel.getTabbedPane())) {
			if (!contentPanel.checkSave()) {
				return;
			}
		}
		frame.dispose();
	}

	private static void cleanupTempFiles() {
		try {
			File parent = new File(CodeComparePlugin.currentSessionTempFolder).getParentFile();
			if (parent != null) {
				FileWorker.cleanupDirs(parent.getAbsolutePath());
			}
		} catch (Throwable t) {
			t.printStackTrace();
		}
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
