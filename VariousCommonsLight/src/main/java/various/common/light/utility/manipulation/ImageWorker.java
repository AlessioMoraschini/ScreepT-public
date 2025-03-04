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
package various.common.light.utility.manipulation;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Random;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;

import org.apache.commons.io.FilenameUtils;

import various.common.light.utility.log.SafeLogger;


public class ImageWorker {

	static SafeLogger logger = new SafeLogger(ImageWorker.class);

	public final static int ORIZ_OPTION = 0;
	public final static int VERT_OPTION = 1;

	public final static String[] SUPPORTED_EXTENSIONS = ImageIO.getWriterFileSuffixes();

	SecureRandom rand;

	protected BufferedImage image;
	protected File imageFile;

	public ImageWorker(String imgPath) throws IOException {
		imageFile = new File(imgPath);
		image = ImageIO.read(imageFile);
	}

	public ImageWorker(File imgFile) throws IOException {
		imageFile = imgFile;
		image = ImageIO.read(imgFile);
	}

	public ImageWorker(BufferedImage img) {
		image = img;
	}



	public int Height() {
		return image.getHeight();
	}

	public int Width() {
		return image.getWidth();
	}



	/**
	 * Metodo senza parametri che ritorna la matrice di interi x pixel (getRGB(x,y))
	 */
	public Integer[][] readPxValue() throws IOException {

		// calcolo larghezza e altezza immagine
		int height = image.getHeight();
		int width = image.getWidth();

		// matrice rappresentante gli interi getRGB(x,y) -> � il valore del pixel
		Integer[][] imgPixels = new Integer[width][height];

		// ciclo di scansione immagine

		// per ogni riga
		for (int i = 0; i < height; i++) {

			// per ogni colonna
			for (int j = 0; j < width; j++) {
				Integer px = image.getRGB(j, i);
				imgPixels[j][i] = px;
				logger.debug("[X=" + j + " ; Y=" + i + "] Value: " + px + " ");
			}
		}
		return imgPixels;
	}

	/**
	 * Metodo senza parametri che setta il valore RGB dell'immagine di destinazione
	 * (source.getRGB(x,y))
	 */
	public BufferedImage writePxValue(BufferedImage srcImg, int bufferedImageType) throws IOException {

		// calcolo larghezza e altezza immagine
		int height = srcImg.getHeight();
		int width = srcImg.getWidth();

		BufferedImage resultImg = new BufferedImage(width, height, bufferedImageType);

		// ciclo di scansione immagine
		// per ogni riga
		for (int i = 0; i < height; i++) {

			// per ogni colonna
			for (int j = 0; j < width; j++) {
				resultImg.setRGB(j, i, srcImg.getRGB(j, i));
			}
		}
		return resultImg;
	}

	/**
	 * Converts a given Image into a BufferedImage
	 *
	 * @param img The Image to be converted
	 * @return The converted BufferedImage
	 */
	public static BufferedImage toBufferedImage(Image img)
	{
	    if (img instanceof BufferedImage)
	    {
	        return (BufferedImage) img;
	    }

	    // Create a buffered image with transparency
	    BufferedImage bimage = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_ARGB);

	    // Draw the image on to the buffered image
	    Graphics2D bGr = bimage.createGraphics();
	    bGr.drawImage(img, 0, 0, null);
	    bGr.dispose();

	    // Return the buffered image
	    return bimage;
	}




	/**
	 * this method resize an image to the given wanted dimensions keeping the same
	 * width/height ratio. returns a BufferedImage resized
	 *
	 * @param image
	 * @param imageType
	 * @param newWidth
	 * @param newHeight
	 * @return
	 */
	public static BufferedImage scaleImage(BufferedImage image, int imageType, int newWidth, int newHeight) {
		// Make sure the aspect ratio is maintained, so the image is not distorted
		double thumbRatio = (double) newWidth / (double) newHeight;
		int imageWidth = image.getWidth(null);
		int imageHeight = image.getHeight(null);
		double aspectRatio = (double) imageWidth / (double) imageHeight;

		if (thumbRatio < aspectRatio) {
			newHeight = (int) (newWidth / aspectRatio);
		} else {
			newWidth = (int) (newHeight * aspectRatio);
		}

		// Draw the scaled image
		BufferedImage newImage = new BufferedImage(newWidth, newHeight, imageType);
		Graphics2D graphics2D = newImage.createGraphics();
		graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		graphics2D.drawImage(image, 0, 0, newWidth, newHeight, null);

		return newImage;
	}

	public static Dimension getDimensionSameRatio(Image image, int newWidth, int newHeight) {
		// Make sure the aspect ratio is maintained, so the image is not distorted
		double thumbRatio = (double) newWidth / (double) newHeight;
		int imageWidth = image.getWidth(null);
		int imageHeight = image.getHeight(null);
		double aspectRatio = (double) imageWidth / (double) imageHeight;

		if (thumbRatio < aspectRatio) {
			newHeight = (int) (newWidth / aspectRatio);
		} else {
			newWidth = (int) (newHeight * aspectRatio);
		}

		return new Dimension(newWidth, newHeight);
	}

	/**
	 * this method resize an image to the given wanted dimensions keeping the same
	 * width/height ratio. returns a Image resized
	 */
	public static Image scaleImageSameRatio(Image image, int newWidth, int newHeight) {

		Dimension sameRatio = getDimensionSameRatio(image, newWidth, newHeight);
		return image.getScaledInstance(sameRatio.width, sameRatio.height, Image.SCALE_SMOOTH);
	}

	/**
	 * this method resize an image to the given wanted dimensions keeping the same
	 * width/height ratio. returns a Image resized
	 */
	public static Image scaleImage(Image image, int newWidth, int newHeight, int scaleMode) {
		// Make sure the aspect ratio is maintained, so the image is not distorted
		Dimension sameRatio = getDimensionSameRatio(image, newWidth, newHeight);

		return image.getScaledInstance(sameRatio.width, sameRatio.height, scaleMode);
	}

	/**
	 * this method generate a random color of the type argb
	 * more the value is high and more the color is dark
	 *
	 * @return
	 */
	public static Color randomColorRGB(float darkness) {
		Random rand = new Random();
		float r = rand.nextFloat() / darkness;
		float g = rand.nextFloat() / darkness;
		float b = rand.nextFloat() / darkness;
		float a = rand.nextFloat();
		if (a < 0.4) {
			// guarantee opaque colors
			a += 0.5;
		}
		if(r<0) {
			r=0;
		}else if(r>1) {
			r=1;
		}
		if(g<0) {
			g=0;
		}else if(g>1) {
			g=1;
		}
		if(b<0) {
			b=0;
		}else if(b>1) {
			b=1;
		}

		return new Color(r, g, b, a);
	}

	public static int getColorComponentsTotRGBA(Color color) {
		int a = color.getAlpha();
		int r = color.getRed();
		int g = color.getGreen();
		int b = color.getBlue();

		return a+r+g+b;
	}

	/**
	 * return the percent integer from 0 to 100 representing how much a color is light
	 * @param color
	 * @return lightness in perceptual
	 */
	public static int getPercentOfLightness(Color color) {
		int sum = getColorComponentsTotRGBA(color);
		int max = 255*4;
		return ((max/sum)*100);
	}

	 /**
	   * Returns the complimentary (opposite) color.
	   * @param color int RGB color to return the compliment of
	   * @return int RGB of compliment color
	   */
	  public static Color getComplimentColor(Color color) {

		  if(color == null) {return Color.WHITE;}
		  int colorSrc = color.getRGB();

		  return new Color(colorSrc ^ 0x00ffffff);
	  }

	/**
	 * this method is used to convert an image in another format and write it on
	 * file. It can manipulate image output type, algorithm, and output extension.
	 */
	public static boolean convertAndWrite(BufferedImage srcImage, String outExtension, File destination) {

		try (OutputStream os = new FileOutputStream(destination)) {
			ImageIO.write(srcImage, outExtension, os);
			os.close();
			return true;
		} catch (Exception exp) {
			return false;
		}
	}

	public static Image convert(Image srcImageRaw, String outExtension) throws IOException {
		BufferedImage srcImage = toBufferedImage(srcImageRaw);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		byte[] converted = new byte[256];
		try {
			ImageIO.write(srcImage, outExtension, baos);
			converted = baos.toByteArray();
			baos.close();
		} catch (Exception exp) {
			return null;
		}
		ByteArrayInputStream is = new ByteArrayInputStream(converted);
		return ImageIO.read(is);
	}



	/**
	 * method to clone BufferedImage by value
	 *
	 * @param source image to clone
	 *
	 * @return the cloned BufferedImage
	 */
	public static BufferedImage copyImage(BufferedImage source){
	    BufferedImage b = new BufferedImage(source.getWidth(), source.getHeight(), source.getType());
	    Graphics g = b.getGraphics();
	    g.drawImage(source, 0, 0, null);
	    g.dispose();
	    return b;
	}

	/**
	 * This method compress an image with given params and returns the compressed image as byte array
	 *
	 * @param sourceImage the image to be compressed
	 * @param extension is the string extension for the output bytes (loseless modes as "png" do not lose information)
	 * @param factor the compression factor between 0.0 and 1.0
	 *
	 * @author Alessio Moraschini
	 */
	public static byte[] compressIMGjpgNoMeta(BufferedImage sourceImage, float factor) throws UnsupportedOperationException {

		ByteArrayOutputStream compressed = new ByteArrayOutputStream();

		try {
			// intialize ImageStream to byteArrayStream
			ImageOutputStream outputStream = ImageIO.createImageOutputStream(compressed);

			// initialize type use it to create outWriter
			ImageTypeSpecifier type = ImageTypeSpecifier.createFromBufferedImageType(sourceImage.getType());
			ImageWriter writer = ImageIO.getImageWriters(type, "jpg").next();

			// calculate param for compression
			ImageWriteParam param = writer.getDefaultWriteParam();
			if (param.canWriteCompressed()) {
				param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
				param.setCompressionQuality(factor);
			}else {
				// free used in-memory structures
				compressed.flush();
				outputStream.flush();
				outputStream.close();
				writer.dispose();
				throw new UnsupportedOperationException();
			}
			// now write image to in-memory buffer
			writer.setOutput(outputStream);
			writer.write(null, new IIOImage(sourceImage, null, null), param);

			// free used in-memory structures
			outputStream.close();
			writer.dispose();

		} catch (IOException e) {
			logger.error("Exception happened!", e);
		}catch (IllegalArgumentException e) {
			logger.error("Error ", e);
		}

		return compressed.toByteArray();
	}

	/**
	 * This method compress an image with given params and returns the compressed image as byte array
	 *
	 * @param sourceImage the image to be compressed
	 * @param extension is the string extension for the output bytes (loseless modes as "png" do not lose information)
	 * @param factor the compression factor between 0.0 and 1.0
	 * @param keepMetadata true if want to keep metadata, false to discard metadata
	 *
	 * @author Alessio Moraschini
	 *
	 * @throws IOException
	 */
	public static byte[] compressIMGjpg(File sourceImageFile, float factor, boolean keepMetadata) throws IOException, UnsupportedOperationException {

		ByteArrayOutputStream compressed = new ByteArrayOutputStream();
		BufferedImage srcImage = null;
		IIOMetadata metadata = null;

		if(keepMetadata) {
			try {
				ImageInputStream in = ImageIO.createImageInputStream(sourceImageFile);
				srcImage = ImageIO.read(sourceImageFile);
				ImageReader readerImg = ImageIO.getImageReadersByFormatName(FilenameUtils.getExtension(sourceImageFile.getName())).next();
				readerImg.setInput(in, true, false);
				metadata = readerImg.getImageMetadata(0);
			} catch (IOException e) {
				logger.error("Exception retrieving image: "+sourceImageFile.getAbsolutePath(), e);
			}
		}else {
			srcImage = ImageIO.read(sourceImageFile);
		}

		try {
			// intialize ImageStream to byteArrayStream
			ImageOutputStream outputStream = ImageIO.createImageOutputStream(compressed);

			// initialize type use it to create outWriter
			ImageTypeSpecifier type = ImageTypeSpecifier.createFromBufferedImageType(srcImage.getType());
			ImageWriter writer = ImageIO.getImageWriters(type, "jpg").next();

			// calculate param for compression
			ImageWriteParam param = writer.getDefaultWriteParam();
			if (param.canWriteCompressed()) {
				param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
				param.setCompressionQuality(factor);
			}else {
				// free used in-memory structures
				outputStream.close();
				writer.dispose();
				throw new UnsupportedOperationException();
			}

			// now write image to in-memory buffer
			writer.setOutput(outputStream);
			writer.write(null, new IIOImage(srcImage, null, metadata), param);

			// free used in-memory structures
			compressed.flush();
			outputStream.flush();
			outputStream.close();
			writer.dispose();

		} catch (FileNotFoundException e) {
			logger.error("Exception happened!", e);
		}

		return compressed.toByteArray();
	}

	/**
	 * Metodo statico che, dato il file dell'immagine e il logger ritorna la matrice
	 * di interi x pixel (getRGB(x,y))
	 */
	public static Integer[][] readPxValue(File imgFile, File fileLogger) throws IOException {

		// inizializzo variabili necessarie globali
		BufferedImage img = null;
		FileWriter writer;
		// creo variabile immagine
		img = ImageIO.read(imgFile);

		// se file di log non esiste lo creo
		if (!fileLogger.exists()) {
			fileLogger.createNewFile();
		}
		// associo il file e abilito la sovrascrittura
		writer = new FileWriter(fileLogger, false);

		// calcolo larghezza e altezza immagine
		int height = img.getHeight();
		int width = img.getWidth();

		// matrice rappresentante gli interi getRGB(x,y) -> � il valore del pixel
		Integer[][] imgPixels = new Integer[width][height];

		// ciclo di scansione immagine

		// per ogni riga
		for (int i = 0; i < height; i++) {

			// per ogni colonna
			for (int j = 0; j < width; j++) {
				Integer px = img.getRGB(j, i);
				imgPixels[j][i] = px;
				ImageWorker.logger.debug("[X=" + j + " ; Y=" + i + "] Value: " + px + " ");
				writer.write("[X=" + j + " ; Y=" + i + "] Value: " + px + " ");
			}
		}

		// chiudo il writer per non sprecare memoria
		writer.close();

		return imgPixels;
	}

	/**
	 * @param File
	 *            logger : this variable will contain file rapresentation Metodo con
	 *            parametro logger che ritorna la matrice di interi x pixel
	 *            (getRGB(x,y))
	 */
	public Integer[][] readPxValueAndLog(File fileLogger) throws IOException {

		FileWriter writer;

		// se file di log non esiste lo creo
		if (!fileLogger.exists()) {
			fileLogger.createNewFile();
		}
		// associo il file e abilito la sovrascrittura
		writer = new FileWriter(fileLogger, false);

		// calcolo larghezza e altezza immagine
		int height = image.getHeight();
		int width = image.getWidth();

		// matrice rappresentante gli interi getRGB(x,y) -> � il valore del pixel
		Integer[][] imgPixels = new Integer[width][height];

		// ciclo di scansione immagine

		// per ogni riga
		for (int i = 0; i < height; i++) {

			writer.write("###################\n");

			// per ogni colonna
			for (int j = 0; j < width; j++) {
				Integer px = image.getRGB(j, i);
				imgPixels[j][i] = px;
				logger.debug("[X=" + j + " ; Y=" + i + "] Value: " + px + " ");
				writer.write("[X=" + j + " ; Y=" + i + "] Value: " + px + " ");
			}

			writer.write("\n");
		}
		// chiudo il writer per non sprecare memoria
		writer.close();
		return imgPixels;
	}

	/**
	 * Metodo statico che, dato il nome (String) del file dell'immagine e il logger
	 * ritorna la matrice di interi x pixel (getRGB(x,y))
	 */
	public static Integer[][] readPxValues(String imgPath, File logger) throws IOException {

		// inizializzo variabili necessarie globali
		BufferedImage img = null;
		File fileImg = null;
		FileWriter writer;

		fileImg = new File(imgPath);
		img = ImageIO.read(fileImg);

		// se file di log non esiste lo creo
		if (!logger.exists()) {
			logger.createNewFile();
		}
		// associo il file e abilito la sovrascrittura
		writer = new FileWriter(logger, false);

		// calcolo larghezza e altezza immagine
		int height = img.getHeight();
		int width = img.getWidth();

		// matrice rappresentante gli interi getRGB(x,y) -> � il valore del pixel
		Integer[][] imgPixels = new Integer[width][height];

		// ciclo di scansione immagine

		// per ogni riga
		for (int i = 0; i < height; i++) {

			// per ogni colonna
			for (int j = 0; j < width; j++) {
				Integer px = img.getRGB(j, i);
				imgPixels[j][i] = px;
				ImageWorker.logger.debug("[X=" + j + " ; Y=" + i + "] Value: " + px + " ");
				writer.write("[X=" + j + " ; Y=" + i + "] Value: " + px + " ");
			}
		}

		// chiudo il writer per non sprecare memoria
		writer.close();

		return imgPixels;
	}

	/**
	 * Questo metodo restituisce un array contenente il valore dei pixel ottenuti scansionando l'immagine orizzontalmente, distanziati dall'intervallo fornito
	 * @param image
	 * @param N E' il numero di pixel da estrarre. Può essere anche maggiore del numero di pixel contenuti nell'immagine
	 * @param interval intervallo tra un pixel e il successivo durante la scansione
	 * @return array di interi rappresentanti il valore dei pixel scansionati
	 * @throws STEGException
	 */
	public static int[] readNSpacedValuesFromImage(BufferedImage image, int N, int interval) throws Exception {
		int H = 0;
		int W = 0;
		if (image!=null && N>0) {
			H = image.getHeight();
			W = image.getWidth();
			if(interval == 0) {
				interval = (W/N)%W;
			}
		}else throw new Exception("Invalid input");

		if(N<=(W*H)) {
			int[] read = new int[N];
			int row=0;
			int col=0;
			for(int i = 0; i<N; i++) {
				if(row>H-1) {
					row=0;
					col++;
				}
				if(col>W-1) {
					col=0;
					row++;
				}

				read[i] = image.getRGB(col, row);
				col += interval%W;
			}
			return read;
		}else {
			return null;
		}
	}

	public static Color getDarkerNtimes(Color color, int n) {
		for (int i = 0; i < n; i++) {
			color = color.darker();
		}
		return color;
	}

	public static Color getBrighterNtimes(Color color, int n) {
		for (int i = 0; i < n; i++) {
			color = color.brighter();
		}
		return color;
	}


	/**
	 * metodo che crea un'immagine casuale di i*j pixel e la salva nel file
	 * specificato come parametro
	 *
	 * @return null : in caso di errore ritorna null
	 *
	 * @param extension
	 *            of the file of the type ".jpg" or others formats
	 * @return BufferedImage keyImage
	 * @throws IOException
	 */
	public synchronized static BufferedImage generateKeyImageAndWrite(int width, int height, int imgType,
			File destinationFile, String imgExtension) throws IOException {

		BufferedImage generatedKeyimg = new BufferedImage(width, height, imgType);

		// use secure random for more entrophy on the key
		SecureRandom random;
		try {
			SecureRandom.getInstanceStrong();
		} catch (NoSuchAlgorithmException e) {
		}
		random = new SecureRandom();

		int pixelRGBValue = 0;

		// per ogni riga
		for (int i = 0; i < height; i++) {
			// per ogni colonna
			for (int j = 0; j < width; j++) {
				pixelRGBValue = random.nextInt(Integer.MAX_VALUE)-random.nextInt(Integer.MAX_VALUE);
				generatedKeyimg.setRGB(j, i, pixelRGBValue);

			}
		}
		if (!destinationFile.exists()) {
			if (!destinationFile.getParentFile().exists()) {
				destinationFile.getParentFile().mkdirs();
			}
			destinationFile.createNewFile();
		}
		boolean writeEsit = ImageIO.write(generatedKeyimg, imgExtension, destinationFile);
		// in caso di errore ritorna null
		if (!writeEsit) {
			generatedKeyimg = null;
		}
		return generatedKeyimg;
	}

	/**
	 * questo metodo ritorna un array contenente i valori interi di N pixel, scorrendo l'immagine fornita secondo l'ordine scelto
	 * @param image immagine sorgente
	 * @param N numero di valori da estrarre: se maggiore del numero di pixel presenti in image allora ritorna null
	 * @param option : ORIZ_OPTION oppure VERT_OPTION (definiti in questa classe come statici)
	 * @throws STEGException
	 */
	public static int[] readNValuesFromImage(BufferedImage image, int N, int option) throws Exception {
		int H = 0;
		int W = 0;
		if (image!=null && N>0) {
			H = image.getHeight();
			W = image.getWidth();
		}else throw new Exception("invalid input!");

		if(N<=(W*H)) {
			int[] read = new int[N];
			int row=0;
			int col=0;
			for(int i = 0; i<N; i++) {
				if(row>H-1) {
					row=0;
					col++;
				}
				if(col>W-1) {
					col=0;
					row++;
				}

				if(option==ORIZ_OPTION) {
					read[i] = image.getRGB(col, row);
					col++;
				}else if(option==VERT_OPTION) {
					read[i] = image.getRGB(col, row);
					row++;
				}
			}
			return read;
		}else {
			return null;
		}
	}

	/////////////////////
	// GETTERS & SETTERS /
	//////////////////////

	public File getImageFile() {
	return imageFile;
	}

	public void setImageFile(File imageFile) {
	this.imageFile = imageFile;
	}

	public BufferedImage getImage() {
	return image;
	}

	public void setImage(BufferedImage image) {
	this.image = image;
	}

}
