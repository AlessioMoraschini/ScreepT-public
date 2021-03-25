package various.common.light.gui.selection.color;

import javax.swing.JPanel;
import java.awt.Color;
import net.miginfocom.swing.MigLayout;
import various.common.light.gui.GuiUtils;
import various.common.light.gui.selection.color.ColorChooserGrid.UserChoice;
import various.common.light.gui.selection.color.om.ColorSelectionElement;

import javax.swing.JLabel;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.border.LineBorder;

public class ColorSelector extends JPanel {
	private static final long serialVersionUID = -5210045991796060664L;

	private JLabel lblElementName;
	private JLabel lblForeground;
	private JPanel panelForeground;
	private JLabel lblBackground;
	private JPanel panelBackground;
	
	public Runnable onColorChange;
	
	public ColorSelectionElement elementOm;
	
	public ColorSelector(ColorSelectionElement colorSelectionElement, Runnable onColorChange) {
		this(
				colorSelectionElement.elementLabel,
				colorSelectionElement.background,
				colorSelectionElement.foreground,
				onColorChange
		);
	}
	
	/**
	 * @wbp.parser.constructor
	 */
	public ColorSelector(String elementName, Color backGround, Color foreground, Runnable onColorChange) {
		
		elementOm = new ColorSelectionElement(foreground, backGround, elementName);
		
		this.onColorChange = onColorChange != null ? onColorChange : ()->{};
		
		setBackground(Color.LIGHT_GRAY);
		setLayout(new MigLayout("", "[][grow][80px:n][:50px:50px][80px:n][:50px:50px]", "[grow]"));
		
		lblElementName = new JLabel("Element");
		lblElementName.setFont(new Font("Tahoma", Font.BOLD, 12));
		lblElementName.setText(elementOm.elementLabel);
		add(lblElementName, "cell 0 0 2 1,alignx left,aligny center");
		
		lblForeground = new JLabel("Foreground");
		lblForeground.setFont(new Font("Tahoma", Font.PLAIN, 15));
		add(lblForeground, "cell 2 0,alignx right");
		
		panelForeground = new JPanel();
		panelForeground.setBorder(new LineBorder(new Color(0, 0, 0)));
		panelForeground.setCursor(GuiUtils.CURSOR_HAND);
		panelForeground.setBackground(elementOm.foreground);
		panelForeground.addMouseListener(panelMouseListener(panelForeground, false));
		add(panelForeground, "cell 3 0,grow");
		
		lblBackground = new JLabel("Background");
		lblBackground.setFont(new Font("Tahoma", Font.PLAIN, 15));
		add(lblBackground, "cell 4 0,alignx right");
		
		panelBackground = new JPanel();
		panelBackground.setBorder(new LineBorder(new Color(0, 0, 0)));
		panelBackground.setCursor(GuiUtils.CURSOR_HAND);
		panelBackground.setBackground(elementOm.background);
		panelBackground.addMouseListener(panelMouseListener(panelBackground, true));
		add(panelBackground, "cell 5 0,grow");
	}
	
	public void removeBackground() {
		remove(lblBackground);
		remove(panelBackground);
	}
	
	public void removeForeground() {
		remove(lblForeground);
		remove(panelForeground);
	}
	
	public MouseAdapter panelMouseListener(JPanel target, boolean background) {
		return new MouseAdapter() {

			@Override
			public void mouseReleased(MouseEvent e) {
				super.mouseReleased(e);
				UserChoice userChoice = ColorDialogHelper.askForColorGrid(target.getBackground());
				
				if (userChoice.equals(UserChoice.ACCEPT)) {
					target.setBackground(userChoice.getChosenColor());
					
					if(background)
						elementOm.background = userChoice.getChosenColor();
					else
						elementOm.foreground = userChoice.getChosenColor();
						
						
					onColorChange.run();
				}
			}
		};
	}
	
}
