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
package frameutils.frame;

import javax.swing.JFrame;

import org.apache.log4j.Logger;

import frameutils.frame.arch.ParentFrame;
import frameutils.utils.GuiUtilsExtended;
import gui.GuiUtils;
import gui.dialogs.color.ColorDialogHelper;
import mouseutils.MousePointerCaller;
import mouseutils.MousePointerUtils;
import net.miginfocom.swing.MigLayout;
import resources.GeneralConfig;
import resources.IconsPathConfigurator;
import utility.KeyListenerUtils;
import utility.manipulation.ConversionUtils;
import utility.manipulation.ImageWorker;
import utility.reflection.ReflectionUtils;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;

import javax.swing.JLabel;
import java.awt.Font;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;

import java.awt.SystemColor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.JButton;

public class ColorPickerFrame extends ParentFrame implements MousePointerCaller{
	private static final long serialVersionUID = -8518015676595593365L;

	static Logger logger = Logger.getLogger(ColorPickerFrame.class);
	
	public static ColorPickerFrame singletonInstance = null;
	
	// GUI ELEMENTS
	private JTextField txtRgb;
	private JTextField txtHex;
	private JLabel lblHex;
	private JLabel lblRgb;
	private JLabel lblColorPicker;
	private JButton btnPickerOn;
	private JPanel panelHeader;
	
	// INSTANCE LOGIC FIELDS
	public boolean active = false;
	public volatile AtomicBoolean hasFocus = new AtomicBoolean(false);
	private MousePointerUtils pointerUtils = null;
	private JLabel lblHsbhsv;
	private JTextField txtHsb;
	private JTextField txtHsl;
	private JLabel lblHsl;
	private JPanel paneMiniColorSample; // COLOR sample updated every time
	
	public JFrame caller;
	
	public static ColorDialogHelper colorDialogHelper = new ColorDialogHelper();
	
	public ColorPickerFrame(JFrame caller) {
		
		colorDialogHelper.colorChangedAction = (color)->{
			updateTextBoxes(color);
		};
		
		this.caller = caller != null ? caller : new JFrame();
		ApplyCursorSUbElements(GuiUtilsExtended.CURSOR_CROSS, this.caller);
		
		active = true;
		
		singletonInstance = this;
		
		setTitle("Color Picker Frame");
		
		getContentPane().setBackground(Color.DARK_GRAY);
		getContentPane().setLayout(new MigLayout("", "[grow][25px:30px,grow]", "[35px:35px,grow][grow]"));
		
		GuiUtilsExtended.setFrameIcon(IconsPathConfigurator.ICON_COLOR_SAMPLER_BIG, this);
		
		initializeGui();
		
		pointerUtils = new MousePointerUtils(getOnClickAction(), this, this);
		
		paneMiniColorSample = new JPanel();
		paneMiniColorSample.setOpaque(true);
		paneMiniColorSample.setCursor(GuiUtils.CURSOR_HAND);
		panelHeader.add(paneMiniColorSample, "cell 2 0,grow");
		
		addHandlers();
		
		setAlwaysOnTop(true);
		hasFocus.set(false);
		GuiUtils.centerComponent(this, getDefaultDimension());
		
		SwingUtilities.invokeLater(()->{
			updateTextBoxes(ImageWorker.randomColorRGB(1.0f));
			setVisible(true);	
			resizeToDefault(true, true, true);
		});
	}
	
	/**
	 * This is the main action to trigger when listener is added and active, and user clicks with mouse
	 */
	public Runnable getOnClickAction() {
		Runnable onclick = new Runnable() {
			@Override
			public void run() {
				if (pointerUtils != null) {
					Color currentSelected = pointerUtils.getCurrentLoadedPXColor();
					updateTextBoxes(currentSelected);
				}
			}
		};
		
		return onclick;
	}
	
	private void updateTextBoxes(Color currentSelected) {
		txtRgb.setText(ConversionUtils.getRgbString(currentSelected, ","));
		txtHsb.setText(ConversionUtils.getHsbString(currentSelected, ":"));
		txtHsl.setText(ConversionUtils.getHslString(currentSelected, ":"));
		txtHex.setText(ConversionUtils.getHexString(currentSelected));
		paneMiniColorSample.setBackground(currentSelected);
	}

	private void updateTextBoxesExceptOne(Color currentSelected, JTextComponent toSkip) {
		
		SwingUtilities.invokeLater(() -> {
			if (!txtRgb.equals(toSkip)) {
				txtRgb.setText(ConversionUtils.getRgbString(currentSelected, ","));
			}
			if (!txtHsb.equals(toSkip)) {
				txtHsb.setText(ConversionUtils.getHsbString(currentSelected, ":"));
			}
			if (!txtHsl.equals(toSkip)) {
				txtHsl.setText(ConversionUtils.getHslString(currentSelected, ":"));
			}
			if (!txtHex.equals(toSkip)) {
				txtHex.setText(ConversionUtils.getHexString(currentSelected));
			}
			paneMiniColorSample.setBackground(currentSelected);
		});
	}

	public FocusListener focusColorPicker() {
		return new FocusListener() {
			
			@Override
			public void focusLost(FocusEvent e) {
				hasFocus.set(false);
			}
			
			@Override
			public void focusGained(FocusEvent e) {
				hasFocus.set(true);
			}
		};
	}
	
	public void addHandlers() {
		// DEFAULT CLOSE ACTIONS 
		addWindowListener(new java.awt.event.WindowAdapter() {
		    @Override
		    public void windowClosing(java.awt.event.WindowEvent windowEvent) {
		            
	        	active = false;
				logger.debug("Closing "+getClass().getName());
				pointerUtils.removeMouseListener();
				ApplyCursorSUbElements(GuiUtilsExtended.CURSOR_DEFAULT, caller);
				singletonInstance = null;
	        	dispose();
		    }
		});
		
		btnPickerOn.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				if(!pointerUtils.enabled.get()) {
					btnPickerOn.setIcon(IconsPathConfigurator.IMG_ICON_LIGHT_ON);
					ApplyCursorSUbElements(GuiUtilsExtended.CURSOR_CROSS, caller);
					pointerUtils.enabled.set(true);
				}else {
					btnPickerOn.setIcon(IconsPathConfigurator.IMG_ICON_LIGHT_OFF);
					ApplyCursorSUbElements(GuiUtilsExtended.CURSOR_DEFAULT, caller);
					pointerUtils.enabled.set(false);
				}
			}
		});
		
		txtRgb.getDocument().addDocumentListener(new DocumentListener() {
			
			@Override
			public void removeUpdate(DocumentEvent e) {
				rgbUpdate();
			}
			
			@Override
			public void insertUpdate(DocumentEvent e) {
				rgbUpdate();
			}
			
			@Override
			public void changedUpdate(DocumentEvent e) {
				rgbUpdate();
			}
		});
		txtRgb.addKeyListener(KeyListenerUtils.getKeyListenerIntTxtField(txtRgb, ",", 1, 0, 255));

		txtHex.getDocument().addDocumentListener(new DocumentListener() {
			
			@Override
			public void removeUpdate(DocumentEvent e) {
				hexUpdate();
			}
			
			@Override
			public void insertUpdate(DocumentEvent e) {
				hexUpdate();
			}
			
			@Override
			public void changedUpdate(DocumentEvent e) {
				hexUpdate();
			}
		});
		txtHex.addKeyListener(KeyListenerUtils.getKeyListenerHexTxtField(txtHex, ",", 1000, 0, 0xFFFFFF));

		txtHsb.getDocument().addDocumentListener(new DocumentListener() {
			
			@Override
			public void removeUpdate(DocumentEvent e) {
				hsbUpdate();
			}
			
			@Override
			public void insertUpdate(DocumentEvent e) {
				hsbUpdate();
			}
			
			@Override
			public void changedUpdate(DocumentEvent e) {
				hsbUpdate();
			}
		});
		txtHsb.addKeyListener(KeyListenerUtils.getKeyListenerFloatTxtField(txtHsb, ":", 0.01f, 0, 1.0f));

		txtHsl.getDocument().addDocumentListener(new DocumentListener() {
			
			@Override
			public void removeUpdate(DocumentEvent e) {
				hslUpdate();
			}
			
			@Override
			public void insertUpdate(DocumentEvent e) {
				hslUpdate();
			}
			
			@Override
			public void changedUpdate(DocumentEvent e) {
				hslUpdate();
			}
		});
		txtHsl.addKeyListener(KeyListenerUtils.getKeyListenerIntTxtField(txtHsl, ":", 1, 0, new int[] {360,100,100}));
		
		paneMiniColorSample.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {

				boolean oldEnabled = pointerUtils.enabled.get();
				pointerUtils.enabled.set(false);
				
				Color selected = colorDialogHelper.askForColor(paneMiniColorSample, false);
				
				if (selected != null) {
					updateTextBoxes(selected);
				}
				
				GuiUtils.launchThreadSafeSwing(() -> {
					try {
						Thread.sleep(500);
					} catch (InterruptedException e1) {
						e1.printStackTrace();
					}
					pointerUtils.enabled.set(oldEnabled);
				});
			}
		});
	}
	
	private void hslUpdate() {
		if (txtHsl.hasFocus()) {
			Color newColor = ConversionUtils.getColorFromHslString(txtHsl.getText(), ":", paneMiniColorSample.getBackground());
			updateTextBoxesExceptOne(newColor, txtHsl);
		}
	}
	
	private void hsbUpdate() {
		if (txtHsb.hasFocus()) {
			Color newColor = ConversionUtils.getColorFromHsbString(txtHsb.getText(), ":", paneMiniColorSample.getBackground());
			updateTextBoxesExceptOne(newColor, txtHsb);
		}
	}
	
	private void rgbUpdate() {
		if (txtRgb.hasFocus()) {
			Color newColor = ConversionUtils.getColorFromRgbString(txtRgb.getText().replaceAll("\\s", ""), ",");
			updateTextBoxesExceptOne(newColor, txtRgb);
		}
	}
	
	private void hexUpdate() {
		if (txtHex.hasFocus()) {
			Color newColor = ConversionUtils.getColorFromHexString(txtHex.getText().replaceAll("\\s", ""), paneMiniColorSample.getBackground());
			updateTextBoxesExceptOne(newColor, txtHex);
		}
	}
	
	public void ApplyCursorSUbElements(Cursor cursor, Component instance) {
		instance.setCursor(cursor);
		try {
			
			List<JScrollPane> resultSet2 = ReflectionUtils.retrieveFieldsByType(instance, JScrollPane.class);
			for (JScrollPane curr : resultSet2) {
				curr.setCursor(cursor);
			}

			List<JTextArea> resultSet = ReflectionUtils.retrieveFieldsByType(instance, JTextArea.class);
			for (JTextArea curr : resultSet) {
				curr.setCursor(cursor);
			}
			
		} catch (IllegalArgumentException | IllegalAccessException e) {
			logger.warn("Error trying to update cursors with reflection");
		}
	}
	
	public void initializeGui() {
		panelHeader = new JPanel();
		panelHeader.setForeground(SystemColor.menu);
		panelHeader.setBorder(null);
		panelHeader.setBackground(Color.BLACK);
		getContentPane().add(panelHeader, "cell 0 0 2 1,grow");
		panelHeader.setLayout(new MigLayout("insets 1, fill", "[25px:n][grow,fill][40px:n]", "[]"));
		
		btnPickerOn = new JButton("");
		btnPickerOn.setCursor(GuiUtilsExtended.CURSOR_HAND);
		btnPickerOn.setBackground(GeneralConfig.BUTTON_BCKG_COLOR_TXT_EDITOR);
		btnPickerOn.setFont(GeneralConfig.BUTTON_DEFAULT_FONT);
		btnPickerOn.setMinimumSize(GeneralConfig.MINIMUM_BUTTON_SIZE);
		btnPickerOn.setIcon(IconsPathConfigurator.IMG_ICON_LIGHT_ON);
		GuiUtilsExtended.makeTransparentJButton(btnPickerOn);
		
		btnPickerOn.setBorder(null);
		panelHeader.add(btnPickerOn, "cell 0 0,grow");
		
		lblColorPicker = new JLabel("Click elements to retrieve their color (or press [SHIFT + CTRL] and move mouse)");
		lblColorPicker.setHorizontalAlignment(SwingConstants.CENTER);
		lblColorPicker.setForeground(Color.WHITE);
		lblColorPicker.setFont(new Font("Segoe UI", Font.PLAIN, 14));
		lblColorPicker.setBackground(SystemColor.activeCaptionBorder);
		panelHeader.add(lblColorPicker, "cell 1 0,growx");
		
		JPanel panelMain = new JPanel();
		panelMain.setLayout(new MigLayout("insets 1, fill",
				"[120px:120px,grow][100px:100px,grow][145px:145px,grow][145px:145px,grow]",
				"[15px:15px][20px:100px,grow]"));
		panelMain.setBackground(Color.LIGHT_GRAY);
		getContentPane().add(panelMain, "cell 0 1 2 1,grow");
		
		lblRgb = new JLabel("RGB");
		lblRgb.setForeground(Color.BLACK);
		lblRgb.setFont(new Font("Segoe UI", Font.BOLD, 14));
		panelMain.add(lblRgb, "cell 0 0,alignx center,aligny center");
		
		lblHex = new JLabel("HEX");
		lblHex.setForeground(Color.BLACK);
		lblHex.setFont(new Font("Segoe UI", Font.BOLD, 14));
		panelMain.add(lblHex, "cell 1 0,alignx center,aligny center");
		
		lblHsbhsv = new JLabel("HSB/HSV");
		lblHsbhsv.setForeground(Color.BLACK);
		lblHsbhsv.setFont(new Font("Segoe UI", Font.BOLD, 14));
		panelMain.add(lblHsbhsv, "cell 2 0,alignx center,aligny center");
		
		lblHsl = new JLabel("HSL");
		lblHsl.setForeground(Color.BLACK);
		lblHsl.setFont(new Font("Segoe UI", Font.BOLD, 14));
		panelMain.add(lblHsl, "cell 3 0,alignx center,aligny center");
		
		txtRgb = new JTextField();
		txtRgb.setMaximumSize(new Dimension(2147483647, 40));
		txtRgb.setFont(new Font("Segoe UI", Font.BOLD, 13));
		txtRgb.setHorizontalAlignment(SwingConstants.CENTER);
		panelMain.add(txtRgb, "cell 0 1,grow");
		txtRgb.setColumns(10);
		
		txtHex = new JTextField();
		txtHex.setMaximumSize(new Dimension(2147483647, 40));
		txtHex.setFont(new Font("Segoe UI", Font.BOLD, 13));
		txtHex.setHorizontalAlignment(SwingConstants.CENTER);
		txtHex.setColumns(10);
		panelMain.add(txtHex, "cell 1 1,grow");
		
		txtHsb = new JTextField();
		txtHsb.setMaximumSize(new Dimension(2147483647, 40));
		txtHsb.setHorizontalAlignment(SwingConstants.CENTER);
		txtHsb.setFont(new Font("Segoe UI", Font.BOLD, 13));
		txtHsb.setColumns(10);
		panelMain.add(txtHsb, "cell 2 1,grow");
		
		txtHsl = new JTextField();
		txtHsl.setMaximumSize(new Dimension(2147483647, 40));
		txtHsl.setHorizontalAlignment(SwingConstants.CENTER);
		txtHsl.setFont(new Font("Segoe UI", Font.BOLD, 13));
		txtHsl.setColumns(10);
		panelMain.add(txtHsl, "cell 3 1,grow");
	}
	
	@Override
	public Dimension getDefaultDimension() {
		return new Dimension(650, 160);
	}
	
	public static ColorPickerFrame getInstance(JFrame parentFrame) {
		return singletonInstance == null ? new ColorPickerFrame(parentFrame) : singletonInstance;
	}

	@Override
	public List<Component> getDisabledElements() {
		return Arrays.asList(this.getComponents());
	}
}
