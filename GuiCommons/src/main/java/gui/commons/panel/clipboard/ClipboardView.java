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
package gui.commons.panel.clipboard;

import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.HierarchyEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.text.JTextComponent;

import org.apache.log4j.Logger;

import gui.commons.dialogutils.JOptionHelperExtended;
import gui.commons.frameutils.frame.arch.ToolIcons;
import gui.commons.frameutils.frame.arch.ToolStyle;
import initializer.configs.impl.om.TextEditorOption;
import net.miginfocom.swing.MigLayout;
import resources.GeneralConfig;
import various.common.light.gui.GuiUtils;
import various.common.light.gui.ShortcutKeys;
import various.common.light.i18n.I18n;

/**
 * Clipboard panel shared by the text editor (Alt+A, syntax text area) and the Clipboard frame (plain text area):
 * a list of text files in one folder, one of them shown in the text area. All the colors and fonts come from the look
 * and feel ({@link ToolStyle}); the text area is given by the host, which also styles it.
 * <p>
 * Changes are saved by themselves a moment after the user stops typing, when another clipboard is chosen, when the
 * panel is hidden and when the text area loses the focus; there is no "saved!" popup, the badge in the header says it.
 */
public class ClipboardView extends JPanel implements Scrollable {
	private static final long serialVersionUID = 1L;

	private static final Logger logger = Logger.getLogger(ClipboardView.class);

	/** Bigger files are not loaded: a text area is not the tool for them */
	private static final long MAX_BYTES = 8L * 1024 * 1024;
	private static final int AUTOSAVE_MS = 1500;
	private static final String ILLEGAL_NAME_CHARS = "\\/:*?\"<>|";

	private final File folder;
	private final TextEditorOption options;
	private final JTextComponent area;
	private final Runnable restyler;
	private final JOptionHelperExtended dialogs;

	private final JComboBox<String> combo = new JComboBox<String>();
	private final ToolStyle.Badge badge = ToolStyle.badge();
	private final JLabel stats = ToolStyle.mutedLabel(" ");
	private final JButton btnSave = ToolStyle.iconButton(ToolIcons.Kind.SAVE, I18n.t("common.save_current_clipboard"));
	private final JButton btnReload = ToolStyle.iconButton(ToolIcons.Kind.REFRESH, I18n.t("common.reload_current_clipboard_be_careful_unsaved"));
	private final JButton btnNew = ToolStyle.iconButton(ToolIcons.Kind.ADD, I18n.t("common.add_new_clipboard_to_this_list"));
	private final JButton btnRename = ToolStyle.iconButton(ToolIcons.Kind.PENCIL, I18n.t("clip.rename_tip"));
	private final JButton btnDelete = ToolStyle.iconButton(ToolIcons.Kind.TRASH, I18n.t("clip.delete_tip"));
	private final JButton btnFolder = ToolStyle.iconButton(ToolIcons.Kind.FOLDER, I18n.t("common.open_current_clipboard_in_file_system"));
	private final JButton btnCopy = ToolStyle.iconButton(ToolIcons.Kind.COPY, I18n.t("clip.copy_tip"));
	private final JButton btnPaste = ToolStyle.iconButton(ToolIcons.Kind.PASTE, I18n.t("clip.paste_tip"));
	private final JButton btnClear = ToolStyle.iconButton(ToolIcons.Kind.CLEAR, I18n.t("clip.clear_tip"));
	private final Timer autosave;
	private final Timer statsTimer;

	/** Called after a file has been put in the text area (the syntax text area drops its undo history here) */
	public Runnable onLoaded;

	/** File whose text is in the area (name inside the folder) */
	private String loadedName;
	private long loadedStamp;
	private long savedAt;
	private boolean dirty;
	private boolean saveFailed;
	private boolean tooLarge;
	/** Programmatic text changes: they are not user edits */
	private boolean loading;
	private boolean adjustingCombo;

	/**
	 * @param area text area shown by the panel (already created by the host)
	 * @param restyler applies the host style to the area (theme, font); can be null
	 * @param options text editor options: remembers the clipboard in use
	 * @param folder folder with the clipboards (null: the one in the configuration)
	 */
	public ClipboardView(JTextComponent area, Runnable restyler, TextEditorOption options, File folder) {
		this.area = area;
		this.restyler = restyler;
		this.options = options != null ? options : new TextEditorOption();
		this.folder = folder != null ? folder : new File(GeneralConfig.CLIBPOARDS_PANEL_SRC_FOLDER);
		this.dialogs = new JOptionHelperExtended(this);
		this.autosave = new Timer(AUTOSAVE_MS, e -> save());
		this.autosave.setRepeats(false);
		this.statsTimer = new Timer(200, e -> updateStats());
		this.statsTimer.setRepeats(false);

		buildGui();
		addHandlers();
		initFiles();
	}

	// ------------------------------------------------------------------ gui

	private void buildGui() {
		setLayout(new MigLayout("insets 8, gap 6, wrap 1", "[grow,fill]", "[][][][grow,fill][]"));

		JPanel header = new JPanel(new MigLayout("insets 0", "[grow][]", "[]"));
		header.setOpaque(false);
		JLabel title = new JLabel(I18n.t("clip.title"));
		title.setFont(ToolStyle.font(3f, java.awt.Font.BOLD));
		header.add(title);
		header.add(badge);
		add(header);

		combo.setToolTipText(I18n.t("common.choose_between_loaded_clipboards"));
		combo.setMinimumSize(new Dimension(40, combo.getMinimumSize().height));
		JPanel files = new JPanel(new MigLayout("insets 0, gap 2", "[grow,fill][][][][]", "[]"));
		files.setOpaque(false);
		files.add(combo);
		files.add(btnNew);
		files.add(btnRename);
		files.add(btnDelete);
		files.add(btnFolder);
		add(files);

		JPanel tools = new JPanel(new MigLayout("insets 0, gap 2", "[][]12[][][]", "[]"));
		tools.setOpaque(false);
		tools.add(btnSave);
		tools.add(btnReload);
		tools.add(btnCopy);
		tools.add(btnPaste);
		tools.add(btnClear);
		add(tools);

		JScrollPane scroll = new JScrollPane(area);
		scroll.setMinimumSize(new Dimension(60, 60));
		scroll.setPreferredSize(new Dimension(220, 220));
		area.setMargin(new Insets(6, 8, 6, 8));
		add(scroll, "grow, push");

		stats.setHorizontalAlignment(SwingConstants.LEFT);
		add(stats);
	}

	private void addHandlers() {
		area.getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void insertUpdate(DocumentEvent e) {
				edited();
			}

			@Override
			public void removeUpdate(DocumentEvent e) {
				edited();
			}

			@Override
			public void changedUpdate(DocumentEvent e) {
			}
		});

		area.addFocusListener(new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent e) {
				flush();
			}
		});

		area.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_S, ShortcutKeys.MENU_MASK), "clipboard.save");
		area.getActionMap().put("clipboard.save", new AbstractAction() {
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				save();
			}
		});

		addHierarchyListener(e -> {
			if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
				if (isShowing()) {
					refreshFromDisk();
					refreshList();
				} else {
					flush();
				}
			}
		});

		combo.addActionListener(e -> {
			String chosen = (String) combo.getSelectedItem();
			if (adjustingCombo || chosen == null || chosen.equals(loadedName)) {
				return;
			}
			flush();
			load(chosen, false);
		});
		// the list follows the folder: files added or removed outside show up when the list opens
		combo.addPopupMenuListener(new PopupMenuListener() {
			@Override
			public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
				refreshList();
			}

			@Override
			public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
			}

			@Override
			public void popupMenuCanceled(PopupMenuEvent e) {
			}
		});

		btnSave.addActionListener(e -> save());
		btnReload.addActionListener(e -> reloadFromDisk());
		btnNew.addActionListener(e -> createNew());
		btnRename.addActionListener(e -> renameCurrent());
		btnDelete.addActionListener(e -> deleteCurrent());
		btnFolder.addActionListener(e -> openFolder());
		btnCopy.addActionListener(e -> copy());
		btnPaste.addActionListener(e -> {
			if (area.isEditable()) {
				area.requestFocusInWindow();
				area.paste();
			}
		});
		btnClear.addActionListener(e -> clear());
	}

	@Override
	public void updateUI() {
		super.updateUI();
		// theme switched while the panel is open: colors set by hand are read again
		if (stats != null) {
			stats.setForeground(ToolStyle.muted());
			updateBadge();
		}
	}

	// ------------------------------------------------------------------ files

	private void initFiles() {
		try {
			folder.mkdirs();
			File main = new File(folder, GeneralConfig.CLIPBOARD_PANEL_DEFAULT);
			if (!main.exists()) {
				main.createNewFile();
			}
		} catch (Exception e) {
			logger.error("Cannot prepare the clipboards folder " + folder, e);
		}
		List<String> names = listNames();
		String wanted = options.getLastFileOpenedClipboard();
		if (wanted == null || !names.contains(wanted)) {
			wanted = names.contains(GeneralConfig.CLIPBOARD_PANEL_DEFAULT) ? GeneralConfig.CLIPBOARD_PANEL_DEFAULT : (names.isEmpty() ? null : names.get(0));
		}
		fillCombo(names, wanted);
		if (wanted != null) {
			load(wanted, false);
		} else {
			updateBadge();
			updateStats();
		}
	}

	private List<String> listNames() {
		List<String> names = new ArrayList<String>();
		File[] files = folder.listFiles();
		if (files != null) {
			for (File file : files) {
				if (file.isFile() && !file.isHidden()) {
					names.add(file.getName());
				}
			}
		}
		Collections.sort(names, String.CASE_INSENSITIVE_ORDER);
		return names;
	}

	private void fillCombo(List<String> names, String selected) {
		adjustingCombo = true;
		try {
			combo.setModel(new DefaultComboBoxModel<String>(names.toArray(new String[0])));
			if (selected != null && names.contains(selected)) {
				combo.setSelectedItem(selected);
			} else {
				combo.setSelectedIndex(-1);
			}
		} finally {
			adjustingCombo = false;
		}
	}

	/** Reads the folder again, keeping the clipboard in use selected */
	public void refreshList() {
		List<String> names = listNames();
		String shown = loadedName;
		if (shown != null && !names.contains(shown) && dirty) {
			names.add(shown); // removed outside while it has unsaved text: keep it, the next save writes it again
			Collections.sort(names, String.CASE_INSENSITIVE_ORDER);
		}
		if (shown != null && !names.contains(shown)) {
			shown = names.contains(GeneralConfig.CLIPBOARD_PANEL_DEFAULT) ? GeneralConfig.CLIPBOARD_PANEL_DEFAULT : (names.isEmpty() ? null : names.get(0));
			fillCombo(names, shown);
			if (shown != null) {
				load(shown, false);
			}
			return;
		}
		fillCombo(names, shown);
	}

	/** Shows the clipboard with the given file name (saves the current one first) */
	public void select(String name) {
		if (name == null || name.equals(loadedName)) {
			return;
		}
		List<String> names = listNames();
		if (names.contains(name)) {
			flush();
			fillCombo(names, name);
			load(name, false);
		}
	}

	public String getLoadedName() {
		return loadedName;
	}

	public JTextComponent getTextArea() {
		return area;
	}

	private void load(String name, boolean keepCaret) {
		File file = new File(folder, name);
		loading = true;
		try {
			int caret = keepCaret ? area.getCaretPosition() : 0;
			String text = "";
			tooLarge = false;
			if (file.isFile()) {
				if (file.length() > MAX_BYTES) {
					tooLarge = true;
				} else {
					text = new String(Files.readAllBytes(file.toPath()), Charset.defaultCharset()).replace("\r\n", "\n").replace('\r', '\n');
				}
			}
			area.setEditable(!tooLarge);
			area.setText(text);
			area.setCaretPosition(Math.min(caret, text.length()));
			loadedName = name;
			loadedStamp = file.lastModified();
			dirty = false;
			saveFailed = false;
			savedAt = 0;
			options.setLastFileOpenedClipboard(name);
			if (onLoaded != null) {
				onLoaded.run();
			}
		} catch (Exception e) {
			logger.error("Cannot load the clipboard " + file, e);
			saveFailed = true;
		} finally {
			loading = false;
		}
		autosave.stop();
		updateStats();
		updateBadge();
	}

	/** Shows again the file if it was changed outside and there is nothing unsaved */
	public void refreshFromDisk() {
		if (loadedName != null && !dirty && !tooLarge) {
			File file = new File(folder, loadedName);
			if (file.isFile() && file.lastModified() != loadedStamp) {
				load(loadedName, true);
			}
		}
	}

	private void reloadFromDisk() {
		if (loadedName == null) {
			return;
		}
		if (dirty && !dialogs.yesOrNo(I18n.t("clip.reload_confirm"), I18n.t("clip.title"))) {
			return; // cancelled by user
		}
		load(loadedName, true);
	}

	/** Writes the text now; true if it is on disk (or there was nothing to write) */
	public boolean save() {
		autosave.stop();
		if (loadedName == null || tooLarge) {
			return true;
		}
		File file = new File(folder, loadedName);
		try {
			folder.mkdirs();
			Files.write(file.toPath(), area.getText().getBytes(Charset.defaultCharset()));
			loadedStamp = file.lastModified();
			savedAt = System.currentTimeMillis();
			dirty = false;
			saveFailed = false;
			updateBadge();
			return true;
		} catch (Exception e) {
			logger.error("Cannot save the clipboard " + file, e);
			saveFailed = true;
			updateBadge();
			return false;
		}
	}

	/** Saves only if something changed (hide, switch, close) */
	public void flush() {
		if (dirty || saveFailed) {
			save();
		}
	}

	// ------------------------------------------------------------------ actions

	private void createNew() {
		String raw = dialogs.askForStringNullable(I18n.t("clip.name_prompt"), I18n.t("clip.title"));
		if (raw == null) {
			return; // cancelled by user
		}
		String problem = create(raw);
		if (problem != null) {
			dialogs.error(problem, I18n.t("clip.title"));
		} else {
			area.requestFocusInWindow();
		}
	}

	/** Creates an empty clipboard and shows it; null if done, otherwise the message for the user */
	String create(String raw) {
		String name = cleanName(raw);
		if (name == null) {
			return I18n.t("clip.name_invalid");
		}
		File file = new File(folder, name);
		if (file.exists()) {
			return I18n.fmt("clip.name_exists", name);
		}
		flush();
		try {
			folder.mkdirs();
			file.createNewFile();
		} catch (IOException e) {
			logger.error("Cannot create the clipboard " + file, e);
			return I18n.t("common.ioexception_while_trying_to_create_new");
		}
		fillCombo(listNames(), name);
		load(name, false);
		return null;
	}

	private void renameCurrent() {
		if (loadedName == null) {
			return;
		}
		String raw = isMain(loadedName) ? null : dialogs.askForStringNullable(I18n.fmt("clip.rename_prompt", loadedName), I18n.t("clip.title"), loadedName);
		if (raw == null && !isMain(loadedName)) {
			return; // cancelled by user
		}
		String problem = rename(raw);
		if (problem != null) {
			dialogs.warn(problem, I18n.t("clip.title"));
		}
	}

	/** Renames the clipboard in use; null if done, otherwise the message for the user */
	String rename(String raw) {
		if (loadedName == null || isMain(loadedName)) {
			return I18n.t("clip.main_protected");
		}
		String name = cleanName(raw);
		if (name == null) {
			return I18n.t("clip.name_invalid");
		}
		if (name.equals(loadedName)) {
			return null;
		}
		File target = new File(folder, name);
		if (target.exists()) {
			return I18n.fmt("clip.name_exists", name);
		}
		if (!save()) {
			return I18n.t("clip.state_error");
		}
		try {
			Files.move(new File(folder, loadedName).toPath(), target.toPath());
		} catch (Exception e) {
			logger.error("Cannot rename the clipboard " + loadedName, e);
			return I18n.t("clip.rename_failed");
		}
		loadedName = name;
		loadedStamp = target.lastModified();
		options.setLastFileOpenedClipboard(name);
		fillCombo(listNames(), name);
		updateBadge();
		return null;
	}

	private void deleteCurrent() {
		if (loadedName == null) {
			return;
		}
		if (!isMain(loadedName) && !dialogs.yesOrNo(I18n.fmt("clip.delete_confirm", loadedName), I18n.t("clip.title"))) {
			return; // cancelled by user
		}
		String problem = delete();
		if (problem != null) {
			dialogs.warn(problem, I18n.t("clip.title"));
		}
	}

	/** Deletes the clipboard in use and shows the main one; null if done, otherwise the message for the user */
	String delete() {
		if (loadedName == null || isMain(loadedName)) {
			return I18n.t("clip.main_protected");
		}
		try {
			Files.deleteIfExists(new File(folder, loadedName).toPath());
		} catch (Exception e) {
			logger.error("Cannot delete the clipboard " + loadedName, e);
			return I18n.t("clip.delete_failed");
		}
		dirty = false;
		loadedName = null;
		List<String> names = listNames();
		String next = names.contains(GeneralConfig.CLIPBOARD_PANEL_DEFAULT) ? GeneralConfig.CLIPBOARD_PANEL_DEFAULT : (names.isEmpty() ? null : names.get(0));
		fillCombo(names, next);
		if (next != null) {
			load(next, false);
		}
		return null;
	}

	private void copy() {
		String selected = area.getSelectedText();
		ToolStyle.copyToClipboard(selected != null && !selected.isEmpty() ? selected : area.getText());
	}

	private void clear() {
		if (!area.isEditable() || area.getDocument().getLength() == 0) {
			return;
		}
		if (dialogs.yesOrNo(I18n.fmt("clip.clear_confirm", loadedName), I18n.t("clip.title"))) {
			area.setText("");
		}
	}

	private void openFolder() {
		final File target = folder;
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					GuiUtils.openInFileSystem(target);
				} catch (Throwable e) {
					logger.warn("Cannot open the clipboards folder", e);
				}
			}
		}, "clipboard-open-folder").start();
	}

	private static boolean isMain(String name) {
		return GeneralConfig.CLIPBOARD_PANEL_DEFAULT.equalsIgnoreCase(name);
	}

	/** File name for what the user typed: trimmed, ".txt" when there is no extension; null if it cannot be a file name */
	static String cleanName(String raw) {
		if (raw == null) {
			return null;
		}
		String name = raw.trim();
		if (name.isEmpty() || name.startsWith(".") || name.endsWith(".") || name.length() > 120) {
			return null;
		}
		for (int i = 0; i < name.length(); i++) {
			char c = name.charAt(i);
			if (c < 32 || ILLEGAL_NAME_CHARS.indexOf(c) >= 0) {
				return null;
			}
		}
		return name.indexOf('.') < 0 ? name + ".txt" : name;
	}

	// ------------------------------------------------------------------ state

	private void edited() {
		if (loading) {
			return;
		}
		dirty = true;
		updateBadge();
		autosave.restart();
		statsTimer.restart();
	}

	private void updateBadge() {
		if (tooLarge) {
			badge.setStatus(I18n.t("clip.state_too_large"), null, ToolStyle.RED);
		} else if (saveFailed) {
			badge.setStatus(I18n.t("clip.state_error"), null, ToolStyle.RED);
		} else if (dirty) {
			badge.setStatus(I18n.t("clip.state_unsaved"), null, ToolStyle.ORANGE);
		} else if (savedAt > 0) {
			badge.setStatus(I18n.fmt("clip.state_saved", new SimpleDateFormat("HH:mm:ss").format(new Date(savedAt))), null, ToolStyle.GREEN);
		} else {
			badge.setStatus(I18n.t("clip.state_clean"), null, null);
		}
		badge.setToolTipText(I18n.t("clip.autosave_tip"));
		boolean has = loadedName != null;
		btnSave.setEnabled(has && !tooLarge);
		btnReload.setEnabled(has);
		btnRename.setEnabled(has);
		btnDelete.setEnabled(has);
		btnCopy.setEnabled(has);
		btnPaste.setEnabled(has && !tooLarge);
		btnClear.setEnabled(has && !tooLarge);
	}

	private void updateStats() {
		String text = area.getText();
		int words = 0;
		boolean inWord = false;
		for (int i = 0; i < text.length(); i++) {
			boolean space = Character.isWhitespace(text.charAt(i));
			if (!space && !inWord) {
				words++;
			}
			inWord = !space;
		}
		int lines = text.isEmpty() ? 0 : 1;
		for (int i = 0; i < text.length(); i++) {
			if (text.charAt(i) == '\n') {
				lines++;
			}
		}
		stats.setText(I18n.fmt("clip.stats", text.length(), words, lines));
	}

	/** Applies again the style of the host to the text area (theme or font changed) */
	public void restyle() {
		if (restyler != null) {
			restyler.run();
		}
		area.setMargin(new Insets(6, 8, 6, 8));
		repaint();
	}

	// ------------------------------------------------------------------ Scrollable (the host may wrap the panel in a scroll pane)

	@Override
	public Dimension getPreferredScrollableViewportSize() {
		return getPreferredSize();
	}

	@Override
	public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
		return 16;
	}

	@Override
	public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
		return 64;
	}

	@Override
	public boolean getScrollableTracksViewportWidth() {
		return true;
	}

	@Override
	public boolean getScrollableTracksViewportHeight() {
		return true;
	}
}
