package ch.ivyteam.ivy.maven;

import static org.fest.assertions.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.jar.Manifest;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

public class TestManifest
{
  @Test
  public void test() throws IOException
  {
    updateAndCompareString("MANIFEST_CASEMAP.MF");
    //updateAndCompareString("MANIFEST_PAGE.MF");
  }
  
  private void updateAndCompareString(String file) throws IOException
  {
    String original = IOUtils.toString(getClass().getResourceAsStream(file));
    MetaInfMojo mojo = new MetaInfMojo();
    mojo.requiredLine = "Eclipse-ExtensibleAPI:true";
    mojo.classpathExtension = "patch.jar";
    String updatedContent = mojo.updateManifest(original);
    
    System.err.println(updatedContent);
    assertThat(updatedContent).isEqualTo(original);
  }

  private void updateAndCompare(String file) throws IOException
  {
    InputStream is = getClass().getResourceAsStream(file);
    MetaInfMojo mojo = new MetaInfMojo();
    mojo.requiredLine = "Eclipse-ExtensibleAPI:true";
    mojo.classpathExtension = "patch.jar";
    Manifest mf = mojo.updateManifest(is);
    
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    mf.write(bos);
    String updatedContent = bos.toString();
    System.err.println(updatedContent);
    String original = IOUtils.toString(getClass().getResourceAsStream(file));
    assertThat(updatedContent).isEqualTo(original);
  }

}
