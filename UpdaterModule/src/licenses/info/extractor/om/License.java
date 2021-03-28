package licenses.info.extractor.om;

import java.net.URL;
import java.util.Arrays;

import javax.xml.ws.http.HTTPException;

import various.common.light.gui.GuiUtils;
import various.common.light.network.http.HttpHelper;
import various.common.light.network.http.HttpHelper.HttpRequestMetod;
import various.common.light.network.utils.DownloadUtils;
import various.common.light.utility.string.StringWorker;

public class License {

	public String name = "";
	public String url = "";
	public String file = "";
	public String content = "";

	public License(String name, String url, String file) {
		super();
		this.name = name;
		this.url = url;
		this.file = file;
	}

	public String asHtml() {
		StringBuilder dependencyBuilder = new StringBuilder();
		dependencyBuilder.append("<div>");
		dependencyBuilder.append(GuiUtils.encapsulateInTagPre(("License type: " + name), "h3"));

		dependencyBuilder.append(GuiUtils.encapsulateInTag(("<b>License Files:</b> "), "pre"));
		String[] licenses = new String[] {
				GuiUtils.encapsulateInAnchorTag("Local: " +
					StringWorker.replaceEmpyWithDefault(file, "Undefined."), file, true),
				GuiUtils.encapsulateInAnchorTag("Remote: " +
					StringWorker.replaceEmpyWithDefault(url, "Undefined."), url, true)
		};
		dependencyBuilder.append(GuiUtils.getAsHtmlList(Arrays.asList(licenses)));
		dependencyBuilder.append("</div>");

		return dependencyBuilder.toString();
	}

	public String downloadContent(boolean catchExceptions) throws Exception {
		try {
			if(StringWorker.isEmpty(url))
				return "";

			if(HttpHelper.is404(url, HttpRequestMetod.GET))
				throw new HTTPException(404);

			content = DownloadUtils.downloadFileContent(true, new URL(url), null);
		} catch (Exception e) {
			e.printStackTrace();
			if(!catchExceptions)
				throw e;
		}

		return content;
	}
}
