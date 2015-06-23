package ch.ivyteam.db.meta.generator.internal.persistency;

import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;

import ch.ivyteam.db.meta.model.internal.SqlMeta;
import freemarker.cache.ClassTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.Template;
import freemarker.template.TemplateException;

/**
 * A Meta data generator that creates persistency service implementation classes for entity java classes
 * @author rwei
 * @since 16.10.2009
 */
abstract class AbstractJavaClassPersistencyServiceImplementationTemplateWriter
{                                                                                                                  
  protected Configuration cfg;
  protected String targetPackage;
  protected SqlMeta meta;

  /**
   * Constructor
   * @param meta
   * @param targetPackage
   */
  public AbstractJavaClassPersistencyServiceImplementationTemplateWriter(SqlMeta meta,
          String targetPackage)
  {
    this.targetPackage = targetPackage;
    this.meta = meta;
  }

  /**
   * Writes the java class persistency service implementation class for a table
   * @param javaSourceFile
   * @throws Exception
   */
  public void writeToFile(File javaSourceFile) throws Exception
  {
    Template temp = getConfiguration().getTemplate(getTemplateName());
    Writer writer = null;
    try
    {
      writer = new FileWriter(javaSourceFile);
      temp.process(getDataMap(), writer);
      writer.flush();
    }
    catch (TemplateException ex)
    {
      throw new IllegalStateException("Could not generate Query class: " + javaSourceFile.getAbsolutePath(), ex);
    }
    finally
    {
      IOUtils.closeQuietly(writer);
    }
  }
  
  /**
   * @return template name to use
   */
  protected abstract String getTemplateName();

  protected Map<String, Object> getDataMap()
  {
    Map<String, Object> root = new HashMap<String, Object>();

    // add libraries
//    root.put("StringUtils", createStringUtilsTemplateModel());
    
    // add data variables
    root.put("packageName", targetPackage);        
    return root;
  }

  private Configuration getConfiguration()
  {
    if (cfg == null)
    {
      cfg = new Configuration();
      //File template = new File("template");
      cfg.setTemplateLoader(new ClassTemplateLoader(getClass(), ""));
      //cfg.setDirectoryForTemplateLoading(template);
      cfg.setObjectWrapper(new DefaultObjectWrapper());
    }
    return cfg;
  }

}
