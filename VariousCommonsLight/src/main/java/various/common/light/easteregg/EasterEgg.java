package various.common.light.easteregg;

import various.common.light.om.ShortcutFilter;

public class EasterEgg {

	private String id;
	private Runnable action;
	private ShortcutFilter shortcutFilter;
	
	public EasterEgg() {
		id = "";
		action = () -> {};
	}
	
	public EasterEgg(String id, Runnable action, ShortcutFilter shortcutFilter) {
		this.id = id;
		this.action = action != null ? action : () -> {};
		this.shortcutFilter = shortcutFilter != null ? shortcutFilter : (e) -> {return false;};
	}

	public Runnable getAction() {
		return action;
	}

	public void setAction(Runnable action) {
		this.action = action;
	}
	
	public ShortcutFilter getShortcutFilter() {
		return shortcutFilter;
	}

	public void setShortcutFilter(ShortcutFilter shortcutFilter) {
		this.shortcutFilter = shortcutFilter;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	@Override
	public String toString() {
		return "EasterEgg [id=" + id + "]";
	}
	
}
