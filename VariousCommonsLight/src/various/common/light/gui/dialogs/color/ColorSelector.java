package various.common.light.gui.dialogs.color;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import java.awt.Color;
import java.awt.Dimension;

import net.miginfocom.swing.MigLayout;
import various.common.light.gui.GuiUtils;
import various.common.light.gui.dialogs.color.ColorChooserGrid.UserChoice;
import various.common.light.gui.dialogs.color.om.ColorSelectionElement;
import various.common.light.gui.dialogs.font.om.FontType;
import various.common.light.gui.dialogs.font.om.FontTypeRenderer;

import javax.swing.JLabel;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.border.LineBorder;
import javax.swing.JComboBox;

public class ColorSelector extends JPanel {
	private static final long serialVersionUID = -5210045991796060664L;
	
	public static Font DEFAULT_FONT = new Font("Tahoma", Font.PLAIN, 15);

	private JLabel lblElementName;
	private JLabel lblForeground;
	private JPanel panelForeground;
	private JLabel lblBackground;
	private JPanel panelBackground;
	private JLabel lblFont;
	private JComboBox<FontType> comboFont;
	public FontType fontType;
	private JLabel lblOpaqueBackground;
	private JComboBox<Boolean> comboOpaqueBackground;
	public Boolean opaqueBackground = false;
	
	public Runnable onValueChange;
	
	public boolean enabled = true;
	
	public ColorSelectionElement elementOm;
	
	public ColorSelector(ColorSelectionElement colorSelectionElement, Boolean opaqueBackground, FontType fontType, Runnable onValueChange) {
		this(
				colorSelectionElement.elementLabel,
				opaqueBackground,
				fontType,
				colorSelectionElement.background,
				colorSelectionElement.foreground,
				onValueChange
		);
	}
	
	/**
	 * @wbp.parser.constructor
	 */
	public ColorSelector(String elementName, Boolean opaqueBackground, FontType fontType, Color backGround, Color foreground, Runnable onValueChange) {
		
		this.elementOm = new ColorSelectionElement(foreground, backGround, elementName);
		this.opaqueBackground = opaqueBackground;
		this.fontType = fontType != null ? fontType : FontType.Plain;
		this.enabled = true;
		
		this.onValueChange = onValueChange != null ? onValueChange : ()->{};
		
		setBackground(Color.LIGHT_GRAY);
		setLayout(new MigLayout("", "[][grow][][][][][80px:n][:50px:50px][80px:n][:50px:50px]", "[grow]"));
		
		lblElementName = new JLabel("Element");
		lblElementName.setFont(new Font("Tahoma", Font.BOLD, 12));
		lblElementName.setText(elementOm.elementLabel);
		add(lblElementName, "cell 0 0 2 1,alignx left,aligny center");
		
		lblFont = new JLabel("");
		lblFont.setFont(DEFAULT_FONT);
		add(lblFont, "cell 2 0,alignx trailing");
		
		comboFont = new JComboBox<FontType>(FontType.values());
		comboFont.setMinimumSize(new Dimension(75,30));
		comboFont.setPreferredSize(new Dimension(75,30));
		comboFont.setRenderer(new FontTypeRenderer());
		comboFont.setSelectedItem(this.fontType);
		comboFont.setFont(DEFAULT_FONT.deriveFont(this.fontType.type));
		add(comboFont, "cell 3 0,growx");

		lblOpaqueBackground = new JLabel("  Opaque");
		lblOpaqueBackground.setFont(DEFAULT_FONT);
		add(lblOpaqueBackground, "cell 4 0,alignx trailing");
		
		comboOpaqueBackground = new JComboBox<Boolean>(new Boolean[] {true, false});
		comboOpaqueBackground.setMinimumSize(new Dimension(75,30));
		comboOpaqueBackground.setPreferredSize(new Dimension(75,30));
		comboOpaqueBackground.setSelectedItem(this.fontType);
		comboOpaqueBackground.setFont(DEFAULT_FONT);
		add(comboOpaqueBackground, "cell 5 0,growx");
		
		lblForeground = new JLabel("Foreground");
		lblForeground.setFont(DEFAULT_FONT);
		add(lblForeground, "cell 6 0,alignx right");
		
		panelForeground = new JPanel();
		panelForeground.setBorder(new LineBorder(new Color(0, 0, 0)));
		panelForeground.setCursor(GuiUtils.CURSOR_HAND);
		panelForeground.setBackground(elementOm.foreground);
		panelForeground.addMouseListener(panelMouseListener(panelForeground, false));
		add(panelForeground, "cell 7 0,grow");
		
		lblBackground = new JLabel("Background");
		lblBackground.setFont(DEFAULT_FONT);
		add(lblBackground, "cell 8 0,alignx right");
		
		panelBackground = new JPanel();
		panelBackground.setBorder(new LineBorder(new Color(0, 0, 0)));
		panelBackground.setCursor(GuiUtils.CURSOR_HAND);
		panelBackground.setBackground(elementOm.background);
		panelBackground.addMouseListener(panelMouseListener(panelBackground, true));
		add(panelBackground, "cell 9 0,grow");
		
		addHandlers();
		
		setEnabled(enabled);
		
		SwingUtilities.invokeLater(()->{
			updateComboOpaqueBckg(opaqueBackground, false);
		});
	}
	
	public void updateComboOpaqueBckg(boolean opaqueBackground, boolean syncGui) {
		this.comboOpaqueBackground.setSelectedItem(opaqueBackground);
		this.opaqueBackground = opaqueBackground;
		
		if (syncGui) {
			comboOpaqueBackground.setFont(DEFAULT_FONT.deriveFont(opaqueBackground ? Font.BOLD : Font.PLAIN));
			if (!opaqueBackground)
				removeBackground();
			else if (!GuiUtils.checkIfComponentContains(this, panelBackground)) {
				add(lblBackground, "cell 8 0,alignx right");
				add(panelBackground, "cell 9 0,grow");
			}
		}
	}
	
	private void addHandlers() {
		comboFont.addActionListener((e) -> {
			FontType selection = (FontType)comboFont.getSelectedItem();
			fontType = selection;
			comboFont.setFont(DEFAULT_FONT.deriveFont(this.fontType.type));
			onValueChange.run();
		});

		comboOpaqueBackground.addActionListener((e) -> {
			updateComboOpaqueBckg((Boolean)comboOpaqueBackground.getSelectedItem(), true);
			onValueChange.run();
		});
	}
	
	public void removeBackground() {
		remove(lblBackground);
		remove(panelBackground);
	}
	
	public void removeForeground() {
		remove(lblForeground);
		remove(panelForeground);
	}
	
	public void removeFont() {
		remove(lblFont);
		remove(comboFont);
	}

	public void removeOpaqueBackground() {
		remove(lblOpaqueBackground);
		remove(comboOpaqueBackground);
	}
	
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
		setBackground(enabled ? Color.WHITE : Color.GRAY);
		panelBackground.setCursor(enabled ? GuiUtils.CURSOR_HAND : GuiUtils.CURSOR_DEFAULT);
		panelForeground.setCursor(enabled ? GuiUtils.CURSOR_HAND : GuiUtils.CURSOR_DEFAULT);
		comboFont.setEnabled(enabled);
		comboOpaqueBackground.setEnabled(enabled);
	}
	
	public boolean isOpaqueBackground() {
		return opaqueBackground;
	}

	public MouseAdapter panelMouseListener(JPanel target, boolean background) {
		return new MouseAdapter() {

			@Override
			public void mouseReleased(MouseEvent e) {
				
				if(!enabled)
					return;
				
				super.mouseReleased(e);
				
				String titleAdd = elementOm.elementLabel + (background ? "(Background)" : "(Foreground)");
				UserChoice userChoice = ColorDialogHelper.askForColorGrid(target.getBackground(), titleAdd);
				
				if (userChoice.equals(UserChoice.ACCEPT)) {
					target.setBackground(userChoice.getChosenColor());
					
					if(background)
						elementOm.background = userChoice.getChosenColor();
					else
						elementOm.foreground = userChoice.getChosenColor();
						
					onValueChange.run();
				}
			}
		};
	}
	
}
