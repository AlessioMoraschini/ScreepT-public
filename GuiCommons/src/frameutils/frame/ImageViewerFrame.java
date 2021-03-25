package frameutils.frame;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Image;
import java.awt.Window;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowStateListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;

import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import javax.swing.JPanel;
import javax.swing.border.LineBorder;

import frameutils.frame.om.ImageConverterOpener;
import frameutils.frame.panels.FileSelectorDnD;
import resources.IconsPathConfigurator;
import various.common.light.files.FileHistory;
import various.common.light.gui.GuiUtils;
import various.common.light.utility.log.SafeLogger;
import various.common.light.utility.manipulation.ConversionUtils;
import various.common.light.utility.manipulation.ImageWorker;

import javax.swing.JButton;
import javax.swing.SwingUtilities;

public class ImageViewerFrame extends JFrame {
	private static final long serialVersionUID = 8293717691849535908L;
	
	public static SafeLogger logger = new SafeLogger(ImageViewerFrame.class);
	
	public static ImageConverterOpener imageConverterOpener = null;
	
	public static final int REFRESH_PERIOD = 300;
	public static final int MAX_HISTORY_SIZE = 40;

	BufferedImage image = null;
	public File imageFile = null;
	
	JLabel imageContainer = null;
	
	static int headerHeight = 40;
	Dimension imgDimension = new Dimension(900, 500);
	Dimension dimension = new Dimension(900, 500 + headerHeight);
	private JButton btnReload;
	
	public JFrame thisFrame;
	private FileSelectorDnD panelDropper;
	private JLabel dynLblFileSize;
	private JLabel lblFileSize;
	private JLabel lblWidth;
	private JLabel lblHeight;
	private JLabel dynLblWidth;
	private JLabel dynLblHeight;
	
	public static FileHistory history = new FileHistory(MAX_HISTORY_SIZE);
	public static File currentLoaded = null;
	
	boolean active = false;
	private JButton btnPrev;
	private JButton btnNext;
	private JButton btnConvert;
	
	public ImageViewerFrame(File imageFileSource) {
		
		thisFrame = this;
		active = true;
		currentLoaded = imageFileSource != null ? imageFileSource : currentLoaded;
		
		getContentPane().setBackground(Color.BLACK);
		
		GuiUtils.setFrameIcon(IconsPathConfigurator.F_ICON_IMAGE, this);
		
		GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[]{0, 432, 0, 0};
		gridBagLayout.rowHeights = new int[]{40, 253, 0};
		gridBagLayout.columnWeights = new double[]{1.0, 1.0, 1.0, Double.MIN_VALUE};
		gridBagLayout.rowWeights = new double[]{0.0, 1.0, Double.MIN_VALUE};
		getContentPane().setLayout(gridBagLayout);
		
		JPanel panel = new JPanel();
		panel.setMaximumSize(new Dimension(32767, headerHeight));
		panel.setPreferredSize(new Dimension(10, headerHeight));
		panel.setMinimumSize(new Dimension(10, headerHeight));
		panel.setBorder(new LineBorder(Color.LIGHT_GRAY));
		panel.setBackground(new Color(128, 128, 128));
		GridBagConstraints gbc_panel = new GridBagConstraints();
		gbc_panel.gridwidth = 3;
		gbc_panel.insets = new Insets(0, 0, 5, 0);
		gbc_panel.fill = GridBagConstraints.HORIZONTAL;
		gbc_panel.gridx = 0;
		gbc_panel.gridy = 0;
		getContentPane().add(panel, gbc_panel);
		GridBagLayout gbl_panel = new GridBagLayout();
		gbl_panel.columnWidths = new int[]{30, 0, 90, 350, 25, 25, 0, 35, 0, 35, 0, 15, 0, 15};
		gbl_panel.rowHeights = new int[]{0, 0};
		gbl_panel.columnWeights = new double[]{0.0, 0.0, 0.0, 1000.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, Double.MIN_VALUE};
		gbl_panel.rowWeights = new double[]{1.0, Double.MIN_VALUE};
		panel.setLayout(gbl_panel);
		
		btnReload = new JButton("");
		btnReload.setIcon(new ImageIcon(IconsPathConfigurator.ICON_RELOAD));
		btnReload.setCursor(GuiUtils.CURSOR_HAND);
		GuiUtils.makeTransparentJButton(btnReload);
		GridBagConstraints gbc_btnReload = new GridBagConstraints();
		gbc_btnReload.gridwidth = 2;
		gbc_btnReload.insets = new Insets(0, 0, 0, 5);
		gbc_btnReload.gridx = 0;
		gbc_btnReload.gridy = 0;
		panel.add(btnReload, gbc_btnReload);
		
		btnConvert = new JButton("Convert");
		btnConvert.setCursor(GuiUtils.CURSOR_HAND);
		btnConvert.setBackground(new Color(112, 128, 144));
		btnConvert.setPreferredSize(new Dimension(70, 30));
		btnConvert.setFont(new Font("Segoe UI", Font.PLAIN, 15));
		btnConvert.setMinimumSize(new Dimension(95, 28));
		GridBagConstraints gbc_btnConvert = new GridBagConstraints();
		gbc_btnConvert.fill = GridBagConstraints.HORIZONTAL;
		gbc_btnConvert.insets = new Insets(0, 0, 0, 5);
		gbc_btnConvert.gridx = 2;
		gbc_btnConvert.gridy = 0;
		panel.add(btnConvert, gbc_btnConvert);
		
		lblFileSize = new JLabel("File size:");
		lblFileSize.setForeground(Color.BLACK);
		lblFileSize.setFont(new Font("Tahoma", Font.BOLD, 14));
		GridBagConstraints gbc_lblFileSize = new GridBagConstraints();
		gbc_lblFileSize.insets = new Insets(0, 0, 0, 5);
		gbc_lblFileSize.gridx = 4;
		gbc_lblFileSize.gridy = 0;
		panel.add(lblFileSize, gbc_lblFileSize);
		
		dynLblFileSize = new JLabel("");
		dynLblFileSize.setForeground(Color.WHITE);
		dynLblFileSize.setFont(new Font("Tahoma", Font.BOLD, 15));
		GridBagConstraints gbc_dynLblFileSize = new GridBagConstraints();
		gbc_dynLblFileSize.insets = new Insets(0, 0, 0, 5);
		gbc_dynLblFileSize.gridx = 5;
		gbc_dynLblFileSize.gridy = 0;
		panel.add(dynLblFileSize, gbc_dynLblFileSize);
		
		imageContainer = new JLabel();
		imageContainer.setFont(new Font("Segoe UI", Font.BOLD, 17));
		imageContainer.setForeground(Color.LIGHT_GRAY);
		GridBagConstraints gbc_imageContainer = new GridBagConstraints();
		gbc_imageContainer.insets = new Insets(0, 0, 0, 5);
		gbc_imageContainer.anchor = GridBagConstraints.CENTER;
		gbc_imageContainer.gridx = 1;
		gbc_imageContainer.gridy = 1;
		getContentPane().add(imageContainer, gbc_imageContainer);
		
		panelDropper = new FileSelectorDnD(() -> {
			loadResizedImageFile(panelDropper.loadedFile, true);
			SwingUtilities.invokeLater(() -> {
				history.addToHistory(imageFile);
			});
		});
		panelDropper.setBackground(new Color(128, 128, 128));
		panelDropper.addNewTarget(imageContainer);
		panelDropper.txtFieldFilePath.setBackground(Color.DARK_GRAY);
		panelDropper.txtFieldFilePath.setForeground(Color.WHITE);
		panelDropper.txtFieldFilePath.setFont(new Font("Tahoma", Font.BOLD, 14));
		
		GridBagConstraints gbc_panelDropper = new GridBagConstraints();
		gbc_panelDropper.insets = new Insets(0, 0, 0, 5);
		gbc_panelDropper.fill = GridBagConstraints.BOTH;
		gbc_panelDropper.gridx = 3;
		gbc_panelDropper.gridy = 0;
		panel.add(panelDropper, gbc_panelDropper);
		
		lblWidth = new JLabel("Width:");
		lblWidth.setForeground(Color.BLACK);
		lblWidth.setFont(new Font("Tahoma", Font.BOLD, 14));
		GridBagConstraints gbc_lblWidth = new GridBagConstraints();
		gbc_lblWidth.insets = new Insets(0, 0, 0, 5);
		gbc_lblWidth.gridx = 6;
		gbc_lblWidth.gridy = 0;
		panel.add(lblWidth, gbc_lblWidth);
		
		dynLblWidth = new JLabel("");
		dynLblWidth.setForeground(Color.WHITE);
		dynLblWidth.setFont(new Font("Tahoma", Font.BOLD, 15));
		GridBagConstraints gbc_dynLblWidth = new GridBagConstraints();
		gbc_dynLblWidth.insets = new Insets(0, 0, 0, 5);
		gbc_dynLblWidth.gridx = 7;
		gbc_dynLblWidth.gridy = 0;
		panel.add(dynLblWidth, gbc_dynLblWidth);
		
		lblHeight = new JLabel("Height:");
		lblHeight.setForeground(Color.BLACK);
		lblHeight.setFont(new Font("Tahoma", Font.BOLD, 14));
		GridBagConstraints gbc_lblHeight = new GridBagConstraints();
		gbc_lblHeight.insets = new Insets(0, 0, 0, 5);
		gbc_lblHeight.gridx = 8;
		gbc_lblHeight.gridy = 0;
		panel.add(lblHeight, gbc_lblHeight);
		
		dynLblHeight = new JLabel("");
		dynLblHeight.setForeground(Color.WHITE);
		dynLblHeight.setFont(new Font("Tahoma", Font.BOLD, 15));
		GridBagConstraints gbc_dynLblHeight = new GridBagConstraints();
		gbc_dynLblHeight.anchor = GridBagConstraints.WEST;
		gbc_dynLblHeight.insets = new Insets(0, 0, 0, 5);
		gbc_dynLblHeight.gridx = 9;
		gbc_dynLblHeight.gridy = 0;
		panel.add(dynLblHeight, gbc_dynLblHeight);
		
		btnPrev = new JButton("");
		btnPrev.setPreferredSize(new Dimension(18, 12));
		btnPrev.setMinimumSize(new Dimension(18, 12));
		btnPrev.setMaximumSize(new Dimension(18, 12));
		btnPrev.setCursor(GuiUtils.CURSOR_HAND);
		btnPrev.setIcon(new ImageIcon(IconsPathConfigurator.ICON_PREV));
		GuiUtils.makeTransparentJButton(btnPrev);
		GridBagConstraints gbc_btnPrev = new GridBagConstraints();
		gbc_btnPrev.anchor = GridBagConstraints.EAST;
		gbc_btnPrev.insets = new Insets(0, 0, 0, 5);
		gbc_btnPrev.gridx = 11;
		gbc_btnPrev.gridy = 0;
		panel.add(btnPrev, gbc_btnPrev);
		
		btnNext = new JButton("");
		btnNext.setPreferredSize(new Dimension(18, 12));
		btnNext.setMinimumSize(new Dimension(18, 12));
		btnNext.setMaximumSize(new Dimension(18, 12));
		btnNext.setCursor(GuiUtils.CURSOR_HAND);
		btnNext.setIcon(new ImageIcon(IconsPathConfigurator.ICON_NEXT));
		GuiUtils.makeTransparentJButton(btnNext);
		GridBagConstraints gbc_btnNext = new GridBagConstraints();
		gbc_btnNext.anchor = GridBagConstraints.WEST;
		gbc_btnNext.gridx = 12;
		gbc_btnNext.gridy = 0;
		panel.add(btnNext, gbc_btnNext);
		
		addHandlers();
		
		loadResizedImageFile(imageFileSource, false);
		
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		
		setMinimumSize(getDimension());
		GuiUtils.centerComponent(this, getDimension());
		
		setVisible(true);
	}
	
	public static void loadFromHistory(boolean previous) {
		if (!history.isEmpty()) {
			currentLoaded = previous ? history.getPreviousInHistory(currentLoaded) : history.getNextInHistory(currentLoaded);
			loadIntoLastExistingInstance(currentLoaded);
		}
	}
	
	private void addHandlers() {
		
		addWindowListener(new java.awt.event.WindowAdapter() {
		    @Override
		    public void windowClosing(java.awt.event.WindowEvent windowEvent) {
		            
	        	logger.debug("Closing Frame: " + getClass().getName());	        	
	        	active = false;
	        	
	        	dispose();
		    }
		});
		
		thisFrame.addWindowStateListener(new WindowStateListener() {
			@Override
			public void windowStateChanged(WindowEvent e) {
				setDimension(thisFrame.getSize());
				setImgDimension(new Dimension(getDimension().width - 5, getDimension().height - headerHeight));
			}
		});
		
		thisFrame.addComponentListener(new ComponentListener() {
			@Override
			public void componentShown(ComponentEvent e) {
			}
			@Override
			public void componentResized(ComponentEvent e) {
				setDimension(thisFrame.getSize());
				setImgDimension(new Dimension(getDimension().width - 5, getDimension().height - headerHeight));
			}
			@Override
			public void componentMoved(ComponentEvent e) {
			}
			@Override
			public void componentHidden(ComponentEvent e) {
			}
		});

		GuiUtils.launchThreadSafeSwing(() -> {
			Dimension lastFrameDimension = getDimension();
			while(active) {
				if(lastFrameDimension != getDimension()){
					try {
						lastFrameDimension = getDimension();
						setImgDimension(getSize() != null ? getSize() : getImgDimension());
						loadResizedImageFile(imageFile, false);
						Thread.sleep(REFRESH_PERIOD);
					} catch (InterruptedException e1) {
						logger.error("an error occurred while refreshing current file: " + imageFile, e1);
					}
				}
			}
		});
		
		btnReload.addActionListener((e) -> {
			loadResizedImageFile(imageFile, true);
		});
		
		btnConvert.addActionListener((e) -> {
			if (imageConverterOpener != null) {
				imageConverterOpener.run();
			}
		});
		
		btnPrev.addActionListener((e) -> {
			loadFromHistory(true);
		});

		btnNext.addActionListener((e) -> {
			loadFromHistory(false);
		});
	}
	
	
	public Dimension getImgDimension() {
		return imgDimension;
	}

	public void setImgDimension(Dimension imgDimension) {
		this.imgDimension = imgDimension;
	}

	public Dimension getDimension() {
		return dimension;
	}
	
	public void setDimension(Dimension dimension) {
		this.dimension = dimension;
	}
	
	private void loadResizedImageFile(File imageFile, boolean forceRefresh) {
		try {
			imageContainer.setText("");

			if (forceRefresh || this.imageFile == null || !this.imageFile.equals(imageFile)) {
				setTitle(imageFile.getName());
				dynLblFileSize.setText(ConversionUtils.coolFileSize(imageFile));
				panelDropper.txtFieldFilePath.setText(imageFile.getCanonicalPath());
				panelDropper.txtFieldFilePath.setToolTipText(imageFile.getCanonicalPath());
				this.imageFile = imageFile;
				this.image = ImageIO.read(imageFile);
				dynLblWidth.setText(String.valueOf(image.getWidth()) + " Px");
				dynLblHeight.setText(String.valueOf(image.getHeight()) + " Px");
				
				history.addToHistoryIfNotPresent(imageFile);
			}
			
			imageContainer.setCursor(GuiUtils.CURSOR_WAIT);
			SwingUtilities.invokeLater(() -> {
				Dimension sameRatioDim = ImageWorker.getDimensionSameRatio(image, getImgDimension().width, getImgDimension().height);
				imageContainer.setIcon(new ImageIcon(image.getScaledInstance(sameRatioDim.width, sameRatioDim.height, Image.SCALE_DEFAULT)));
				imageContainer.setCursor(GuiUtils.CURSOR_DEFAULT);
				
			});
			
		} catch (Exception e) {
			imageContainer.setIcon(null);
			imageContainer.setText("There was an error while loading image :(");
			logger.error("Cannot preload image, an error occurred", e);
		}
	}
	
	public static void newInstanceIfNotExisting(File imageFile) {
		
		logger.info("Image file to open: " + imageFile);
		
		if(imageFile == null || !imageFile.exists()) {
			logger.warn("Cannot load image -> Invalid input file: " + imageFile);
			return;
		}

		boolean instanceFileFound = false;
		for(ImageViewerFrame viewer : getViewerInstanceList()) {
			if(viewer.imageFile != null && viewer.imageFile.equals(imageFile)) {
				instanceFileFound = true;
				viewer.toFront();
				viewer.setVisible(true);
			}
		} 

		if(!instanceFileFound){
			GuiUtils.launchThreadSafeSwing(() -> {
				new ImageViewerFrame(imageFile);
				logger.info("Image file loaded into viewer!");
			});
		}
	}

	public static void loadIntoLastExistingInstance(File file) {
		
		File imageFile = file;
		
		logger.info("Image file to open: " + imageFile);
		
		if(imageFile == null || !imageFile.exists()) {
			if(!history.isEmpty()) {
				imageFile = history.getLast();
			}
			logger.warn("Cannot load image -> Invalid input file: " + imageFile + " -> Last in history loaded: " + history.getLast());
		}
		
		boolean instanceFileFound = false;
		for(ImageViewerFrame viewer : getViewerInstanceList()) {
			instanceFileFound = true;
			viewer.toFront();
			viewer.setVisible(true);
			viewer.loadResizedImageFile(imageFile, false);
			logger.info("Image file already loaded into viewer!");
			return;
		} 
		
		if(!instanceFileFound){
			new ImageViewerFrame(imageFile);
			logger.info("Image file loaded into viewer!");
		}
	}
	
	public static ArrayList<ImageViewerFrame> getViewerInstanceList(){
		ArrayList<ImageViewerFrame> viewers = new ArrayList<>();
		for(Window window : Window.getWindows()) {
			if(window instanceof ImageViewerFrame) {
				viewers.add((ImageViewerFrame) window);
			}
		}
		
		return viewers;
	}
}
