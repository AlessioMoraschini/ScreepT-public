package gui.config;

import java.awt.Color;
import java.awt.Window;
import java.awt.event.WindowEvent;

import javax.swing.JPanel;

import utils.config.AbstractConfig;
import various.common.light.gui.dialogs.msg.JOptionHelper;

public abstract class AbstractOptionPanel extends JPanel {
	private static final long serialVersionUID = 3303466739933233690L;
	
	AbstractConfig config;
	JOptionHelper dialogHelper;

	public AbstractOptionPanel(AbstractConfig config) {
		this.setBackground(Color.LIGHT_GRAY);
		this.dialogHelper = new JOptionHelper(this.getParent());
		this.config = config;
	}
	
	public void reload() {
		if(config != null) {
			config.getProperties().readFromFile();
		}
	};
	
	public boolean save() {
		boolean result = false;

		if(config != null) {
			result = config.getProperties().saveAllProperties();
		}
		
		
		return result;
	};
	
	protected void showOutcomePopUp(boolean saved) {
		if (saved) {
			dialogHelper.info(config.getClass().getSimpleName() + " saved!");
		} else {
			dialogHelper.error("Error saving: " + config.getClass().getSimpleName());
		}
	}
	
	public void discard() {
		Window parent = (Window)this.getParent().getParent().getParent().getParent().getParent().getParent().getParent();
		parent.dispatchEvent(new WindowEvent(parent, WindowEvent.WINDOW_CLOSING));
	};

	public abstract void initializeFromConfig();
}
