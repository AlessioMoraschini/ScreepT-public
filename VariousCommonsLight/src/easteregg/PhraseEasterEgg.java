package easteregg;

import java.awt.Dimension;
import java.util.UUID;

import various.common.light.gui.dialogs.msg.JOptionHelper;
import various.common.light.om.ShortcutFilter;
import various.common.light.om.TextRetriever;

public class PhraseEasterEgg extends EasterEgg {

	private TextRetriever textRetriever;
	
	private static Dimension defaultDialogSize = new Dimension(750, 150);
	
	public PhraseEasterEgg(String id, TextRetriever textRetriever, ShortcutFilter shortcutFilter) {
		super(id, () -> {
			new JOptionHelper(null).infoScroll(textRetriever.retrieveText(), "Code can be a poem, or even a nightmare.", defaultDialogSize);
		}, shortcutFilter);
		this.textRetriever = textRetriever;
	}
	
	public PhraseEasterEgg(TextRetriever textRetriever) {
		this(UUID.randomUUID().toString(), textRetriever, null);
	}
	
	public TextRetriever getTextRetriever() {
		return textRetriever;
	}

	public void setTextRetriever(TextRetriever textRetriever) {
		this.textRetriever = textRetriever;
	}

	@Override
	public String toString() {
		return super.toString();
	}
	
}
