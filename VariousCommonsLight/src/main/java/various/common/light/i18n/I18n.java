/**
 *
 * =========================================================================================
 *  Copyright (C) 2019-2020
 *
 *  AM-Design-Development - (Alessio Moraschini) - All Rights Reserved
 * =========================================================================================
 *
 * You should have received a copy of the license with this file.
 * If not, please write to: info@am-design-development.com, or visit : https://www.am-design-development.com
 */
package various.common.light.i18n;

import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.HierarchyEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;

import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.TitledBorder;

/**
 * Translation of the whole interface with plain Java, no dependencies.
 * <p>
 * The texts of the code are the English ones and they are also the keys: a language pack is a UTF-8 text file
 * {@code /i18n/<code>.txt} in the resources, one line per text: {@code english<TAB>translation} ({@code \n} is a new line,
 * lines that start with # are comments). Every Swing component that is shown (label, button, menu, tooltip, tab title,
 * titled border, window title) is translated by a global listener, so the code does not need to call anything; a text
 * missing from the pack stays in English. Switching language translates again the windows that are already open.
 * <p>
 * RULE: every new or changed user text must also be added to every pack in {@code /i18n}.
 */
public final class I18n {

	/** A language of the application: ISO code and its name written in that language */
	public static final class Language {
		public final String code;
		public final String nativeName;

		Language(String code, String nativeName) {
			this.code = code;
			this.nativeName = nativeName;
		}

		@Override
		public String toString() {
			return nativeName;
		}
	}

	public static final List<Language> LANGUAGES = Collections.unmodifiableList(java.util.Arrays.asList(
			new Language("en", "English"),
			new Language("it", "Italiano"),
			new Language("es", "Español"),
			new Language("fr", "Français"),
			new Language("de", "Deutsch"),
			new Language("pt", "Português"),
			new Language("zh", "中文 (简体)")));

	private static final String SOURCE_TEXT = "i18n.text";
	private static final String LAST_TEXT = "i18n.text.last";
	private static final String SOURCE_TIP = "i18n.tip";
	private static final String LAST_TIP = "i18n.tip.last";
	private static final String SOURCE_BORDER = "i18n.border";
	private static final String LAST_BORDER = "i18n.border.last";
	private static final String SOURCE_PLACEHOLDER = "i18n.placeholder";
	private static final String LAST_PLACEHOLDER = "i18n.placeholder.last";
	private static final String SOURCE_TABS = "i18n.tabs";
	private static final String HOOKED = "i18n.hooked";

	private static volatile String current = "en";
	private static volatile Map<String, String> table = Collections.emptyMap();
	/** Texts that start with a fixed part followed by something dynamic (a file name, a number...): pack lines that start with ~ */
	private static volatile List<String[]> prefixes = Collections.emptyList();
	private static boolean installed = false;
	private static boolean applying = false;
	private static final Map<Window, String[]> WINDOW_TITLES = new WeakHashMap<Window, String[]>();

	private I18n() {
	}

	/** "it_IT", "IT", "it-it" -> "it"; anything not supported -> "en" */
	public static String normalize(String code) {
		if (code == null) {
			return "en";
		}
		String c = code.trim().toLowerCase(Locale.ROOT);
		int cut = 0;
		while (cut < c.length() && Character.isLetter(c.charAt(cut))) {
			cut++;
		}
		c = c.substring(0, cut);
		for (Language language : LANGUAGES) {
			if (language.code.equals(c)) {
				return c;
			}
		}
		return "en";
	}

	public static String getLanguage() {
		return current;
	}

	public static Language getLanguageInfo(String code) {
		String c = normalize(code);
		for (Language language : LANGUAGES) {
			if (language.code.equals(c)) {
				return language;
			}
		}
		return LANGUAGES.get(0);
	}

	/** Changes the language of the application, translating also the windows that are already open */
	public static synchronized void setLanguage(String code) {
		current = normalize(code);
		Map<String, String> loaded = "en".equals(current) ? Collections.<String, String>emptyMap() : load(current);
		List<String[]> prefixList = new ArrayList<String[]>();
		for (Map.Entry<String, String> entry : loaded.entrySet()) {
			if (entry.getKey().startsWith("~")) {
				prefixList.add(new String[] {entry.getKey().substring(1), entry.getValue()});
			}
		}
		prefixList.sort((a, b) -> b[0].length() - a[0].length());
		// case insensitive fallback: a text that differs only in capitals from a key of the pack
		Map<String, String> lowerCase = new HashMap<String, String>();
		for (Map.Entry<String, String> entry : loaded.entrySet()) {
			if (!entry.getKey().startsWith("~")) {
				lowerCase.put("#lc#" + entry.getKey().toLowerCase(Locale.ROOT), entry.getValue());
			}
		}
		loaded.putAll(lowerCase);
		prefixes = prefixList;
		table = loaded;
		installButtonsTexts();
		install();
		if (SwingUtilities.isEventDispatchThread()) {
			retranslateOpenWindows();
		} else {
			SwingUtilities.invokeLater(I18n::retranslateOpenWindows);
		}
	}

	/** Text translated in the current language, or the same text if it is not in the pack */
	public static String tr(String english) {
		return translateText(english);
	}

	// ------------------------------------------------------------------ packs

	private static Map<String, String> load(String code) {
		Map<String, String> map = new HashMap<String, String>();
		try (InputStream in = I18n.class.getResourceAsStream("/i18n/" + code + ".txt")) {
			if (in == null) {
				return map;
			}
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
				String line;
				while ((line = reader.readLine()) != null) {
					if (line.isEmpty() || line.charAt(0) == '#') {
						continue;
					}
					int tab = line.indexOf('\t');
					if (tab <= 0) {
						continue;
					}
					map.put(line.substring(0, tab).replace("\\n", "\n"), line.substring(tab + 1).replace("\\n", "\n"));
				}
			}
		} catch (Exception e) {
			// no pack: English
		}
		return map;
	}

	private static void installButtonsTexts() {
		String[][] keys = {
				{"OptionPane.yesButtonText", "Yes"}, {"OptionPane.noButtonText", "No"},
				{"OptionPane.okButtonText", "OK"}, {"OptionPane.cancelButtonText", "Cancel"},
				{"OptionPane.titleText", "Select an Option"}, {"OptionPane.messageDialogTitle", "Message"},
				{"OptionPane.inputDialogTitle", "Input"}, {"OptionPane.yesButtonText", "Yes"}};
		for (String[] pair : keys) {
			UIManager.put(pair[0], translateText(pair[1]));
		}
	}

	// ------------------------------------------------------------------ text lookup

	private static String translateText(String text) {
		Map<String, String> map = table;
		if (text == null || text.isEmpty() || map.isEmpty()) {
			return text;
		}
		int a = 0;
		int b = text.length();
		while (a < b && Character.isWhitespace(text.charAt(a))) {
			a++;
		}
		while (b > a && Character.isWhitespace(text.charAt(b - 1))) {
			b--;
		}
		if (a >= b) {
			return text;
		}
		String core = text.substring(a, b);
		String found = lookup(map, core);
		if (found == null && core.regionMatches(true, 0, "<html>", 0, 6)) {
			// <html>text</html> with the same text in the pack
			String inner = core.substring(6);
			String closing = "";
			if (inner.toLowerCase(Locale.ROOT).endsWith("</html>")) {
				closing = inner.substring(inner.length() - 7);
				inner = inner.substring(0, inner.length() - 7);
			}
			String innerFound = lookup(map, inner.trim());
			if (innerFound != null) {
				found = core.substring(0, 6) + innerFound + closing;
			}
		}
		if (found == null && (core.indexOf('<') >= 0 && core.indexOf('>') > core.indexOf('<'))) {
			found = translateMarkup(map, core);
		}
		if (found == null) {
			for (String[] prefix : prefixes) {
				if (core.length() > prefix[0].length() && core.startsWith(prefix[0])) {
					found = prefix[1] + core.substring(prefix[0].length());
					break;
				}
			}
		}
		if (found == null) {
			found = translatePieces(map, core);
		}
		return found == null ? text : text.substring(0, a) + found + text.substring(b);
	}

	/** Messages made of several lines or sentences: every line / sentence found in the pack is translated, the rest stays */
	private static String translatePieces(Map<String, String> map, String core) {
		boolean multiline = core.indexOf('\n') >= 0;
		String[] pieces = multiline ? core.split("(?<=\n)") : SENTENCES.split(core);
		if (pieces.length < 2) {
			return null;
		}
		StringBuilder out = new StringBuilder(core.length() + 16);
		boolean changed = false;
		int position = 0;
		for (String piece : pieces) {
			String trimmed = piece.trim();
			String found = trimmed.isEmpty() ? null : lookup(map, trimmed);
			int start = core.indexOf(piece, position);
			out.append(core, position, start);
			if (found != null) {
				int inner = piece.indexOf(trimmed);
				out.append(piece, 0, inner).append(found).append(piece.substring(inner + trimmed.length()));
				changed = true;
			} else {
				out.append(piece);
			}
			position = start + piece.length();
		}
		out.append(core.substring(position));
		return changed ? out.toString() : null;
	}

	/** HTML texts (tooltips, messages): every piece of text between the tags is translated on its own */
	private static String translateMarkup(Map<String, String> map, String html) {
		StringBuilder out = new StringBuilder(html.length() + 16);
		boolean changed = false;
		int i = 0;
		while (i < html.length()) {
			int open = html.indexOf('<', i);
			int close = open < 0 ? -1 : html.indexOf('>', open);
			if (open < 0 || close < 0) {
				changed |= appendTranslated(map, out, html.substring(i));
				break;
			}
			changed |= appendTranslated(map, out, html.substring(i, open));
			out.append(html, open, close + 1);
			i = close + 1;
		}
		return changed ? out.toString() : null;
	}

	private static boolean appendTranslated(Map<String, String> map, StringBuilder out, String piece) {
		String core = piece.trim();
		if (core.isEmpty()) {
			out.append(piece);
			return false;
		}
		String found = lookup(map, core);
		if (found == null) {
			out.append(piece);
			return false;
		}
		int start = piece.indexOf(core);
		out.append(piece, 0, start).append(found).append(piece.substring(start + core.length()));
		return true;
	}

	private static final java.util.regex.Pattern SHORTCUT = java.util.regex.Pattern.compile("^(\\[[^\\]]{1,40}\\]\\s*)(.+)$", java.util.regex.Pattern.DOTALL);
	private static final java.util.regex.Pattern SENTENCES = java.util.regex.Pattern.compile("(?<=[.!?])\\s+");

	private static String lookup(Map<String, String> map, String core) {
		String found = map.get(core);
		if (found != null) {
			return found;
		}
		java.util.regex.Matcher shortcut = SHORTCUT.matcher(core);
		if (shortcut.matches()) {
			// "[Ctrl + S] Save current file": the key is the description, the shortcut stays as it is
			String description = lookup(map, shortcut.group(2).trim());
			if (description != null) {
				return shortcut.group(1) + description;
			}
		}
		String lower = map.get("#lc#" + core.toLowerCase(Locale.ROOT));
		if (lower != null) {
			return lower;
		}
		for (String suffix : new String[] {":", "...", "…", " *", " (*)"}) {
			if (core.endsWith(suffix) && core.length() > suffix.length()) {
				String base = map.get(core.substring(0, core.length() - suffix.length()).trim());
				if (base != null) {
					return base + suffix;
				}
			}
		}
		return null;
	}

	// ------------------------------------------------------------------ components

	private static synchronized void install() {
		if (installed) {
			return;
		}
		installed = true;
		Toolkit.getDefaultToolkit().addAWTEventListener(event -> {
			if (table.isEmpty() || !(event instanceof HierarchyEvent)) {
				return;
			}
			HierarchyEvent h = (HierarchyEvent) event;
			if ((h.getChangeFlags() & (HierarchyEvent.PARENT_CHANGED | HierarchyEvent.SHOWING_CHANGED | HierarchyEvent.DISPLAYABILITY_CHANGED)) != 0) {
				translateComponent(h.getComponent());
			}
		}, AWTEvent.HIERARCHY_EVENT_MASK);
	}

	private static void retranslateOpenWindows() {
		for (Window window : Window.getWindows()) {
			translateTree(window);
		}
	}

	private static void translateTree(Component component) {
		translateComponent(component);
		if (component instanceof JMenu) {
			for (Component item : ((JMenu) component).getMenuComponents()) {
				translateTree(item);
			}
		}
		if (component instanceof javax.swing.JPopupMenu) {
			for (Component item : ((javax.swing.JPopupMenu) component).getComponents()) {
				translateTree(item);
			}
		}
		if (component instanceof Container) {
			for (Component child : ((Container) component).getComponents()) {
				translateTree(child);
			}
		}
		if (component instanceof JComponent) {
			javax.swing.JPopupMenu popup = ((JComponent) component).getComponentPopupMenu();
			if (popup != null) {
				translateTree(popup);
			}
		}
	}

	private static void translateComponent(Component component) {
		if (component == null || applying) {
			return;
		}
		applying = true;
		try {
			if (component instanceof Window) {
				translateWindowTitle((Window) component);
			}
			if (component instanceof JComponent) {
				JComponent c = (JComponent) component;
				translateText(c);
				translateProperty(c, SOURCE_TIP, LAST_TIP, c.getToolTipText(), (value) -> c.setToolTipText(value));
				Object placeholder = c.getClientProperty("JTextField.placeholderText");
				if (placeholder instanceof String || c.getClientProperty(SOURCE_PLACEHOLDER) != null) {
					translateProperty(c, SOURCE_PLACEHOLDER, LAST_PLACEHOLDER, (String) placeholder, (value) -> c.putClientProperty("JTextField.placeholderText", value));
				}
				translateBorder(c);
				if (c instanceof JTabbedPane) {
					translateTabs((JTabbedPane) c);
				}
				hook(c);
			}
		} finally {
			applying = false;
		}
	}

	private static String getText(JComponent c) {
		if (c instanceof JLabel) {
			return ((JLabel) c).getText();
		}
		if (c instanceof AbstractButton) {
			return ((AbstractButton) c).getText();
		}
		return null;
	}

	private static void setText(JComponent c, String text) {
		if (c instanceof JLabel) {
			((JLabel) c).setText(text);
		} else if (c instanceof AbstractButton) {
			((AbstractButton) c).setText(text);
		}
	}

	private static void translateText(JComponent c) {
		translateProperty(c, SOURCE_TEXT, LAST_TEXT, getText(c), (value) -> setText(c, value));
		fixFont(c, getText(c));
	}

	private static final String ORIGINAL_FONT = "i18n.font";
	private static volatile String cjkFamily;

	/** The fonts of the interface have no Chinese characters: for those texts a font that has them is used, the original one comes back with the other languages */
	private static void fixFont(JComponent c, String shown) {
		Font now = c.getFont();
		if (now == null) {
			return;
		}
		Font original = (Font) c.getClientProperty(ORIGINAL_FONT);
		Font base = original != null ? original : now;
		if (shown != null && base.canDisplayUpTo(shown) != -1) {
			String family = cjkFamily();
			if (family != null) {
				c.putClientProperty(ORIGINAL_FONT, base);
				c.setFont(new Font(family, base.getStyle(), base.getSize()));
			}
		} else if (original != null) {
			c.setFont(original);
			c.putClientProperty(ORIGINAL_FONT, null);
		}
	}

	private static String cjkFamily() {
		if (cjkFamily == null) {
			java.util.Set<String> installed = new java.util.HashSet<String>(java.util.Arrays.asList(
					java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()));
			String found = Font.DIALOG;
			for (String candidate : new String[] {"Microsoft YaHei UI", "Microsoft YaHei", "SimSun", "Noto Sans CJK SC", "PingFang SC", "WenQuanYi Micro Hei"}) {
				if (installed.contains(candidate)) {
					found = candidate;
					break;
				}
			}
			cjkFamily = found;
		}
		return cjkFamily;
	}

	/**
	 * Keeps the English source of a text in the component, so that the same text can be translated again in another
	 * language; a text that the code sets later is a new source.
	 */
	private static void translateProperty(JComponent c, String sourceKey, String lastKey, String currentValue, java.util.function.Consumer<String> setter) {
		if (currentValue == null) {
			c.putClientProperty(sourceKey, null);
			c.putClientProperty(lastKey, null);
			return;
		}
		String source = (String) c.getClientProperty(sourceKey);
		String last = (String) c.getClientProperty(lastKey);
		if (source == null || !Objects.equals(currentValue, last)) {
			source = currentValue; // first time, or the code has set a new text
		}
		String translated = translateText(source);
		c.putClientProperty(sourceKey, source);
		if (!Objects.equals(translated, currentValue)) {
			setter.accept(translated);
		}
		c.putClientProperty(lastKey, translated);
	}

	private static void translateBorder(JComponent c) {
		TitledBorder titled = findTitled(c.getBorder());
		if (titled == null) {
			return;
		}
		String source = (String) c.getClientProperty(SOURCE_BORDER);
		String last = (String) c.getClientProperty(LAST_BORDER);
		String now = titled.getTitle();
		if (source == null || !Objects.equals(now, last)) {
			source = now;
		}
		String translated = translateText(source);
		c.putClientProperty(SOURCE_BORDER, source);
		if (!Objects.equals(translated, now)) {
			titled.setTitle(translated);
			c.repaint();
		}
		c.putClientProperty(LAST_BORDER, translated);
	}

	private static TitledBorder findTitled(Border border) {
		if (border instanceof TitledBorder) {
			return (TitledBorder) border;
		}
		if (border instanceof CompoundBorder) {
			TitledBorder outside = findTitled(((CompoundBorder) border).getOutsideBorder());
			return outside != null ? outside : findTitled(((CompoundBorder) border).getInsideBorder());
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	private static void translateTabs(JTabbedPane tabs) {
		List<String[]> memory = (List<String[]>) tabs.getClientProperty(SOURCE_TABS);
		if (memory == null) {
			memory = new ArrayList<String[]>();
			tabs.putClientProperty(SOURCE_TABS, memory);
		}
		for (int i = 0; i < tabs.getTabCount(); i++) {
			if (i >= memory.size()) {
				memory.add(new String[] {null, null});
			}
			String now = tabs.getTitleAt(i);
			if (now == null || tabs.getTabComponentAt(i) != null) {
				continue; // custom tab header: its labels are translated by themselves
			}
			String[] entry = memory.get(i);
			if (entry[0] == null || !Objects.equals(now, entry[1])) {
				entry[0] = now;
			}
			String translated = translateText(entry[0]);
			if (!Objects.equals(translated, now)) {
				tabs.setTitleAt(i, translated);
			}
			entry[1] = translated;
		}
	}

	private static void translateWindowTitle(Window window) {
		String now = window instanceof Frame ? ((Frame) window).getTitle() : window instanceof Dialog ? ((Dialog) window).getTitle() : null;
		if (now == null) {
			return;
		}
		String[] entry = WINDOW_TITLES.get(window);
		if (entry == null) {
			entry = new String[] {now, now};
			WINDOW_TITLES.put(window, entry);
		}
		if (!Objects.equals(now, entry[1])) {
			entry[0] = now;
		}
		String translated = translateText(entry[0]);
		if (!Objects.equals(translated, now)) {
			if (window instanceof Frame) {
				((Frame) window).setTitle(translated);
			} else {
				((Dialog) window).setTitle(translated);
			}
		}
		entry[1] = translated;
	}

	/** A text that the code sets after the component is shown is translated too */
	private static void hook(JComponent c) {
		if (Boolean.TRUE.equals(c.getClientProperty(HOOKED))) {
			return;
		}
		c.putClientProperty(HOOKED, Boolean.TRUE);
		PropertyChangeListener listener = event -> {
			if (applying || table.isEmpty()) {
				return;
			}
			applying = true;
			try {
				if ("text".equals(event.getPropertyName())) {
					translateText(c);
				} else if ("ToolTipText".equals(event.getPropertyName())) {
					translateProperty(c, SOURCE_TIP, LAST_TIP, c.getToolTipText(), (value) -> c.setToolTipText(value));
				}
			} finally {
				applying = false;
			}
		};
		if (c instanceof JLabel || c instanceof AbstractButton) {
			c.addPropertyChangeListener("text", listener);
		}
		c.addPropertyChangeListener("ToolTipText", listener);
	}
}
