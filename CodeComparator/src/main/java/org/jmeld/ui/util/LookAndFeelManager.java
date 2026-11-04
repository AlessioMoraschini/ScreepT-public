package org.jmeld.ui.util;

import java.util.ArrayList;
import java.util.List;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import com.jidesoft.plaf.LookAndFeelFactory;

import org.jmeld.JMeld;

/**
 * ScreepT replacement of the original JMeld class (same package and name, so it takes
 * the place of the one inside the JMeld jar).
 *
 * The original installed its own look and feel (JGoodies Plastic) in the whole JVM, so
 * opening a comparison changed the look of the main ScreepT window as well. Here the look
 * and feel is never changed: the host application's one is inherited, and JMeld only
 * extends it with the Jide component defaults it needs.
 */
public class LookAndFeelManager {

	private static LookAndFeelManager instance;

	private LookAndFeelManager() {
	}

	public static synchronized LookAndFeelManager getInstance() {
		if (instance == null) {
			instance = new LookAndFeelManager();
		}
		return instance;
	}

	/**
	 * Does NOT change the look and feel: only completes the current one with the Jide defaults.
	 */
	public void install() {
		try {
			LookAndFeelFactory.installJideExtension();

			java.awt.Component root = SwingUtilities.getRoot(JMeld.getJMeldPanel());
			if (root != null) {
				SwingUtilities.updateComponentTreeUI(root);
			}
		} catch (Throwable e) {
			// the host look and feel stays in place whatever happens here
		}
	}

	public String getInstalledLookAndFeelName() {
		return UIManager.getLookAndFeel().getName();
	}

	/**
	 * Only the inherited look and feel is offered: the JMeld settings combo box has nothing to switch to.
	 */
	public List<String> getInstalledLookAndFeels() {
		List<String> result = new ArrayList<String>();
		result.add(getInstalledLookAndFeelName());
		return result;
	}
}
