package ch.ivyteam.ivy.maven;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TestImageComparer
{
  
  private LogCollector log;
  private File refConditionTab;
  private File newConditionTab;
  private Path refRoot;
  private Path newRoot;

  @Before
  public void setUp() throws IOException
  {
    log = new LogCollector();
    refRoot = Files.createTempDirectory("refRoot");
    newRoot = Files.createTempDirectory("newRoot");
    
    refConditionTab = load(refRoot, "refImg/1-Condition Tab.png");
    newConditionTab = load(newRoot, "newImg/1-Condition Tab.png");
  }
  
  @After
  public void tearDown() throws IOException
  {
    FileUtils.deleteDirectory(refRoot.toFile());
    FileUtils.deleteDirectory(newRoot.toFile());
  }

  @Test
  public void compareIdenticalImages()
  {
    new ImageComparer(refRoot.toFile(), refRoot.toFile(), asList(refConditionTab), 99.99f, log).compare();
    assertThat(log.getWarnings()).isEmpty();
    assertThat(log.getDebug().iterator().next().toString()).startsWith("comparing").contains("1-Condition Tab.png");
  }
  
  @Test
  public void compareDifferentImages()
  {
    compare(newConditionTab);
    assertThat(log.getWarnings().iterator().next().toString()).contains("Image only has similarity of");
  }
  
  @Test
  public void compareDifferentSizedImages() throws IOException
  {
    load(refRoot, "refImg/0-Name Tab.png");
    File newNameTab = load(newRoot, "newImg/0-Name Tab.png");
    compare(newNameTab);
    assertThat(log.getWarnings().iterator().next().toString()).contains("Different sized image:");
  }
  
  @Test
  public void compareNonExistentImages()
  {
    File nonExistentImage = new File(newConditionTab.getParent(), "NonExistentImage.png");
    compare(nonExistentImage);
    assertThat(log.getWarnings().iterator().next().toString()).contains("Could not read image:");
  }

  private void compare(File nonExistentImage)
  {
    new ImageComparer(refRoot.toFile(), newRoot.toFile(), asList(nonExistentImage), 99.99f, log).compare();
  }
  
  private File load(Path rootDir, String testRes) throws IOException
  {
    try(InputStream is = TestImageComparer.class.getResourceAsStream("../../../../"+testRes))
    {
      File elementDir = new File(rootDir.toFile(), "MyElement");
      elementDir.mkdir();
      File img = new File(elementDir, new File(testRes).getName());
      Files.copy(is, img.toPath(), StandardCopyOption.REPLACE_EXISTING);
      return img;
    }
  }
  
}
