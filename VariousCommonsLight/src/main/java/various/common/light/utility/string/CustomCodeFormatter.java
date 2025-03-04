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
package various.common.light.utility.string;

import javax.script.ScriptException;

import various.common.light.runtime.javascript.JsRunner;

public class CustomCodeFormatter {

	public static final String ERROR = "ERROR :(";

	public JsRunner jsRunner;
	public int indentSize = 3; // default


	public CustomCodeFormatter(String jsLibFolder) {
		jsRunner = new JsRunner();

		if (jsLibFolder != null) {
			jsRunner.initRunnerFromFolderLib(jsLibFolder);
		}
	}


//	/**
//	 * Uses editor standard code formatter to format given code and return back formatted string
//	 * @throws FormatterException
//	 */
//	public static String googleJavaCodeFormat(String toFormat) throws FormatterException{
//		Formatter formatter =  new Formatter();
//		return formatter.formatSource(toFormat);
//	}

	/** JAVASCRIPT EXTERNAL CALLS - HTML/CSS/JS FORMATTER OR STRINGS
	 * **/

	 /**
	 * @throws ScriptException
	 * @throws NoSuchMethodException **/
	public String jsFormatJSLIB(String source, int indentSize) throws NoSuchMethodException, ScriptException {
		this.indentSize = indentSize;
		return (String)jsRunner.invokeFunction("js_beautify", source, new JsFormatParams(indentSize));
	}

	public String htmlFormatJSLIB(String source, int indentSize) throws NoSuchMethodException, ScriptException {
		this.indentSize = indentSize;
		return (String)jsRunner.invokeFunction("style_html", source, new JsFormatParams(indentSize));
	}

	public String cssFormatJSLIB(String source, int indentSize) throws NoSuchMethodException, ScriptException {
		this.indentSize = indentSize;
		return (String)jsRunner.invokeFunction("css_beautify", source, new JsFormatParams(indentSize));
	}

	public String getJsParams() {
		return "{"+
			      "'indent_size': " +indentSize+ ","+
			      "'indent_char': '\t',"+
			      "'selector_separator': '\t',"+
			      "'end_with_newline': true,"+
			      "'newline_between_rules': true,"+
			      "'space_around_selector_separator': true"+
			      "'indent_with_tabs': true"+
			    "}";
	}

	public class JsFormatParams{
		int indent_size = 5;
		String indent_char = "\t";
		boolean newline_between_rules = true;
		boolean end_with_newline = true;

		public JsFormatParams(int indent_size) {
			this.indent_size = indent_size;
		}
	}
}
