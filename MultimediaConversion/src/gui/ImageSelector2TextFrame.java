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
package gui;

import java.io.File;
import java.io.IOException;

import javax.swing.JFrame;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import org.apache.log4j.Logger;

import conversion.ImageToTextExtractor;
import dialogutils.GenericFileChooserDialog;
import dialogutils.JOptionHelperExtended;
import frameutils.utils.GuiUtilsExtended;
import resources.GeneralConfig;
import resources.IconsPathConfigurator;
import resources.ImagesPathConfigurator;
import splitpanel.SplitPaneUtils;
import utils.ImageWorkerExtended;
import net.miginfocom.swing.MigLayout;
import net.sourceforge.tess4j.TesseractException;

import java.awt.Color;
import java.awt.Cursor;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.LineBorder;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;

import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import java.awt.Font;
import java.awt.Image;

public class ImageSelector2TextFrame extends JFrame{
	private static final long serialVersionUID = -3553414020339319808L;

	// GraphicPreferencesFrame logger
	static Logger logger = Logger.getLogger(ImageSelector2TextFrame.class); 
	
	private static final String PARSED_DEFAULT_TEXT = "Click \"Extract text\" to preview text";
	
	// flag to signal active
	public static boolean active = false;
	
	public int WIDTH = 880;
	public int HEIGHT = GuiUtilsExtended.getScreenSize().height-200;
	
	private JPanel imageContainerPanel;
	private JTextArea txtAreaParsedText;
	private JScrollPane textPreviewScrollPane;
	private JLabel labelLoadedImage;
	private JButton btnLoadImage;
	private JButton btnParseImage;
	private JButton btnInjectSelected;
	
	public JSplitPane splitMainPane;
	
	public JFrame mainFrameParent;
	public JFrame thisFrame;
	public JTextArea outputTEXTarea;
	
	private File selectedImageSource;
	public ImageToTextExtractor imageExtractor;
	
	JOptionHelperExtended dialogHelper;
	GenericFileChooserDialog fileDialogHelper;

	
	public ImageSelector2TextFrame(JFrame parentRootFrame, JTextArea outputTxtArea, String title) {
		getContentPane().setBackground(Color.LIGHT_GRAY);
		logger.info(this.getClass().getName() + " Starting...");
		active = true;
		
		dialogHelper = new JOptionHelperExtended(this);
		fileDialogHelper = new GenericFileChooserDialog(null);
		
		imageExtractor = new ImageToTextExtractor();
		mainFrameParent = parentRootFrame;
		thisFrame = this;
		outputTEXTarea = outputTxtArea;

		this.setAlwaysOnTop(true);
		this.setTitle(title);
		GuiUtilsExtended.setFrameIcon(ImagesPathConfigurator.MAIN_APP_ICON, this);
		GuiUtilsExtended.centerFrame(this, WIDTH, HEIGHT);
		GuiUtilsExtended.moveFrame(this, GuiUtilsExtended.getScreenSize(mainFrameParent).width/4, 0);
		GuiUtilsExtended.showOnSameScreen(this, mainFrameParent);
		getContentPane().setLayout(new MigLayout(GeneralConfig.DEBUG_GRAPHICS+"", "[130px][130px][grow][grow][350px]", "[grow][300px][35px]"));
		
		imageContainerPanel = new JPanel();
		imageContainerPanel.setBackground(Color.BLACK);
		imageContainerPanel.setBorder(new LineBorder(new Color(0, 0, 0), 3, true));
		imageContainerPanel.setLayout(new MigLayout(GeneralConfig.DEBUG_GRAPHICS+"", "[grow]", "[grow]"));
		
		labelLoadedImage = new JLabel("");
		imageContainerPanel.add(labelLoadedImage, "cell 0 0");
		
		textPreviewScrollPane = new JScrollPane();
		textPreviewScrollPane.setBorder(new LineBorder(new Color(0, 0, 0), 4));
		textPreviewScrollPane.setBackground(Color.WHITE);
//		getContentPane().add(textPreviewScrollPane, "cell 0 1 5 1,grow");
		
		splitMainPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		splitMainPane.setBackground(Color.BLACK);
		splitMainPane.setTopComponent(imageContainerPanel);
		splitMainPane.setBottomComponent(textPreviewScrollPane);
		splitMainPane.setDividerLocation(HEIGHT-300);
		getContentPane().add(splitMainPane, "cell 0 0 5 2,grow");
		SplitPaneUtils.setDividerColors(Color.BLACK, Color.GRAY, splitMainPane);
		
		txtAreaParsedText = new JTextArea();
		txtAreaParsedText.setWrapStyleWord(true);
		txtAreaParsedText.setMargin(new Insets(15, 15, 15, 15));
		txtAreaParsedText.setText(PARSED_DEFAULT_TEXT);
		textPreviewScrollPane.setViewportView(txtAreaParsedText);
		
		btnLoadImage = new JButton("Load Image");
		btnLoadImage.setBackground(new Color(230, 230, 250));
		btnLoadImage.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		btnLoadImage.setIcon(new ImageIcon(IconsPathConfigurator.ICON_LOAD));
		btnLoadImage.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 14));
		getContentPane().add(btnLoadImage, "cell 0 2,grow");
		
		btnParseImage = new JButton("Extract text");
		btnParseImage.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 14));
		btnParseImage.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		btnParseImage.setIcon(new ImageIcon(IconsPathConfigurator.ICON_EXPORT_LOG));
		btnParseImage.setBackground(new Color(230, 230, 250));
		getContentPane().add(btnParseImage, "cell 1 2,grow");
		
		btnInjectSelected = new JButton("Inject Selected");
		btnInjectSelected.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 14));
		btnInjectSelected.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		btnInjectSelected.setIcon(new ImageIcon(IconsPathConfigurator.ICON_CHECKMARK));
		btnInjectSelected.setBackground(new Color(176, 196, 222));
		getContentPane().add(btnInjectSelected, "cell 4 2,alignx right,growy");
		
		addWindowListener(new java.awt.event.WindowAdapter() {
		    @Override
		    public void windowClosing(java.awt.event.WindowEvent windowEvent) {
					
				logger.debug("Closing "+getClass().getName());
				active = false;
	        	dispose();
		    }
		});
		
		addHandlers();
		
		logger.debug(this.getClass().getName() + "### started");
	}
	
	private void addHandlers() {
		
		// LOAD AND DISPLAY IMAGE
		btnLoadImage.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				final File selected = fileDialogHelper.fileImgRead(thisFrame);
				if(selected != null && selected.exists()) {
					final Cursor backup = imageContainerPanel.getCursor();
					imageContainerPanel.setCursor(GuiUtilsExtended.CURSOR_WAIT);
					
					Runnable runner = new Runnable() {
						public void run() {
							try {
								displayImage(selected);
								selectedImageSource = selected;
							} catch (IOException e1) {
								dialogHelper.error("An error occurred, cannot load image :(", "IO Error");
								logger.error(e);
							} finally {

								imageContainerPanel.setCursor(backup);
							}
						}
					};
					SwingUtilities.invokeLater(runner);
				}
			}
		});
		
		// PARSE CURRENT IMAGE TO TEXT
		btnParseImage.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				
				imageContainerPanel.setCursor(GuiUtilsExtended.CURSOR_WAIT);
				txtAreaParsedText.setCursor(GuiUtilsExtended.CURSOR_WAIT);
				btnParseImage.setEnabled(false);
				
				SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>() {
					@Override
				    public Boolean doInBackground() {
						boolean result = true;
						try {
							txtAreaParsedText.setText(imageExtractor.readTextFromImage(selectedImageSource));
						} catch (TesseractException e1) {
							logger.error(e1);
							result = false;
							dialogHelper.error("An error occurred parsing the image :(","Parse Error");
						} 
						imageContainerPanel.setCursor(GuiUtilsExtended.CURSOR_DEFAULT);
						txtAreaParsedText.setCursor(GuiUtilsExtended.CURSOR_TEXT);
						btnParseImage.setEnabled(true);
						return result;
				    }
				};
				worker.execute();
			}
		});
		
		// INJECT TEXT SELECTED
		btnInjectSelected.addActionListener(new ActionListener() {
			
			public void actionPerformed(ActionEvent e) {
				
				String parsedText = txtAreaParsedText.getText();
				String outOldTextRef = outputTEXTarea.getText();
				if(parsedText.equals(PARSED_DEFAULT_TEXT)) {
					return;
				}
				
				boolean useAllText = txtAreaParsedText.getSelectedText() == null;
				
				// backup selected index
				int selectionStart = outputTEXTarea.getSelectionStart();
				int selectionEnd = outputTEXTarea.getSelectionEnd();
				String prefix = outOldTextRef.substring(0, selectionStart);
				String postfix = outOldTextRef.substring(selectionEnd);
				
				outputTEXTarea.setText(prefix);
				if(useAllText) {
					outputTEXTarea.append(txtAreaParsedText.getText());
				}else {
					outputTEXTarea.append(txtAreaParsedText.getSelectedText());
				}
				
				// reset selection using 
				outputTEXTarea.append(postfix);
				outputTEXTarea.setSelectionStart(selectionStart);
				outputTEXTarea.setSelectionEnd(selectionStart + parsedText.length());
			}
		});
	}
	
	private void displayImage(File sourceFile) throws IOException {
		Image sourceFileImage = ImageIO.read(sourceFile);
		Image image = ImageWorkerExtended.scaleImage(sourceFileImage, imageContainerPanel.getWidth(), imageContainerPanel.getHeight(), Image.SCALE_REPLICATE);
		labelLoadedImage.setIcon(new ImageIcon(image));
	}
	
}
