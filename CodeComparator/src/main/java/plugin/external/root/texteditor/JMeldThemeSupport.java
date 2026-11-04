package plugin.external.root.texteditor;

import java.awt.Color;

import javax.swing.UIManager;

import org.jmeld.settings.EditorSettings;
import org.jmeld.settings.JMeldSettings;
import org.jmeld.ui.util.Colors;

/**
 * JMeld highlights added / changed / deleted lines with pastel colors made for a light theme:
 * with a dark look and feel (white text) they become unreadable. The dark variants are used
 * only when the inherited look and feel is dark, and only while the colors are still the
 * default ones: a color chosen by the user in the JMeld settings is never overwritten.
 */
public final class JMeldThemeSupport {

	static final Color DARK_ADDED = new Color(31, 77, 43);
	static final Color DARK_CHANGED = new Color(31, 63, 110);
	static final Color DARK_DELETED = new Color(110, 42, 54);

	private JMeldThemeSupport() {
	}

	public static boolean isDarkLookAndFeel() {
		Color background = UIManager.getColor("TextArea.background");
		if (background == null) {
			background = UIManager.getColor("Panel.background");
		}
		if (background == null) {
			return false;
		}
		double luminance = (0.299 * background.getRed() + 0.587 * background.getGreen() + 0.114 * background.getBlue()) / 255d;
		return luminance < 0.5;
	}

	/** Chooses default or dark diff colors according to the inherited look and feel. */
	public static void adaptDiffColors() {
		try {
			EditorSettings editor = JMeldSettings.getInstance().getEditor();
			boolean untouched = isOneOf(editor.getAddedColor(), Colors.ADDED, DARK_ADDED)
					&& isOneOf(editor.getChangedColor(), Colors.CHANGED, DARK_CHANGED)
					&& isOneOf(editor.getDeletedColor(), Colors.DELETED, DARK_DELETED);
			if (!untouched) {
				return;
			}

			if (isDarkLookAndFeel()) {
				editor.setAddedColor(DARK_ADDED);
				editor.setChangedColor(DARK_CHANGED);
				editor.setDeletedColor(DARK_DELETED);
			} else {
				editor.setAddedColor(Colors.ADDED);
				editor.setChangedColor(Colors.CHANGED);
				editor.setDeletedColor(Colors.DELETED);
			}
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}

	private static boolean isOneOf(Color color, Color first, Color second) {
		return color != null && (color.equals(first) || color.equals(second));
	}
}
