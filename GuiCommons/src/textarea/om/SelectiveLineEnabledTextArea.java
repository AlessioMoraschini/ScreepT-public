package textarea.om;

import java.awt.Point;
import java.util.Vector;

import javax.swing.JTextArea;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

public class SelectiveLineEnabledTextArea extends JTextArea {
	private static final long serialVersionUID = -5219128302650244807L;

	private Vector<Point> enabledLineIntervals;
	
	public SelectiveLineEnabledTextArea() {
		((AbstractDocument) getDocument()).setDocumentFilter(getDocumentFilter());
		enabledLineIntervals = new Vector<Point>();
	}
	
	
	/**
	 * The lines intervals for which the text editing is enabled (extremes included)
	 */
	public Vector<Point> getEnabledIntervals() {
		return enabledLineIntervals;
	}
	
	/**
	 * The lines intervals for which the text editing is enabled (extremes included)
	 */
	public void setEnabledIntervals(Vector<Point> enabledIntervals) {
		this.enabledLineIntervals = enabledIntervals;
	}
	
	/**
	 * Removes only exact interval match from enbled lines
	 */
	public boolean removeInterval(Point p) {
		for(Point point : enabledLineIntervals) {
			if(point.x == p.x && point.y == p.y ) {
				return enabledLineIntervals.remove(point);
			}
		}
		
		return false;
	}
	
	public Point addEnabledLines(Point lines) {
		this.enabledLineIntervals.add(lines);
		return lines;
	}
	public Point addEnabledLine(int line) {
		Point point = new Point(line, line);
		this.enabledLineIntervals.add(point);
		return point;
	}
	public Point addEnabledLineFromCaret(int caretPosition) throws BadLocationException {
		int line = getLineOfOffset(caretPosition);
		Point point = new Point(line, line);
		this.enabledLineIntervals.add(point);
		return point;
	}
	public Point addEnabledLines(int startOffset, int endOffset) throws BadLocationException {
		int lineStart = getLineOfOffset(startOffset);
		int lineEnd = getLineOfOffset(endOffset);
		Point point = new Point(lineStart, lineEnd);
		this.enabledLineIntervals.add(point);
		return point;
	}
	
	private DocumentFilter getDocumentFilter() {
		return new DocumentFilter() {

            private boolean allowChange(int offset) {
            	for(Point point : enabledLineIntervals) {
            		try {
						int offsetStartLine = getLineCount() == 0 ? 0 : getLineStartOffset(point.x);
						int offsetEndLine = getLineCount() == 0 ? 0 : getLineStartOffset(point.y);
						
						if(offsetStartLine <= offset && offset <= offsetEndLine) {
							return true;
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
            	}
            	
                return false;
            }

            @Override
            public void remove(FilterBypass fb, int offset, int length) throws BadLocationException {
                if (allowChange(offset)) {
                    super.remove(fb, offset, length);
                }
            }

            @Override
            public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
                if (allowChange(offset)) {
                    super.replace(fb, offset, length, text, attrs);
                }
            }

            @Override
            public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
                if (allowChange(offset)) {
                    super.insertString(fb, offset, string, attr);
                }
            }
        };
	}
}
