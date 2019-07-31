package ch.ivyteam.ivy.maven;

import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.io.File;
import java.nio.file.Path;
import java.util.List;

import javax.imageio.ImageIO;

import org.apache.maven.plugin.logging.Log;

public class ImageComparer
{
  private final List<File> newImages;
  private final Log log;
  private final File refRoot;
  private final File newRoot;

  public ImageComparer(File refRoot, File newRoot, List<File> newImages, Log log)
  {
    this.refRoot = refRoot;
    this.newRoot = newRoot;
    this.newImages = newImages;
    this.log = log;
  }

  public void compare()
  {
    newImages.stream().forEach(img -> compareImg(img, getRefImg(img)));
  }

  private File getRefImg(File img)
  {
    Path relativize = newRoot.toPath().relativize(img.toPath());
    Path ref = refRoot.toPath().resolve(relativize);
    return ref.toFile();
  }

  private void compareImg(File img, File ref)
  {
    log.debug("comparing "+img+" with "+ref);

    float percentage = 0;

    try
    {
      BufferedImage imageA = ImageIO.read(img);
      DataBuffer imageABuffer = imageA.getData().getDataBuffer();
      int sizeImageA = imageABuffer.getSize();

      BufferedImage imageB = ImageIO.read(ref);
      DataBuffer imageBBuffer = imageB.getData().getDataBuffer();
      int sizeImageB = imageBBuffer.getSize();

      int count = 0;
      if (sizeImageA == sizeImageB)
      {
        for (int i = 0; i < sizeImageA; i++)
        {
          if (imageABuffer.getElem(i) == imageBBuffer.getElem(i))
          {
              count = count + 1;
          }
        }

        percentage = (count * 100f) / sizeImageA;
      }
      else
      {
        logWarning(img, "Different sized image");
      }

      validateSimilarity(img, percentage);
    }
    catch (Exception e)
    {
      logWarning(img, "Could not read image");
    }
  }

  private void validateSimilarity(File img, float percentage)
  {
    if (percentage < 99.99f)
    {
      logWarning(img, "Images are different");
    }
  }
  
  private void logWarning(File img, String msg)
  {
    String imageName = img.getParent() + "/" + img.getName();
    log.warn(msg + ": " + imageName);
  }
  
}
