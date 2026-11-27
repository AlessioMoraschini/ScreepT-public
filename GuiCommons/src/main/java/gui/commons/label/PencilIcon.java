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
package gui.commons.label;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;

import javax.swing.Icon;

/** Small "edit" pencil drawn with the color of the component (so it follows every theme), no image file */
public class PencilIcon implements Icon {

	private final int size;

	public PencilIcon(int size) {
		this.size = size;
	}

	@Override
	public void paintIcon(Component c, Graphics g0, int x, int y) {
		Graphics2D g = (Graphics2D) g0.create();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
		Color color = c.isEnabled() ? c.getForeground() : Color.GRAY;
		g.setColor(color);
		g.translate(x, y);
		double s = size / 16.0;
		g.scale(s, s);
		// body of the pencil, from the top right corner down to the tip
		Path2D body = new Path2D.Double();
		body.moveTo(11.2, 1.6);
		body.lineTo(14.4, 4.8);
		body.lineTo(5.6, 13.6);
		body.lineTo(1.6, 14.4);
		body.lineTo(2.4, 10.4);
		body.closePath();
		g.setStroke(new java.awt.BasicStroke(1.3f, java.awt.BasicStroke.CAP_ROUND, java.awt.BasicStroke.JOIN_ROUND));
		g.draw(body);
		// the line that separates the eraser end
		g.draw(new java.awt.geom.Line2D.Double(9.4, 3.4, 12.6, 6.6));
		// the tip, filled
		Path2D tip = new Path2D.Double();
		tip.moveTo(2.4, 10.4);
		tip.lineTo(5.6, 13.6);
		tip.lineTo(1.6, 14.4);
		tip.closePath();
		g.fill(tip);
		g.dispose();
	}

	@Override
	public int getIconWidth() {
		return size;
	}

	@Override
	public int getIconHeight() {
		return size;
	}
}
