package cchesser.javaperf.mcp.calcite.rules;

import org.apache.calcite.plan.RelOptRuleCall;
import org.apache.calcite.plan.RelRule;
import org.apache.calcite.rel.core.Correlate;

/**
 * Sourced from <a href="https://github.com/vlsi/mat-calcite-plugin">MAT Calcite Plugin</a>.
 * Original class: {@code com.github.vlsi.mat.calcite.rules.InstanceAccessByIdRule}.
 */
public class InstanceAccessByIdRule extends RelRule<RelRule.Config> {
  public static final InstanceAccessByIdRule INSTANCE =
      new InstanceAccessByIdRule(
          DefaultRuleConfig.EMPTY
              .withOperandSupplier(
                  b0 ->
                      b0.operand(Correlate.class)
                          .anyInputs()
              )
      );

  public InstanceAccessByIdRule(RelRule.Config config) {
    super(config);
  }

  @Override
  public void onMatch(RelOptRuleCall call) {
    System.out.println("InstanceAccessByIdRule fired for call " + call);
  }
}
