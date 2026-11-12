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
package gui.commons.frameutils.frame;

import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;

import gui.commons.dialogutils.NativeFileChooser;
import gui.commons.frameutils.frame.arch.ToolStyle;
import gui.commons.frameutils.frame.om.ImageConverterOpener;
import gui.commons.imagestudio.BatchDialog;
import gui.commons.imagestudio.ImageEdit;
import gui.commons.imagestudio.ImageStudioPanel;
import gui.commons.imagestudio.StudioMode;
import net.miginfocom.swing.MigLayout;
import resources.IconsPathConfigurator;
import various.common.light.files.FileHistory;
import various.common.light.gui.GuiUtils;
import various.common.light.gui.dnd.FileDrop;
import various.common.light.utility.log.SafeLogger;

/**
 * Image Studio window: several images in tabs, each with its own editing panel (see {@link ImageStudioPanel}), the
 * conversion options on the right, batch processing from the header and the Basic / Classic / Nerd mode selector.
 * The same panel also opens as an integrated tab of the text editor.
 */
public class ImageViewerFrame extends JFrame {
	private static final long serialVersionUID = 8293717691849535908L;

	public static SafeLogger logger = new SafeLogger(ImageViewerFrame.class);

	/** Kept for compatibility: the studio converts by itself, there is no separate converter to open any more */
	public static ImageConverterOpener imageConverterOpener = null;

	public static final int REFRESH_PERIOD = 300;
	public static final int MAX_HISTORY_SIZE = 40;

	public static FileHistory history = new FileHistory(MAX_HISTORY_SIZE);
	public static File currentLoaded = null;

	/** File of the image in the selected tab */
	public File imageFile = null;
	public JFrame thisFrame;
	boolean active = false;

	private final JTabbedPane tabs = new JTabbedPane();
	private final List<ImageStudioPanel> panels = new ArrayList<ImageStudioPanel>();
	private JComboBox<StudioMode> comboMode;
	private final Runnable modeListener = () -> SwingUtilities.invokeLater(() -> {
		if (comboMode.getSelectedItem() != StudioMode.current()) {
			comboMode.setSelectedItem(StudioMode.current());
		}
	});

	public ImageViewerFrame(File imageFileSource) {

		thisFrame = this;
		active = true;

		GuiUtils.setFrameIcon(IconsPathConfigurator.F_ICON_IMAGE, this);
		setTitle("Image Studio");

		getContentPane().setLayout(new MigLayout("fill, insets 10, gapy 8", "[grow,fill]", "[][grow,fill]"));

		JPanel header = new JPanel(new MigLayout("insets 0, gapx 8", "[][][][grow,fill][][]", "[]"));
		header.setOpaque(false);
		JButton open = ToolStyle.button("Open images...", null, "Open one or more images in new tabs (Ctrl+O)");
		open.addActionListener(e -> chooseAndOpen());
		JButton batch = ToolStyle.button("Batch...", null, "Apply the same edits and conversion to many images");
		batch.addActionListener(e -> new BatchDialog(this, currentFile()).setVisible(true));
		header.add(open);
		header.add(batch);
		JButton clean = ToolStyle.button("Clean metadata...", null, "Remove EXIF, GPS and other metadata from many images, without losing quality");
		clean.addActionListener(e -> new gui.commons.imagestudio.MetadataCleanerDialog(this, currentFile()).setVisible(true));
		header.add(clean);
		header.add(new JLabel(), "growx");
		header.add(ToolStyle.mutedLabel("Mode"));
		comboMode = new JComboBox<StudioMode>(StudioMode.values());
		comboMode.setSelectedItem(StudioMode.current());
		comboMode.setToolTipText("<html><b>Basic</b>: convert and resize.<br><b>Classic</b>: + adjustments, filters, rotate, flip.<br><b>Nerd</b>: + every option, histogram, grid.</html>");
		ToolStyle.hand(comboMode);
		comboMode.addActionListener(e -> StudioMode.set((StudioMode) comboMode.getSelectedItem()));
		header.add(comboMode);
		getContentPane().add(header, "growx, wrap");

		tabs.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
		tabs.addChangeListener(e -> onTabSelected());
		getContentPane().add(tabs, "grow");
		new FileDrop(tabs, BorderFactory.createLineBorder(ToolStyle.BLUE, 3), files -> SwingUtilities.invokeLater(() -> openFiles(files)));

		StudioMode.addListener(modeListener);

		addWindowListener(new WindowAdapter() {
		    @Override
		    public void windowClosing(WindowEvent windowEvent) {
	        	logger.debug("Closing Frame: " + getClass().getName());
	        	closeWindow();
		    }
		});
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

		if (imageFileSource != null) {
			openFile(imageFileSource);
		}

		setMinimumSize(new Dimension(900, 560));
		GuiUtils.centerComponent(this, new Dimension(1280, 800));
		setVisible(true);
	}

	private File currentFile() {
		return imageFile;
	}

	private void chooseAndOpen() {
		NativeFileChooser chooser = new NativeFileChooser();
		chooser.setMultiSelectionEnabled(true);
		chooser.setDialogTitle("Open images");
		if (currentLoaded != null && currentLoaded.getParentFile() != null) {
			chooser.setCurrentDirectory(currentLoaded.getParentFile());
		}
		if (chooser.showDialog(this, "Open") == NativeFileChooser.APPROVE_OPTION) {
			openFiles(chooser.getSelectedFiles());
		}
	}

	/** Opens every image file in its own tab (an image already open is just selected) */
	public void openFiles(File[] files) {
		if (files == null) {
			return;
		}
		for (File file : files) {
			if (ImageEdit.isImageFile(file)) {
				openFile(file);
			}
		}
	}

	/** Opens the image in a tab, or selects its tab when it is already open */
	public void openFile(File file) {
		if (file == null || !file.exists()) {
			return;
		}
		for (int i = 0; i < panels.size(); i++) {
			File open = panels.get(i).getFile();
			if (open != null && open.equals(file)) {
				tabs.setSelectedIndex(i);
				return;
			}
		}
		final ImageStudioPanel panel = new ImageStudioPanel(file);
		panels.add(panel);
		tabs.addTab(panel.getTitle(), panel);
		int index = tabs.getTabCount() - 1;
		tabs.setTabComponentAt(index, buildTabHeader(panel));
		panel.setChangeListener(() -> SwingUtilities.invokeLater(() -> refreshTabHeader(panel)));
		tabs.setSelectedIndex(index);
		history.addToHistoryIfNotPresent(file);
		currentLoaded = file;
	}

	private JPanel buildTabHeader(final ImageStudioPanel panel) {
		JPanel header = new JPanel(new MigLayout("insets 0, gapx 6", "[][]", "[]"));
		header.setOpaque(false);
		JLabel label = new JLabel(panel.getTitle());
		label.setName("title");
		JButton close = new JButton("x");
		close.putClientProperty("JButton.buttonType", "toolBarButton");
		close.setToolTipText("Close this image (Ctrl+W)");
		close.setFocusable(false);
		close.setMargin(new java.awt.Insets(0, 4, 0, 4));
		close.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
		close.addActionListener(e -> closeTab(panel));
		header.add(label);
		header.add(close);
		return header;
	}

	private void refreshTabHeader(ImageStudioPanel panel) {
		int index = panels.indexOf(panel);
		if (index < 0 || index >= tabs.getTabCount()) {
			return;
		}
		java.awt.Component header = tabs.getTabComponentAt(index);
		if (header instanceof JPanel) {
			for (java.awt.Component c : ((JPanel) header).getComponents()) {
				if (c instanceof JLabel && "title".equals(c.getName())) {
					((JLabel) c).setText(panel.getTitle() + (panel.isModified() ? " *" : ""));
				}
			}
		}
	}

	private void closeTab(ImageStudioPanel panel) {
		if (!panel.confirmClose(this)) {
			return;
		}
		int index = panels.indexOf(panel);
		if (index >= 0) {
			tabs.removeTabAt(index);
			panels.remove(index);
		}
		panel.dispose();
		if (panels.isEmpty()) {
			imageFile = null;
			setTitle("Image Studio");
		}
	}

	private void onTabSelected() {
		int index = tabs.getSelectedIndex();
		if (index >= 0 && index < panels.size()) {
			ImageStudioPanel panel = panels.get(index);
			imageFile = panel.getFile();
			currentLoaded = imageFile != null ? imageFile : currentLoaded;
			setTitle(panel.getTitle() + " - Image Studio");
			panel.onSelected();
		}
	}

	private void closeWindow() {
		for (ImageStudioPanel panel : new ArrayList<ImageStudioPanel>(panels)) {
			tabs.setSelectedIndex(panels.indexOf(panel));
			if (!panel.confirmClose(this)) {
				return;
			}
		}
		active = false;
		StudioMode.removeListener(modeListener);
		for (ImageStudioPanel panel : panels) {
			panel.dispose();
		}
		panels.clear();
		dispose();
	}

	// ---------------------------------------------------------------- STATIC ENTRY POINTS

	/** Previous / next image: selects the neighbour tab of the last window, or walks the opened files history */
	public static void loadFromHistory(boolean previous) {
		ArrayList<ImageViewerFrame> viewers = getViewerInstanceList();
		if (!viewers.isEmpty()) {
			JTabbedPane tabs = viewers.get(0).tabs;
			int count = tabs.getTabCount();
			if (count > 1) {
				tabs.setSelectedIndex((tabs.getSelectedIndex() + (previous ? -1 : 1) + count) % count);
				return;
			}
		}
		if (!history.isEmpty()) {
			currentLoaded = previous ? history.getPreviousInHistory(currentLoaded) : history.getNextInHistory(currentLoaded);
			loadIntoLastExistingInstance(currentLoaded);
		}
	}

	public static void newInstanceIfNotExisting(File imageFile) {

		logger.info("Image file to open: " + imageFile);

		if(imageFile == null || !imageFile.exists()) {
			logger.warn("Cannot load image -> Invalid input file: " + imageFile);
			return;
		}
		loadIntoLastExistingInstance(imageFile);
	}

	/** Opens the image in a new tab of the open window, creating the window if there is none */
	public static void loadIntoLastExistingInstance(File file) {

		File imageFile = file;

		logger.info("Image file to open: " + imageFile);

		if(imageFile == null || !imageFile.exists()) {
			if(!history.isEmpty()) {
				imageFile = history.getLast();
			}
			logger.warn("Cannot load image -> Invalid input file: " + imageFile + " -> Last in history loaded: " + history.getLast());
		}

		final File target = imageFile;
		GuiUtils.launchThreadSafeSwing(() -> {
			ArrayList<ImageViewerFrame> viewers = getViewerInstanceList();
			if (!viewers.isEmpty()) {
				ImageViewerFrame viewer = viewers.get(0);
				viewer.openFile(target);
				gui.commons.imagestudio.DialogFit.bringToFront(viewer);
				logger.info("Image file loaded into the open Image Studio!");
			} else {
				gui.commons.imagestudio.DialogFit.bringToFront(new ImageViewerFrame(target));
				logger.info("Image file loaded into a new Image Studio!");
			}
		});
	}

	public static ArrayList<ImageViewerFrame> getViewerInstanceList(){
		ArrayList<ImageViewerFrame> viewers = new ArrayList<>();
		for(Window window : Window.getWindows()) {
			if(window instanceof ImageViewerFrame) {
				viewers.add((ImageViewerFrame) window);
			}
		}

		return viewers;
	}
}
