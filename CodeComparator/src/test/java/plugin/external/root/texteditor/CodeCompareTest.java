package plugin.external.root.texteditor;

public class CodeCompareTest {

	static String[] arguments = {
			"src/main/java/plugin/external/root/texteditor/CodeComparePlugin.java",
			"src/test/java/plugin/external/root/texteditor/CodeCompareTest.java"
	};

	public static void main(String[] args) {
		CodeComparePlugin plugin = new CodeComparePlugin();
		plugin.launchMain(arguments);
	}
}
