package ch.ivyteam.maven.updater;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Collections;

import org.apache.commons.io.IOUtils;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.codehaus.plexus.util.StringUtils;
import org.junit.Test;

import ch.ivyteam.maven.BundleManifestFileUpdater;

public class TestRemoveRequiredBundleVersion
{

  @Test
  public void testCanHandleRequiredBundleRanges() throws IOException
  {
    File srcPluginDir = Files.createTempDirectory("SourcePlugin").toFile();
    File manifest = new File(srcPluginDir, BundleManifestFileUpdater.MANIFEST_MF);
    createFile(manifest, streamOf("MANIFEST.MF_source"));
    
    new BundleManifestFileUpdater(srcPluginDir, "6.1.0.XXX", new SystemStreamLog(), 
            Collections.emptyList(), false).update();
    
    String updatedManifest = IOUtils.toString(new FileInputStream(manifest));
    String expectedManifest = IOUtils.toString(streamOf("MANIFEST.MF_expected"));
    assertEquals(expectedManifest, updatedManifest);
  }
  
  @Test
  public void testCanRemoveBundleRanges() throws IOException
  {
    File srcPluginDir = Files.createTempDirectory("SourcePlugin").toFile();
    File manifest = new File(srcPluginDir, BundleManifestFileUpdater.MANIFEST_MF);
    createFile(manifest, streamOf("MANIFEST.MF_source"));
    
    new BundleManifestFileUpdater(srcPluginDir, "6.1.0.XXX", new SystemStreamLog(), 
            Collections.emptyList(), true).update();
    
    String updatedManifest = IOUtils.toString(new FileInputStream(manifest));
    String expectedManifest = IOUtils.toString(streamOf("MANIFEST.MF_expectedNoRanges"));
    assertEquals(StringUtils.deleteWhitespace(expectedManifest), StringUtils.deleteWhitespace(updatedManifest));
  }
  
  private static void createFile(File file, InputStream content) throws IOException
  {
    file.getParentFile().mkdirs();
    file.createNewFile();
    IOUtils.copy(content, new FileOutputStream(file));
  }

  private static InputStream streamOf(String resouce)
  {
    return TestRemoveRequiredBundleVersion.class.getResourceAsStream(resouce);
  }
  
}
