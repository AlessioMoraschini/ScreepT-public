package jmenu;

import java.awt.Font;
import java.awt.Insets;
import java.io.IOException;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.SwingUtilities;

import gui.MenuSetter;
import gui.dialogs.msg.JOptionHelper;
import jmenu.om.UrlRetriever;
import utility.log.SafeLogger;
import utils.SysUtils;
import utils.SysUtils.Browser;

public class BrowserOpenerMenu extends JMenu {
	private static final long serialVersionUID = -4628690082885140560L;
	
	private static SafeLogger logger = new SafeLogger(BrowserOpenerMenu.class);
	
	private JOptionHelper dialogHelper;
	
	UrlRetriever urlRetriever;
	
	public BrowserOpenerMenu(String textLabel, Insets margin, Font subFont, UrlRetriever urlRetriever) {
		super();
		setText(textLabel);
		
		
		this.urlRetriever = urlRetriever;
		
		addBrowserJmenus();
		
		if (margin != null) {
			MenuSetter.setMargin(this, margin);
		}
		
		if (subFont != null) {
			MenuSetter.setFont(this, subFont);
		}
		
		SwingUtilities.invokeLater(() -> {
			dialogHelper = new JOptionHelper(this.getTopLevelAncestor());
		});
	}
	
	private void addBrowserJmenus() {
		
		for(Browser browser : Browser.values()) {
			
			JMenuItem browserItem = new JMenuItem(browser.browserName);
			
			browserItem.addActionListener((e) -> {
				String url = "[UNDEFINED]";
				
				try {
					url = urlRetriever.getDestinationUrl();
					SysUtils.openUrlWithSpecificBrowser(url, browser);
				
				} catch (IOException | InterruptedException e1) {
					logger.error("Error adding browser listener for " + browser, e1);
					dialogHelper.error("Cannot open " + url + " with " + browser.browserName + ": software not found.");
				
				} catch (Exception e1) {
					logger.error("Invalid source URL: " + url, e1);
					dialogHelper.error("Invalid source URL: " + url);
				}
			});
			
			add(browserItem);
		}
	}
}
