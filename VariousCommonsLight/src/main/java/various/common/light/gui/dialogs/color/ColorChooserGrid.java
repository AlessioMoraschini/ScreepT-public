package various.common.light.gui.dialogs.color;

import java.awt.Color;
import java.awt.Dimension;

import javax.swing.JDialog;
import net.miginfocom.swing.MigLayout;
import various.common.light.gui.GuiUtils;
import various.common.light.om.HslColor;
import various.common.light.utility.manipulation.ConversionUtils;

import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import java.awt.Font;

import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import javax.swing.JTextField;
import java.awt.GridLayout;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class ColorChooserGrid extends JDialog {
	private static final long serialVersionUID = 4022751431727286180L;

	public static Color DEFAULT_COLOR = Color.WHITE;
	public static Border DEFAULT_BORDER = new LineBorder(Color.BLACK, 2);
	public static Border SELECTED_BORDER = new LineBorder(Color.GRAY, 4);
	public static Font DEFAULT_BUTTON_FONT = new Font("Tahoma", Font.PLAIN, 16);

	public Color selectedColor;
	
	private Dimension DEFAULT_SIZE = new Dimension(550, 450);
	
	public int rows = 14;
	public int columns = 15;
	public int cells = rows*columns;
	
	private JTextField txtHexColorSelection;
	private JPanel panelGridColors;
	private JPanel panelColorSelectionPreview;
	private JButton btnAdvancedColor;
	private JButton btnUseColor;
	
	private JPanel[][] panelMatrix;
	
	public UserChoice userChoice = UserChoice.NONE;
	
	protected ColorChooserGrid() {
		
		commonInit();
		updateColorSelectionPreview(null);
		
		setVisible(true);
	}

	protected ColorChooserGrid(int rows, int columns) {
		
		this.rows = rows;
		this.columns = columns;
		
		commonInit();
		updateColorSelectionPreview(null);
		
		setVisible(true);
	}
	
	protected ColorChooserGrid(Color selectedColor, String title) {
		commonInit();

		if(selectedColor != null) {
			this.selectedColor = selectedColor;
		}
		updateColorSelectionPreview(null);
		setTitle("Select Color - " + title);

		setVisible(true);
	}
	
	protected ColorChooserGrid(Color selectedColor) {
		commonInit();

		if(selectedColor != null) {
			this.selectedColor = selectedColor;
		}
		updateColorSelectionPreview(null);
		
		setVisible(true);
	}
	
	private void commonInit() {
		cells = rows*columns;
		
		panelMatrix = new JPanel[rows][columns];
		
		if(selectedColor != null) {
			this.selectedColor = DEFAULT_COLOR;
		}
		
		setModal(true);
		setModalityType(ModalityType.TOOLKIT_MODAL);
		setAlwaysOnTop(true);
		
		initGui();
		
		txtHexColorSelection.setEditable(false);
	}
	
	private void updateColorSelectionPreview(Color color) {
		selectedColor = color != null ? color : selectedColor;
		panelColorSelectionPreview.setBackground(selectedColor);
		txtHexColorSelection.setText(ConversionUtils.getHexString(selectedColor).toUpperCase());
	}
	
	private void initGui() {
		
		setTitle("Select Color");
		setIconImage(new ImageIcon(getClass().getResource("/res/imgs/paintPalette.png")).getImage());
		
		getContentPane().setLayout(new MigLayout("", "[][grow][40px:n:40px][grow]", "[grow][5px:5px][:40px:40px,grow]"));
		
		setMinimumSize(getDefaultDimension());
		setPreferredSize(getDefaultDimension());
		getContentPane().setBackground(Color.LIGHT_GRAY);
		
		panelGridColors = new JPanel();
		panelGridColors.setBackground(Color.LIGHT_GRAY);
		
		getContentPane().add(panelGridColors, "cell 0 0 4 1,grow");
		panelGridColors.setLayout(new GridLayout(rows, columns, 5, 5));

		getContentPane().add(new JSeparator(), "cell 0 1 4 1,grow");
		
		txtHexColorSelection = new JTextField();
		getContentPane().add(txtHexColorSelection, "cell 1 2,grow");
		txtHexColorSelection.setColumns(10);
		
		panelColorSelectionPreview = new JPanel();
		panelColorSelectionPreview.setBorder(new LineBorder(new Color(0, 0, 0)));
		getContentPane().add(panelColorSelectionPreview, "cell 2 2,grow");
		
		btnAdvancedColor = new JButton("Advanced ...");
		btnAdvancedColor.setFont(DEFAULT_BUTTON_FONT);
		btnAdvancedColor.setCursor(GuiUtils.CURSOR_HAND);
		getContentPane().add(btnAdvancedColor, "cell 0 2,grow");
		
		btnUseColor = new JButton("Use Color");
		btnUseColor.setCursor(GuiUtils.CURSOR_HAND);
		btnUseColor.setFont(DEFAULT_BUTTON_FONT);
		getContentPane().add(btnUseColor, "cell 3 2,grow");
		
		initColorGrid();
		
		addHandlers();
		
		GuiUtils.centerComponent(this);
	}
	
	private void addHandlers() {
		
		KeyEventDispatcher keyListener = new KeyEventDispatcher() {
			
			@Override
			public boolean dispatchKeyEvent(KeyEvent e) {
				if(KeyEvent.VK_ESCAPE == e.getKeyCode()) {
					userChoice = UserChoice.NONE;  
					dispose();
				}
				return false;
			}
		};
		KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(keyListener);
		
		addWindowListener(new java.awt.event.WindowAdapter() {
		    @Override
		    public void windowClosing(java.awt.event.WindowEvent windowEvent) {
		    	KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(keyListener);
		        userChoice = UserChoice.NONE;  
	        	dispose();
		    }
		});
		
		btnUseColor.addActionListener((e)-> {
			userChoice = UserChoice.ACCEPT;
			userChoice.setChosenColor(selectedColor);
			dispose();
		});
		
		btnAdvancedColor.addActionListener((e)-> {
			Color selection = new ColorDialogHelper((color)-> {
				updateColorSelectionPreview(color);
			}).askForColor(panelColorSelectionPreview, false);
			
			updateColorSelectionPreview(selection);
		});
	}
	
	private void initColorGrid() {
		int hIncrement = 360 / columns;
		int sIncrement = 100 / columns;
		int greyIncrement = 100 / columns;
		int greyIterator = 0;
		int hIterator = 0;
		int sIterator = 0;

		int iterator = 0;
		
		for(int i = 0; i < rows; i++) {
			
			for(int j = 0; j < columns; j++) {
				panelMatrix[i][j] = new JPanel();
				panelGridColors.add(panelMatrix[i][j], iterator);
				
				Color currColor;
				// First line -> grayScale
				if (i == 0) {
					currColor = HslColor.toRGB(new float[] { 0, 0, greyIterator });
				} else {
					currColor = HslColor.toRGB(new float[] { hIterator, sIterator, sIterator });
				}
				
				panelMatrix[i][j].setBackground(currColor);
				panelMatrix[i][j].setCursor(GuiUtils.CURSOR_HAND);
				panelMatrix[i][j].setBorder(DEFAULT_BORDER);
				
				final int row = i;
				final int col = j;
				panelMatrix[i][j].addMouseListener(new MouseAdapter() {
					@Override
					public void mouseReleased(MouseEvent e) {
						selectionRunner(currColor).run();
						selectedGridElementGui(row, col);
						super.mouseReleased(e);
					}
				});
				
				iterator++;
				greyIterator += greyIncrement;
				hIterator += hIncrement;
			}
			sIterator += sIncrement;
		}
	}
	
	public void selectedGridElementGui(int row, int column) {
		for(int i = 0; i < rows; i++) {
			for(int j = 0; j < columns; j++) {
				if(i == row && j == column) {
					panelMatrix[i][j].setBorder(SELECTED_BORDER);
				} else {
					panelMatrix[i][j].setBorder(DEFAULT_BORDER);
				}
			}
		}
	}
	
	public Dimension getDefaultDimension() {
		return DEFAULT_SIZE; 
	}
	
	public void setDefaultDimension(Dimension newSize) {
		DEFAULT_SIZE = newSize;
	}
	
	private Runnable selectionRunner(final Color color) {
		return () -> {
			updateColorSelectionPreview(color);
		};
	}
	
	public enum UserChoice {
		ACCEPT(Color.WHITE),
		NONE(Color.WHITE);
		
		private Color chosenColor;
		
		private UserChoice(Color selection) {
			chosenColor = selection;
		}

		public Color getChosenColor() {
			return chosenColor;
		}

		public void setChosenColor(Color chosenColor) {
			this.chosenColor = chosenColor;
		}
	}
}
