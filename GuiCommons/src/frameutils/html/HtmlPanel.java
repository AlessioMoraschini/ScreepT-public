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
package frameutils.html;

import javax.swing.JPanel;
import net.miginfocom.swing.MigLayout;
import resources.GeneralConfig;

import java.awt.Color;

import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

import org.apache.log4j.Logger;

import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.BorderFactory;
import javax.swing.JEditorPane;

public class HtmlPanel extends JPanel{
	private static final long serialVersionUID = -8889757473722770879L;

	// TextFileWriter logger creation
	static Logger logger = Logger.getLogger(HtmlPanel.class);
	
	private JPanel headerPanel;
	public JavaFxPanelHtml jfxPanel;
	public JScrollPane mainScrollPane;
	private JEditorPane mainHtmlEditorPane;
	private String indexID = "indexList";

	public boolean javaFxPaneSelected = false;
	
	/**
	 * Simple java swing version - limited css and effects
	 * @param headerHeight
	 */
	public HtmlPanel(int headerHeight) {
		
		logger.info(this.getClass().getName() + " Starting...");
		
		setBorder(new LineBorder(Color.GRAY, 2, true));
		setBackground(Color.BLACK);
		setLayout(new MigLayout(GeneralConfig.DEBUG_GRAPHICS+"", "[100px][100px,grow][100px]", "[:"+headerHeight+"px:"+headerHeight+"px][100px,grow]"));
		
		headerPanel = new JPanel();
		headerPanel.setBorder(new LineBorder(Color.GRAY, 2));
		headerPanel.setBackground(Color.DARK_GRAY);
		mainScrollPane = new JScrollPane();
		mainScrollPane.setBorder(BorderFactory.createEmptyBorder());
		if (headerHeight > 0) {
			add(headerPanel, "cell 0 0 3 1,grow");
			add(mainScrollPane, "cell 0 1 3 1,grow");
		}else {
			add(mainScrollPane, "cell 0 0 3 2,grow");
		}
		
		mainHtmlEditorPane = new JEditorPane();
		mainHtmlEditorPane.setEditable(false);
		mainScrollPane.setViewportView(mainHtmlEditorPane);
		
		logger.info(this.getClass().getName() + " Initialized!");
	}

	/**
	 * Java fx test contructor
	 * @param headerHeight
	 * @param URL
	 */
	public HtmlPanel(Integer headerHeight, String indexID) {
		
		logger.info(this.getClass().getName() + " Starting...");
		
		this.indexID = indexID;
		javaFxPaneSelected = true;
		jfxPanel = new JavaFxPanelHtml();
//		jfxPanel.add(new JLabel("Loading..."));
		
		setBorder(BorderFactory.createEmptyBorder());
		setBackground(Color.BLACK);
		setLayout(new MigLayout(GeneralConfig.DEBUG_GRAPHICS+", fill, insets 0 ", "[100px][100px,grow][100px]", "[:"+headerHeight+"px:"+headerHeight+"px][100px,grow]"));
		
		headerPanel = new HeaderPanelSearch(jfxPanel);
		headerPanel.setBorder(new EmptyBorder(0,0,0,0));
		headerPanel.setBackground(Color.BLACK);
		jfxPanel.setBorder(BorderFactory.createEmptyBorder());
		if (headerHeight > 0) {
			add(headerPanel, "cell 0 0 3 1,grow");
			add(jfxPanel, "cell 0 1 3 1,grow");
		}else {
			add(jfxPanel, "cell 0 0 3 2,grow");
		}

//		mainScrollPane.setViewportView(jfxPanel);
		
		logger.info(this.getClass().getName() + " Initialized!");
	}

	/**
	 * Load into javaFx version panel
	 * @param pathToHtml
	 */
	public void loadHtmlFx(final String pathToHtml) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				logger.info("Loading html from: " + pathToHtml);
				jfxPanel.loadUrl(pathToHtml, indexID);
				logger.info(pathToHtml + " : Loaded!");
			}
		});
	}
	
	/**
	 * Load into simple java swing version panel
	 * @param pathToHtml
	 */
	public void loadHtml(String pathToHtml) {
		HtmlLoader.loadHtmlInEditPane(mainHtmlEditorPane, pathToHtml);
	}
	
	public JavaFxPanelHtml getFxHtmlPane() {
		return jfxPanel;
	}
	
	public JEditorPane getMainHtmlEditorPane() {
		return mainHtmlEditorPane;
	}

	public void setMainHtmlEditorPane(JEditorPane mainHtmlEditorPane) {
		this.mainHtmlEditorPane = mainHtmlEditorPane;
	}

	public JPanel getHeaderPanel() {
		return headerPanel;
	}

	public void setHeaderPanel(JPanel headerPanel) {
		this.headerPanel = headerPanel;
	}

	public JScrollPane getMainScrollPane() {
		return mainScrollPane;
	}

	public void setMainScrollPane(JScrollPane mainScrollPane) {
		this.mainScrollPane = mainScrollPane;
	}
	
}
