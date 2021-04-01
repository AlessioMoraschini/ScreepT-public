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
package gui.commons.frameutils.frame.panels;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;

import gui.commons.combobox.TextPaneComboBoxEditor;
import gui.commons.frameutils.frame.panels.arch.IParentPanel;
import gui.commons.frameutils.frame.panels.arch.ParentPanel;
import gui.commons.frameutils.utils.GuiUtilsExtended;
import initializer.configs.impl.om.TextEditorOption;
import net.miginfocom.swing.MigLayout;
import resources.GeneralConfig;
import resources.IconsPathConfigurator;
import various.common.light.files.FileVarious;
import various.common.light.files.FileWorker;
import various.common.light.gui.GuiUtils;
import various.common.light.gui.ScreepTGuiFactory;

import java.awt.Dimension;
import javax.swing.JLabel;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.LineBorder;
import java.awt.Font;
import java.awt.Insets;

public class ClipboardPanelLight  extends ParentPanel implements IParentPanel{
	private static final long serialVersionUID = 4527321717233899589L;
	
	// ClipboardPanel logger creation
	static Logger logger = Logger.getLogger(ClipboardPanelLight.class);
	
	public static ClipboardPanelLight singletonInstance;

	public JTextArea txtAreaClipboard;
	private JScrollPane txtAreaContainer;
	
	JButton btnSave;
	JButton btnReload;
	JButton btnReloadClipboards;
	JButton btnOpenInFilesystem;
	public JComboBox<String> comboClipboards;
	private JLabel lblClipboardPanel;
	private JSeparator separator;
	private JSeparator separator_1;
	private JSeparator separator_2;
	public JScrollPane currentScrollPane;
	private TextEditorOption options;
	private JButton btnAddNewClipboard;
	
	private AtomicBoolean isReloadingCombo = new AtomicBoolean();
	
	public ClipboardPanelLight() {
		this(new TextEditorOption());
	}
	
	/**
	 * Initialize panel with target directory to write in memory files, which has custom behaviour 
	 * according to the interface passed as parameter.
	 *  
	 *  The file will be inserted as clipboardMain in current map
	 *  
	 * @param dialogManager
	 * @param clipboardPers
	 */
	public ClipboardPanelLight(TextEditorOption options) {
		this.options = options;

		super.init();
		
		// Disable advices until instantiation is ended
		dialogHelper.enabledAdvices = false;
	}
	
	public void applyClipboardTextAreaGraphics() {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				txtAreaClipboard.setFont(GeneralConfig.TXT_EDITOR_CLIPBOARD_FONT);
				txtAreaClipboard.setWrapStyleWord(true);
				txtAreaClipboard.setLineWrap(true);
				txtAreaClipboard.setEditable(true);
				txtAreaClipboard.revalidate();
				txtAreaClipboard.repaint();
			}
		});
	}
	
	@Override
	protected void initGui() {
		
		currentScrollPane = ScreepTGuiFactory.getScrollPane(true, true);
		
		this.dataMap = GeneralConfig.CLIPBOARD_PANEL_MAP;
		this.txtAreaClipboard = new JTextArea();
		txtAreaClipboard.setMaximumSize(new Dimension(1500, GuiUtilsExtended.getScreenSize().height));
		this.txtAreaContainer = new JScrollPane();
		txtAreaClipboard.setSelectionColor(Color.GRAY);
		txtAreaClipboard.setSelectedTextColor(Color.WHITE);
		txtAreaContainer.setMinimumSize(new Dimension(18, 18));
		txtAreaContainer.setPreferredSize(new Dimension(20, 20));
		txtAreaContainer.setBackground(Color.DARK_GRAY);
		txtAreaContainer.setForeground(Color.WHITE);
		txtAreaContainer.setViewportView(txtAreaClipboard);
		
//		setLayout(new MigLayout("fill, insets 1, hidemode 2", "[200px,grow]", "[270px,grow]"));
		setLayout(new MigLayout("fill, insets 1, hidemode 2", "[50px,grow]", "[50px,grow]"));
		setBorder(new EmptyBorder(0,0,0,0));
		
		// add main pane to main scroller
		add(currentScrollPane, "cell 0 0,grow");

		// main panel layout
		MigLayout panelLayout = new MigLayout("insets 8, hidemode 2", "[::20px,grow][::20px,grow][::20px,grow][5px,grow][110px:110px][5px,grow][::20px,grow][]", "[:15px:20px][25px][5px][30px,grow][30px,grow][][]");
		
		// main layout
		JPanel mainPane = new JPanel();
		mainPane.setLayout(panelLayout);
		mainPane.setBackground(Color.DARK_GRAY);
		currentScrollPane.setViewportView(mainPane);
		currentScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		currentScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		
		separator = new JSeparator();
		separator.setBackground(Color.GRAY);
		mainPane.add(separator, "cell 0 0 4 1,growx");
		
		lblClipboardPanel = new JLabel("ClipBoard Panel");
		lblClipboardPanel.setFont(new Font("Tahoma", Font.PLAIN, 15));
		lblClipboardPanel.setForeground(Color.LIGHT_GRAY);
		mainPane.add(lblClipboardPanel, "cell 4 0,alignx center,aligny top");
		
		separator_1 = new JSeparator();
		separator_1.setBackground(Color.GRAY);
		mainPane.add(separator_1, "cell 5 0 3 1,growx");
		
		// Add subcomponents to main pane
		btnSave = new JButton("");
		btnSave.setMinimumSize(new Dimension(20, 20));
		btnSave.setMaximumSize(new Dimension(20, 20));
		mainPane.add(btnSave, "cell 0 1,alignx center");
		btnSave.setCursor(GuiUtilsExtended.CURSOR_HAND);
		GuiUtilsExtended.makeTransparentJButton(btnSave);
		btnSave.setToolTipText("Save current Clipboard");
		btnSave.setForeground(Color.BLACK);
		btnSave.setIcon(new ImageIcon(IconsPathConfigurator.ICON_FILE_SAVE));
		
		btnReload = new JButton("");
		btnReload.setPreferredSize(new Dimension(20, 20));
		btnReload.setMinimumSize(new Dimension(20, 9));
		btnReload.setMaximumSize(new Dimension(20, 20));
		btnReload.setForeground(Color.BLACK);
		btnReload.setToolTipText("Reload current Clipboard (be careful, unsaved changes will be lost)");
		btnReload.setCursor(GuiUtilsExtended.CURSOR_HAND);
		GuiUtilsExtended.makeTransparentJButton(btnReload);
		btnReload.setIcon(new ImageIcon(IconsPathConfigurator.ICON_RELOAD_CONF));
		mainPane.add(btnReload, "cell 1 1,alignx center");
		
		btnAddNewClipboard = new JButton("");
		btnAddNewClipboard.setToolTipText("Add new clipboard to this list");
		btnAddNewClipboard.setPreferredSize(new Dimension(20, 20));
		btnAddNewClipboard.setMinimumSize(new Dimension(20, 9));
		btnAddNewClipboard.setMaximumSize(new Dimension(20, 20));
		btnAddNewClipboard.setForeground(Color.BLACK);
		btnAddNewClipboard.setCursor(GuiUtilsExtended.CURSOR_HAND);
		GuiUtilsExtended.makeTransparentJButton(btnAddNewClipboard);
		btnAddNewClipboard.setIcon(new ImageIcon(IconsPathConfigurator.ICON_ADD));
		mainPane.add(btnAddNewClipboard, "cell 2 1");
		
		String[] clipBoards = dataMap.keySet().toArray(new String[2]);
		comboClipboards = new JComboBox<>(clipBoards);
		comboClipboards.setCursor(GuiUtilsExtended.CURSOR_HAND);
		comboClipboards.setFont(GeneralConfig.BUTTON_DEFAULT_FONT.deriveFont(Font.PLAIN));
		comboClipboards.setToolTipText("Choose between loaded Clipboards");
		comboClipboards.setForeground(Color.BLACK);
		comboClipboards.setEditor( new TextPaneComboBoxEditor() );
		mainPane.add(comboClipboards, "cell 3 1 3 1,growx");
		
		btnReloadClipboards = new JButton("");
		btnReloadClipboards.setToolTipText("Reload Clipboard List");
		btnReloadClipboards.setPreferredSize(new Dimension(20, 20));
		btnReloadClipboards.setMinimumSize(new Dimension(20, 9));
		btnReloadClipboards.setMaximumSize(new Dimension(20, 20));
		btnReloadClipboards.setCursor(GuiUtilsExtended.CURSOR_HAND);
		GuiUtilsExtended.makeTransparentJButton(btnReloadClipboards);
		btnReloadClipboards.setIcon(new ImageIcon(IconsPathConfigurator.ICON_RELOAD4));
		btnReloadClipboards.setForeground(Color.BLACK);
		mainPane.add(btnReloadClipboards, "cell 6 1");
		
		btnOpenInFilesystem = new JButton("");
		btnOpenInFilesystem.setToolTipText("Open current clipboard in File System");
		btnOpenInFilesystem.setPreferredSize(new Dimension(20, 20));
		btnOpenInFilesystem.setMinimumSize(new Dimension(20, 9));
		btnOpenInFilesystem.setMaximumSize(new Dimension(20, 20));
		btnOpenInFilesystem.setCursor(GuiUtilsExtended.CURSOR_HAND);
		GuiUtilsExtended.makeTransparentJButton(btnOpenInFilesystem);
		btnOpenInFilesystem.setIcon(new ImageIcon(IconsPathConfigurator.iconFolderPath));
		btnOpenInFilesystem.setForeground(Color.BLACK);
		mainPane.add(btnOpenInFilesystem, "cell 7 1");
		
		separator_2 = new JSeparator();
		separator_2.setBackground(Color.GRAY);
		mainPane.add(separator_2, "cell 0 2 8 1,growx");
		
		mainPane.add(txtAreaContainer, "cell 0 3 8 4,grow");
		txtAreaClipboard.setBorder(new LineBorder(new Color(0, 0, 0), 3));

//		GUIStyelSettings.applyNimbusSize(0);
		File mainClip = dataMap.get(options.lastFileOpenedClipboard);
		if(mainClip != null && !mainClip.exists()) {
			try {
				mainClip.createNewFile();
			} catch (IOException e) {
				logger.error("Error when trying to load file", e);
			}
		}else {
			try {
				reload(options.getLastFileOpenedClipboard());
			} catch (IOException e) {
				logger.error("Error initializing clipboard Main at start");
			}
		}
		
		sync();
		FileWorker.loadFileTxtArea(dataMap.get(options.getLastFileOpenedClipboard()), txtAreaClipboard, null, null);
		selectFromComboClipboards(options.getLastFileOpenedClipboard());
		dialogHelper.enabledAdvices = true;
	}
	
	@Override
	protected void addHandlers() {
		
		txtAreaClipboard.addFocusListener(new FocusListener() {
			
			@Override
			public void focusLost(FocusEvent e) {
				
			}
			
			@Override
			public void focusGained(FocusEvent e) {
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						applyClipboardTextAreaGraphics();
					}
				});
			}
		});
		
		// Load action
		btnReload.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						if (options.getLastFileOpenedClipboard() != null) {
							try {
								reload(options.getLastFileOpenedClipboard());
							} catch (IOException e1) {
								logger.error("Error reloading current Clipboard panel from file.");
							}
						}
					}
				});
			}
		});

		// Add new Clipboard action
		btnAddNewClipboard.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						if (options.getLastFileOpenedClipboard() != null) {
							try {
								if (!addNewClipboardGui()) {
									dialogHelper.error("File not saved, maybe folder it's not acessible or locked by an other program?!", (String)null);
								}
							} catch (IOException e1) {
								dialogHelper.error("IOException while trying to create new File!", (String)null);
								logger.error("Error adding new Clipboard panel from scratch.");
							}
						}
					}
				});
			}
		});

		// Save action
		btnSave.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						if (options.getLastFileOpenedClipboard() != null) {
							try {
								doAction(options.getLastFileOpenedClipboard());
							} catch (IOException e1) {
								String msg = "Error Saving current changes to file: " + GeneralConfig.CLIPBOARD_PANEL_MAP.get(options.getLastFileOpenedClipboard());
								logger.error(msg);
								dialogHelper.warn(msg, null);
							}
						}
					}
				});
			}
		});
		
		// Combo list refresh when clicking on combo item
		comboClipboards.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				
				if(!isReloadingCombo.get()) {
					
					if(comboClipboards.getSelectedItem() == null) {return;}
					
					if (options.getLastFileOpenedClipboard() != null && !isReloadingCombo.get()) {
						saveCurrClipboardSilent();
					}
					options.setLastFileOpenedClipboard((String)comboClipboards.getSelectedItem());
					if (options.getLastFileOpenedClipboard() != null) {
						SwingUtilities.invokeLater(new Runnable() {
							public void run() {
								try {
									reload(options.getLastFileOpenedClipboard());
									options.lastFileOpenedClipboard = options.getLastFileOpenedClipboard();
								} catch (IOException e1) {
									logger.error("Error reloading current Clipboard panel from file.");
								}
							}
						});
					}
				}
			}
		});
		
		// reload list of selectable elems from props
		btnReloadClipboards.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				isReloadingCombo.set(true);
				if (dialogHelper.yesOrNo("Wanna save current before reload?", "save current before reload?")) {
					saveCurrClipboardSilent();
				}
				isReloadingCombo.set(false);
				reloadComboClipboards(new File(GeneralConfig.CLIBPOARDS_PANEL_SRC_FOLDER+comboClipboards.getItemAt(0)));
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						btnReload.doClick();
					}
				});
			}
		});
		
		// OPEN CURRENT IN SYSTEM EXPLORER
		btnOpenInFilesystem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				new Thread(new Runnable() {
					public void run() {
						GuiUtilsExtended.openInFileSystem(new File(GeneralConfig.PANEL_FILES_FOLDER));
					}
				}).start();
			}
		});
	}
	
	@Override
	protected void addKeyListeners() {
	}

	/**
	 * This method writes clipBoard content into the file memorized in this map with key equals to the given string
	 * As target you should use one that is present in generalConfig panel clipboard map
	 * @throws IOException 
	 */
	@Override
	public void doAction(String target) throws IOException {
		File selected = dataMap.get(target);
		if(selected != null) {
			selected = new File(GeneralConfig.PANEL_FILES_FOLDER+selected.getName());
			selected.mkdirs();
			if(!selected.exists()) {
				selected.createNewFile();
			}
			
			if(FileWorker.writeStringToFile(txtAreaClipboard.getText(), selected, true)) {
				logger.debug("Current clipboard written: " + selected);
				dialogHelper.info("Current clipboard written: " + selected.getAbsolutePath(), " saved!");
			}else {
				dialogHelper.error("There was an error writing to "+ selected, "Error during file write");
				logger.error("There was an error writing to " + selected);
			}
		}else {
			dialogHelper.warn("Given file is NULL, cannot write.", null);
			logger.error("Given file is NULL, cannot write.");
		}
	}
	
	public void saveCurrClipboardSilent() {
		try {
			File selected = dataMap.get(options.getLastFileOpenedClipboard());
			FileWorker.writeStringToFile(txtAreaClipboard.getText(), selected, true);
			logger.info(selected + "Written to file.");
		} catch (Exception e) {
			logger.warn("Erro saving current clipboard");
		}
	}
	
	/**
	 * This method reinitialize the panel based on data contained in file identified by target key.
	 * You can refer to generalConfig to know which kind of panel you can call and their relative files
	 * 
	 * @throws IOException 
	 */
	@Override
	public void reload(String target) throws IOException {
		if (this.isVisible()) {
			File selected = dataMap.get(target);
			// Create fake safe unexisting file to avoid null pointer
			if (selected == null) {
				dialogHelper.warn("Given file has not been found, cannot load content.", null);
				logger.error("Given file has not been found, cannot load content.");
				return;
			}
			if (selected.exists()) {
				// Update text from file
				txtAreaClipboard.setText("");
				FileWorker.appendFileTxtArea(selected, txtAreaClipboard, true);
				logger.debug("Current clipboard reloaded from file: " + selected);
				options.setLastFileOpenedClipboard(target);
			}
		}
	}
	
	public boolean addNewClipboardGui() throws IOException {
		String newName = dialogHelper.askForStringNullable("Choose a name for the new Clipboard (If existing a new unique name like \"xxxx(1).ext\" will be created)", "Specify new clipboard name");
		if(newName != null && newName.equals("")) {
			dialogHelper.warn("Cannot create File without name", "Must specify a name!");
			return true;
		}else if(newName == null) {
			return true;
		}
		
		String extension = FilenameUtils.getExtension(newName);
		newName += (extension.equals(""))? ".txt" : "";
		
		File newFile = new File(GeneralConfig.CLIBPOARDS_PANEL_SRC_FOLDER + newName);
				
		if(newFile.exists()) {
			newFile = FileVarious.uniqueJavaObjFile(newFile);
		}
		
		boolean result = newFile.createNewFile();
		
		if (result) {
			dataMap.put(newFile.getName(), newFile);
			options.lastFileOpenedClipboard = newFile.getName();
			reloadComboClipboards(newFile);
		}else {
			logger.error("File was not added!" + result);
		}
		
		return result;
	}

	@Override
	public void sync() {
		if (this.isVisible() && txtAreaClipboard != null) {
			
			txtAreaClipboard.setWrapStyleWord(true);
			txtAreaClipboard.setLineWrap(true);
			txtAreaClipboard.setMargin(new Insets(8, 8, 8, 8));
			txtAreaClipboard.repaint();
			
			try {
				File found = ((File)dataMap.get(options.getLastFileOpenedClipboard()));
				if(found != null)
					reload(found.getName());
				else
					reload(((File)dataMap.values().toArray()[0]).getName());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	// COMBO METHODS
	
	public void reloadComboClipboards(File selected) {
		isReloadingCombo.set(true);
		
		comboClipboards.removeAllItems();
		DefaultComboBoxModel<String> model = new DefaultComboBoxModel<String>( GeneralConfig.getRefreshedClipBoardsMap().keySet().toArray(new String[2]) );
		comboClipboards.setModel( model );
		
		if(selected == null || !selected.exists()) {
			if (new File(GeneralConfig.CLIBPOARDS_PANEL_SRC_FOLDER + File.separator + options.lastFileOpenedClipboard).exists()) {
				comboClipboards.setSelectedItem(options.lastFileOpenedClipboard);
			}else {
				try {
					comboClipboards.setSelectedItem(0);
					options.lastFileOpenedClipboard = comboClipboards.getItemAt(0);
				} catch (Exception e) {
					logger.error("Exception reloading clipboard element: "+selected, e);
				}
			}
		}else {
			comboClipboards.setSelectedItem(selected.getName());
			options.lastFileOpenedClipboard = selected.getName();
		}
		
		((JLabel)comboClipboards.getRenderer()).setHorizontalAlignment(JLabel.CENTER);
		
		comboClipboards.revalidate();
		comboClipboards.repaint();
		
		applyClipboardTextAreaGraphics();
		
		isReloadingCombo.set(false);
	}
	
	public void selectFromComboClipboardsByFile(String fileName) {
		for(String key : dataMap.keySet()){
			String currentName = dataMap.get(key).getName();
			if(currentName.equals(fileName)) {
				comboClipboards.setSelectedItem(key);
			}
			((JLabel)comboClipboards.getRenderer()).setHorizontalAlignment(JLabel.CENTER);
			comboClipboards.revalidate();
			comboClipboards.repaint();
		}
	}

	public void selectFromComboClipboards(final String key) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				((JLabel) comboClipboards.getRenderer()).setHorizontalAlignment(JLabel.CENTER);
				comboClipboards.revalidate();
				int index = GuiUtils.getFirstIndexMatchFromStringCombo(comboClipboards, key);
				comboClipboards.setSelectedIndex(index);
				comboClipboards.repaint();
			}
		});
	}
	
	public String getCrrentFromCombo() {
		return (String)comboClipboards.getSelectedItem();
	}
	
	
	public void addClipBoard(String ID, File fileRef) {
		dataMap.put(ID, fileRef);
	}

	public void addClipBoards(HashMap<String, File> map) {
		dataMap.putAll(map);
	}

	/**
	 * Get a shared instance of text area, useful if used only one at once
	 */
	public static ClipboardPanelLight getSingletonLazy() {
		singletonInstance = (singletonInstance == null)? new ClipboardPanelLight() : singletonInstance;
		return singletonInstance;
	}
}
