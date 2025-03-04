package gui.commons.frameutils.frame.om;


public enum ConsoleConcatenationModeWindows {
	AND("&&", " && "), 
	OR("||", " || ");
	
	String trimmed;
	String withSpaces;
	
	private ConsoleConcatenationModeWindows(String trimmed, String withSpaces) {
		this.trimmed = trimmed;
		this.withSpaces = withSpaces;
	}
	
	public String toString() {
		return trimmed;
	}
	
	public static String[] values(boolean getTrimmed) {
		ConsoleConcatenationModeWindows[] values = values();
		String[] stringValues = new String[values.length];
		for(int i = 0; i < values.length; i++) {
			stringValues[i] = getTrimmed ? values[i].trimmed : values[i].withSpaces;
		}
		
		return stringValues;
	}
}
