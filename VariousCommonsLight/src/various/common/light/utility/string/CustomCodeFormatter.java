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

import java.io.File;
import java.io.StringReader;
import java.io.StringWriter;

import javax.script.ScriptException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;

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

	public static String xmlPrettyFormat(String input) throws Exception {
		return xmlPrettyFormat(input, 2);
	}
	
	public static String xmlPrettyFormat(String input, int indent) throws Exception {
        Document original = null;
        original = loadXMLfromString(input);
        
        StringWriter stringWriter = new StringWriter();
        StreamResult xmlOutput = new StreamResult(stringWriter);
        TransformerFactory tf = TransformerFactory.newInstance();
        tf.setAttribute("indent-number", indent);
        Transformer transformer = tf.newTransformer();
        transformer.setOutputProperty(OutputKeys.METHOD, "xml");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", String.valueOf(indent));
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.transform(new DOMSource(original), xmlOutput);
        return xmlOutput.getWriter().toString();
	}
	
	public static Document loadXMLfromFile(String filePath) throws Exception {
	    DocumentBuilderFactory docFactory
	        = DocumentBuilderFactory.newInstance();
	    DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
	    return docBuilder.parse(new File(filePath));
	}
	
	public static Document loadXMLfromString(String xmlString){
		
        //Parser that produces DOM object trees from XML content
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
         
        //API to obtain DOM Document instance
        DocumentBuilder builder = null;
        try{
            //Create DocumentBuilder with default configuration
            builder = factory.newDocumentBuilder();
             
            //Parse the content to Document object
            Document doc = builder.parse(new InputSource(new StringReader(xmlString)));
            return doc;
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return null;
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
