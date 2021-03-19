package crypt.manager;

import amfileutils.FileStuff;
import crypt.aes.AESException;
import crypt.aes.AesCipher;
import crypt.steg.StegCipher;
import crypt.steg.StegOmResult;
import dialogutils.TestUpdateProgress;
import java.awt.image.BufferedImage;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.imageio.ImageIO;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import media.ImageGeneralWorker;
import media.STEGException;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import resources.GeneralConfig;
import variousutils.CustomFileComparator;
import variousutils.EntophyzerUtils;

public class ScreeptManager
{
  static Logger logger = Logger.getLogger(ScreeptManager.class);
  private final String TEMP_PATH = GeneralConfig.RESOURCES_DIR + "temp/";
  private String OUT_PATH = this.TEMP_PATH;
  private String RESULT_PATH = this.TEMP_PATH;
  private static String CRYPT_NAME = "temp_crypter_out";
  private static String DECRYPT_NAME = "temp_decrypter_out";
  private static int AES_BUFFER_SIZE = grantMultipleOfN(Integer.valueOf(1048576), 16);
  private StegCipher imgCoder;
  private AesCipher aesCoder;
  private CryptoMode mode;
  private boolean shuffleResult = false;
  private int w;
  private int h;
  private byte[] buffer;
  private volatile TestUpdateProgress progUpdater;
  boolean wellDone = false;
  public ArrayList<String> writtenPathsList = new ArrayList();
  private String tempMSG = "";
  private List<Closeable> closeables = new Vector();
  public File currentOutFile;
  
  public ScreeptManager()
    throws UnsupportedEncodingException, NoSuchPaddingException, NoSuchAlgorithmException
  {
    this.mode = CryptoMode.COMPLETE;
    this.aesCoder = new AesCipher("".toCharArray());
    this.imgCoder = null;
  }
  
  public ScreeptManager(StegCipher imgCod, AesCipher aesCod)
  {
    this.mode = CryptoMode.COMPLETE;
    this.aesCoder = aesCod;
    this.imgCoder = imgCod;
  }
  
  public ScreeptManager(AesCipher aesCod)
  {
    this.mode = CryptoMode.ONLY_AES;
    this.aesCoder = aesCod;
    this.imgCoder = null;
  }
  
  public ScreeptManager(StegCipher imgCod)
  {
    this.mode = CryptoMode.ONLY_STEG;
    this.aesCoder = null;
    this.imgCoder = imgCod;
  }
  
  public Boolean crypt(File srcFile, TestUpdateProgress progressBarGeneral)
  {
    this.buffer = new byte[AES_BUFFER_SIZE];
    
    this.progUpdater = progressBarGeneral;
    
    this.wellDone = true;
    
    this.tempMSG = (this.tempMSG + "Start encripting " + srcFile.getAbsolutePath() + " ...\n[Selected mode: " + this.mode + " ]\n");
    this.tempMSG = (this.tempMSG + "[ Out folder: " + this.OUT_PATH + " ]\n");
    
    logger.debug("Started Crypt with selected mode: " + this.mode);
    if (this.OUT_PATH.equals(this.TEMP_PATH)) {
      emptyTempPath();
    }
    try
    {
      if ((this.mode.AES) && (!this.mode.STEG))
      {
        logger.debug("AES mode detected!");
        
        onlyAESCrypt(srcFile);
        
        logger.debug("AES Crypt phase - EXITING");
      }

      // This is a commente with wrang text, current dictionary is English ;)
      
      else if ((this.mode.STEG) && (!this.mode.AES))
      {
        this.RESULT_PATH = (this.OUT_PATH + srcFile.getName() + "/");
        File outFolder = FileStuff.renameJavaObjFile(new File(this.RESULT_PATH));
        outFolder.mkdir();
        this.RESULT_PATH = (outFolder.getCanonicalPath() + "/");
        
        this.currentOutFile = outFolder;
        
        this.w = this.imgCoder.getKeyImage().getWidth();
        this.h = this.imgCoder.getKeyImage().getHeight();
        
        this.tempMSG = (this.tempMSG + "\n => Using folder [ " + outFolder.getAbsolutePath() + " ] to contain crypted Images!\n");
        logger.debug("STEG mode detected!");
        
        onlySTEGCrypt(srcFile);
        
        logger.debug("STEG Crypt phase - EXITING");
      }
      else if ((this.mode.STEG) && (this.mode.AES))
      {
        this.RESULT_PATH = (this.OUT_PATH + srcFile.getName() + "/");
        File outFolder = FileStuff.renameJavaObjFile(new File(this.RESULT_PATH));
        outFolder.mkdir();
        this.RESULT_PATH = (outFolder.getCanonicalPath() + "/");
        
        this.currentOutFile = outFolder;
        
        this.w = this.imgCoder.getKeyImage().getWidth();
        this.h = this.imgCoder.getKeyImage().getHeight();
        
        this.tempMSG = (this.tempMSG + "\n => Using folder [ " + outFolder.getAbsolutePath() + " ] to contain crypted Images!\n");
        logger.debug("COMPLETE mode detected!");
        
        completeCrypt(srcFile);
        
        logger.debug("COMPLETE Crypt phase - EXITING");
      }
    }
    catch (STEGException e)
    {
      resetAfterException(e);
    }
    catch (InvalidKeyException e)
    {
      resetAfterException(e);
    }
    catch (NoSuchPaddingException e)
    {
      resetAfterException(e);
    }
    catch (NoSuchAlgorithmException e)
    {
      resetAfterException(e);
    }
    catch (InvalidAlgorithmParameterException e)
    {
      resetAfterException(e);
    }
    catch (BadPaddingException e)
    {
      resetAfterException(e);
    }
    catch (IllegalBlockSizeException e)
    {
      resetAfterException(e);
    }
    catch (IOException e)
    {
      resetAfterException(e);
    }
    catch (AESException e)
    {
      resetAfterException(e);
    }
    catch (Exception e)
    {
      resetAfterException(e);
    }
    logger.debug("Streams closed, exiting encription.");
    this.tempMSG += "\n => Streams closed, exiting encription.";
    
    return Boolean.valueOf(this.wellDone);
  }
  
  public Boolean deCrypt(File srcFile, TestUpdateProgress progressBarGeneral)
  {
    this.progUpdater = progressBarGeneral;
    
    this.buffer = new byte[AES_BUFFER_SIZE + 32];
    
    this.wellDone = true;
    
    this.tempMSG = (this.tempMSG + "Start decripting " + srcFile.getAbsolutePath() + " ...\nSelected mode: [ " + this.mode + " ]\n\n");
    this.tempMSG = (this.tempMSG + "[ Out folder: " + this.OUT_PATH + " ]\n");
    logger.debug("Started Decrypting with selected mode: " + this.mode);
    if (this.OUT_PATH.equals(this.TEMP_PATH)) {
      emptyTempPath();
    }
    try
    {
      if ((this.mode.AES) && (!this.mode.STEG))
      {
        logger.debug("AES mode detected!");
        
        onlyAESDeCrypt(srcFile);
        
        logger.debug("AES Decrypt phase - EXITING");
      }
      else if ((this.mode.STEG) && (!this.mode.AES))
      {
        logger.debug("STEG mode detected!");
        
        this.w = this.imgCoder.getKeyImage().getWidth();
        this.h = this.imgCoder.getKeyImage().getHeight();
        
        logger.debug("STEG mode detected!");
        
        onlySTEGDeCrypt(srcFile);
        
        logger.debug("STEG Decrypt phase - EXITING");
      }
      else if ((this.mode.STEG) && (this.mode.AES))
      {
        this.w = this.imgCoder.getKeyImage().getWidth();
        this.h = this.imgCoder.getKeyImage().getHeight();
        
        logger.debug("COMPLETE mode detected!");
        
        completeDeCrypt(srcFile);
        
        logger.debug("COMPLETE Decrypt phase - EXITING");
      }
    }
    catch (Exception e)
    {
      resetAfterException(e);
    }
    logger.debug("Streams closed, exiting decription.");
    this.tempMSG += "\n => Streams closed, exiting decription.";
    
    return Boolean.valueOf(this.wellDone);
  }
  
  private void resetAfterException(Exception e)
  {
    String msg = "\n => OOOPS! An error occurred :(";
    this.progUpdater.getPb().setValue(100);
    this.progUpdater.forceClose();
    logger.error(msg, e);
    this.tempMSG += msg;
    this.wellDone = false;
    killOpenStreams();
    FileUtils.deleteQuietly(this.currentOutFile);
  }
  
  private void emptyTempPath()
  {
    File temp_folder = new File(this.OUT_PATH);
    String msg = "";
    if ((temp_folder.exists()) && (temp_folder.listFiles().length != 0))
    {
      boolean deleted = FileStuff.deleteDirContentRecursive(temp_folder);
      if (deleted)
      {
        msg = "\n => Temp dir (" + this.OUT_PATH + ") succesfully emptied :)";
        logger.debug(msg);
        this.tempMSG += msg;
      }
      else
      {
        msg = "\n => Some error occurred deleting " + this.OUT_PATH + " content :(";
        logger.error(msg);
        this.tempMSG += msg;
      }
    }
    else
    {
      msg = "\n => Temp dir already empty or not existing";
      logger.warn(msg);
      this.tempMSG += msg;
    }
  }
  
  private void onlyAESCrypt(File srcFile)
    throws IOException, AESException, InvalidKeyException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, BadPaddingException, IllegalBlockSizeException
  {
    if (this.progUpdater == null) {
      this.progUpdater = new TestUpdateProgress();
    }
    this.progUpdater.getPb().setValue(0);
    this.progUpdater.getLabel().setText("Encripting " + srcFile.getName());
    
    FileInputStream streamIN = new FileInputStream(srcFile);
    
    long progN = 0L;
    long available = getAvailable(srcFile, progN);
    if (AES_BUFFER_SIZE >= available) {
      this.buffer = new byte[(int)available];
    }
    int readFlag = streamIN.read(this.buffer);
    
    CRYPT_NAME = srcFile.getName();
    File result = FileStuff.renameJavaObjFile(new File(this.OUT_PATH + CRYPT_NAME));
    
    this.currentOutFile = result;
    
    result.createNewFile();
    
    FileOutputStream outWriter = new FileOutputStream(result, true);
    
    this.closeables.add(streamIN);
    this.closeables.add(outWriter);
    if ((this.aesCoder == null) || (this.aesCoder.getPassword().equals("")))
    {
      streamIN.close();
      outWriter.close();
      logger.error("Error retrieving the AES coder -> null or void passsword!");
      throw new AESException();
    }
    int i = 1;
    while (readFlag > 0)
    {
      byte[] partialResult = this.aesCoder.encrypt(this.buffer);
      outWriter.write(partialResult);
      outWriter.flush();
      
      progN += this.buffer.length;
      available = getAvailable(srcFile, progN);
      if (AES_BUFFER_SIZE >= available) {
        this.buffer = new byte[(int)available];
      }
      readFlag = streamIN.read(this.buffer);
      
      this.progUpdater.getPb().setValue((int)(this.buffer.length / (float)srcFile.length() * i * 100.0F));
      
      i++;
    }
    streamIN.close();
    outWriter.close();
    this.progUpdater.getPb().setValue(100);
    this.progUpdater.getLabel().setText("Encryption completed! ");
    
    String msg = "AES crypt phase ended: Output file => " + result.getName() + "\n";
    this.tempMSG += msg;
    logger.info(msg);
  }
  
  private void onlyAESDeCrypt(File srcFile)
    throws IOException, AESException, InvalidKeyException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, BadPaddingException, IllegalBlockSizeException
  {
    if (this.progUpdater == null) {
      this.progUpdater = new TestUpdateProgress();
    }
    this.progUpdater.getPb().setValue(0);
    this.progUpdater.getLabel().setText("Decripting " + srcFile.getName());
    
    FileInputStream streamIN = new FileInputStream(srcFile);
    
    long progN = 0L;
    long available = getAvailable(srcFile, progN);
    if (AES_BUFFER_SIZE >= available) {
      this.buffer = new byte[(int)available];
    }
    int readFlag = streamIN.read(this.buffer);
    
    DECRYPT_NAME = srcFile.getName();
    File result = FileStuff.renameJavaObjFile(new File(this.OUT_PATH + DECRYPT_NAME));
    
    this.currentOutFile = result;
    
    result.createNewFile();
    
    FileOutputStream outWriter = new FileOutputStream(result.getAbsolutePath(), true);
    
    this.closeables.add(streamIN);
    this.closeables.add(outWriter);
    if ((this.aesCoder == null) || (this.aesCoder.getPassword().equals("")))
    {
      streamIN.close();
      outWriter.close();
      logger.error("Error retrieving the AES coder -> null or void passsword!");
      throw new AESException();
    }
    int i = 1;
    while (readFlag > 0)
    {
      byte[] partialResult = this.aesCoder.decrypt(this.buffer);
      outWriter.write(partialResult);
      
      progN += this.buffer.length;
      available = getAvailable(srcFile, progN);
      if (AES_BUFFER_SIZE >= available) {
        this.buffer = new byte[(int)available];
      }
      readFlag = streamIN.read(this.buffer);
      
      this.progUpdater.getPb().setValue((int)(this.buffer.length / (float)srcFile.length() * i * 100.0F));
      i++;
    }
    streamIN.close();
    outWriter.close();
    this.progUpdater.getPb().setValue(100);
    this.progUpdater.getLabel().setText("Decryption completed! ");
    
    String msg = "AES decrypt phase ended: Output file => " + result.getName() + "\n";
    this.tempMSG += msg;
    logger.info(msg);
  }
  
  private void onlySTEGCrypt(File srcFile)
    throws IOException, STEGException
  {
    int[] keyIndexes = ImageGeneralWorker.readNSpacedValuesFromImage(this.imgCoder.keyImage, 256);
    if (this.progUpdater == null) {
      this.progUpdater = new TestUpdateProgress();
    }
    this.progUpdater.getPb().setValue(0);
    this.progUpdater.getLabel().setText("Encripting " + srcFile.getName());
    
    int mult = this.imgCoder.getBytePerPix();
    
    FileInputStream streamIN = new FileInputStream(srcFile);
    int STEG_BUFFER_SIZE = this.w * this.h * mult;
    
    this.closeables.add(streamIN);
    
    long progN = 0L;
    long available = getAvailable(srcFile, progN);
    if (STEG_BUFFER_SIZE >= available) {
      this.buffer = new byte[(int)available];
    } else {
      this.buffer = new byte[STEG_BUFFER_SIZE];
    }
    int readFlag = streamIN.read(this.buffer);
    if ((this.imgCoder == null) || (this.imgCoder.getKeyImage() == null))
    {
      streamIN.close();
      logger.error("Error retrieving the STEG coder -> null coder or keyImage!");
      throw new STEGException();
    }
    int n = 1;
    while (readFlag > 0)
    {
      if (this.shuffleResult) {
        this.buffer = EntophyzerUtils.orderArrayByKey(this.buffer, keyIndexes);
      }
      StegOmResult result = this.imgCoder.stegaCrypt4ByteXor(this.buffer);
      BufferedImage imgResult = result.getImgRes();
      
      File outFile = new File(this.RESULT_PATH + n + "_" + result.getnBytesCoded() + "_.png");
      
      boolean OK = ImageIO.write(imgResult, "png", outFile);
      
      progN += this.buffer.length;
      available = getAvailable(srcFile, progN);
      if (STEG_BUFFER_SIZE >= available) {
        this.buffer = new byte[(int)available];
      }
      readFlag = streamIN.read(this.buffer);
      if (OK)
      {
        String msg = "\n=> STEG image number " + n + " written: Output file => " + outFile.getAbsolutePath();
        this.tempMSG += msg;
        logger.info(msg);
      }
      else
      {
        streamIN.close();
        String msg = "\n=> Error ocurred writing " + outFile.getAbsolutePath() + " ... exiting";
        this.tempMSG += msg;
        logger.error(msg);
        
        throw new IOException();
      }
      n++;
      
      this.progUpdater.getPb().setValue((int)(this.buffer.length / (float)srcFile.length() * n * 100.0F));
    }
    streamIN.close();
    this.progUpdater.getPb().setValue(100);
    this.progUpdater.getLabel().setText("Encryption completed! ");
  }
  
  private void onlySTEGDeCrypt(File srcFile)
    throws IOException, STEGException
  {
    int[] keyIndexes = ImageGeneralWorker.readNSpacedValuesFromImage(this.imgCoder.keyImage, 256);
    if (this.progUpdater == null) {
      this.progUpdater = new TestUpdateProgress();
    }
    this.progUpdater.getPb().setValue(0);
    this.progUpdater.getLabel().setText("Decripting " + srcFile.getName());
    
    DECRYPT_NAME = srcFile.getName();
    
    File outFile = new File(this.OUT_PATH + DECRYPT_NAME);
    outFile = FileStuff.renameJavaObjFile(outFile);
    
    this.currentOutFile = outFile;
    
    FileOutputStream fout = new FileOutputStream(outFile, true);
    
    this.closeables.add(fout);
    if ((this.imgCoder == null) || (this.imgCoder.getKeyImage() == null))
    {
      fout.close();
      logger.error("Error retrieving the STEG decoder -> null coder or keyImage!");
      throw new STEGException();
    }
    File[] cryptedImgList = srcFile.listFiles(new FilenameFilter()
    {
      public boolean accept(File dir, String name)
      {
        return name.toLowerCase().endsWith(".png");
      }
    });
    Arrays.sort(cryptedImgList, new CustomFileComparator("_", 0));
    
    int i = 1;
    for (File iFile : cryptedImgList)
    {
      int bytesToGet = Integer.valueOf(iFile.getName().split("_")[1]).intValue();
      
      BufferedImage codedImg = ImageIO.read(iFile);
      
      byte[] buffer = this.imgCoder.stegaDeCrypt4ByteXor(codedImg, Integer.valueOf(bytesToGet));
      if (this.shuffleResult) {
        buffer = EntophyzerUtils.reorderBackArrayByKey(buffer, keyIndexes);
      }
      fout.write(buffer);
      
      this.progUpdater.getPb().setValue((int)(i / cryptedImgList.length * 100.0F));
      String msg = "\nSTEG decripting current coded image: " + iFile.getName();
      this.tempMSG += msg;
      logger.info(msg);
      
      i++;
    }
    fout.close();
    
    this.progUpdater.getPb().setValue(100);
    this.progUpdater.getLabel().setText("Decryption completed! ");
    
    String msg = "STEG decrypt phase ended: Output file => " + outFile.getAbsolutePath() + "\n";
    this.tempMSG += msg;
    logger.info(msg);
  }
  
  private void completeCrypt(File srcFile)
    throws IOException, AESException, InvalidKeyException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, BadPaddingException, IllegalBlockSizeException, STEGException
  {
    int[] keyIndexes = ImageGeneralWorker.readNSpacedValuesFromImage(this.imgCoder.keyImage, 256);
    if (this.progUpdater == null) {
      this.progUpdater = new TestUpdateProgress();
    }
    this.progUpdater.getPb().setValue(0);
    this.progUpdater.getLabel().setText("Encripting " + srcFile.getName());
    
    int mult = this.imgCoder.getBytePerPix();
    
    FileInputStream streamIN = new FileInputStream(srcFile);
    
    int STEG_BUFFER_SIZE = this.w * this.h * mult - 32;
    
    this.closeables.add(streamIN);
    
    long srcSize = srcFile.length();
    if (srcSize > 2147483647L) {
      srcSize = 2147483647L;
    }
    int available = (int)srcSize;
    if (STEG_BUFFER_SIZE >= available) {
      this.buffer = new byte[available];
    } else {
      this.buffer = new byte[STEG_BUFFER_SIZE];
    }
    int readFlag = streamIN.read(this.buffer);
    if ((this.aesCoder == null) || (this.aesCoder.getPassword().equals("")))
    {
      streamIN.close();
      logger.error("Error retrieving the AES coder -> null coder or void passsword!");
      throw new AESException();
    }
    if ((this.imgCoder == null) || (this.imgCoder.getKeyImage() == null))
    {
      streamIN.close();
      logger.error("Error retrieving the STEG coder -> null coder or keyImage!");
      throw new STEGException();
    }
    int n = 1;
    int progN = 0;
    while (readFlag > 0)
    {
      this.progUpdater.getLabel().setText("Crypting " + srcFile.getName());
      
      byte[] partialResult = this.aesCoder.encrypt(this.buffer);
      if (this.shuffleResult) {
        partialResult = EntophyzerUtils.orderArrayByKey(partialResult, keyIndexes);
      }
      StegOmResult result = this.imgCoder.stegaCrypt4ByteXor(partialResult);
      BufferedImage imgResult = result.getImgRes();
      
      File outFile = new File(this.RESULT_PATH + n + "_" + result.getnBytesCoded() + "_.png");
      
      boolean OK = ImageIO.write(imgResult, "png", outFile);
      
      progN += this.buffer.length;
      srcSize = srcFile.length();
      if (srcSize > 2147483647L) {
        srcSize = 2147483647L;
      }
      available = (int)srcSize - progN;
      if (STEG_BUFFER_SIZE >= available) {
        this.buffer = new byte[available];
      }
      readFlag = streamIN.read(this.buffer);
      if (OK)
      {
        String msg = "\n=> COMPLETE image number " + n + " written: Output file => " + outFile.getAbsolutePath();
        this.tempMSG += msg;
        logger.info(msg);
      }
      else
      {
        streamIN.close();
        String msg = "\n=> Error ocurred writing " + outFile.getAbsolutePath() + " ... exiting";
        this.tempMSG += msg;
        logger.error(msg);
        
        throw new IOException();
      }
      this.progUpdater.getPb().setValue((int)(this.buffer.length / (float)srcFile.length() * n * 100.0F));
      String msg = "\n COMPLETE crypt curent file ended: Output file => " + outFile.getAbsolutePath();
      this.tempMSG += msg;
      logger.info(msg);
      
      n++;
    }
    streamIN.close();
    this.progUpdater.getPb().setValue(100);
    this.progUpdater.getLabel().setText("Encryption completed! ");
  }
  
  private void completeDeCrypt(File srcFile)
    throws IOException, AESException, InvalidKeyException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, BadPaddingException, IllegalBlockSizeException, STEGException
  {
    int[] keyIndexes = ImageGeneralWorker.readNSpacedValuesFromImage(this.imgCoder.keyImage, 256);
    if (this.progUpdater == null) {
      this.progUpdater = new TestUpdateProgress();
    }
    this.progUpdater.getPb().setValue(0);
    this.progUpdater.getLabel().setText("Decripting " + srcFile.getName());
    
    DECRYPT_NAME = srcFile.getName();
    
    File outFile = new File(this.OUT_PATH + DECRYPT_NAME);
    outFile = FileStuff.renameJavaObjFile(outFile);
    
    this.currentOutFile = outFile;
    
    FileOutputStream fout = new FileOutputStream(outFile, true);
    
    this.closeables.add(fout);
    if ((this.imgCoder == null) || (this.imgCoder.getKeyImage() == null))
    {
      fout.close();
      logger.error("Error retrieving the STEG decoder -> null coder or keyImage!");
      throw new STEGException();
    }
    File[] cryptedImgList = srcFile.listFiles(new FilenameFilter()
    {
      public boolean accept(File dir, String name)
      {
        return name.toLowerCase().endsWith(".png");
      }
    });
    Arrays.sort(cryptedImgList, new CustomFileComparator("_", 0));
    
    int i = 1;
    for (File iFile : cryptedImgList)
    {
      int bytesToGet = Integer.valueOf(iFile.getName().split("_")[1]).intValue();
      
      BufferedImage codedImg = ImageIO.read(iFile);
      
      byte[] partial_res = this.imgCoder.stegaDeCrypt4ByteXor(codedImg, Integer.valueOf(bytesToGet));
      if (this.shuffleResult) {
        partial_res = EntophyzerUtils.reorderBackArrayByKey(partial_res, keyIndexes);
      }
      fout.write(this.aesCoder.decrypt(partial_res));
      
      this.progUpdater.getPb().setValue((int)(i / cryptedImgList.length * 100.0F));
      String msg = "\nCOMPLETE decripting current coded image: " + iFile.getName();
      this.tempMSG += msg;
      logger.info(msg);
      
      i++;
    }
    fout.close();
    
    String msg = "COMPLETE decrypt phase ended: Output file => " + outFile.getAbsolutePath() + "\n";
    this.tempMSG += msg;
    logger.info(msg);
  }
  
  private long getAvailable(File source, long progN)
  {
    long srcSize = source.length();
    if (srcSize > 2147483647L) {
      srcSize = 2147483647L;
    }
    return srcSize - progN;
  }
  
  public static int grantAreaMultipleOfN(Integer W, Integer H, int N)
  {
    if ((W.intValue() == 0) || (H.intValue() == 0)) {
      return 0;
    }
    if ((W.intValue() < N) || (W.intValue() * H.intValue() < N)) {
      return 0;
    }
    W = Integer.valueOf(W.intValue() / N * N);
    
    return W.intValue();
  }
  
  public static int grantMultipleOfN(Integer number, int N)
  {
    if (number.intValue() < N) {
      return 0;
    }
    number = Integer.valueOf(number.intValue() / N * N);
    return number.intValue();
  }
  
  public static BufferedImage subImgMultOfNpx(BufferedImage key, int N)
  {
    int newWidth = grantAreaMultipleOfN(Integer.valueOf(key.getWidth()), Integer.valueOf(key.getHeight()), N);
    BufferedImage multNIMG = ImageGeneralWorker.getPortion(key, newWidth, key.getHeight());
    return multNIMG;
  }
  
  public void disableProgressUpdater()
  {
    if (this.progUpdater != null)
    {
      this.progUpdater.getPb().setValue(100);
      this.progUpdater.forceClose();
    }
  }
  
  public void killOpenStreams()
  {
    for (Closeable closeable : this.closeables) {
      if (closeable != null) {
        try
        {
          closeable.close();
        }
        catch (Exception e)
        {
          logger.error("An error occurred closing opened streams", e);
        }
      }
    }
  }
  
  public void setProgressBarVisible(AtomicBoolean enabled)
  {
    if ((this.progUpdater != null) && (this.progUpdater.progBarEnabled != null)) {
      this.progUpdater.progBarEnabled.set(enabled.get());
    }
  }
  
  public StegCipher getImgCoder()
  {
    return this.imgCoder;
  }
  
  public void setImgCoder(StegCipher imgCoder)
  {
    this.imgCoder = imgCoder;
  }
  
  public AesCipher getAesCoder()
  {
    return this.aesCoder;
  }
  
  public void setAesCoder(AesCipher aesCoder)
  {
    this.aesCoder = aesCoder;
  }
  
  public CryptoMode getMode()
  {
    return this.mode;
  }
  
  public void setMode(CryptoMode mode)
  {
    this.mode = mode;
  }
  
  public String getOutPath()
  {
    return this.OUT_PATH;
  }
  
  public void setOutPath(String outPath)
  {
    this.OUT_PATH = (outPath + "/");
  }
  
  public static int getAES_BUFFER_SIZE()
  {
    return AES_BUFFER_SIZE;
  }
  
  public static String getCryptName()
  {
    return CRYPT_NAME;
  }
  
  public String getOutputStats()
  {
    return this.tempMSG;
  }
  
  public boolean isShuffleResult()
  {
    return this.shuffleResult;
  }
  
  public void setShuffleResult(boolean shuffleResult)
  {
    this.shuffleResult = shuffleResult;
    logger.info("Setting ShuffleMode Option (AKA ultra secure mode: if true values will be sorted before crypt and after decrypt. !ONLY WITH STEG/COMPLETE MODES!) \n => ShuffleMode is " + this.shuffleResult);
  }
}