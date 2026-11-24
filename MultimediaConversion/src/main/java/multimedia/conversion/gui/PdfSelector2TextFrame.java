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
package multimedia.conversion.gui;

import various.common.light.i18n.I18n;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;

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
import gui.commons.imagestudio.DialogFit;
import org.icepdf.ri.common.ComponentKeyBinding;
import org.icepdf.ri.common.SwingController;
import org.icepdf.ri.common.SwingViewBuilder;

import gui.commons.dialogutils.GenericFileChooserDialog;
import gui.commons.dialogutils.JOptionHelperExtended;
import gui.commons.frameutils.utils.GuiUtilsExtended;
import gui.commons.splitpanel.SplitPaneUtils;
import multimedia.conversion.ImageToTextExtractor;
import net.miginfocom.swing.MigLayout;
import net.sourceforge.tess4j.TesseractException;
import resources.GeneralConfig;
import resources.IconsPathConfigurator;
import resources.ImagesPathConfigurator;

public class PdfSelector2TextFrame extends JFrame {
	private static final long serialVersionUID = 1165410307763162121L;

	// GraphicPreferencesFrame logger
	static Logger logger = Logger.getLogger(PdfSelector2TextFrame.class);

	private static final String PARSED_DEFAULT_TEXT = "Click \"Extract text\" to preview text";

	// flag to signal active
	public static boolean active = false;

	public int WIDTH = 800;
	public int HEIGHT = GuiUtilsExtended.getScreenSize().height-200;

	private JPanel imageContainerPanel;
	private JTextArea txtAreaParsedText;
	private JScrollPane textPreviewScrollPane;
	private JButton btnLoadImage;
	private JButton btnParseImage;
	private JButton btnInjectSelected;
	private JButton btnFullScreen;
	private JPanel viewerComponentPanel;
	/** The content of the window (the window itself later wraps it in a scroll pane): what goes full screen, and what a panel (tab) reuses */
	private javax.swing.JComponent viewerRoot;
	/** Panel (tab) look: only the viewer, see {@link #setPanelMode()} */
	private boolean panelMode;

	public JSplitPane splitMainPane;

	public JFrame mainFrameParent;
	public JFrame thisFrame;
	public JTextArea outputTEXTarea;

	private File selectedImageSource;
	public ImageToTextExtractor imageExtractor;
	private SwingController controllerPdf;

	JOptionHelperExtended dialogHelper;
	GenericFileChooserDialog fileDialogHelper;

	public PdfSelector2TextFrame(File file, JFrame parentRootFrame, JTextArea outputTxtArea, String title) {
		this(parentRootFrame, outputTxtArea, title);
		loadFile(file);
	}

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

		// the preferred size of the viewer area must not depend on the loaded PDF (the viewer asks for the size of the page),
		// otherwise the window sized at opening would no longer fit after loading a document
		imageContainerPanel = new JPanel(new java.awt.BorderLayout()) {
			@Override
			public java.awt.Dimension getPreferredSize() {
				if (panelMode) {
					return new java.awt.Dimension(400, 300); // the tab gives the size: nothing here may force scroll bars
				}
				java.awt.Dimension viewerMin = getComponentCount() > 0 ? getComponent(0).getMinimumSize() : new java.awt.Dimension(800, 400);
				return new java.awt.Dimension(viewerMin.width + 8, Math.max(viewerMin.height + 8, HEIGHT - 340));
			}
		};
		imageContainerPanel.setBackground(Color.BLACK);
		imageContainerPanel.setBorder(new LineBorder(new Color(0, 0, 0), 2, true));

		initDisplayPanel(imageContainerPanel);

		textPreviewScrollPane = new JScrollPane();
		textPreviewScrollPane.setBorder(new LineBorder(new Color(0, 0, 0), 4));
		textPreviewScrollPane.setBackground(Color.WHITE);
		textPreviewScrollPane.setPreferredSize(new java.awt.Dimension(300, 160));

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
		btnLoadImage.setToolTipText("Choose a PDF file from disk and show it in the viewer above");
		btnLoadImage.setBackground(new Color(230, 230, 250));
		btnLoadImage.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		btnLoadImage.setIcon(new ImageIcon(IconsPathConfigurator.ICON_LOAD));
		btnLoadImage.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 14));
		getContentPane().add(btnLoadImage, "cell 0 2,grow");

		btnParseImage = new JButton("Extract text");
		btnParseImage.setToolTipText("Read the text contained in the PDF (OCR if the pages are images) and show it in the box below");
		btnParseImage.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 14));
		btnParseImage.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		btnParseImage.setIcon(new ImageIcon(IconsPathConfigurator.ICON_EXPORT_LOG));
		btnParseImage.setBackground(new Color(230, 230, 250));
		getContentPane().add(btnParseImage, "cell 1 2,grow");

		btnInjectSelected = new JButton("Inject Selected");
		btnInjectSelected.setToolTipText("Insert the selected text (or all the extracted text if nothing is selected) into the open file editor");
		btnInjectSelected.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 14));
		btnInjectSelected.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		btnInjectSelected.setIcon(new ImageIcon(IconsPathConfigurator.ICON_CHECKMARK));
		btnInjectSelected.setBackground(new Color(176, 196, 222));
		getContentPane().add(btnInjectSelected, "cell 4 2,alignx right,growy");

		btnFullScreen = new JButton("Full screen");
		btnFullScreen.setToolTipText("Show the PDF full screen (F11 or Esc to leave)");
		btnFullScreen.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 14));
		btnFullScreen.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		btnFullScreen.setBackground(new Color(230, 230, 250));
		// the content itself, not getContentPane() at click time: when the window is reused as a panel (tab) its content pane is an empty stand-in
		viewerRoot = (javax.swing.JComponent) getContentPane();
		btnFullScreen.addActionListener(e -> gui.commons.tabs.ViewerFullScreen.toggle(viewerRoot));
		getContentPane().add(btnFullScreen, "cell 3 2,alignx right,growy");
		gui.commons.tabs.ViewerFullScreen.installKey(viewerRoot);

		addWindowListener(new java.awt.event.WindowAdapter() {
		    @Override
		    public void windowClosing(java.awt.event.WindowEvent windowEvent) {

				logger.debug("Closing "+getClass().getName());
				active = false;
	        	dispose();
		    }
		});

		addHandlers();

		// the window opens fitting its content (viewer toolbar included) and has scroll bars only when the screen is too small
		splitMainPane.setResizeWeight(1.0);
		DialogFit.apply(this, mainFrameParent, WIDTH, 0);
		splitMainPane.setDividerLocation(imageContainerPanel.getPreferredSize().height);
		// the viewer toolbars change size once the window is realized: when opened, grow the window if the content does not fit yet
		addWindowListener(new java.awt.event.WindowAdapter() {
			@Override
			public void windowOpened(java.awt.event.WindowEvent e) {
				growToContent(3);
			}
		});

		logger.debug(this.getClass().getName() + "### started");
	}

	/** Grows the window (up to 96% of the usable area of its monitor) until the content fits; beyond that the scroll bars are used */
	private void growToContent(final int attemptsLeft) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				if (!(getContentPane() instanceof JScrollPane)) {
					return;
				}
				JScrollPane scroll = (JScrollPane) getContentPane();
				java.awt.Dimension pref = scroll.getViewport().getView().getPreferredSize();
				java.awt.Dimension extent = scroll.getViewport().getExtentSize();
				int dw = pref.width - extent.width;
				int dh = pref.height - extent.height;
				if (dw <= 0 && dh <= 0) {
					return;
				}
				java.awt.Rectangle screen = getGraphicsConfiguration().getBounds();
				java.awt.Insets insets = java.awt.Toolkit.getDefaultToolkit().getScreenInsets(getGraphicsConfiguration());
				int maxW = (int) ((screen.width - insets.left - insets.right) * 0.96);
				int maxH = (int) ((screen.height - insets.top - insets.bottom) * 0.96);
				int w = Math.min(maxW, getWidth() + Math.max(0, dw));
				int h = Math.min(maxH, getHeight() + Math.max(0, dh));
				if (w != getWidth() || h != getHeight()) {
					int x = Math.max(screen.x, getX() - (w - getWidth()) / 2);
					int y = Math.max(screen.y, getY() - (h - getHeight()) / 2);
					setBounds(x, y, w, h);
					if (attemptsLeft > 1) {
						growToContent(attemptsLeft - 1);
					}
				}
			}
		});
	}

	/** Closes the open document (also when the content lives in a tab of the file editor, where the window is never closed) */
	public void releaseResources() {
		active = false;
		try {
			if (controllerPdf != null) {
				controllerPdf.closeDocument();
				controllerPdf.dispose();
			}
		} catch (Throwable e) {
			logger.warn("Cannot release the PDF viewer: " + e.getMessage());
		}
	}

	public void loadFile(File selected) {
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
						dialogHelper.error(I18n.t("misc.an_error_occurred_cannot_load_pdf"),	"IO Error");
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

	/**
	 * Look of the viewer inside a tab: just the PDF (its own scroll bar is the only one), without the text extraction area and
	 * its buttons; the full screen button sits in the status bar of the viewer, next to the page counter.
	 */
	public void setPanelMode() {
		panelMode = true;
		// the window wrapped its content in a scroll pane to fit small screens: a tab has its own size, so the content goes in directly
		setContentPane(viewerRoot);
		java.awt.Container content = viewerRoot;
		content.removeAll();
		content.setLayout(new java.awt.BorderLayout());
		imageContainerPanel.setBorder(null);
		content.add(imageContainerPanel, java.awt.BorderLayout.CENTER);

		btnFullScreen.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 12));
		btnFullScreen.setMargin(new Insets(1, 8, 1, 8));
		java.awt.LayoutManager layout = viewerComponentPanel.getLayout();
		java.awt.Component status = layout instanceof java.awt.BorderLayout ? ((java.awt.BorderLayout) layout).getLayoutComponent(java.awt.BorderLayout.SOUTH) : null;
		JPanel bottom = new JPanel(new java.awt.BorderLayout());
		if (status != null) {
			viewerComponentPanel.remove(status);
			bottom.add(status, java.awt.BorderLayout.CENTER);
			bottom.setBackground(status.getBackground());
		}
		JPanel buttonHolder = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 6, 2));
		buttonHolder.setOpaque(status == null || status.isOpaque());
		buttonHolder.setBackground(bottom.getBackground());
		buttonHolder.add(btnFullScreen);
		bottom.add(buttonHolder, java.awt.BorderLayout.EAST);
		viewerComponentPanel.add(bottom, java.awt.BorderLayout.SOUTH);
		content.revalidate();
		content.repaint();
	}

	private void initDisplayPanel(JPanel father) {
		controllerPdf = new SwingController();
		// We created the SwingViewFactory configured with the controller
		SwingViewBuilder factory = new SwingViewBuilder(controllerPdf);
		// We use the factory to build a preconfigured JPanel
		// with a full and active viewer user interface.
		viewerComponentPanel = factory.buildViewerPanel();
		// We add keyboard command
		ComponentKeyBinding.install(controllerPdf, viewerComponentPanel);
		// add interactive mouse link annotation support via callback
		controllerPdf.getDocumentViewController().setAnnotationCallback(
		              new org.icepdf.ri.common.MyAnnotationCallback(controllerPdf.getDocumentViewController()));

		// the viewer has its own scroll pane: it must not be nested in another one (scroll glitches)
		father.add(viewerComponentPanel, java.awt.BorderLayout.CENTER);
	}

	private void displayPdf(File sourceFile) throws IOException {
		controllerPdf.openDocument(sourceFile.getAbsolutePath());
	}


}
