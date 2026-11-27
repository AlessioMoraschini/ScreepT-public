/**
 *
 * =========================================================================================
 *  Copyright (C) 2019-2026
 *
 *  AM-Design-Development - (Alessio Moraschini) - All Rights Reserved
 * =========================================================================================
 *
 * You should have received a copy of the license with this file.
 * If not, please write to: info@am-design-development.com, or visit : https://www.am-design-development.com
 */
package multimedia.conversion.pdfedit;

import java.awt.Color;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.pdfbox.contentstream.PDFGraphicsStreamEngine;
import org.apache.pdfbox.contentstream.operator.Operator;
import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSFloat;
import org.apache.pdfbox.cos.COSInteger;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSNumber;
import org.apache.pdfbox.cos.COSString;
import org.apache.pdfbox.pdfparser.PDFStreamParser;
import org.apache.pdfbox.pdfwriter.ContentStreamWriter;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.common.PDStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDFontDescriptor;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.graphics.state.PDTextState;
import org.apache.pdfbox.util.Matrix;

/**
 * The editable content of a page: lines of text and images (or form objects), and the changes that can be made to them.
 * <p>
 * An element is changed by rewriting the content stream: its operators are removed where they are (the text position keeps
 * advancing, so what follows does not move) and drawn again at the end of the page with the same graphics state.
 * Vector drawings and the text inside form objects are not editable.
 */
public final class PdfContent {

	private PdfContent() {
	}

	/** Graphics state needed to draw again a removed operator; the lists are replaced, never changed */
	private static final class Gfx {
		List<COSName> extStates = Collections.emptyList();
		List<Object> fillSpace, fill, strokeSpace, stroke, lineWidth;
		COSName font;

		Gfx copy() {
			Gfx g = new Gfx();
			g.extStates = extStates;
			g.fillSpace = fillSpace;
			g.fill = fill;
			g.strokeSpace = strokeSpace;
			g.stroke = stroke;
			g.lineWidth = lineWidth;
			g.font = font;
			return g;
		}

		void writeTo(List<Object> out) {
			for (COSName name : extStates) {
				add(out, "gs", name);
			}
			for (List<Object> group : Arrays.asList(fillSpace, fill, strokeSpace, stroke, lineWidth)) {
				if (group != null) {
					out.addAll(group);
				}
			}
		}
	}

	/** One text showing operator */
	private static final class Run {
		int op;
		String opName;
		List<COSBase> operands;
		Matrix textMatrix, ctm;
		float size, charSpacing, wordSpacing, scaling, rise, advance;
		int mode;
		Gfx gfx;
		PDFont font;
		String text;
		Rectangle2D bounds;
		Point2D start, end;
		double height;
	}

	/** Something that can be selected, moved and deleted */
	public static final class Element {
		private final List<Run> runs = new ArrayList<>();
		private int op = -1;
		private List<Object> drawing;
		private Matrix ctm;
		private Gfx gfx;
		private String text = "";
		private Rectangle2D bounds;

		public boolean isText() {
			return !runs.isEmpty();
		}

		public String getText() {
			return text;
		}

		/** Area in page coordinates */
		public Rectangle2D getBounds() {
			return bounds;
		}
	}

	/** The elements of a page, and for each font resource the bytes of the characters the page already shows with it */
	public static final class Scan {
		public final List<Element> elements = new ArrayList<>();
		private final Map<COSName, Map<String, byte[]>> glyphs = new HashMap<>();
	}

	public static Scan scan(PDPage page) throws IOException {
		Scanner scanner = new Scanner(page);
		scanner.processPage(page);
		return scanner.result;
	}

	private static final class Scanner extends PDFGraphicsStreamEngine {
		final Scan result = new Scan();
		private final Deque<Gfx> stack = new ArrayDeque<>();
		private Gfx gfx = new Gfx();
		private Element line;
		private int depth;
		private int index = -1;

		Scanner(PDPage page) {
			super(page);
		}

		@Override
		protected void processOperator(Operator operator, List<COSBase> operands) throws IOException {
			if (depth > 0) {
				super.processOperator(operator, operands); // run by another operator (' " TD...): not one of the page stream
				return;
			}
			index++;
			String name = operator.getName();
			List<COSBase> args = new ArrayList<>(operands);
			depth++;
			try {
				if (("Tj".equals(name) || "TJ".equals(name) || "'".equals(name) || "\"".equals(name)) && getTextMatrix() != null) {
					showText(operator, name, args);
					return;
				}
				if ("Do".equals(name) && !args.isEmpty() && args.get(0) instanceof COSName) {
					object(operator, (COSName) args.get(0));
					return; // forms are one element: their content is not scanned
				}
				if ("BI".equals(name)) {
					addGraphic(Collections.<Object>singletonList(operator), new Rectangle2D.Double(0, 0, 1, 1), new AffineTransform());
					return;
				}
				track(operator, name, args);
				super.processOperator(operator, operands);
			} finally {
				depth--;
			}
		}

		private void track(Operator operator, String name, List<COSBase> args) {
			List<Object> tokens = new ArrayList<Object>(args);
			tokens.add(operator);
			switch (name) {
				case "q": stack.push(gfx.copy()); break;
				case "Q": if (!stack.isEmpty()) { gfx = stack.pop(); } break;
				case "gs":
					if (!args.isEmpty() && args.get(0) instanceof COSName) {
						List<COSName> states = new ArrayList<>(gfx.extStates);
						states.add((COSName) args.get(0));
						gfx.extStates = states;
					}
					break;
				case "Tf":
					if (!args.isEmpty() && args.get(0) instanceof COSName) {
						gfx.font = (COSName) args.get(0);
					}
					break;
				case "g": case "rg": case "k": gfx.fillSpace = null; gfx.fill = tokens; break;
				case "cs": gfx.fillSpace = tokens; gfx.fill = null; break;
				case "sc": case "scn": gfx.fill = tokens; break;
				case "G": case "RG": case "K": gfx.strokeSpace = null; gfx.stroke = tokens; break;
				case "CS": gfx.strokeSpace = tokens; gfx.stroke = null; break;
				case "SC": case "SCN": gfx.stroke = tokens; break;
				case "w": gfx.lineWidth = tokens; break;
				default: break;
			}
		}

		private void showText(Operator operator, String name, List<COSBase> args) throws IOException {
			Matrix before;
			if ("Tj".equals(name) || "TJ".equals(name)) {
				before = getTextMatrix().clone();
			} else { // ' and " go to the next line first
				before = getTextLineMatrix().clone();
				before.concatenate(Matrix.getTranslateInstance(0, -getGraphicsState().getTextState().getLeading()));
			}
			super.processOperator(operator, new ArrayList<>(args));
			Matrix after = getTextMatrix();
			PDTextState state = getGraphicsState().getTextState();
			Run run = new Run();
			run.font = state.getFont();
			run.size = state.getFontSize();
			run.scaling = state.getHorizontalScaling();
			run.mode = state.getRenderingMode().intValue();
			// invisible text (OCR layer) and clipping text stay where they are
			if (run.font == null || run.font.isVertical() || run.size == 0 || run.scaling == 0 || run.mode >= 3 || gfx.font == null) {
				return;
			}
			run.op = index;
			run.opName = name;
			run.operands = args;
			run.textMatrix = before;
			run.ctm = getGraphicsState().getCurrentTransformationMatrix().clone();
			run.charSpacing = state.getCharacterSpacing();
			run.wordSpacing = state.getWordSpacing();
			run.rise = state.getRise();
			run.gfx = gfx.copy();
			float a = before.getScaleX();
			float b = before.getShearY();
			run.advance = Math.abs(a) >= Math.abs(b) ? (after.getTranslateX() - before.getTranslateX()) / a
					: (after.getTranslateY() - before.getTranslateY()) / b;
			run.text = decode(run, args.get(args.size() - 1));

			AffineTransform m = run.ctm.createAffineTransform();
			m.concatenate(before.createAffineTransform());
			run.start = m.transform(new Point2D.Double(0, run.rise), null);
			run.end = m.transform(new Point2D.Double(run.advance, run.rise), null);
			run.height = run.start.distance(m.transform(new Point2D.Double(0, run.rise + run.size), null));
			run.bounds = m.createTransformedShape(new Rectangle2D.Double(Math.min(0, run.advance), run.rise - 0.25 * run.size,
					Math.abs(run.advance), 1.1 * run.size)).getBounds2D();
			if (run.height > 0 && !run.text.isEmpty()) {
				addRun(run);
			}
		}

		private String decode(Run run, COSBase shown) throws IOException {
			StringBuilder text = new StringBuilder();
			Map<String, byte[]> known = result.glyphs.computeIfAbsent(run.gfx.font, k -> new HashMap<>());
			Iterable<COSBase> parts = shown instanceof COSArray ? (COSArray) shown : Collections.singletonList(shown);
			for (COSBase part : parts) {
				if (part instanceof COSNumber) {
					if (((COSNumber) part).floatValue() < -180 && text.length() > 0 && text.charAt(text.length() - 1) != ' ') {
						text.append(' '); // a gap this wide is a space
					}
				} else if (part instanceof COSString) {
					byte[] bytes = ((COSString) part).getBytes();
					ByteArrayInputStream in = new ByteArrayInputStream(bytes);
					while (in.available() > 0) {
						int from = bytes.length - in.available();
						int code = run.font.readCode(in);
						String unicode = run.font.toUnicode(code);
						if (unicode == null) {
							text.append('�');
						} else {
							text.append(unicode);
							known.putIfAbsent(unicode, Arrays.copyOfRange(bytes, from, bytes.length - in.available()));
						}
					}
				}
			}
			return text.toString();
		}

		/** Runs following each other on the same line, with the same look, make one element */
		private void addRun(Run run) {
			if (line != null) {
				Run last = line.runs.get(line.runs.size() - 1);
				double length = last.start.distance(last.end);
				if (length > 0 && last.gfx.font.equals(run.gfx.font) && Math.abs(last.height - run.height) < 0.05 * last.height
						&& String.valueOf(last.gfx.fill).equals(String.valueOf(run.gfx.fill))) {
					double ux = (last.end.getX() - last.start.getX()) / length;
					double uy = (last.end.getY() - last.start.getY()) / length;
					double dx = run.start.getX() - last.end.getX();
					double dy = run.start.getY() - last.end.getY();
					double along = dx * ux + dy * uy;
					double across = Math.abs(dy * ux - dx * uy);
					if (across < 0.3 * run.height && along > -0.5 * run.height && along < run.height) {
						if (along > 0.2 * run.height && !line.text.endsWith(" ") && !run.text.startsWith(" ")) {
							line.text += " ";
						}
						line.text += run.text;
						line.bounds = line.bounds.createUnion(run.bounds);
						line.runs.add(run);
						return;
					}
				}
			}
			line = new Element();
			line.runs.add(run);
			line.text = run.text;
			line.bounds = run.bounds;
			result.elements.add(line);
		}

		private void object(Operator operator, COSName name) throws IOException {
			List<Object> drawing = Arrays.<Object>asList(name, operator);
			PDResources resources = getResources();
			if (resources.isImageXObject(name)) {
				addGraphic(drawing, new Rectangle2D.Double(0, 0, 1, 1), new AffineTransform());
				return;
			}
			PDXObject object = resources.getXObject(name);
			if (object instanceof PDFormXObject) {
				PDRectangle box = ((PDFormXObject) object).getBBox();
				if (box != null) {
					addGraphic(drawing, new Rectangle2D.Double(box.getLowerLeftX(), box.getLowerLeftY(), box.getWidth(), box.getHeight()),
							((PDFormXObject) object).getMatrix().createAffineTransform());
				}
			}
		}

		private void addGraphic(List<Object> drawing, Rectangle2D area, AffineTransform inner) {
			Element element = new Element();
			element.op = index;
			element.drawing = drawing;
			element.ctm = getGraphicsState().getCurrentTransformationMatrix().clone();
			element.gfx = gfx.copy();
			AffineTransform m = element.ctm.createAffineTransform();
			m.concatenate(inner);
			element.bounds = m.createTransformedShape(area).getBounds2D();
			if (!element.bounds.isEmpty()) {
				result.elements.add(element);
				line = null;
			}
		}

		// drawing operations: only their effect on the state matters here
		@Override public void appendRectangle(Point2D p0, Point2D p1, Point2D p2, Point2D p3) { }
		@Override public void drawImage(org.apache.pdfbox.pdmodel.graphics.image.PDImage image) { }
		@Override public void clip(int windingRule) { }
		@Override public void moveTo(float x, float y) { }
		@Override public void lineTo(float x, float y) { }
		@Override public void curveTo(float x1, float y1, float x2, float y2, float x3, float y3) { }
		@Override public Point2D getCurrentPoint() { return new Point2D.Float(); }
		@Override public void closePath() { }
		@Override public void endPath() { }
		@Override public void strokePath() { }
		@Override public void fillPath(int windingRule) { }
		@Override public void fillAndStrokePath(int windingRule) { }
		@Override public void shadingFill(COSName shadingName) { }
	}

	////////////////////////////////// CHANGES //////////////////////////////////

	/** Moves the element by the given offset, in page coordinates */
	public static void move(PDDocument doc, int pageIndex, int elementIndex, double dx, double dy) throws IOException {
		AffineTransform offset = AffineTransform.getTranslateInstance(dx, dy);
		PDPage page = doc.getPage(pageIndex);
		Element element = scan(page).elements.get(elementIndex);
		Map<Integer, List<Object>> replaced = new HashMap<>();
		List<Object> appended = new ArrayList<>();
		if (element.isText()) {
			for (Run run : element.runs) {
				replaced.put(run.op, removal(run));
				draw(appended, run, dx, dy, run.gfx.font, run.operands.get(run.operands.size() - 1));
			}
		} else {
			replaced.put(element.op, Collections.emptyList());
			drawGraphic(appended, element, offset);
		}
		rewrite(doc, page, replaced, appended);
	}

	/** Resizes an image keeping the given point (page coordinates) where it is */
	public static void resize(PDDocument doc, int pageIndex, int elementIndex, double factor, Point2D anchor) throws IOException {
		PDPage page = doc.getPage(pageIndex);
		Element element = scan(page).elements.get(elementIndex);
		if (element.isText()) {
			return;
		}
		AffineTransform change = AffineTransform.getTranslateInstance(anchor.getX(), anchor.getY());
		change.scale(factor, factor);
		change.translate(-anchor.getX(), -anchor.getY());
		List<Object> appended = new ArrayList<>();
		drawGraphic(appended, element, change);
		rewrite(doc, page, Collections.singletonMap(element.op, Collections.emptyList()), appended);
	}

	/** Removes the element from the page content (this is not a secure redaction: resources may stay in the file) */
	public static void delete(PDDocument doc, int pageIndex, int elementIndex) throws IOException {
		PDPage page = doc.getPage(pageIndex);
		Element element = scan(page).elements.get(elementIndex);
		Map<Integer, List<Object>> replaced = new HashMap<>();
		if (element.isText()) {
			for (Run run : element.runs) {
				replaced.put(run.op, removal(run));
			}
		} else {
			replaced.put(element.op, Collections.emptyList());
		}
		rewrite(doc, page, replaced, Collections.emptyList());
	}

	/** Changes the text of a line, keeping its font when it has the needed characters, otherwise using a similar one */
	public static void replaceText(PDDocument doc, int pageIndex, int elementIndex, String text) throws IOException {
		PDPage page = doc.getPage(pageIndex);
		Scan scan = scan(page);
		Element element = scan.elements.get(elementIndex);
		Map<Integer, List<Object>> replaced = new HashMap<>();
		for (Run run : element.runs) {
			replaced.put(run.op, removal(run));
		}
		List<Object> appended = new ArrayList<>();
		if (!text.isEmpty()) {
			Run first = element.runs.get(0);
			COSName fontName = first.gfx.font;
			COSBase shown = withKnownGlyphs(scan.glyphs.get(fontName), text);
			if (shown == null && !first.font.getName().contains("+")) { // not a subset: the font may have other characters
				try {
					shown = new COSString(first.font.encode(text));
				} catch (IllegalArgumentException | IOException | UnsupportedOperationException e) {
					shown = null;
				}
			}
			if (shown == null) {
				PDFont substitute = similarFont(doc, first.font);
				fontName = resourcesOf(page).add(substitute);
				shown = encode(substitute, text);
				embed(substitute);
			}
			draw(appended, first, 0, 0, fontName, shown);
		}
		rewrite(doc, page, replaced, appended);
	}

	/** Writes new text (also on several lines) with its top left corner at the given point, upright as the page is seen */
	public static void addText(PDDocument doc, int pageIndex, Point2D at, String text, float size, Color color) throws IOException {
		PDPage page = doc.getPage(pageIndex);
		PDFont font = windowsFont(doc, "arial", PDType1Font.HELVETICA);
		COSName name = resourcesOf(page).add(font);
		AffineTransform place = AffineTransform.getTranslateInstance(at.getX(), at.getY());
		place.rotate(Math.toRadians(page.getRotation()));
		place.translate(0, -0.85 * size);

		List<Object> out = new ArrayList<>();
		add(out, "q");
		add(out, "BT");
		add(out, "Tf", name, new COSFloat(size));
		add(out, "rg", new COSFloat(color.getRed() / 255f), new COSFloat(color.getGreen() / 255f), new COSFloat(color.getBlue() / 255f));
		matrix(out, "Tm", place);
		boolean first = true;
		for (String row : text.split("\\r?\\n", -1)) {
			if (!first) {
				add(out, "Td", COSInteger.ZERO, new COSFloat(-1.2f * size));
			}
			first = false;
			if (!row.isEmpty()) {
				add(out, "Tj", encode(font, row.replace("\t", "    ")));
			}
		}
		add(out, "ET");
		add(out, "Q");
		embed(font);
		rewrite(doc, page, Collections.emptyMap(), out);
	}

	/** Puts an image with its top left corner at the given point, not wider than half of the page */
	public static void addImage(PDDocument doc, int pageIndex, Point2D at, File file) throws IOException {
		PDPage page = doc.getPage(pageIndex);
		PDImageXObject image = PDImageXObject.createFromFileByContent(file, doc);
		COSName name = resourcesOf(page).add(image);
		PDRectangle box = page.getCropBox();
		double seenWidth = page.getRotation() % 180 == 0 ? box.getWidth() : box.getHeight();
		double width = Math.min(image.getWidth() * 0.75, seenWidth / 2);
		double height = width * image.getHeight() / image.getWidth();
		AffineTransform place = AffineTransform.getTranslateInstance(at.getX(), at.getY());
		place.rotate(Math.toRadians(page.getRotation()));
		place.translate(0, -height);
		place.scale(width, height);

		List<Object> out = new ArrayList<>();
		add(out, "q");
		matrix(out, "cm", place);
		add(out, "Do", name);
		add(out, "Q");
		rewrite(doc, page, Collections.emptyMap(), out);
	}

	////////////////////////////////// STREAM WRITING //////////////////////////////////

	/** Operators taking the place of a removed text: nothing is drawn, what follows keeps its position */
	private static List<Object> removal(Run run) {
		List<Object> out = new ArrayList<>();
		if ("\"".equals(run.opName)) {
			add(out, "Tw", run.operands.get(0));
			add(out, "Tc", run.operands.get(1));
		}
		if ("\"".equals(run.opName) || "'".equals(run.opName)) {
			add(out, "T*");
		}
		if (run.advance != 0) {
			COSArray gap = new COSArray();
			gap.add(new COSFloat(-run.advance * 1000f / (run.size * run.scaling / 100f)));
			add(out, "TJ", gap);
		}
		return out;
	}

	private static void draw(List<Object> out, Run run, double dx, double dy, COSName font, COSBase shown) throws IOException {
		Point2D offset;
		try { // the offset is on the page, the text matrix works inside the transformation of the text
			offset = run.ctm.createAffineTransform().createInverse().deltaTransform(new Point2D.Double(dx, dy), null);
		} catch (NoninvertibleTransformException e) {
			throw new IOException("This element cannot be moved", e);
		}
		add(out, "q");
		run.gfx.writeTo(out);
		matrix(out, "cm", run.ctm.createAffineTransform());
		add(out, "BT");
		add(out, "Tf", font, new COSFloat(run.size));
		add(out, "Tc", new COSFloat(run.charSpacing));
		add(out, "Tw", new COSFloat(run.wordSpacing));
		add(out, "Tz", new COSFloat(run.scaling));
		add(out, "Ts", new COSFloat(run.rise));
		add(out, "Tr", COSInteger.get(run.mode));
		AffineTransform text = run.textMatrix.createAffineTransform();
		matrix(out, "Tm", new AffineTransform(text.getScaleX(), text.getShearY(), text.getShearX(), text.getScaleY(),
				text.getTranslateX() + offset.getX(), text.getTranslateY() + offset.getY()));
		add(out, shown instanceof COSArray ? "TJ" : "Tj", shown);
		add(out, "ET");
		add(out, "Q");
	}

	private static void drawGraphic(List<Object> out, Element element, AffineTransform change) {
		AffineTransform m = new AffineTransform(change);
		m.concatenate(element.ctm.createAffineTransform());
		add(out, "q");
		element.gfx.writeTo(out);
		matrix(out, "cm", m);
		out.addAll(element.drawing);
		add(out, "Q");
	}

	private static void add(List<Object> out, String operator, COSBase... operands) {
		Collections.addAll(out, (Object[]) operands);
		out.add(Operator.getOperator(operator));
	}

	private static void matrix(List<Object> out, String operator, AffineTransform m) {
		add(out, operator, new COSFloat((float) m.getScaleX()), new COSFloat((float) m.getShearY()), new COSFloat((float) m.getShearX()),
				new COSFloat((float) m.getScaleY()), new COSFloat((float) m.getTranslateX()), new COSFloat((float) m.getTranslateY()));
	}

	/** Writes the page content with some operators (by position in the stream) replaced and more content at the end */
	private static void rewrite(PDDocument doc, PDPage page, Map<Integer, List<Object>> replaced, List<Object> appended)
			throws IOException {
		PDFStreamParser parser = new PDFStreamParser(page);
		parser.parse();
		List<Object> out = new ArrayList<>();
		List<Object> operands = new ArrayList<>();
		int index = 0;
		int depth = 0;
		boolean closed = true; // everything inside q ... Q: the appended content starts from a clean state
		for (Object token : parser.getTokens()) {
			if (!(token instanceof Operator)) {
				operands.add(token);
				continue;
			}
			List<Object> replacement = replaced.get(index++);
			if (replacement != null) {
				out.addAll(replacement);
			} else {
				String name = ((Operator) token).getName();
				if ("q".equals(name)) {
					depth++;
				} else if ("Q".equals(name)) {
					if (depth == 0) {
						operands.clear();
						continue; // unbalanced: viewers ignore it
					}
					depth--;
				} else if (depth == 0) {
					closed = false;
				}
				out.addAll(operands);
				out.add(token);
			}
			operands.clear();
		}
		if (!closed || depth > 0) {
			out.add(0, Operator.getOperator("q"));
			for (int i = 0; i <= depth; i++) {
				add(out, "Q");
			}
		}
		out.addAll(appended);
		PDStream stream = new PDStream(doc);
		try (OutputStream output = stream.createOutputStream(COSName.FLATE_DECODE)) {
			new ContentStreamWriter(output).writeTokens(out);
		}
		page.setContents(stream);
	}

	////////////////////////////////// FONTS //////////////////////////////////

	/** The text written with characters the page already uses (the only ones sure to be in a subset font), or null */
	private static COSBase withKnownGlyphs(Map<String, byte[]> known, String text) throws IOException {
		if (known == null) {
			return null;
		}
		COSArray parts = new COSArray();
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		for (int i = 0; i < text.length(); i = text.offsetByCodePoints(i, 1)) {
			String character = new String(Character.toChars(text.codePointAt(i)));
			byte[] glyph = known.get(character);
			if (glyph != null) {
				bytes.write(glyph);
			} else if (" ".equals(character)) { // many files space words with a gap instead of a space character
				parts.add(new COSString(bytes.toByteArray()));
				parts.add(COSInteger.get(-280));
				bytes.reset();
			} else {
				return null;
			}
		}
		if (parts.size() == 0) {
			return new COSString(bytes.toByteArray());
		}
		parts.add(new COSString(bytes.toByteArray()));
		return parts;
	}

	private static COSString encode(PDFont font, String text) throws IOException {
		byte[] bytes;
		try {
			bytes = font.encode(text);
		} catch (IllegalArgumentException e) {
			throw new IOException("Some characters are not available in the font " + font.getName(), e);
		}
		if (font.willBeSubset()) {
			for (int i = 0; i < text.length(); i = text.offsetByCodePoints(i, 1)) {
				font.addToSubset(text.codePointAt(i));
			}
		}
		return new COSString(bytes);
	}

	/**
	 * Writes in the document the font file with the characters used. To be called after all the text was encoded: PDFBox
	 * does it by itself only for text written through its content streams.
	 */
	private static void embed(PDFont font) throws IOException {
		if (font.willBeSubset()) {
			font.subset();
		}
	}

	private static PDFont similarFont(PDDocument doc, PDFont original) throws IOException {
		String name = original.getName().toLowerCase(Locale.ROOT);
		PDFontDescriptor info = original.getFontDescriptor();
		boolean bold = name.contains("bold") || info != null && info.getFontWeight() >= 600;
		boolean italic = name.contains("italic") || name.contains("oblique") || info != null && info.isItalic();
		boolean mono = name.contains("courier") || name.contains("mono") || name.contains("consol") || info != null && info.isFixedPitch();
		boolean serif = name.contains("times") || name.contains("cambria") || name.contains("georgia")
				|| name.contains("serif") && !name.contains("sans") || info != null && info.isSerif();
		String file = mono ? "cour" : serif ? "times" : "arial";
		file += bold && italic ? "bi" : bold ? "bd" : italic ? "i" : "";
		PDFont standard = mono ? (bold ? PDType1Font.COURIER_BOLD : PDType1Font.COURIER)
				: serif ? (bold ? PDType1Font.TIMES_BOLD : italic ? PDType1Font.TIMES_ITALIC : PDType1Font.TIMES_ROMAN)
						: (bold ? PDType1Font.HELVETICA_BOLD : italic ? PDType1Font.HELVETICA_OBLIQUE : PDType1Font.HELVETICA);
		return windowsFont(doc, file, standard);
	}

	/** A font of Windows embedded in the document (all the characters it has), or the standard PDF one */
	private static PDFont windowsFont(PDDocument doc, String file, PDFont standard) throws IOException {
		String windows = System.getenv("WINDIR");
		File ttf = windows == null ? null : new File(windows, "Fonts/" + file + ".ttf");
		return ttf != null && ttf.isFile() ? PDType0Font.load(doc, ttf) : standard;
	}

	private static PDResources resourcesOf(PDPage page) {
		PDResources resources = page.getResources() != null ? page.getResources() : new PDResources();
		page.setResources(resources); // also when inherited from the page tree
		return resources;
	}
}
