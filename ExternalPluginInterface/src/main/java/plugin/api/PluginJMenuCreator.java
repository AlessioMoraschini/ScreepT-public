package plugin.api;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.swing.JMenu;
import javax.swing.JMenuItem;

import plugin.external.arch.IPlugin;
import plugin.external.arch.IPluginParentExecutor.FunctionExecutor;
import plugin.external.arch.IPluginParentExecutor.FunctionType;
import plugin.external.arch.IPluginParentExecutor.ValidationException;
import various.common.light.gui.dialogs.msg.JOptionHelper;
import various.common.light.om.SelectionDtoFull;

public class PluginJMenuCreator {

	@FunctionalInterface
	public interface ObjectRetriever {
		public Object getParam();
	}

	public PluginJMenuCreator() {
	}

	public JMenu getPluginMenu(ObjectRetriever retriever, FunctionType type, IPlugin plugin, boolean specifyFunctionName) {

		ObjectRetriever safeRetriever = retriever == null ? () -> {return null;} : retriever;

		JMenu menu = new JMenu(plugin.getPluginName() + (specifyFunctionName ? " - " + type.readableName : ""));

		Set<String> functions = plugin.getAvailableFunctions(plugin.getID(), type);
		if(functions.isEmpty())
			return menu;

		Iterator<String> iterator = functions.iterator();
		while(iterator.hasNext()) {
			String availableFunction = iterator.next();

			JMenuItem item = new JMenuItem(availableFunction);
			item.addActionListener((e) -> {
				FunctionExecutor executor = plugin.getExecutor(plugin.getID(), type, availableFunction);
				if(executor != null) {

					if(safeRetriever.getParam() == null) {
						return;
					}

					String paramFlag = "main";
					if(safeRetriever.getParam() instanceof List<?>) {
						paramFlag = "list";
					} else if (safeRetriever.getParam() instanceof String[]) {
						paramFlag = "string";
					} else if (safeRetriever.getParam() instanceof SelectionDtoFull) {
						paramFlag = "seldto";
					}

					try {
						executor.validateInput(availableFunction, new Object[] {safeRetriever.getParam()});
					} catch (ValidationException e1) {
						new JOptionHelper(null).error("Unsupported type! " + e1.getMessage());
						return;
					}


					if(paramFlag.equals("main") || paramFlag.equals("string")) {
						executor.executeMain((String[])safeRetriever.getParam());

					} else if(paramFlag.equals("list")) {
						executor.executeFiles((List)safeRetriever.getParam());

					} else if(paramFlag.equals("seldto")) {
						executor.executeSelectionDto((SelectionDtoFull)safeRetriever.getParam());
					}
				}
			});

			menu.add(item);
		}

		return menu;
	}
}
