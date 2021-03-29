package licenses.info.extractor.om;

import java.util.ArrayList;
import java.util.List;

import various.common.light.gui.GuiUtils;
import various.common.light.utility.string.StringWorker;

public class Dependency {

	public String groupId = "";
	public String version = "";
	public String artifactId = "";
	public String sourcesUrl = "";

	public List<License> licenses = new ArrayList<>();

	public String asHtml() {
		StringBuilder dependencyBuilder = new StringBuilder();
		dependencyBuilder.append("<div>");
		dependencyBuilder.append(GuiUtils.encapsulateInTagPre(("Library Name: " + artifactId), "h1"));
		dependencyBuilder.append(GuiUtils.encapsulateInTagPre(("Library Group: " + groupId), "span"));
		dependencyBuilder.append(GuiUtils.encapsulateInTagPre(("Library Version: " + version), "span"));
		dependencyBuilder.append(GuiUtils.encapsulateInAnchorTag(
				("<b>Library sources URL:</b> " +
						StringWorker.replaceEmpyWithDefault(sourcesUrl, "Not found."))
				, sourcesUrl,
				true));

		for (License license : licenses) {
			dependencyBuilder.append(GuiUtils.encapsulateInTag(license.asHtml(), "div"));
		}

		dependencyBuilder.append("</div><hr></hr>");

		return dependencyBuilder.toString();
	}

	@Override
	public String toString() {
		return "Dependency [groupId=" + groupId + ", version=" + version + ", artifactId=" + artifactId + "]";
	}


}
