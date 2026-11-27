/**
 *
 * =========================================================================================
 *  Copyright (C) 2019-2026
 *
 *  AM-Design-Development - (Alessio Moraschini) - All Rights Reserved
 * =========================================================================================
 *
 * You should have received a copy of the license with this file.
 * If not, please write to: info@am-design-development.com, or visit : https://www.am-design-development.com
 */
package various.common.light.gui;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.awt.Dimension;
import java.awt.Rectangle;

import javax.swing.JFrame;

import org.junit.Test;

public class ScreenMemoryTest {

	@Test
	public void maximizedMeansBothTheStateAndTheSize() {
		Rectangle screen = new Rectangle(0, 0, 1920, 1080);
		assertTrue(ScreenMemory.fills(JFrame.MAXIMIZED_BOTH, new Dimension(1920, 1040), screen));
		assertFalse(ScreenMemory.fills(JFrame.MAXIMIZED_BOTH, new Dimension(1500, 850), screen));
		assertFalse(ScreenMemory.fills(JFrame.NORMAL, new Dimension(1920, 1040), screen));
		assertFalse(ScreenMemory.fills(JFrame.MAXIMIZED_VERT, new Dimension(1920, 1040), screen));
	}
}
