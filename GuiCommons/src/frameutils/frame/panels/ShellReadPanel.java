package frameutils.frame.panels;

import java.awt.Color;
import java.awt.Font;
import java.io.PrintStream;

import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;

import frameutils.frame.panels.arch.IParentPanel;
import frameutils.frame.panels.arch.ParentPanel;
import frameutils.utils.GuiUtilsExtended;
import net.miginfocom.swing.MigLayout;
import textarea.om.TextAreaOutputStream;

/**
 * This class implements a console that prints out every output from System.out/System.err streams
 * @author Alessio Moraschini
 *
 */
public class ShellReadPanel extends ParentPanel implements IParentPanel {
	private static final long serialVersionUID = -351031239684511014L;
	
	public TextAreaOutputStream taos;
	public int maxLines;
	JScrollPane currentScrollPane;
	JTextArea textArea; 
	PrintStream ps;
	PrintStream oldPsBackup;
	PrintStream oldPsBackupErr;
	
	public ShellReadPanel(int visibleLines) {
		maxLines = visibleLines;
		super.init();
	}
	
	public ShellReadPanel() {
		this.maxLines = 1000;
		super.init();
	}
	
	@Override
	protected void initGui() {
		
		setLayout(new MigLayout("fill, insets 1, hidemode 2", "[50px,grow]", "[50px,grow]"));
		
		currentScrollPane = getScrollPane(true, true);
		currentScrollPane.setPreferredSize(GuiUtilsExtended.getScreenSize());
		add(currentScrollPane,  "cell 0 0,grow");
		
		textArea = new JTextArea();
		textArea.setSelectionColor(Color.GRAY);
		textArea.setSelectedTextColor(Color.WHITE);
		textArea.setFont(new Font("Segoe UI", Font.PLAIN, 18));
		currentScrollPane.setViewportView(textArea);
		currentScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		currentScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		
		initStream();
	}
	
	@Override
	protected void addHandlers() {
	}
	
	@Override
	protected void addKeyListeners() {
	}

	public void initStream() {
		oldPsBackup = System.out;
		oldPsBackupErr = System.err;
		taos = new TextAreaOutputStream(textArea, maxLines);
        ps = new PrintStream( taos );
        System.setOut( ps );
        System.setErr( ps );
	}
	
	public void closeStream() {
		taos.flush();
		ps.flush();
		taos.close();
		ps.close();
        System.setOut( oldPsBackup );
        System.setErr( oldPsBackupErr );
	}

}