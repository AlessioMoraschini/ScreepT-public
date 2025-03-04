/*
 *
 * =========================================================================================
 *  Copyright (C) 2019-2021
 *
 *  AM-Design-Development - (Alessio Moraschini) - All Rights Reserved
 * =========================================================================================
 *
 * You should have received a copy of the license with this file.
 * If not, please write to: info@am-design-development.com, or visit : https://www.am-design-development.com/
 */
package function;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.JFrame;

public abstract class AbstractCurveFunction extends JFrame {
	private static final long serialVersionUID = -7952145336300308954L;
	
	protected List<Double> x, y;
	protected double minX, minY, maxX, maxY;
	
	private double initialT, finalT;
	private double interval;
	
	public AbstractCurveFunction(double initialT, double interval, double finalT) {
		reset();
		
		this.interval = interval;
		this.initialT = initialT;
		this.finalT = finalT;
	}
	
	protected final void increment() {
		
	}
	
	public abstract double updateX(double t);

	public abstract double updateY(double t);
	
	public final void calculateXY() {
		double tval = initialT;
		int nIntervals = (int)((finalT - initialT)/interval);
		
		for(int i = 0; i < nIntervals; i++) {
			tval += interval;
			x.add(updateX(tval));
			y.add(updateY(tval));
		}
		
		minX = Collections.min(x);
		maxX = Collections.max(x);

		minY = Collections.min(y);
		maxY = Collections.max(y);
	}
	
	public final void rotate90() {
		for(int i = 0; i < x.size(); i++) {
			double temp = x.get(i);
			x.remove(i);
			x.add(i, y.get(i));

			y.remove(i);
			y.add(i, temp);
		}
	}
	
	public void reset() {
		this.x = new ArrayList<Double>();
		this.y = new ArrayList<Double>();
	}
	
	@Override
	public void paint(Graphics g) {
		super.paint(g);
		
		double xInterval = Math.abs(maxX - minX);
		double yInterval = Math.abs(maxY - minY);
		
		double multiplierX = 10d;//getWidth() / (xInterval);
		double multiplierY = 10d;//getHeight() / (yInterval);
		
		g.setColor(Color.BLACK);
		
        for (int i = 0; i < x.size() && i < y.size(); i++) {
        	int xActual = (int)((xInterval/2 +30d + x.get(i))*multiplierX);
        	int yActual = (int)((yInterval/2 + 30d + y.get(i))*multiplierY);
			g.fillRect(
					xActual,
					yActual,
					6, 
					6
				); 
			
			System.out.println("X:" + xActual);
			System.out.println("Y:" + yActual);
		}
        
    }
	
	public static void main(String[] args) {
		AbstractCurveFunction funct = new LoveCurve(-10d, 0.1d, 10d);
		funct.calculateXY();
		funct.rotate90();
		
		funct.setVisible(true);
		funct.setMinimumSize(new Dimension(800,800));
		funct.setLocationRelativeTo(null);
		funct.paint(funct.getGraphics());
		funct.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}
}
