/**
 *
 * =========================================================================================
 *  Copyright (C) 2019-2020
 *
 *  AM-Design-Development - (Alessio Moraschini)
 * =========================================================================================
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */
package gui.commons.frameutils.frame;


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
