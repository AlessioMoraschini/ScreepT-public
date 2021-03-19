package gui.selection;

import javax.swing.JComboBox;

import gui.GuiUtils;

public abstract class AbstractSelector<T> {
	
	public abstract T getLastSelected();
	
	public T setLastSelected(T lastSelected) {
		return lastSelected;
	}
	
	public static <E> AbstractSelector<E> getDefaultSelector(JComboBox<E> combo){
		return new AbstractSelector<E>() {
			@SuppressWarnings("unchecked")
			@Override
			public E getLastSelected() {
				return (E)combo.getSelectedItem();
			}
		};
	}
	
	public static void updateStringComboSelection(AbstractSelector<String> iSelector, JComboBox<String> combo) {
		if (iSelector != null && combo != null) {
			combo.setSelectedIndex(
					GuiUtils.getFirstIndexMatchFromStringCombo(
							combo, iSelector.getLastSelected()));
		}
	}

	public static<E> void updateComboSelection(AbstractSelector<E> iSelector, JComboBox<E> combo) {
		if (iSelector != null && combo != null) {
			combo.setSelectedItem(iSelector.getLastSelected());
		}
	}

	public static<E> void nullSafeSetLastSelected(AbstractSelector<E> iSelector, E element) {
		if (iSelector != null) {
			iSelector.setLastSelected(element);
		}
	}
}
