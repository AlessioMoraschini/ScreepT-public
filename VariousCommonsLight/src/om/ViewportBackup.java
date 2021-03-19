package om;

import java.awt.Point;

import javax.swing.JScrollPane;

public class ViewportBackup {

	public Point viewportBackupPosition;
	public JScrollPane scrollPane;
	
	public ViewportBackup(JScrollPane scrollPane) {
		this.scrollPane = scrollPane;
		this.viewportBackupPosition = scrollPane.getViewport().getViewPosition();
	}
	
	public void resetViewPosition() {
		scrollPane.getViewport().setViewPosition(viewportBackupPosition);
	}
}
