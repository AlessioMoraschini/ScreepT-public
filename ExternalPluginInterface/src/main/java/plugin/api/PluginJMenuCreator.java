package plugin.api;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.swing.JMenu;
import javax.swing.JMenuItem;

import plugin.external.arch.IPlugin;
import plugin.external.arch.IPluginParentExecutor.FunctionExecutor;
import plugin.external.arch.IPluginParentExecutor.FunctionType;
import various.common.light.om.SelectionDtoFull;

public class PluginJMenuCreator {

	public PluginJMenuCreator() {
	}

	public JMenu getPluginMenu(Object parameter, FunctionType type, IPlugin plugin, boolean specifyFunctionName) {

		JMenu menu = new JMenu(plugin.getPluginName() + (specifyFunctionName ? " - " + type.readableName : ""));

		Set<String> functions = plugin.getAvailableFunctions(plugin.getID(), type);
		if(parameter == null || functions.isEmpty())
			return menu;


		String paramFlag = "main";
		if(parameter instanceof List<?>) {
			paramFlag = "list";
		} else if (parameter instanceof String[]) {
			paramFlag = "string";
		} else if (parameter instanceof SelectionDtoFull) {
			paramFlag = "seldto";
		} else {
			parameter = new String[0];
		}

		Iterator<String> iterator = functions.iterator();
		while(iterator.hasNext()) {
			String availableFunction = iterator.next();

			JMenuItem item = new JMenuItem(availableFunction);
			final String flag = paramFlag;
			final Object paramObj = parameter;
			item.addActionListener((e) -> {
				FunctionExecutor executor = plugin.getExecutor(plugin.getID(), type, availableFunction);
				if(executor != null) {
					if(flag.equals("main") || flag.equals("string")) {
						executor.executeMain((String[])paramObj);

					} else if(flag.equals("list")) {
						executor.executeFiles((List)paramObj);

					} else if(flag.equals("seldto")) {
						executor.executeSelectionDto((SelectionDtoFull)paramObj);
					}
				}
			});

			menu.add(item);
		}

		return menu;
	}
}
