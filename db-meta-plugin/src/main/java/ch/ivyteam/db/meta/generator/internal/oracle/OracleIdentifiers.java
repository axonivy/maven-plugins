package ch.ivyteam.db.meta.generator.internal.oracle;

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collections;

import ch.ivyteam.db.meta.generator.internal.Identifiers;

final class OracleIdentifiers extends Identifiers {

  OracleIdentifiers() {
    super(Identifiers.STANDARD_QUOTE, true, Collections.emptyList(), Arrays.asList("STATE"));
  }

  @Override
  protected void generateAs(PrintWriter pr) {
    pr.print(" ");
  }
}
