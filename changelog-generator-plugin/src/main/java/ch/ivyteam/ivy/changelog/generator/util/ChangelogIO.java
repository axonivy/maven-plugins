package ch.ivyteam.ivy.changelog.generator.util;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipParameters;
import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoExecutionException;

public class ChangelogIO
{
  private static final String encoding = "UTF-8";
  private File sourceFile;
  private File outputFile;
  
  public ChangelogIO(File sourceFile, File outputFile)
  {
    this.sourceFile = sourceFile;
    this.outputFile = outputFile;
  }
  
  public String getTemplateContent() throws MojoExecutionException
  {
    try
    {
      return FileUtils.readFileToString(sourceFile, encoding);
    }
    catch (IOException ex)
    {
      throw new MojoExecutionException("Failed to read template file "+ sourceFile, ex);
    }
  }
  
  public void writeResult(String outputContent) throws MojoExecutionException
  {
    try
    {
      FileUtils.write(outputFile, outputContent, encoding);
    }
    catch (IOException ex)
    {
      throw new MojoExecutionException("Failed to write generated text to " + outputFile, ex);
    }
  }
  
  public void compressMaxGzipFile(String outputContent)
  {
    String gzipFile = getGzipFile();
    GzipParameters parameters = new GzipParameters();
    parameters.setCompressionLevel(9);
    try (InputStream in = new ByteArrayInputStream(outputContent.getBytes(encoding));
            OutputStream os = new FileOutputStream(gzipFile);
            BufferedOutputStream out = new BufferedOutputStream(os);
            GzipCompressorOutputStream gzOut = new GzipCompressorOutputStream(out, parameters);)
    {
      final byte[] buffer = new byte[1024];
      int n = 0;
      while (-1 != (n = in.read(buffer))) {
          gzOut.write(buffer, 0, n);
      }
    }
    catch (IOException ex)
    {
      ex.printStackTrace();
    }
  }

  private String getGzipFile()
  {
    String gzipFile = outputFile.getAbsolutePath() + ".gz";
    File file = new File(gzipFile);
    try
    {
      new File(file.getParent()).mkdirs();
      file.createNewFile();
    }
    catch (IOException ex)
    {
      ex.printStackTrace();
    }
    return gzipFile;
  }
}
