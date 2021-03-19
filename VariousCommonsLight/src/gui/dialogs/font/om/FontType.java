package gui.dialogs.font.om;

import java.awt.Font;

public enum FontType {
		Plain(Font.PLAIN),
		Bold(Font.BOLD),
		Italic(Font.ITALIC);

	public int type;

	private FontType(int type) {
		this.type = type;
	}
	
	/**
	 * Initialize enum from Font.getStyle() BOLD|PLAIN|ITALIC
	 * @param fontSstyle
	 */
	public static FontType fromStyle(int fontSstyle) {
		for(FontType value : values()) {
			if(value.type == fontSstyle) {
				return value;
			}
		}
		
		return Plain;
	}

	/**
	 * Initialize enum from Font.getStyle() BOLD|PLAIN|ITALIC
	 * @param fontSstyle
	 */
	public static FontType fromFont(Font font) {
		for(FontType value : values()) {
			if(font != null && value.type == font.getStyle()) {
				return value;
			}
		}
		
		return Plain;
	}
}
