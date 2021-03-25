package utils;

import java.awt.AlphaComposite;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Window;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;

public class GuiUtils {

	public static void refreshAllUIWindows() {
		for(Window window : Window.getWindows()) {
			SwingUtilities.updateComponentTreeUI(window);
		}
	}
	
	public static Image getResizedImage(String imagePath, Integer img_width, Integer img_height) throws IOException {
		BufferedImage originalImage = ImageIO.read(new File(imagePath));
        int type = originalImage.getType() == 0? BufferedImage.TYPE_INT_ARGB
                                               : originalImage.getType();

        return resizeImage(originalImage, type, img_width, img_height);
	}

	public static Image getResizedImageOpacity(String imagePath, float opacity, Integer img_width, Integer img_height) throws IOException {
		BufferedImage originalImage = ImageIO.read(new File(imagePath));
		int type = originalImage.getType() == 0? BufferedImage.TYPE_INT_ARGB
				: originalImage.getType();
		
		return resizeImageOpacity(originalImage, opacity, type, img_width, img_height);
	}

	public static Image getResizedImage(Image image, Integer img_width, Integer img_height) throws IOException {
		
		return resizeImage(image, BufferedImage.TYPE_INT_ARGB, img_width, img_height);
	}
	
	public static Image resizeImage(Image originalImage, int type, Integer img_width, Integer img_height) {
		BufferedImage resizedImage = new BufferedImage(img_width, img_height, type);
		Graphics2D g = resizedImage.createGraphics();
		g.drawImage(originalImage, 0, 0, img_width, img_height, null);
		g.dispose();

		return resizedImage;
	}

	public static Image resizeImageOpacity(Image originalImage, float opacity, int type, Integer img_width, Integer img_height) {
		if(opacity < 0.0f)
			opacity = 0.0f;
		else if(opacity > 1.0f)
			opacity = 1f;
		
		BufferedImage resizedImage = new BufferedImage(img_width, img_height, type);
		Graphics2D g = resizedImage.createGraphics();
		g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity));
		g.drawImage(originalImage, 0, 0, img_width, img_height, null);
		g.dispose();
		
		return resizedImage;
	}
	
	public static Dimension getScaledDimension(Dimension imgSize, Dimension boundary) {

	    int original_width = imgSize.width;
	    int original_height = imgSize.height;
	    int bound_width = boundary.width;
	    int bound_height = boundary.height;
	    int new_width = original_width;
	    int new_height = original_height;

	    // first check if we need to scale width
	    if (original_width > bound_width) {
	        //scale width to fit
	        new_width = bound_width;
	        //scale height to maintain aspect ratio
	        new_height = (new_width * original_height) / original_width;
	    }

	    // then check if we need to scale even with the new height
	    if (new_height > bound_height) {
	        //scale height to fit instead
	        new_height = bound_height;
	        //scale width to maintain aspect ratio
	        new_width = (new_height * original_width) / original_height;
	    }

	    return new Dimension(new_width, new_height);
	}
}
