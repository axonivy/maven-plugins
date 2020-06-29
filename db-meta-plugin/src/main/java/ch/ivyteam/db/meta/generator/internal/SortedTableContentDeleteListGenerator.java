package ch.ivyteam.db.meta.generator.internal;

import java.io.PrintWriter;
import java.util.List;

import org.apache.commons.io.IOUtils;

import ch.ivyteam.db.meta.model.internal.SqlMeta;
import ch.ivyteam.db.meta.model.internal.SqlTable;

/**
 * Generates a list of tables. The tables are sorted in the way that the content of the table can be deleted in
 * this order without to break any references.
 * @author rwei
 * @since 22.10.2009
 */
public class SortedTableContentDeleteListGenerator extends AbstractSortedTableContentListGenerator
{
  @Override
  public void generateMetaOutput(SqlMeta metaDefinition) throws Exception
  {
    PrintWriter pr;
    pr = new NewLinePrintWriter(fOutputFile);
    try
    {
      List<SqlTable> tables = new TablesSortedByDeleteOrder(new DbHints(fDatabaseSystem), metaDefinition).byDeleteOrder();
      for (SqlTable table : tables)
      {
        pr.println(table.getId());
      }
    }
    finally
    {
      IOUtils.closeQuietly(pr);
    }
  }
  /**
   * @see ch.ivyteam.db.meta.generator.internal.IMetaOutputGenerator#printHelp()
   */
  @Override
  public void printHelp()
  {
    System.out.println("SortedTableContentDeleteListGenerator Options: -outputFile {outputFile}");    
  }
}
