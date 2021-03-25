/**
 *
 * =========================================================================================
 *  Copyright (C) 2019-2020
 *
 *  AM-Design-Development - (Alessio Moraschini) - All Rights Reserved
 * =========================================================================================
 *
 * You should have received a copy of the license with this file.
 * If not, please write to: info@am-design-development.com, or visit : https://www.am-design-development.com
 */
package frameutils.frame;


import java.awt.Graphics;
import java.awt.Image;

import javax.swing.JPanel;

import various.common.light.utility.manipulation.ImageWorker;

public class BackgroundFrame extends JPanel{
	private static final long serialVersionUID = -2332359202457198415L;
	
	private Image backgroundImage;
	private boolean scaleIMG;
	
	public BackgroundFrame() {
		scaleIMG = true;
	}
	
	public BackgroundFrame(Image background) {
		
		backgroundImage = background;
		scaleIMG = true;
	}
	
	public BackgroundFrame(Image background, boolean scaleImage) {
		backgroundImage = background;
		scaleIMG = scaleImage;
	}
	
	@Override
	  protected void paintComponent(Graphics g) {
		
	    super.paintComponent(g);
	    if (backgroundImage != null) {
			if (!scaleIMG) {
				g.drawImage(backgroundImage, 0, 0, null);
			}else {
				int Y=0;
				int X=0;
				
				int W = this.getWidth();
				int H = this.getHeight();
				
				if(W >= 0 && H >= 0) {
					backgroundImage = ImageWorker.scaleImageSameRatio(backgroundImage, W, H);
					X = (W - backgroundImage.getWidth(null)) / 2;
					Y = (H - backgroundImage.getHeight(null)) / 2;
				}
				g.drawImage(backgroundImage, X, Y, null);
			}
		}
	}
	
}
