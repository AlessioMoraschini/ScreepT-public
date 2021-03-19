package gui.dialogs.color.om;

import java.awt.Color;

public class ColorSelectionElement {
	public Color foreground;
	public Color background;
	public String elementLabel;
	
	public ColorSelectionElement(Color foreground, Color background, String elementLabel) {
		this.foreground = foreground != null ? foreground : Color.BLACK;
		this.background = background;
		this.elementLabel = elementLabel != null ? elementLabel : "";
	}

	@Override
	public String toString() {
		return "ColorSelectionElement [foreground=" + foreground + ", background=" + background + ", elementLabel=" + elementLabel + "]";
	}
}
