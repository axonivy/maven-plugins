package ch.ivyteam.db.meta.generator.internal.persistency;

import java.util.Map;

import ch.ivyteam.db.meta.generator.internal.JavaClassGeneratorUtil;
import ch.ivyteam.db.meta.model.internal.SqlMeta;
import ch.ivyteam.db.meta.model.internal.SqlView;


/**
 * Generates a java class for a view
 */
public class JavaClassForViewTemplateWriter extends
        AbstractJavaClassPersistencyServiceImplementationTemplateWriter
{
  private SqlView view;

  /**
   * @param view
   * @param meta
   * @param targetPackage
   */
  public JavaClassForViewTemplateWriter(SqlView view, SqlMeta meta, String targetPackage)
  {
    super(meta, targetPackage);
    this.view = view;
  }

  @Override
  protected String getTemplateName()
  {
    return "JavaClassForView.ftl";
  }
  
  @Override
  protected Map<String, Object> getDataMap()
  {
    Map<String, Object> map = super.getDataMap();
    map.put("view",  view);
    map.put("className", "Db"+JavaClassGeneratorUtil.getEntityClassName(view));
    map.put("columns",  ViewColumnInfo.getViewColumns(view));
    return map;
  }

}
