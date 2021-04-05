package various.common.light.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;

import net.miginfocom.swing.MigLayout;
import various.common.light.files.CustomFileFilters;
import various.common.light.files.FileVarious;
import various.common.light.files.FileWorker;
import various.common.light.gui.dialogs.msg.JOptionHelper;
import various.common.light.utility.string.StringWorker;

public class LightLicenseViewer extends JDialog {
	private static final long serialVersionUID = -4557971895329584958L;

	private static File MAIN_PARENT_FOLDER = new File(System.getProperty("user.dir")).getParentFile();
	public static File MAIN_LIBRARIES_FOLDER = new File(MAIN_PARENT_FOLDER.getAbsolutePath() + File.separator + "Libraries_licenses");
	public static File MAIN_LIBRARIES_FOLDER_DETAIL = new File(MAIN_LIBRARIES_FOLDER.getAbsolutePath() + File.separator + "detailedReport");

	public enum UserLicenseChoice{
		ACCEPTED,
		REJECTED;
	}

	private static final Dimension DEFAULT_PREF_DIMENSION = GuiUtils.getScreenSizePerc(75);

	public UserLicenseChoice userChoice = UserLicenseChoice.REJECTED;
	File sourceLicense;

	private JScrollPane scrollPane;
	private JEditorPane jEditorPane;
	private JLabel lblNewLabel;
	private JButton btnDecline;
	private JButton btnAccept;

	private JOptionHelper dialogHelper;

	public boolean licenseLoadedCorrectly;

	public static LightLicenseViewer instance = null;

	public static boolean mandatoryChoice = true;
	private JMenuBar menuBar;
	private JMenuItem mntmScreepT;
	private JMenu mnLibraries;
	private JMenu mnLibrariesDetail;
	private JMenu mnLibrariesDetailPlugins;
	private JMenuItem mntmOpenWithBrowser;

	private File loadedLicense;

	public LightLicenseViewer(File sourceLicense, String header, String title) {
		this(sourceLicense, header, title, null);
	}

	public LightLicenseViewer(File sourceLicense, String header, String title, boolean... submenuVisible) {

		instance = this;

		loadedLicense = sourceLicense;

		String correctHeader = !StringWorker.isEmpty(header) ? header : "Accept licence to continue using this software";
		String correctTitle = !StringWorker.isEmpty(title) ? title : "EULA - acceptance";

		this.licenseLoadedCorrectly = false;
		this.sourceLicense = sourceLicense;
		this.dialogHelper = new JOptionHelper(this);

		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		setPreferredSize(DEFAULT_PREF_DIMENSION);
		setModalityType(ModalityType.APPLICATION_MODAL);
		setIconImage(GuiUtils.iconToImage(UIManager.getIcon("OptionPane.informationIcon")));
		setAlwaysOnTop(true);
		toFront();

		getContentPane().setBackground(Color.DARK_GRAY);
		getContentPane().setLayout(new MigLayout(
				"fill, insets 4",
				"[27px,grow][27px,grow]",
				"[:35px:80px][27px,grow][40px:n]"));

		setTitle(correctTitle);

		JPanel panel = new JPanel();
		panel.setBorder(new LineBorder(new Color(192, 192, 192)));
		panel.setBackground(Color.BLACK);
		getContentPane().add(panel, "cell 0 0 2 1,grow");
		panel.setLayout(new MigLayout("", "[grow]", "[]"));

		lblNewLabel = new JLabel(correctHeader);
		lblNewLabel.setForeground(Color.WHITE);
		lblNewLabel.setFont(new Font("Segoe UI", Font.BOLD, 15));
		panel.add(lblNewLabel, "cell 0 0,alignx center");

		scrollPane = new JScrollPane();
		scrollPane.setBorder(new LineBorder(Color.BLACK, 2));
		scrollPane.setPreferredSize(new Dimension(2000, 2000));
		int scrollPaneHeigth = mandatoryChoice ? 1 : 2;
		getContentPane().add(scrollPane, "cell 0 1 2 " + scrollPaneHeigth + ",alignx center,aligny center");

		jEditorPane = new JEditorPane();
		jEditorPane.setEditable(false);
		jEditorPane.setCursor(GuiUtils.CURSOR_TEXT);
		scrollPane.setViewportView(jEditorPane);
		addHtmlStyle();

		btnDecline = new JButton("Decline");
		btnDecline.setCursor(GuiUtils.CURSOR_HAND);
		btnDecline.setBackground(Color.GRAY);
		btnDecline.setFont(new Font("Segoe UI", Font.BOLD, 14));
		btnDecline.setMinimumSize(new Dimension(200, 32));

		btnAccept = new JButton("Accept");
		btnAccept.setCursor(GuiUtils.CURSOR_HAND);
		btnAccept.setBackground(Color.GRAY);
		btnAccept.setFont(new Font("Segoe UI", Font.BOLD, 14));
		btnAccept.setMinimumSize(new Dimension(200, 32));

		if (mandatoryChoice) {
			getContentPane().add(btnDecline, "cell 0 2,alignx center,growy");
			getContentPane().add(btnAccept, "cell 1 2,alignx center,growy");
		}

		loadFile(sourceLicense);

		GuiUtils.centerComponent(this, DEFAULT_PREF_DIMENSION);

		menuBar = new JMenuBar();
		menuBar.setBackground(Color.WHITE);
		menuBar.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
		setJMenuBar(menuBar);

		updateMenuBar();
		setSubMenusVisible(submenuVisible);

		addHandlers();

		setVisible(true);
	}

	public void addHandlers() {

		// default close operation(same as reset)
		addWindowListener(new WindowAdapter() {

			@Override
			public void windowClosing(WindowEvent e) {
				if (mandatoryChoice && !askExitRejecting()) {
					setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
					return;
				} else {
					setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
					instance = null;
					dispose();
				}
			}
		});

		btnAccept.addActionListener((e) -> {
			exitAccepting();
		});

		btnDecline.addActionListener((e) -> {
			askExitRejecting();
		});

		mntmScreepT.addActionListener((e) -> {
			loadFile(sourceLicense);
		});
	}

	public void setSubMenusVisible(boolean... indexes) {
		if(indexes == null)
			return;

		// TODO fixme

		checkIndex(mntmScreepT, 0, indexes);
		checkIndex(mnLibraries, 1, indexes);
		checkIndex(mnLibrariesDetail, 2, indexes);
		checkIndex(mntmOpenWithBrowser, 3, indexes);
	}

	private boolean isContainedInMenu(JMenuItem item) {
		for(Component comp : menuBar.getComponents()) {
			if(comp == item) {
				return true;
			}
		}

		return false;
	}

	private void checkIndex(JMenuItem item, int i, boolean... indexes) {
		boolean visible = (indexes.length > i ? indexes[i] : true);
		if (visible && !isContainedInMenu(item)) {
			menuBar.add(item);
		} else if(!visible && isContainedInMenu(item)) {
			menuBar.remove(item);
		}
	}

	public void updateMenuBar() {

		menuBar.removeAll();

		mntmScreepT = new JMenuItem("ScreepT License");
		mntmScreepT.setBackground(Color.WHITE);
		mntmScreepT.setAlignmentX(Component.LEFT_ALIGNMENT);
		mntmScreepT.setHorizontalAlignment(SwingConstants.LEFT);
		mntmScreepT.setHorizontalTextPosition(SwingConstants.LEFT);
		mntmScreepT.setPreferredSize(new Dimension(160, 20));
		mntmScreepT.setMaximumSize(new Dimension(180, 20));
		mntmScreepT.setFont(new Font("Segoe UI", Font.BOLD, 17));
		menuBar.add(mntmScreepT);

		mnLibraries = new JMenu("Libraries");
		mnLibraries.setAlignmentX(Component.LEFT_ALIGNMENT);
		mnLibraries.setBorder(new EmptyBorder(0, 0, 0, 0));
		mnLibraries.setPreferredSize(new Dimension(100, 20));
		mnLibraries.setFont(new Font("Segoe UI", Font.BOLD, 17));
		menuBar.add(mnLibraries);

		for(File libraryLicense : MAIN_LIBRARIES_FOLDER.listFiles(CustomFileFilters.customTypeFilter(CustomFileFilters.FILE_ONLY))) {
			JMenuItem licenseFileMenuItem = new JMenuItem(libraryLicense.getName());
			licenseFileMenuItem.setFont(new Font("Segoe UI", Font.PLAIN, 15));
			mnLibraries.add(licenseFileMenuItem);

			licenseFileMenuItem.addActionListener((e) -> {
				loadFile(libraryLicense);
			});
		}

		mnLibrariesDetail = new JMenu("Detailed Reports");
		mnLibrariesDetail.setBorder(new EmptyBorder(0, 0, 0, 0));
		mnLibrariesDetail.setAlignmentX(Component.RIGHT_ALIGNMENT);
		mnLibrariesDetail.setFont(new Font("Segoe UI", Font.BOLD, 17));
		menuBar.add(mnLibrariesDetail);

		mnLibrariesDetailPlugins = new JMenu("Plugins");
		mnLibrariesDetailPlugins.setBorder(new EmptyBorder(0, 0, 0, 0));
		mnLibrariesDetailPlugins.setAlignmentX(Component.RIGHT_ALIGNMENT);
		mnLibrariesDetailPlugins.setFont(new Font("Segoe UI", Font.PLAIN, 15));
		mnLibrariesDetail.add(mnLibrariesDetailPlugins);

		List<File> details = FileWorker.getAllContent(MAIN_LIBRARIES_FOLDER_DETAIL, CustomFileFilters.FILE_ONLY);
		if(details != null)
			for(File libraryLicense : details) {
				JMenuItem licenseFileMenuItem = new JMenuItem(libraryLicense.getName());
				licenseFileMenuItem.setFont(new Font("Segoe UI", Font.PLAIN, 15));

				if(libraryLicense.getName().toLowerCase().contains("screept"))
					mnLibrariesDetail.add(licenseFileMenuItem);
				else
					mnLibrariesDetailPlugins.add(licenseFileMenuItem);

				licenseFileMenuItem.addActionListener((e) -> {
					loadFile(libraryLicense);
				});
			}

		JMenuItem itemPadding = new JMenuItem();
		itemPadding.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
		itemPadding.setMaximumSize(new Dimension(5000, 20));
		itemPadding.setPreferredSize(new Dimension(700, 20));
		itemPadding.setMinimumSize(new Dimension(600, 20));
		itemPadding.setEnabled(false);
		menuBar.add(itemPadding);

		mntmOpenWithBrowser = new JMenuItem("Open with browser");
		mntmOpenWithBrowser.setBorder(new EmptyBorder(0, 0, 0, 0));
		mntmOpenWithBrowser.setAlignmentX(Component.RIGHT_ALIGNMENT);
		mntmOpenWithBrowser.setFont(new Font("Segoe UI", Font.BOLD, 17));
		mntmOpenWithBrowser.addActionListener((e) -> {
			try {
				Desktop.getDesktop().browse(FileVarious.getCanonicalFileSafe(loadedLicense).toURI());
			} catch (Exception e1) {
				e1.printStackTrace();
			}
		});
		menuBar.add(mntmOpenWithBrowser);
	}

	private void addHtmlStyle() {

		HTMLEditorKit kit = new HTMLEditorKit();
        jEditorPane.setEditorKit(kit);

		// add some styles to the html
        StyleSheet styleSheet = kit.getStyleSheet();
        styleSheet.addRule("body {color:#000; font-family:times; margin: 4px; }");
        styleSheet.addRule("h1 {color: blue;}");
        styleSheet.addRule("h2 {color: #ff0000;}");
        styleSheet.addRule("pre {font : 10px monaco; color : black; background-color : #fafafa; }");

        jEditorPane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        jEditorPane.setFont(new Font("Segoe UI", Font.PLAIN, 18));

	}

	private void loadFile(File licenseToLoad) {
		String fileContent = "License not found at: " + licenseToLoad;
		try {
			fileContent = FileWorker.readFileAsString(licenseToLoad, "UTF-8");
		} catch (Exception e) {
			dialogHelper.warn("License file not found!");
			licenseLoadedCorrectly = false;
		}

		fileContent = StringWorker.isEmpty(fileContent) ? "Not found" : fileContent;
		jEditorPane.setText(fileContent);
		licenseLoadedCorrectly = !StringWorker.isEmpty(fileContent);

		GuiUtils.scrollJScrollPane(scrollPane, false);

		lblNewLabel.setText("Loaded license: " + licenseToLoad.getName());

		loadedLicense = licenseToLoad;
	}

	private boolean askExitRejecting() {
		if (dialogHelper.yesOrNo("Are you sure to exit license acceptance? It's required to proceed.", "Exit license acceptance?")) {
			userChoice = UserLicenseChoice.REJECTED;
			instance = null;
			dispose();
			return true;
		} else {
			return false;
		}
	}

	private void exitAccepting() {
		dialogHelper.info(
			GuiUtils.encapsulateInHtml("<b>License accepted!</b></br>If you want to read it again you can find it at: " + sourceLicense),
			"License accepted"
		);
		userChoice = UserLicenseChoice.ACCEPTED;
		dispose();
	}

	public static LightLicenseViewer getInstance(File sourceLicenseFile, String header, String title, boolean... visibleFlags) {
		return instance == null ? new LightLicenseViewer(sourceLicenseFile, header, title, visibleFlags) : instance;
	}

	public static File getMAIN_LIBRARIES_FOLDER() {
		return MAIN_LIBRARIES_FOLDER;
	}

	public static void setMAIN_LIBRARIES_FOLDER(File mAIN_LIBRARIES_FOLDER) {
		MAIN_LIBRARIES_FOLDER = mAIN_LIBRARIES_FOLDER;
	}

	public static File getMAIN_LIBRARIES_FOLDER_DETAIL() {
		return MAIN_LIBRARIES_FOLDER_DETAIL;
	}

	public static void setMAIN_LIBRARIES_FOLDER_DETAIL(File mAIN_LIBRARIES_FOLDER_DETAIL) {
		MAIN_LIBRARIES_FOLDER_DETAIL = mAIN_LIBRARIES_FOLDER_DETAIL;
	}

}
