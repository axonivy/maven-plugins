package ch.ivyteam.db.meta.model.internal;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

public class SqlFunction extends SqlAtom {

  private String name;
  private List<SqlAtom> arguments;

  public SqlFunction(String name, SqlAtom... arguments) {
    this.name = name;
    this.arguments = Arrays.asList(arguments);
  }

  public SqlFunction(String name, List<SqlAtom> arguments) {
    this.name = name;
    this.arguments = arguments;
  }

  public String getName() {
    return name;
  }

  public List<SqlAtom> getArguments() {
    return arguments;
  }

  @Override
  public String toString() {
    return name + "(" + StringUtils.join(arguments, ",") + ")";
  }
}
