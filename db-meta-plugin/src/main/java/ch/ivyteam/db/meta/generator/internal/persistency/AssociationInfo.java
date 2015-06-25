package ch.ivyteam.db.meta.generator.internal.persistency;

import java.util.ArrayList;
import java.util.List;

import ch.ivyteam.db.meta.generator.internal.ConstantBuilder;
import ch.ivyteam.db.meta.generator.internal.JavaClassGenerator;
import ch.ivyteam.db.meta.generator.internal.JavaClassGeneratorUtil;
import ch.ivyteam.db.meta.model.internal.MetaException;
import ch.ivyteam.db.meta.model.internal.SqlMeta;
import ch.ivyteam.db.meta.model.internal.SqlObject;
import ch.ivyteam.db.meta.model.internal.SqlTable;
import ch.ivyteam.db.meta.model.internal.SqlTableColumn;

/**
 * Info about an accociation that is used in the JavaClassPersistencyServiceImplemenation.ftl
 */
public class AssociationInfo
{

  private SqlTable associationTable;
  private SqlObject table;

  /**
   * Constructor
   * @param table 
   * @param associationTable
   */
  private AssociationInfo(SqlTable table, SqlTable associationTable)
  {
    this.table = table;
    this.associationTable = associationTable;
  }
  

  /**
   * @return associations
   */
  static List<AssociationInfo> getAssociations(SqlTable table, SqlMeta meta)
  {
    List<AssociationInfo> associations = new ArrayList<AssociationInfo>();
    
    for (SqlTable associationTable : meta.getArtifacts(SqlTable.class))
    {
      if (associationTable.getDatabaseManagementSystemHints(JavaClassGenerator.JAVA).isHintSet(JavaClassGenerator.AS_ASSOCIATION))
      {
        if (associationTable.getColumns().size() != 2)
        {
          throw new MetaException("Association table "+associationTable.getId()+"must have two columns");
        }
        if (associationTable.getColumns().get(0).getReference() == null)
        {
          throw new MetaException("No reference defined on the column "+associationTable.getColumns().get(0).getId()+" of association table "+associationTable.getId());
        }
        if (associationTable.getColumns().get(1).getReference() == null)
        {
          throw new MetaException("No reference defined on the column "+associationTable.getColumns().get(1).getId()+" of association table "+associationTable.getId());
        }
        if (associationTable.getColumns().get(0).getReference().getForeignTable().equals(table.getId())||
            associationTable.getColumns().get(1).getReference().getForeignTable().equals(table.getId()))
        {
          associations.add(new AssociationInfo(table, associationTable));          
        }
      }
    }
    return associations;
  }

  /**
   * @return name
   */
  public String getName()
  {
    return JavaClassGeneratorUtil.removeTablePrefix(associationTable.getId());
  }
  
  /**
   * @return constant
   */
  public String getConstant()
  {
    return new ConstantBuilder(getName()).toConstant();
  }
  
  /** 
   * @return table
   */
  public String getTable()
  {
    return associationTable.getId();
  }
  
  /**
   * @return foreign key
   */
  public String getForeignKey()
  {
    for (SqlTableColumn column: associationTable.getColumns())
    {
      if (column.getReference().getForeignTable().equals(table.getId()))
      {
        return column.getId();
      }
    }
    return null;
  }
  
  /**
   * @return foreign key
   */
  public String getForeignKeyConstant()
  {
    return new ConstantBuilder(getForeignKey()).toConstant();
  }

}
