package ch.ivyteam.db.meta.generator.internal.persistency;

import java.util.Map;

import ch.ivyteam.db.meta.model.internal.SqlMeta;
import ch.ivyteam.db.meta.model.internal.SqlTable;

public class JavaClassPersistencyServiceImplementationTemplateWriter extends AbstractJavaClassPersistencyServiceImplementationTemplateWriter {

  private static final String PERSISTENCY_SERVICE_IMPLEMENTATION_CLASS_TEMPLATE = "JavaClassPersistencyServiceImplementation.ftl";
  private SqlTable table;
  private String entityPackage;

  public JavaClassPersistencyServiceImplementationTemplateWriter(SqlTable table, SqlMeta meta, String targetPackage, String entityPackage) {
    super(meta, targetPackage);
    this.table = table;
    this.entityPackage = entityPackage;
  }

  @Override
  protected Map<String, Object> getDataMap() {
    Map<String, Object> root = super.getDataMap();
    root.put("table", TableInfo.create(table, entityPackage));
    root.put("columns", ColumnInfo.getColumns(table));
    root.put("queryViewColumns", ViewColumnInfo.getViewColumns(table, meta));
    root.put("columnsWithoutPrimaryAndParent", ColumnInfo.getColumnsWithoutPrimaryAndParent(table));
    root.put("columnsWithoutPrimaryParentAndLob", ColumnInfo.getColumnsWithoutPrimaryParentAndLob(table));
    root.put("numberOfColumns", ColumnInfo.getColumns(table).size());
    root.put("associations", AssociationInfo.getAssociations(table, meta));
    root.put("optimisticLockingColumn", ColumnInfo.getOptimisticLockingColumn(table));
    return root;
  }

  @Override
  protected String getTemplateName() {
    return PERSISTENCY_SERVICE_IMPLEMENTATION_CLASS_TEMPLATE;
  }
}
