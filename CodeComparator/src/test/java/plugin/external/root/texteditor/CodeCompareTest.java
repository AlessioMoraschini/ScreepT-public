package plugin.external.root.texteditor;

import javax.swing.JFrame;

public class CodeCompareTest {

	static String[] arguments = {
			"src/main/java/plugin/external/root/texteditor/CodeComparePlugin.java",
			"src/test/java/plugin/external/root/texteditor/CodeCompareTest.java"
	};

	public static void main(String[] args) {
		JMeldLauncher.DEFAULT_CLOSE_OPTION = JFrame.DISPOSE_ON_CLOSE;
		CodeComparePlugin plugin = new CodeComparePlugin();
		plugin.launchMain(new String[0]);
	}
}
