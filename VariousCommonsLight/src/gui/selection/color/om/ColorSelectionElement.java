package gui.selection.color.om;

import java.awt.Color;

public class ColorSelectionElement {
	public Color foreground;
	public Color background;
	public String elementLabel;
	
	public ColorSelectionElement(Color foreground, Color background, String elementLabel) {
		this.foreground = foreground != null ? foreground : Color.BLACK;
		this.background = background != null ? background : new Color(240, 240, 240, 0);
		this.elementLabel = elementLabel != null ? elementLabel : "";
	}

	@Override
	public String toString() {
		return "ColorSelectionElement [foreground=" + foreground + ", background=" + background + ", elementLabel=" + elementLabel + "]";
	}
}
