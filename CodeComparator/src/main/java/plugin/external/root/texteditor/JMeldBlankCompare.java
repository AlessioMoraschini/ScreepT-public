package plugin.external.root.texteditor;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JOptionPane;

import org.jmeld.ui.JMeldPanel;
import org.jmeld.ui.util.ImageUtil;
import org.jmeld.ui.util.WidgetFactory;

/**
 * "Blank compare": JMeld's "New" button only compares files that already exist. This opens two empty
 * temporary documents side by side, to type or paste text on both sides. The temporary files live in the
 * plugin session folder and are deleted when the last Code Compare window is closed.
 */
public final class JMeldBlankCompare {

	private static final AtomicInteger counter = new AtomicInteger(0);

	private JMeldBlankCompare() {
	}

	/** Toolbar button, same look as the other JMeld toolbar buttons. */
	public static JButton createButton(final JMeldPanel panel) {
		AbstractAction action = new AbstractAction("Blank") {
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				open(panel);
			}
		};
		action.putValue(Action.SHORT_DESCRIPTION, "Compare two empty documents: type or paste text on both sides (temporary files, deleted on close)");
		ImageIcon small = ImageUtil.getSmallImageIcon("stock_edit");
		ImageIcon large = ImageUtil.getImageIcon("stock_edit");
		action.putValue(Action.SMALL_ICON, small);
		action.putValue(Action.LARGE_ICON_KEY, large);
		return WidgetFactory.getToolBarButton(action);
	}

	public static void open(JMeldPanel panel) {
		try {
			File folder = new File(CodeComparePlugin.currentSessionTempFolder);
			folder.mkdirs();

			int number = counter.incrementAndGet();
			File left = new File(folder, "Left_empty_" + number + ".txt");
			File right = new File(folder, "Right_empty_" + number + ".txt");
			left.createNewFile();
			right.createNewFile();
			left.deleteOnExit();
			right.deleteOnExit();

			panel.openComparison(left.getAbsolutePath(), right.getAbsolutePath());
		} catch (IOException ex) {
			JOptionPane.showMessageDialog(panel, "Cannot create the temporary files: " + ex.getMessage(), "Blank compare", JOptionPane.ERROR_MESSAGE);
		}
	}
}
