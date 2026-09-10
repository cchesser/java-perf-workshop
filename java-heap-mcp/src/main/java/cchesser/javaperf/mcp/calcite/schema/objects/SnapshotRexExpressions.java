package cchesser.javaperf.mcp.calcite.schema.objects;

import cchesser.javaperf.mcp.calcite.rex.RexBuilderContext;

import org.apache.calcite.rex.RexNode;

/**
 * Sourced from <a href="https://github.com/vlsi/mat-calcite-plugin">MAT Calcite Plugin</a>.
 * Original class: {@code com.github.vlsi.mat.calcite.schema.objects.SnapshotRexExpressions}.
 */
public interface SnapshotRexExpressions {
  static RexNode computeThis(RexBuilderContext context) {
    return context.getBuilder()
        .makeCall(HeapOperatorTable.TO_HEAP_REFERENCE, context.getIObject());
  }

  static RexNode resolveField(RexBuilderContext context, String fieldName) {
    return context.getBuilder()
        .makeCall(HeapOperatorTable.RESOLVE_VALUE,
            context.getIObject(),
            context.getBuilder().makeLiteral(fieldName));
  }

  static RexNode getClassOf(RexBuilderContext context, RexNode iobject) {
    return context.getBuilder().makeCall(HeapOperatorTable.GET_CLASS_OF, context.getSnapshot(), iobject);
  }

  static RexNode getSuper(RexBuilderContext context, RexNode iclass) {
    return context.getBuilder().makeCall(HeapOperatorTable.GET_SUPER, iclass);
  }

  static RexNode getClassLoader(RexBuilderContext context, RexNode iclass) {
    return context.getBuilder().makeCall(HeapOperatorTable.GET_CLASS_LOADER, iclass);
  }

  static RexNode getClassName(RexBuilderContext context, RexNode iclass) {
    return context.getBuilder().makeCall(HeapOperatorTable.GET_CLASS_NAME, iclass);
  }
}
