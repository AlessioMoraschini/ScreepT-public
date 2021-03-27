package gui.commons.frameutils.frame.panels.common;

import javax.swing.JButton;

import gui.commons.frameutils.frame.panels.arch.ParentPanel;
import net.miginfocom.swing.MigLayout;
import resources.IconsPathConfigurator;

public class LoadNewSavePanel extends ParentPanel {
	private static final long serialVersionUID = -3789652661940094712L;
	
	Runnable loadAction = () -> {};
	Runnable newAction = () -> {};
	Runnable saveAction = () -> {};
	Runnable saveAsAction = () -> {};
	
	private JButton loadButton;
	private JButton newButton;
	private JButton saveButton;
	private JButton saveAsButton;
	
	public LoadNewSavePanel() {
		this(() -> {},() -> {},() -> {}, () -> {});
	}
	
	public LoadNewSavePanel(Runnable loadAction, Runnable newAction, Runnable saveAction, Runnable saveAsAction) {
		this.loadAction = loadAction != null ? loadAction : () -> {};
		this.newAction = newAction != null ? newAction : () -> {};
		this.saveAction = saveAction != null ? saveAction : () -> {};
		this.saveAsAction = saveAsAction != null ? saveAsAction : () -> {};
	
		super.init();
	}
	
	@Override
	public void initGui() {
		setLayout(new MigLayout("fill, insets 1, hidemode 2", "[::33px,grow][::33px,grow][::33px,grow][::33px,grow]", "[::9px,grow]"));
		
		newButton = getButton("", null, null, IconsPathConfigurator.ICON_FILE_NEW, true);
		add(newButton, "cell 0 0,alignx left,aligny center");
		
		loadButton = getButton("", null, null, IconsPathConfigurator.ICON_FILE_LOAD, true);
		add(loadButton, "cell 1 0,alignx left,aligny center");

		saveButton = getButton("", null, null, IconsPathConfigurator.ICON_FILE_SAVE, true);
		add(saveButton, "cell 2 0,alignx left,aligny center");
		
		saveAsButton = getButton("", null, null, IconsPathConfigurator.ICON_FILE_SAVE_AS, true);
		add(saveAsButton, "cell 3 0,alignx left,aligny center");
		
		initButtons();
	}
	
	public void initButtons() {
		setAllButtonsEnabled(true);
	}
	
	@Override
	protected void addKeyListeners() {
	}

	@Override
	public void addHandlers() {
		saveButton.addActionListener((e) -> {
			setAllButtonsEnabled(false);
			saveAction.run();
			setAllButtonsEnabled(true);
		});

		saveAsButton.addActionListener((e) -> {
			setAllButtonsEnabled(false);
			saveAsAction.run();
			setAllButtonsEnabled(true);
		});
		
		loadButton.addActionListener((e) -> {
			setAllButtonsEnabled(false);
			loadAction.run();
			setAllButtonsEnabled(true);
		});
		
		newButton.addActionListener((e) -> {
			setAllButtonsEnabled(false);
			newAction.run();
			setAllButtonsEnabled(true);
		});
	}

	public void setAllButtonsEnabled(boolean buttonsEnabled) {
		saveButton.setEnabled(buttonsEnabled);
		saveAsButton.setEnabled(buttonsEnabled);
		newButton.setEnabled(buttonsEnabled);
		loadButton.setEnabled(buttonsEnabled);
	}
	
	public void setLoadVisible(boolean loadVisible) {
		loadButton.setVisible(loadVisible);
	}
	public void setSaveVisible(boolean saveVisible) {
		saveButton.setVisible(saveVisible);
	}
	public void setSaveAsVisible(boolean saveAsVisible) {
		saveAsButton.setVisible(saveAsVisible);
	}
	public void setNewVisible(boolean newVisible) {
		newButton.setVisible(newVisible);
	}

	public JButton getLoadButton() {
		return loadButton;
	}

	public JButton getNewButton() {
		return newButton;
	}

	public JButton getSaveButton() {
		return saveButton;
	}

	public JButton getSaveAsButton() {
		return saveAsButton;
	}

	
}
