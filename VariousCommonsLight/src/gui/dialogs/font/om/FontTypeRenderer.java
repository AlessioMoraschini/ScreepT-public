package gui.dialogs.font.om;

import java.awt.Component;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

public class FontTypeRenderer implements ListCellRenderer<FontType>{
	
	protected DefaultListCellRenderer defaultRenderer;

	public FontTypeRenderer() {
		defaultRenderer = new DefaultListCellRenderer();
	}

	@Override
	public Component getListCellRendererComponent(JList<? extends FontType> list, FontType value, int index, boolean isSelected, boolean cellHasFocus) {
		JLabel renderer = (JLabel) defaultRenderer.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
		renderer.setFont(renderer.getFont().deriveFont(value.type));
		return renderer;
	}

}
