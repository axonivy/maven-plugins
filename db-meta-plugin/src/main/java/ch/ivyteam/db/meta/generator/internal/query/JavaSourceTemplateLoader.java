package ch.ivyteam.db.meta.generator.internal.query;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import freemarker.cache.TemplateLoader;

/**
 * Loads a template out of a java source file. 
 * @author rwei
 * @since Jan 16, 2012
 */
class JavaSourceTemplateLoader implements TemplateLoader
{
  private final File sourceDir;
  private final String packageName;

  public JavaSourceTemplateLoader(File sourceDir, String packageName)
  {
    this.sourceDir = sourceDir;
    this.packageName = packageName;
  }

  @Override
  public void closeTemplateSource(Object arg0) throws IOException
  {
  }

  @Override
  public Object findTemplateSource(String name) throws IOException
  {
    String sourceFileName = StringUtils.substringBefore(name, ":");
    String includeTag = StringUtils.substringAfter(name, ":");
    
    File file = new File(sourceDir, packageName.replace('.', File.separatorChar));    
    file = new File(file, sourceFileName);
    if (!file.exists())
    {
      return null;
    }
    return new ImmutablePair<File, String>(file, includeTag);
  }

  @Override
  public long getLastModified(Object arg0)
  {
    @SuppressWarnings("unchecked")
    Pair<File, String> source = (Pair<File, String>)arg0;
    return source.getLeft().lastModified();
  }

  @Override
  public Reader getReader(Object templateSource, String encoding) throws IOException
  {
    @SuppressWarnings("unchecked")
    Pair<File, String> source = (Pair<File, String>)templateSource;
    String template = FileUtils.readFileToString(source.getLeft(), encoding);
    String startTag = "// Include Start "+source.getRight();
    if (template.indexOf(startTag) < 0)
    {
      // tag not found inside source file -> return empty string so that nothing gets included
      System.err.println("Start tag '"+startTag+"' not found in template file "+source.getLeft().getAbsolutePath());
      return new StringReader(""); 
    }
    template = StringUtils.substringAfter(template, startTag);
    template = StringUtils.substringAfter(template, "\n");
    template = StringUtils.substringBefore(template, "// Include Stop");
    return new StringReader(template);
  }

}
