package ch.ivyteam.db.meta.generator.internal.query;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import ch.ivyteam.db.meta.model.internal.SqlMeta;
import ch.ivyteam.db.meta.model.internal.SqlTable;
import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.FileTemplateLoader;
import freemarker.cache.MultiTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.ext.beans.BeansWrapper;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateHashModel;

/**
 * Writer for java query classes using the template
 * {@link #QUERY_CLASS_TEMPLATE}.
 * @author fs
 * @since 11.01.2012
 */
public class JavaQueryClassTemplateWriter {

  private static final String QUERY_CLASS_TEMPLATE = "QueryClass.ftl";
  private final String packageName;
  private final TableInfo tableInfo;
  private Configuration cfg;
  private File templateDir;
  private File sourceDir;
  private SqlMeta meta;

  public JavaQueryClassTemplateWriter(SqlMeta meta, SqlTable table, String packageName, File templateDir,
          File sourceDir) {
    this.templateDir = templateDir;
    this.tableInfo = TableInfo.create(table, meta);
    this.packageName = packageName;
    this.sourceDir = sourceDir;
    this.meta = meta;
  }

  public void writeToFile(File javaSourceFile) throws IOException {
    Template temp = getConfiguration().getTemplate(QUERY_CLASS_TEMPLATE);
    try (Writer writer = new FileWriter(javaSourceFile)) {
      temp.process(getDataMap(), writer);
      writer.flush();
    } catch (TemplateException ex) {
      throw new IllegalStateException("Could not generate Query class: " + javaSourceFile.getAbsolutePath(),
              ex);
    }
  }

  private Map<String, Object> getDataMap() {
    Map<String, Object> root = new HashMap<>();
    // add libraries
    root.put("StringUtils", createStringUtilsTemplateModel());
    // add data variables
    root.put("packageName", packageName);
    root.put("table", getTableInfo());
    List<ColumnInfo> columns = ColumnInfo.getColumns(meta, tableInfo);
    root.put("columns", columns);
    root.put("enumerationInfos", ColumnInfo.getEnumerationInfos(columns));
    return root;
  }

  TableInfo getTableInfo() {
    return tableInfo;
  }

  private Configuration getConfiguration() throws IOException {
    if (cfg == null) {
      cfg = new Configuration();
      // File template = new File("template");
      cfg.setTemplateLoader(new MultiTemplateLoader(new TemplateLoader[] {
          new ClassTemplateLoader(getClass(), ""),
          new FileTemplateLoader(templateDir),
          new JavaSourceTemplateLoader(sourceDir, packageName)}));
      // cfg.setDirectoryForTemplateLoading(template);
      cfg.setObjectWrapper(new DefaultObjectWrapper());
    }
    return cfg;
  }

  private TemplateHashModel createStringUtilsTemplateModel() {
    try {
      BeansWrapper wrapper = BeansWrapper.getDefaultInstance();
      TemplateHashModel staticModels = wrapper.getStaticModels();
      TemplateHashModel fileStatics = (TemplateHashModel) staticModels.get(StringUtils.class.getName());
      return fileStatics;
    } catch (Exception ex) {
      throw new IllegalStateException("Could not create template model for StringUtils", ex);
    }
  }
}
