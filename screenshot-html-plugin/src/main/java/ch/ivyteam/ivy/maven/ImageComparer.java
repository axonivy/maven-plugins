package ch.ivyteam.ivy.maven;

import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import javax.imageio.ImageIO;

import org.apache.maven.plugin.logging.Log;

public class ImageComparer
{
  private final List<File> newImages;
  private final Log log;
  private final File refRoot;
  private final Path newRoot;
  private float requiredSimilarity;

  public ImageComparer(File refRoot, File newRoot, List<File> newImages, float requiredSimilarity, Log log)
  {
    this.refRoot = refRoot;
    this.newRoot = newRoot.toPath();
    this.newImages = newImages;
    this.requiredSimilarity = requiredSimilarity;
    this.log = log;
  }

  public void compare()
  {
    newImages.stream().forEach(img -> compareImg(img, getRefImg(img)));
  }

  private File getRefImg(File img)
  {
    Path relativize = relativizeToRoot(img);
    Path ref = refRoot.toPath().resolve(relativize);
    return ref.toFile();
  }

  private Path relativizeToRoot(File img)
  {
    return newRoot.relativize(img.toPath());
  }

  private void compareImg(File img, File ref)
  {
    log.debug("comparing "+img+" with "+ref);

    try
    {
      DataBuffer imageABuffer = toDataBuffer(img);
      DataBuffer imageBBuffer = toDataBuffer(ref);
      
      if (imageABuffer.getSize() != imageBBuffer.getSize())
      {
        logWarning(img, "Different sized image");
        return;
      }
        
      int matchedPixels = countMatchingPixels(imageABuffer, imageBBuffer);

      float similarity = (matchedPixels * 100f) / imageABuffer.getSize();
      if (similarity < requiredSimilarity)
      {
        logWarning(img, "Image only has similarity of " + similarity);
      }
    }
    catch (IOException e)
    {
      logWarning(img, "Could not read image");
    }
  }
  
  private static DataBuffer toDataBuffer(File imgFile) throws IOException
  {
    BufferedImage bufferedImage = ImageIO.read(imgFile);
    return bufferedImage.getData().getDataBuffer();
  }

  private int countMatchingPixels(DataBuffer imageABuffer, DataBuffer imageBBuffer)
  {
    int matchedPixels = 0;
    for (int i = 0; i < imageABuffer.getSize(); i++)
    {
      if (imageABuffer.getElem(i) == imageBBuffer.getElem(i))
      {
        matchedPixels++;
      }
    }
    return matchedPixels;
  }

  private void logWarning(File img, String msg)
  {
    String imageName = relativizeToRoot(img).toString();
    log.warn(msg + ": " + imageName);
  }
  
}
