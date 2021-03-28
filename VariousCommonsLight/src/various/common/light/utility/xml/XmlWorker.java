package various.common.light.utility.xml;

import java.io.File;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public class XmlWorker {

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

	public static String xmlPrettyFormat(String input) throws Exception {
		return xmlPrettyFormat(input, 2);
	}

	public static String xmlPrettyFormat(String input, int indent) throws Exception {
		return xmlPrettyFormat(input, indent, true);
	}

	public static String xmlPrettyFormat(String input, int indent, boolean omitXmlDeclaration) throws Exception {
		return xmlPrettyFormat(loadXMLfromString(input), indent, omitXmlDeclaration);
	}

	public static String xmlPrettyFormat(Document original, int indent, boolean omitXmlDeclaration) throws Exception {

        StringWriter stringWriter = new StringWriter();
        StreamResult xmlOutput = new StreamResult(stringWriter);
        TransformerFactory tf = TransformerFactory.newInstance();
        tf.setAttribute("indent-number", indent);
        Transformer transformer = tf.newTransformer();
        transformer.setOutputProperty(OutputKeys.METHOD, "xml");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", String.valueOf(indent));
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, omitXmlDeclaration ? "no" : "yes");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.transform(new DOMSource(original), xmlOutput);
        return xmlOutput.getWriter().toString();
	}

	public static NodeList selectNodeList(Node doc, String xpath) throws XPathExpressionException {
		XPath xPath =  XPathFactory.newInstance().newXPath();
		return (NodeList) xPath.compile(xpath).evaluate(doc, XPathConstants.NODESET);
	}

	public static Node importNode(Document toDoc, Node toParent, Node fromParent, String xpathTo, String xpathFrom) throws XPathExpressionException {
		Node nodeTo = selectNode(toParent, xpathTo);
		Node nodeFrom = selectNode(fromParent, xpathFrom);

		return importNode(toDoc, nodeTo, nodeFrom);
	}

	public static Node importNode(Document toDoc, Node to, Node from) throws XPathExpressionException {
		return to.appendChild(toDoc.adoptNode(from.cloneNode(true)));
	}

	public static Node selectNode(Node doc, String xpath) throws XPathExpressionException {
		XPath xPath =  XPathFactory.newInstance().newXPath();
		return (Node) xPath.compile(xpath).evaluate(doc, XPathConstants.NODE);
	}

	public static String readNodeText(Node doc, String xpath) throws XPathExpressionException {
		Node node = selectNode(doc, xpath);
		if(node != null) {
			return node.getTextContent();
		} else {
			return null;
		}
	}
	public static List<String> readNodeTexts(Node doc, String xpath) throws XPathExpressionException {
		List<String> output = new ArrayList<>();

		if(doc != null && xpath != null) {
			NodeList nodes = selectNodeList(doc, xpath);
			if(nodes != null) {
				for(int i = 0; i < nodes.getLength(); i++) {
					Node node = nodes.item(i);
					if(node != null) {
						output.add(node.getTextContent());
					}
				}
			}
		}

		return output;
	}
}
