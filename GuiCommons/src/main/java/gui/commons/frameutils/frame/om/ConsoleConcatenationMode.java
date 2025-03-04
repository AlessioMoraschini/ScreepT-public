package gui.commons.frameutils.frame.om;

public enum ConsoleConcatenationMode {
	AND("&&", " && "), 
	OR("||", " || "),
	MULTI(";", " ; ");
	
	String trimmed;
	String withSpaces;
	
	private ConsoleConcatenationMode(String trimmed, String withSpaces) {
		this.trimmed = trimmed;
		this.withSpaces = withSpaces;
	}
	
	public static String[] values(boolean getTrimmed) {
		ConsoleConcatenationMode[] values = values();
		String[] stringValues = new String[values.length];
		for(int i = 0; i < values.length; i++) {
			stringValues[i] = getTrimmed ? values[i].trimmed : values[i].withSpaces;
		}
		
		return stringValues;
	}
	
	public String toString() {
		return trimmed;
	}
}
