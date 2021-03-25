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

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.border.LineBorder;

import org.apache.log4j.Logger;
import org.icepdf.ri.common.ComponentKeyBinding;
import org.icepdf.ri.common.SwingController;
import org.icepdf.ri.common.SwingViewBuilder;

import conversion.ImageToTextExtractor;
import dialogutils.GenericFileChooserDialog;
import dialogutils.JOptionHelperExtended;
import frameutils.utils.GuiUtilsExtended;
import net.miginfocom.swing.MigLayout;
import net.sourceforge.tess4j.TesseractException;
import plugin.external.arch.IPlugin;
import resources.GeneralConfig;
import resources.IconsPathConfigurator;
import resources.ImagesPathConfigurator;
import splitpanel.SplitPaneUtils;

public class PdfSelector2TextFrame extends JFrame {
	private static final long serialVersionUID = 1165410307763162121L;

	// GraphicPreferencesFrame logger
	static Logger logger = Logger.getLogger(PdfSelector2TextFrame.class);

	private static final String PARSED_DEFAULT_TEXT = "Click \"Extract text\" to preview text";

	// flag to signal active
	public static boolean active = false;

	public int WIDTH = 800;
	public int HEIGHT = GuiUtilsExtended.getScreenSize().height-200;

	private JScrollPane imageContainerPanel;
	private JTextArea txtAreaParsedText;
	private JScrollPane textPreviewScrollPane;
	private JButton btnLoadImage;
	private JButton btnParseImage;
	private JButton btnInjectSelected;

	public JSplitPane splitMainPane;

	public JFrame mainFrameParent;
	public JFrame thisFrame;
	public JTextArea outputTEXTarea;

	private File selectedImageSource;
	public ImageToTextExtractor imageExtractor;
	private SwingController controllerPdf;

	JOptionHelperExtended dialogHelper;
	GenericFileChooserDialog fileDialogHelper;

	public PdfSelector2TextFrame(JFrame parentRootFrame, JTextArea outputTxtArea, String title) {
		getContentPane().setBackground(Color.LIGHT_GRAY);
		logger.info(this.getClass().getName() + " Starting...");
		active = true;

		dialogHelper = new JOptionHelperExtended(this);

		imageExtractor = new ImageToTextExtractor();
		mainFrameParent = parentRootFrame == null ? new JFrame() : parentRootFrame;
		thisFrame = this;
		outputTEXTarea = outputTxtArea == null ? new JTextArea() : outputTxtArea;

		fileDialogHelper = new GenericFileChooserDialog(null);

		this.setAlwaysOnTop(true);
		this.setTitle(title);
		GuiUtilsExtended.setFrameIcon(ImagesPathConfigurator.MAIN_APP_ICON, this);
		GuiUtilsExtended.centerFrame(this, WIDTH, HEIGHT);
		GuiUtilsExtended.moveFrame(this, GuiUtilsExtended.getScreenSize().width/4, 0);
		GuiUtilsExtended.showOnSameScreen(this, mainFrameParent);
		getContentPane().setLayout(new MigLayout(GeneralConfig.DEBUG_GRAPHICS+"", "[130px][130px][grow][grow][350px]", "[grow][300px][35px]"));

		imageContainerPanel = new JScrollPane();
		imageContainerPanel.setBackground(Color.BLACK);
		imageContainerPanel.setBorder(new LineBorder(new Color(0, 0, 0), 2, true));

		initDisplayPanel(imageContainerPanel);

		textPreviewScrollPane = new JScrollPane();
		textPreviewScrollPane.setBorder(new LineBorder(new Color(0, 0, 0), 4));
		textPreviewScrollPane.setBackground(Color.WHITE);

		splitMainPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		splitMainPane.setBackground(Color.BLACK);
		splitMainPane.setTopComponent(imageContainerPanel);
		splitMainPane.setBottomComponent(textPreviewScrollPane);
		splitMainPane.setDividerLocation(HEIGHT-300);
		SplitPaneUtils.setDividerColors(Color.BLACK, Color.GRAY, splitMainPane);
		getContentPane().add(splitMainPane, "cell 0 0 5 2,grow");

		txtAreaParsedText = new JTextArea();
		txtAreaParsedText.setWrapStyleWord(true);
		txtAreaParsedText.setMargin(new Insets(15, 15, 15, 15));
		txtAreaParsedText.setText(PARSED_DEFAULT_TEXT);
		textPreviewScrollPane.setViewportView(txtAreaParsedText);

		btnLoadImage = new JButton("Load Pdf");
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

	private void loadFile(File selected) {
		if(selected != null && selected.exists()) {
			final Cursor backup = imageContainerPanel.getCursor();
			imageContainerPanel.setCursor(GuiUtilsExtended.CURSOR_WAIT);

			Runnable runner = new Runnable() {
				@Override
				public void run() {
					try {
						displayPdf(selected);
						selectedImageSource = selected;
					} catch (IOException e1) {
						dialogHelper.error("An error occurred, cannot load pdf :(",	"IO Error");
						logger.error(e1);
					} finally {

						imageContainerPanel.setCursor(backup);
					}
				}
			};
			SwingUtilities.invokeLater(runner);
		}
	}

	private void addHandlers() {

		// LOAD AND DISPLAY IMAGE
		btnLoadImage.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				final File selected = fileDialogHelper.fileRead(thisFrame);
				loadFile(selected);
			}
		});

		// PARSE CURRENT IMAGE TO TEXT
		btnParseImage.addActionListener(new ActionListener() {
			@Override
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

			@Override
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

	private void initDisplayPanel(JScrollPane father) {
		controllerPdf = new SwingController();
		// We created the SwingViewFactory configured with the controller
		SwingViewBuilder factory = new SwingViewBuilder(controllerPdf);
		// We use the factory to build a preconfigured JPanel
		// with a full and active viewer user interface.
		JPanel viewerComponentPanel = factory.buildViewerPanel();
		viewerComponentPanel.setPreferredSize(new Dimension(400, 243));
		viewerComponentPanel.setMaximumSize(new Dimension(400, 243));
		// We add keyboard command
		ComponentKeyBinding.install(controllerPdf, viewerComponentPanel);
		// add interactive mouse link annotation support via callback
		controllerPdf.getDocumentViewController().setAnnotationCallback(
		              new org.icepdf.ri.common.MyAnnotationCallback(controllerPdf.getDocumentViewController()));

		father.setViewportView(viewerComponentPanel);
	}

	private void displayPdf(File sourceFile) throws IOException {
		controllerPdf.openDocument(sourceFile.getAbsolutePath());
	}

	public static class PDFReaderPlugin implements IPlugin {
		@Override
		public String getPluginName() {
			return "Pdf Viewer";
		}

		@Override
		public String getPluginZipName() {
			return "Tesseract.zip";
		}

		@Override
		public void initialize() {
			// TODO Auto-generated method stub
		}

		@Override
		public boolean openFrame(List<File> files) {
			PdfSelector2TextFrame pdfParserBox = new PdfSelector2TextFrame(null, null,
					GeneralConfig.APPLICATION_NAME + " - Pdf2Text");
			pdfParserBox.setVisible(true);
			if(files != null && !files.isEmpty())
				pdfParserBox.loadFile(files.get(0));
			return true;
		}

		@Override
		public boolean launchMain(String[] args) {
			PdfSelector2TextFrame pdfParserBox = new PdfSelector2TextFrame(null, null,
					GeneralConfig.APPLICATION_NAME + " - Pdf2Text");
			pdfParserBox.setVisible(true);
			if(args != null && args.length > 0)
				pdfParserBox.loadFile(new File(args[0]));

			return true;
		}

		@Override
		public void kill() {
			// TODO Auto-generated method stub
		}
	}

}
