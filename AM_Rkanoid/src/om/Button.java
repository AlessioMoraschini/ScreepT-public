package om;

import java.awt.Container;
import java.awt.Graphics;
import java.awt.Image;
import java.io.IOException;

import utils.GuiUtils;

public class Button extends Rectangle {

	private volatile Image buttonImageDeselected;
	private volatile Image buttonImageSelected;
	private volatile Image hoverButtonImageDeselected;
	private volatile Image hoverButtonImageSelected;
	
	public volatile boolean isSelected;
	public volatile boolean isMouseHover;
	
	public Container owner;
	
	private Runnable onClickAction = () -> {};
	
	public Button(int x, int y, int sizeX, int sizeY, Container owner) {
		this.x = x;
		this.y = y;
		this.sizeX = sizeX;
		this.sizeY = sizeY;
		this.owner = owner;
	}
	
	public void setOnClickAction(Runnable onclickAction) {
		this.onClickAction = onclickAction != null ? onclickAction : () -> {};
	}
	
	public void setSelectedImage(String path) throws IOException {
		hoverButtonImageSelected = GuiUtils.getResizedImage(path, (int)sizeX, (int)sizeY);
		buttonImageSelected = GuiUtils.getResizedImageOpacity(path, 0.7f, (int)sizeX, (int)sizeY);
	}

	public void setDeselectedImage(String path) throws IOException {
		hoverButtonImageDeselected = GuiUtils.getResizedImage(path, (int)sizeX, (int)sizeY);
		buttonImageDeselected = GuiUtils.getResizedImageOpacity(path, 0.7f, (int)sizeX, (int)sizeY);
	}
	
	public boolean checkIfHover(int mouseX, int mouseY) {
		if(mouseX >= left() && mouseX <= right() && mouseY >= top() && mouseY <= bottom()) {
			isMouseHover = true;
			return true;
		} else {
			isMouseHover = false;
			return false;
		}
	}
	
	public Runnable getOnClickAction() {
		return onClickAction;
	}
	
	public void click() {
		if(onClickAction != null) {
			onClickAction.run();
		}
		
		isSelected = !isSelected;
	}
	
	public void onClick(int mouseX, int mouseY) {
		
		if(!checkIfHover(mouseX, mouseY))
			return;
		
		click();
	}

	public void draw(Graphics g) {
		if (isSelected && buttonImageSelected != null) {
			g.drawImage(isMouseHover ? hoverButtonImageSelected : buttonImageSelected, (int) left(), (int) top(), null);
		
		} else if(buttonImageDeselected != null) {
			g.drawImage(isMouseHover ? hoverButtonImageDeselected : buttonImageDeselected, (int) left(), (int) top(), null);
		}
	}

	public double left() {
		return x - sizeX/2.0d;
	}

	public double right() {
		return x + sizeX/2.0d;
	}

	public double top() {
		return y - sizeY/2.0d;
	}

	public double bottom() {
		return y + sizeY/2.0d;
	}
	
}
