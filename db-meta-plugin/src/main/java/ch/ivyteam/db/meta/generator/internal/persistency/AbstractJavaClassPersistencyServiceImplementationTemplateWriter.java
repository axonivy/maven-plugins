package ch.ivyteam.db.meta.generator.internal.persistency;

import java.io.File;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Map;

import ch.ivyteam.db.meta.model.internal.SqlMeta;
import freemarker.cache.ClassTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.Template;
import freemarker.template.TemplateException;

abstract class AbstractJavaClassPersistencyServiceImplementationTemplateWriter {
  protected Configuration cfg;
  protected String targetPackage;
  protected SqlMeta meta;

  public AbstractJavaClassPersistencyServiceImplementationTemplateWriter(SqlMeta meta,
          String targetPackage) {
    this.targetPackage = targetPackage;
    this.meta = meta;
  }

  public void writeToFile(File javaSourceFile) throws Exception {
    Template temp = getConfiguration().getTemplate(getTemplateName());
    try (var writer = new FileWriter(javaSourceFile)) {
      temp.process(getDataMap(), writer);
      writer.flush();
    } catch (TemplateException ex) {
      throw new IllegalStateException("Could not generate Query class: " + javaSourceFile.getAbsolutePath(), ex);
    }
  }

  protected abstract String getTemplateName();

  protected Map<String, Object> getDataMap() {
    var root = new HashMap<String, Object>();
    root.put("packageName", targetPackage);
    return root;
  }

  private Configuration getConfiguration() {
    if (cfg == null) {
      cfg = new Configuration();
      cfg.setTemplateLoader(new ClassTemplateLoader(getClass(), ""));
      cfg.setObjectWrapper(new DefaultObjectWrapper());
    }
    return cfg;
  }
}
