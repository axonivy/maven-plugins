package ch.ivyteam.ivy.maven;

import static java.util.Arrays.asList;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.junit.Test;

public class TestImageComparer
{

  @Test
  public void compare() throws IOException
  {
    File conditionTab = load("refImg/1-Condition Tab.png");
    new ImageComparer(asList(conditionTab), asList(conditionTab), null).compare();
  }

  private File load(String testRes) throws IOException
  {
    try(InputStream is = TestImageComparer.class.getResourceAsStream("../../../../"+testRes))
    {
      Path img = Files.createTempFile(new File(testRes).getName(), ".jpg");
      Files.copy(is, img, StandardCopyOption.REPLACE_EXISTING);
      return img.toFile();
    }
  }
  
}
