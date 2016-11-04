package ch.ivyteam.db.meta.generator.internal;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import ch.ivyteam.db.meta.generator.Target;
import ch.ivyteam.db.meta.model.internal.MetaException;
import ch.ivyteam.db.meta.model.internal.SqlMeta;
import ch.ivyteam.db.meta.model.internal.SqlTable;
import ch.ivyteam.db.meta.model.internal.SqlTableColumn;

/**
 * Base class for all java class meta output generators
 * @author rwei
 */
public abstract class JavaClassGenerator implements IMetaOutputGenerator
{
  private static final String OPTION_TABLES = "tables";
  private static final String OPTION_PACKAGE = "package";
  private static final String OPTION_OUTPUT_DIR = "outputDir";

  /**  */
  protected final Options options = new Options()
    .addOption(Option.builder().desc("Output root directory for the generated classes.").required().hasArg().longOpt(OPTION_OUTPUT_DIR).build())
    .addOption(Option.builder().desc("Package name of the created java classes.").required().hasArg().longOpt(OPTION_PACKAGE).build())
    .addOption(Option.builder().desc("Tables, the classes has to be generated for").required().hasArgs().longOpt(OPTION_TABLES).build());

  /** The output directory */
  private File fOutputDirectory;

  /** The target package */
  private String fTargetPackage;

  /** The tables to generate entity classes for */
  private List<String> fTablesToGenerateJavaClassFor = new ArrayList<String>();

  /** Database System Name */
  public static final String JAVA = "Java";

  /** Class name databse system hint. Specifies the class name of the java entitiy class generated for the table */
  public static final String CLASS_NAME = "ClassName";
  
  /** Secondary keys database system hints. Use this to specify list of columns that build the secondary keys */
  public static final String SECONDARY_KEYS = "SecondaryKeys";

  /** Parent can be modified database system hint. Marks that the parent key can be modified */
  public static final String PARENT_CAN_BE_MODIFIED = "ParentCanBeModified";

  /** As Parent database system hint. Marks a column as the parent foreign key */
  public static final String AS_PARENT = "AsParent";

  /** Password database system hint. Marks that the column value has to be encrypted in the database  */
  public static final String PASSWORD = "Password";

  /** Truncate database system hint. Marks that values have to be truncated before writting into the databse */
  public static final String TRUNCATE = "Truncate";

  /** DataType database system hint. Marks that the given java data type should be used instead the default one */
  public static final String DATA_TYPE = "DataType";

  /** 
   * Database system hint. Indicates, that the database field is of type 'int' and in java an enumeration is used with the given name.
   * The defined enumeration must implement the interface IPersistentEnumeration */
  public static final String ENUM = "Enum";

  /** Attribute name database system hint. Specifies the attribute name of the java entity class generated for the column */
  public static final String ATTRIBUTE_NAME = "AttributeName";

  /** Additional set methods database system hint. Specifies the column name that should be set with the additional set method */
  public static final String ADDITIONAL_SET_METHODS = "AdditionalSetMethods";

  /** As Association database system hint. Specifies that Association constants should be generated in the foreign tables of this association table */
  public static final String AS_ASSOCIATION = "AsAssociation";

  /** Query table name system hint. Specifies the table name that should be used for reading entity data */
  public static final String QUERY_TABLE_NAME = "QueryTableName";
  
  /** query field name system hint. 
   * Specifies the name of a column in a generated query class. 
   * If not set the name of the column is used as field name  
   */ 
  public static final String QUERY_FIELD_NAME = "QueryFieldName";
  
  /**
   * Specifies that this field should not appear as queryable field on the generated Query (e.g. CaseQuery)
   */
  public static final String HIDE_FIELD_ON_QUERY = "HideFieldOnQuery";
  
  /**  
   * <p>Specifies the filter query data type of a column in a generated query class. 
   * If not set the data type of the column is used to evaluate the as field name.</p>
   * <p>Most often used to keep the old filter query data type when a data type of a column changes 
   * so that the query interface is still compatible with older versions.</p>  
   */ 
  public static final String FILTER_QUERY_DATA_TYPE = "FilterQueryDataType";
  
  /** Field used for optimistic locking (e.g. Versioning of a row) **/
  public static final String FIELD_FOR_OPTIMISTIC_LOCKING = "FieldForOptimisticLocking";                                                 

  /**
   * @see ch.ivyteam.db.meta.generator.internal.IMetaOutputGenerator#analyseArgs(java.lang.String[])
   */
  @Override
  public void analyseArgs(String[] args) throws Exception
  {
    CommandLine commandLine = new DefaultParser().parse(options, args);
    fOutputDirectory = new File(commandLine.getOptionValue(OPTION_OUTPUT_DIR));
    if (!fOutputDirectory.exists())
    {
      throw new Exception("Output directory does not exists");
    }

    fTargetPackage = commandLine.getOptionValue(OPTION_PACKAGE);
    for (String table : commandLine.getOptionValues(OPTION_TABLES))
    {
      fTablesToGenerateJavaClassFor.add(table);
    }
    analyseAdditionalArgs(commandLine);
  }
  
  @Override
  public Target getTarget()
  {
    return Target.createTargetFiles(getTargetDirectory(), fTablesToGenerateJavaClassFor.size());
  }
  
  protected File getTargetDirectory()
  {
    return new File(getOutputDirectory(), getTargetPackage().replace('.', File.separatorChar));
  }

  /**
   * Gets the entity class name for the given table
   * @param table table
   * @return entity class name
   */
  protected String getEntityClassName(SqlTable table)
  {
   return JavaClassGeneratorUtil.getEntityClassName(table);
  }

  /**
   * Override this method to analyse more arguments
   * @param commandLine
   * @throws Exception
   */
  protected void analyseAdditionalArgs(@SuppressWarnings("unused") CommandLine commandLine) throws Exception
  {
  }

  /**
   * @see ch.ivyteam.db.meta.generator.internal.IMetaOutputGenerator#printHelp()
   */
  @Override
  public void printHelp()
  {
    new HelpFormatter().printHelp(getClass().getSimpleName(), options);
  }
  
  /**
   * Gets the tables to generate java classes for
   * @return list with table names
   */
  public List<String> getTablesToGenerateJavaClassFor()
  {
    return fTablesToGenerateJavaClassFor;
  }

  /**
   * Gets the target package
   * @return target package
   */
  public String getTargetPackage()
  {
    return fTargetPackage;
  }

  /**
   * Gets the output directory
   * @return output directory
   */
  public File getOutputDirectory()
  {
    return fOutputDirectory;
  }

  /**
   * Writes information about the given column
   * @param pr the print writer
   * @param table the table of the column
   * @param column the column
   * @throws MetaException
   */
  protected void writeColumnInformation(PrintWriter pr, SqlTable table, SqlTableColumn column) throws MetaException
  {
    boolean isPrimary, isParent;
    pr.print(column.getId());
    pr.print(" ");
    pr.print(column.getDataType());
    pr.print(" ");
    if (column.isCanBeNull())
    {
      pr.print("NULL");
    }
    else
    {
      pr.print("NOT NULL");
    }

    isPrimary = JavaClassGeneratorUtil.getPrimaryKeyColumn(table).equals(column);
    isParent = column.equals(JavaClassGeneratorUtil.getParentKeyColumn(table));
    if ((column.getReference() != null)||isPrimary||isParent)
    {
      pr.print(" (");
      if (isPrimary)
      {
        pr.print("primary key");
      }
      else if (isParent)
      {
        pr.print("parent key");
      }
      if (column.getReference()!=null)
      {
        if (isPrimary||isParent)
        {
          pr.print(" ");
        }
        pr.print("references ");
        pr.print(column.getReference().getForeignTable());
        pr.print(".");
        pr.print(column.getReference().getForeignColumn());
        if (column.getReference().getForeignKeyAction()!=null)
        {
          pr.print(" ");
          pr.print(column.getReference().getForeignKeyAction());
        }
      }
      pr.print(")");
    }
  }

  /**
   * Writes the the given comment into javadoc
   * @param pr
   * @param indent
   * @param comment
   */
  protected void writeJavaDocComment(PrintWriter pr, int indent, String comment)
  {
    for (String line : comment.split("\n"))
    {
      writeIndent(pr, indent);
      pr.print(" * ");
      pr.print(line);
      pr.println("<br>");
    }
  }



  /**
   * Writes an ident
   * @param pr
   * @param indent
   */
  protected void writeIndent(PrintWriter pr, int indent)
  {
    for (int pos = 0; pos < indent; pos++)
    {
      pr.print(' ');
    }

  }

  /**
   * Gets the associations tables where the given table is reference as foreign key
   * @param table the table
   * @param metaDefinition the meta definition
   * @return list of association tables
   * @throws MetaException 
   */
  protected List<SqlTable> getAssociationTables(SqlTable table, SqlMeta metaDefinition) throws MetaException
  {
    List<SqlTable> associationTables = new ArrayList<SqlTable>();
    
    for (SqlTable associationTable : metaDefinition.getArtifacts(SqlTable.class))
    {
      if (associationTable.getDatabaseManagementSystemHints(JavaClassGenerator.JAVA).isHintSet(AS_ASSOCIATION))
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
          associationTables.add(associationTable);          
        }
      }
    }
    return associationTables;
  }}
