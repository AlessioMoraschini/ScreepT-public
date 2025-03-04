package various.common.light.gui.dialogs.color.om;

public class SelectionColorChangedAbstract implements SelectionColorChanged {

	@Override
	public ColorChanged backgroundChanged() {
		return (color) -> {
			System.out.print("Background changed: " + color);
		};
	}

	@Override
	public ColorChanged foregroundChanged() {
		return (color) -> {
			System.out.print("Foreground changed: " + color);
		};
	}
	
	public static SelectionColorChanged initFromSameColorChanged(ColorChanged colorChanged) {
		return new SelectionColorChanged() {
			
			@Override
			public ColorChanged foregroundChanged() {
				return colorChanged;
			}
			
			@Override
			public ColorChanged backgroundChanged() {
				return colorChanged;
			}
		};
	}
}
