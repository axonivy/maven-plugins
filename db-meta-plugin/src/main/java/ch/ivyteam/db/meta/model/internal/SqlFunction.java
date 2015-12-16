package ch.ivyteam.db.meta.model.internal;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

public class SqlFunction extends SqlAtom
{
  private String functionName;
  private List<SqlAtom> args;

  public SqlFunction(String functionName, SqlAtom... args)
  {
    this.functionName = functionName;
    this.args = Arrays.asList(args);
  }
  
  @Override
  public String toString()
  {
    return functionName+"("+StringUtils.join(args,",")+")";
  }
}