package ch.ivyteam.db.meta.model.internal;

/**
 * An sql binary relation expression
 * @author rwei
 * @since 02.10.2009
 */
public class SqlBinaryRelation extends SqlSimpleExpr
{
  /** The first parameter of the relation */
  private SqlAtom fFirst;
  /** The relation operator */
  private String fOperator;
  /** The second paramter of the relation */
  private SqlAtom fSecond;

  /**
   * Constructor
   * @param first
   * @param operator
   * @param second
   */
  public SqlBinaryRelation(SqlAtom first, String operator, SqlAtom second)
  {
    assert first != null : "Parameter first must not be null";
    assert operator != null : "Parameter operator must not be null";
    assert second != null : "Parameter second must not be null";
    fFirst = first;
    fOperator = operator;
    fSecond = second;
  }
  
  /**
   * Returns the first
   * @return the first
   */
  public SqlAtom getFirst()
  {
    return fFirst;
  }
  
  /**
   * Returns the second
   * @return the second
   */
  public SqlAtom getSecond()
  {
    return fSecond;
  }
  
  /**
   * Returns the operator
   * @return the operator
   */
  public String getOperator()
  {
    return fOperator;
  }
  
  
  
  /**
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString()
  {
    String operator;
    
    operator = fOperator;
    if (Character.isLetter(operator.charAt(0)))
    {
      operator = " "+operator+" ";
    }
    return fFirst.toString()+operator+fSecond.toString();
  }

}
