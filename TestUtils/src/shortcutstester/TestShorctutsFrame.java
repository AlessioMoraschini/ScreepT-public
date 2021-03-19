package shortcutstester;

import java.awt.KeyboardFocusManager;
import java.awt.Rectangle;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import utils.TestUtils;

public class TestShorctutsFrame {
	
	static int i = 0;
	static final int maxAdvices = 50;
	static final int REFRESH_ADVICE_PERIOD = 2000;

	public static void main(String[] args) {
		analyzeTextInput();
	}
	
	
	public static void analyzeTextInput() {
		JFrame frame = new JFrame("Test shortcuts");
		JTextArea textAreaS = new JTextArea();
		JScrollPane scrollPane = new JScrollPane(textAreaS);
		scrollPane.setViewportView(textAreaS);
		textAreaS.setText("0123456789");
		textAreaS.setSelectionStart(3);
		textAreaS.setSelectionEnd(1);
//		textAreaS.setCaretPosition(1);
		frame.add(scrollPane);
		frame.setVisible(true);
		frame.setAlwaysOnTop(true);
		KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(new ShortcutTester());
		frame.setBounds(new Rectangle(100, 100, 400, 300));
		frame.addWindowListener(new WindowListener() {
			
			public void windowOpened(WindowEvent e) {
			}
			public void windowIconified(WindowEvent e) {
			}
			public void windowDeiconified(WindowEvent e) {
			}
			public void windowDeactivated(WindowEvent e) {
			}
			public void windowClosing(WindowEvent e) {
				System.exit(0);
			}
			public void windowClosed(WindowEvent e) {
			}
			public void windowActivated(WindowEvent e) {
			}
		});
		
		textAreaS.addFocusListener(new FocusListener() {
			
			@Override
			public void focusLost(FocusEvent e) {
				TestUtils.printl("\n=> Focus lost\n");
			}
			
			@Override
			public void focusGained(FocusEvent e) {
				TestUtils.printl("\n=> Focus gained\n");
			}
		});
		
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				while (i < maxAdvices) {
					try {
						++i;
						textAreaS.requestFocus();
						int start = textAreaS.getSelectionStart();
						int end = textAreaS.getSelectionEnd();
						int caret = textAreaS.getCaretPosition();
						TestUtils.printl("Start: " + start);
						TestUtils.printl("End: " + end);
						TestUtils.printl("Caret" + caret);
						Thread.sleep(REFRESH_ADVICE_PERIOD);
					} catch (InterruptedException e) {
						e.printStackTrace();
					} 
				}
			}
		}).start();
	}
}
