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
package gui.commons.frameutils.html;

import com.sun.javafx.application.PlatformImpl;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.concurrent.Worker;
import javafx.concurrent.Worker.State;
import javafx.embed.swing.JFXPanel;
import javafx.event.EventHandler;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import net.miginfocom.swing.MigLayout;
import os.commons.utils.SysUtils;
import resources.GeneralConfig;
import resources.SoundsConfigurator;
import resources.SoundsManagerExtended;

import java.awt.Color;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.events.Event;
import org.w3c.dom.events.EventTarget;
import com.sun.webkit.dom.HTMLAnchorElementImpl;
import com.sun.webkit.dom.HTMLElementImpl;
  
/** 
 * SwingFXWebView 
 */  
@SuppressWarnings("restriction")
public class JavaFxPanelHtml extends JPanel {  
	private static final long serialVersionUID = 5581554551184234562L;

	private int W = 1200;
	private int H = 700;
	public String URL_Loaded;
	public HTMLElementImpl indexElement = null;
	/** Ctrl+F pressed inside the page (run on the EDT) */
	public volatile Runnable onFindShortcut; 
	public static final String DESELECT_NSTART = " if (window.getSelection) {window.getSelection().removeAllRanges();}\r\n" + 
												 " else if (document.selection) {document.selection.empty();}";
	
    private Stage stage;  
    private WebView browser;  
    private JFXPanel jfxPanel;  
    private WebEngine webEngine;  
  
    public JavaFxPanelHtml(){  
    	jfxPanel = new JFXPanel();  
    	jfxPanel.setBackground(Color.DARK_GRAY);
    	setBorder(BorderFactory.createEmptyBorder());
    	setBackground(Color.BLACK);
        setLayout(new MigLayout(GeneralConfig.DEBUG_GRAPHICS+", fill, insets 0", "[1000px,grow]", "[1000px,grow]"));
        add(jfxPanel, "cell 0 0,grow");
        
    }
    
	public void resizeComp(int width, int height) {
    	W = width;
    	H = height;
    	
    	try {
			browser.setPrefWidth(W);
			browser.setPrefHeight(H);
			stage.setMaxWidth(W);
			stage.setHeight(H);
		} catch (NullPointerException e) {
			
		}
    }
     
	public void loadUrl(String url, String indexId) {
    	PlatformImpl.runAndWait(new Runnable() {  
            @Override
            public void run() {  
                
                stage = new Stage();  
                 
                stage.setTitle("Hello Java FX");  
                stage.setResizable(true);  
   
                Group root = new Group();  
                Scene scene = new Scene(root,1400,1400);  
                stage.setScene(scene);  
                 
                // Set up the embedded browser:
                browser = new WebView();
                webEngine = browser.getEngine();
                webEngine.load(url);
                URL_Loaded = url;
                
                resizeComp(W, H);
                
                ObservableList<Node> children = root.getChildren();
                children.add(browser);
                jfxPanel.setScene(scene);
                
                // ON READY EVENTS (OPEN HTTP ANCHORS IN BROWSER)
                addEventsHandlers(scene, indexId);
            }  
        });  
    }
    
    public void addEventsHandlers(Scene scene, String indexId) {
    	webEngine.getLoadWorker().stateProperty().addListener(new ChangeListener<Worker.State>() {
        	
        	@Override
        	public void changed(ObservableValue<? extends State> observable, State oldValue, State newValue) {
        		if (webEngine.getLoadWorker().stateProperty().getValue().equals(State.SUCCEEDED)) {
        			openClassAnchorsInExternalBrowser(webEngine.getDocument());
        			indexElement = initIndex(indexId);
        			
        			scene.setOnKeyReleased(new EventHandler<KeyEvent>() {
						@SuppressWarnings("incomplete-switch")
						@Override
						public void handle(KeyEvent event) {
							
							// ALT PRESSED
							if (event.isAltDown()) {
								switch (event.getCode()) {
									case LEFT: {
										webEngine.executeScript("history.back()");
										break;
									}
									case RIGHT: {
										webEngine.executeScript("history.forward()");
										break;
									}
								}
								
							// CTRL + F: go to the search field
							}else if (event.isControlDown() && !event.isShiftDown() && event.getCode() == KeyCode.F) {
								Runnable findShortcut = onFindShortcut;
								if (findShortcut != null) {
									SwingUtilities.invokeLater(findShortcut);
								}

							// CTRL + SHIFT + 0 PRESSED
							}else if (event.isControlDown() && event.isShiftDown()) {
								switch (event.getCode()) {
								case DIGIT0:	{
									webEngine.reload();
									break;
								}
								default:
									break;
								}
							}
						}
					});// end key released handler
        		}
        	}
        });
    }
    
    public HTMLElementImpl initIndex(String indexId) {
    	if (webEngine != null) {
			if (webEngine.getLoadWorker().stateProperty().getValue().equals(State.SUCCEEDED)) {
				return (HTMLElementImpl) webEngine.getDocument().getElementById(indexId);
			}
		}
    	
    	return null;
    }
    
    private void openClassAnchorsInExternalBrowser(Document document) {
    	NodeList nodeList = document.getElementsByTagName("a");
        for (int i = 0; i < nodeList.getLength(); i++)
        {
            org.w3c.dom.Node node= nodeList.item(i);
            EventTarget eventTarget = (EventTarget) node;
            eventTarget.addEventListener("click", new org.w3c.dom.events.EventListener()
            {
                public void handleEvent(Event evt)
                {
					EventTarget target = evt.getCurrentTarget();
					HTMLAnchorElementImpl anchorElement = (HTMLAnchorElementImpl) target;
					String href = anchorElement.getHref();
					if (href.startsWith("http")) {
						SysUtils.openUrlInBrowser(href);
						evt.preventDefault();
					}
                }
            }, false);
        }
    }
    
    public void reload() {
    	Platform.runLater(new Runnable() {

            @Override
            public void run() {
                webEngine.reload();
                indexElement = null;
            }
        });
    }
    
    public void goBack() {
    	Platform.runLater(new Runnable() {

            @Override
            public void run() {
            	webEngine.executeScript("history.back()");
            }
        });
    }
    
    public void goNext() {
    	Platform.runLater(new Runnable() {

            @Override
            public void run() {
            	webEngine.executeScript("history.forward()");
            }
        });
    }
    
    public void findTextNHighLight(String targetText) {
    	SoundsManagerExtended.playSound(SoundsConfigurator.CLICK_2, null);
    	findText(targetText, false, false);
    }

    /**
     * Selects and scrolls to the next (or previous) match, wrapping around the page
     * @param fromStart restart from the top of the page (used while typing)
     */
    public void findText(String targetText, boolean backwards, boolean fromStart) {
    	if (targetText == null || targetText.isEmpty()) {
    		deselectAll();
    		return;
    	}
    	String script = (fromStart ? DESELECT_NSTART : "")
    			+ "window.find('" + escapeJs(targetText) + "', false, " + backwards + ", true, false, false, false);";
    	Platform.runLater(() -> webEngine.executeScript(script));
    }

    private static String escapeJs(String text) {
    	return text.replace("\\", "\\\\").replace("'", "\\'").replace("\r", " ").replace("\n", " ");
    }
    
    public void deselectAll() {
    	
    	Platform.runLater(new Runnable() {

            @Override
            public void run() {
            	webEngine.executeScript(DESELECT_NSTART);
            }
    	});
    }

    @Deprecated
    public void selectAll(String match, String backgroundJsColor) {
    	
    	String selectAllScript = "if (window.find && window.getSelection) { "
    			+ "document.designMode = \"on\";"
    			+ "var sel = window.getSelection();"
    			+"sel.collapse(document.body, 0);"
    			+"while (window.find("+match+")) {"
                	+"document.execCommand(\"HiliteColor\", false, "+backgroundJsColor+");"
                	+"sel.collapseToEnd();"
            	+"} document.designMode = \"off\";}";
    	
    	Platform.runLater(new Runnable() {
    		@Override
    		public void run() {
    			webEngine.executeScript(selectAllScript);
    		}
    	});
    }
    
    public void goToIndex() {
    	Platform.runLater(new Runnable() {

            @Override
            public void run() {
		    	if(indexElement != null) {
		    		//indexElement.scrollIntoView(true);
		    		webEngine.executeScript(getToIndexJS());
		    	}
            }
    	});
    }
    
    private String getToIndexJS() {
    	if(indexElement == null) {
    		return "";
    	}else {
    		return " var elmnt = document.getElementById(\""+ indexElement.getId() +"\");\r\n elmnt.scrollIntoView();";
    	}
    }
    
    public boolean openInBrowser() {
    	if (URL_Loaded != null && SysUtils.openUrlInBrowserNative(URL_Loaded)) {
			return true;
		}else {
			return false;
		}
    }
}