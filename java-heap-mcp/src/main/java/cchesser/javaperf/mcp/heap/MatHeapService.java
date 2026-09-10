package cchesser.javaperf.mcp.heap;

import cchesser.javaperf.mcp.calcite.CalciteDataSource;
import cchesser.javaperf.mcp.calcite.RowSetTable;
import cchesser.javaperf.mcp.config.ServerConfig;
import org.eclipse.mat.SnapshotException;
import org.eclipse.mat.inspections.LeakHunterQuery;
import org.eclipse.mat.query.IResultTable;
import org.eclipse.mat.snapshot.ClassHistogramRecord;
import org.eclipse.mat.snapshot.Histogram;
import org.eclipse.mat.snapshot.IOQLQuery;
import org.eclipse.mat.snapshot.ISnapshot;
import org.eclipse.mat.snapshot.SnapshotFactory;
import org.eclipse.mat.snapshot.model.Field;
import org.eclipse.mat.snapshot.model.FieldDescriptor;
import org.eclipse.mat.snapshot.model.GCRootInfo;
import org.eclipse.mat.snapshot.model.IClass;
import org.eclipse.mat.snapshot.model.IInstance;
import org.eclipse.mat.snapshot.model.IObject;
import org.eclipse.mat.snapshot.model.NamedReference;
import org.eclipse.mat.util.IProgressListener;
import org.eclipse.mat.util.VoidProgressListener;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.sql.rowset.CachedRowSet;
import javax.sql.rowset.RowSetFactory;
import javax.sql.rowset.RowSetProvider;

public final class MatHeapService implements HeapAnalysisService {
    private static final Logger LOGGER = Logger.getLogger(MatHeapService.class.getName());
    @SuppressWarnings("unused")
    private final ServerConfig config;

    public MatHeapService(ServerConfig config) {
        this.config = config;
        MatRuntime.initialize();
    }

    @Override
    public LoadedHeap open(Path heapDumpPath) {
        try {
            ISnapshot snapshot = SnapshotFactory.openSnapshot(heapDumpPath.toFile(), progress());
            return new MatLoadedHeap(heapDumpPath, snapshot, snapshotSummary(snapshot));
        } catch (SnapshotException exception) {
            throw classifySnapshotException("Failed to open heap dump " + heapDumpPath, exception);
        }
    }

    @Override
    public HeapOverview overview(LoadedHeap heap, int limit) {
        ISnapshot snapshot = snapshot(heap);
        Map<String, Object> summary = snapshotSummary(snapshot);
        List<HistogramEntry> topClasses = histogram(heap, "retained", limit);
        List<DominatorEntry> topDominators = dominators(heap, null, limit);
        return new HeapOverview(summary, topClasses, topDominators);
    }

    @Override
    public OqlQueryResult runOql(LoadedHeap heap, String query, int limit, int offset) {
        ISnapshot snapshot = snapshot(heap);
        LOGGER.info(() -> "Executing OQL query (limit=" + limit + ", offset=" + offset + "): " + diagnosticQuery(query));
        try {
            // Calcite is the primary query engine. It provides SQL joins, grouping,
            // ordering, lateral table functions, and the MAT-aware functions exposed
            // by the embedded mat-calcite-plugin implementation.
            try {
                return tableResult(query, executeCalcite(snapshot, query), offset, limit);
            } catch (SQLException calciteException) {
                // Keep MAT OQL available for existing callers using MAT-only syntax.
                LOGGER.fine(() -> "Query was not accepted by Calcite; trying MAT OQL: " + calciteException.getMessage());
            }

            IOQLQuery compiledQuery = SnapshotFactory.createQuery(query);
            Object raw = compiledQuery.execute(snapshot, progress());
            if (raw instanceof int[] objectIds) {
                List<Map<String, Object>> rows = new ArrayList<>();
                int end = Math.min(objectIds.length, offset + limit);
                for (int index = offset; index < end; index++) {
                    int objectId = objectIds[index];
                    IObject object = snapshot.getObject(objectId);
                    rows.add(Map.of(
                            "objectId", objectId,
                            "address", snapshot.mapIdToAddress(objectId),
                            "className", object.getClazz().getName(),
                            "displayName", safeDisplayName(object),
                            "shallowHeapBytes", snapshot.getHeapSize(objectId),
                            "retainedHeapBytes", snapshot.getRetainedHeapSize(objectId)
                    ));
                }
                return new OqlQueryResult(query, List.of("objectId", "address", "className", "displayName", "shallowHeapBytes", "retainedHeapBytes"),
                        rows, offset, limit, end < objectIds.length, objectIds.length);
            }
            if (raw instanceof IResultTable table) {
                return tableResult(query, table, offset, limit);
            }
            LOGGER.info(() -> "OQL scalar result type: " + (raw == null ? "null" : raw.getClass().getName()));
            Map<String, Object> scalarRow = new LinkedHashMap<>();
            scalarRow.put("value", raw);
            return new OqlQueryResult(query, List.of("value"), List.of(scalarRow), offset, limit, false, 1);
        } catch (SnapshotException exception) {
            LOGGER.log(Level.WARNING, "OQL query failed: " + diagnosticQuery(query), exception);
            throw new HeapOperationException(HeapErrorCode.OQL_ERROR, "Failed to execute OQL query: " + diagnosticQuery(query), exception);
        } catch (RuntimeException exception) {
            LOGGER.log(Level.WARNING, "OQL query failed with runtime error: " + diagnosticQuery(query), exception);
            throw exception;
        }
    }

    private IResultTable executeCalcite(ISnapshot snapshot, String query) throws SQLException {
        Connection connection = null;
        Statement statement = null;
        ResultSet resultSet = null;
        try {
            connection = CalciteDataSource.getConnection(snapshot);
            statement = connection.createStatement();
            resultSet = statement.executeQuery(query);
            RowSetFactory rowSetFactory = RowSetProvider.newFactory();
            CachedRowSet rowSet = rowSetFactory.createCachedRowSet();
            rowSet.populate(resultSet);
            return new RowSetTable(rowSet);
        } finally {
            CalciteDataSource.close(resultSet, statement, connection);
        }
    }

    private static String diagnosticQuery(String query) {
        if (query == null) {
            return "<null>";
        }
        return query.replace("\r", "\\r").replace("\n", "\\n");
    }

    @Override
    public List<HistogramEntry> histogram(LoadedHeap heap, String sortBy, int limit) {
        ISnapshot snapshot = snapshot(heap);
        try {
            Histogram histogram = snapshot.getHistogram(progress());
            Comparator<HistogramEntry> comparator = switch (sortBy == null ? "retained" : sortBy) {
                case "count" -> Comparator.comparingLong(HistogramEntry::objectCount).reversed();
                case "shallow" -> Comparator.comparingLong(HistogramEntry::shallowHeapBytes).reversed();
                default -> Comparator.<HistogramEntry>comparingLong(
                        entry -> entry.retainedHeapBytes() == null ? Long.MIN_VALUE : entry.retainedHeapBytes()
                ).reversed();
            };
            return histogram.getClassHistogramRecords().stream()
                    .map(record -> toHistogramEntry(snapshot, record))
                    .sorted(comparator)
                    .limit(limit)
                    .toList();
        } catch (SnapshotException exception) {
            throw classifySnapshotException("Failed to compute histogram", exception);
        }
    }

    @Override
    public List<DominatorEntry> dominators(LoadedHeap heap, Integer rootObjectId, int limit) {
        ISnapshot snapshot = snapshot(heap);
        try {
            int[] roots = rootObjectId == null ? snapshot.getImmediateDominatedIds(-1) : snapshot.getImmediateDominatedIds(rootObjectId);
            if (roots == null) {
                return List.of();
            }
            return Arrays.stream(roots)
                    .mapToObj(objectId -> toDominatorEntry(snapshot, objectId))
                    .sorted(Comparator.comparingLong(DominatorEntry::retainedHeapBytes).reversed())
                    .limit(limit)
                    .toList();
        } catch (SnapshotException exception) {
            throw classifySnapshotException("Failed to compute dominators", exception);
        }
    }

    @Override
    public ObjectInspection inspectObject(LoadedHeap heap, int objectId, int limit) {
        ISnapshot snapshot = snapshot(heap);
        try {
            IObject object = snapshot.getObject(objectId);
            List<ObjectFieldValue> fields = extractFields(object);
            List<ObjectReference> inbound = Arrays.stream(snapshot.getInboundRefererIds(objectId))
                    .limit(limit)
                    .mapToObj(id -> toReference(snapshot, id))
                    .toList();
            List<ObjectReference> outbound = Arrays.stream(snapshot.getOutboundReferentIds(objectId))
                    .limit(limit)
                    .mapToObj(id -> toReference(snapshot, id))
                    .toList();
            GCRootInfo[] gcRootInfos = snapshot.getGCRootInfo(objectId);
            return new ObjectInspection(
                    objectId,
                    snapshot.mapIdToAddress(objectId),
                    object.getClazz().getName(),
                    safeDisplayName(object),
                    snapshot.getHeapSize(objectId),
                    snapshot.getRetainedHeapSize(objectId),
                    snapshot.getImmediateDominatorId(objectId),
                    gcRootInfos == null ? List.of() : List.of(GCRootInfo.getTypeSetAsString(gcRootInfos)),
                    fields,
                    inbound,
                    outbound
            );
        } catch (SnapshotException exception) {
            throw classifySnapshotException("Failed to inspect object " + objectId, exception);
        }
    }

    @Override
    public List<GcRootPath> findPathsToGcRoots(LoadedHeap heap, int objectId, boolean excludeWeakRefs, int limit) {
        ISnapshot snapshot = snapshot(heap);
        try {
            Map<IClass, java.util.Set<String>> exclusions = excludeWeakRefs ? Map.of() : null;
            var computer = snapshot.getPathsFromGCRoots(objectId, exclusions);
            List<GcRootPath> paths = new ArrayList<>();
            while (paths.size() < limit) {
                int[] rawPath = computer.getNextShortestPath();
                if (rawPath == null) {
                    break;
                }
                List<ObjectReference> references = Arrays.stream(rawPath)
                        .mapToObj(id -> toReference(snapshot, id))
                        .toList();
                paths.add(new GcRootPath(objectId, references));
            }
            return paths;
        } catch (SnapshotException exception) {
            throw classifySnapshotException("Failed to find GC root paths for object " + objectId, exception);
        }
    }

    @Override
    public Object leakSuspects(LoadedHeap heap, int limit) {
        ISnapshot snapshot = snapshot(heap);
        try {
            LeakHunterQuery query = new LeakHunterQuery();
            query.snapshot = snapshot;
            Object result = query.execute(progress());
            if (result instanceof IResultTable table) {
                return tableResult("leak_suspects", table, 0, limit);
            }
            return Map.of("summary", String.valueOf(result));
        } catch (Exception exception) {
            return Map.of(
                    "available", false,
                    "message", "Leak suspect analysis is not available for this heap dump or MAT runtime.",
                    "details", exception.getMessage()
            );
        }
    }

    private OqlQueryResult tableResult(String query, IResultTable table, int offset, int limit) {
        List<String> columns = Arrays.stream(table.getColumns()).map(column -> column.getLabel()).toList();
        int totalRows = table.getRowCount();
        int end = Math.min(totalRows, offset + limit);
        List<Map<String, Object>> rows = new ArrayList<>();
        for (int rowIndex = offset; rowIndex < end; rowIndex++) {
            Object row = table.getRow(rowIndex);
            Map<String, Object> values = new LinkedHashMap<>();
            for (int columnIndex = 0; columnIndex < columns.size(); columnIndex++) {
                values.put(columns.get(columnIndex), table.getColumnValue(row, columnIndex));
            }
            rows.add(values);
        }
        return new OqlQueryResult(query, columns, rows, offset, limit, end < totalRows, totalRows);
    }

    private HistogramEntry toHistogramEntry(ISnapshot snapshot, ClassHistogramRecord record) {
        try {
            long retained = record.getRetainedHeapSize();
            if (retained == 0L) {
                retained = record.calculateRetainedSize(snapshot, true, false, progress());
            }
            return new HistogramEntry(
                    record.getClassId(),
                    record.getLabel(),
                    record.getNumberOfObjects(),
                    record.getUsedHeapSize(),
                    retained == 0L ? null : Math.abs(retained)
            );
        } catch (SnapshotException exception) {
            return new HistogramEntry(record.getClassId(), record.getLabel(), record.getNumberOfObjects(), record.getUsedHeapSize(), null);
        }
    }

    private DominatorEntry toDominatorEntry(ISnapshot snapshot, int objectId) {
        try {
            IObject object = snapshot.getObject(objectId);
            return new DominatorEntry(
                    objectId,
                    object.getClazz().getName(),
                    snapshot.getHeapSize(objectId),
                    snapshot.getRetainedHeapSize(objectId),
                    safeDisplayName(object)
            );
        } catch (SnapshotException exception) {
            throw classifySnapshotException("Failed to convert dominator " + objectId, exception);
        }
    }

    private ObjectReference toReference(ISnapshot snapshot, int objectId) {
        try {
            IObject object = snapshot.getObject(objectId);
            return new ObjectReference(objectId, object.getClazz().getName(), snapshot.mapIdToAddress(objectId), safeDisplayName(object));
        } catch (SnapshotException exception) {
            throw classifySnapshotException("Failed to resolve object reference " + objectId, exception);
        }
    }

    private List<ObjectFieldValue> extractFields(IObject object) {
        if (!(object instanceof IInstance instance)) {
            List<ObjectFieldValue> pseudoFields = new ArrayList<>();
            for (NamedReference reference : object.getOutboundReferences()) {
                Integer referencedObjectId;
                try {
                    referencedObjectId = reference.getObjectId();
                } catch (SnapshotException exception) {
                    referencedObjectId = null;
                }
                pseudoFields.add(new ObjectFieldValue(reference.getName(), "reference", reference.getObjectAddress(), referencedObjectId));
            }
            return pseudoFields;
        }

        List<ObjectFieldValue> values = new ArrayList<>();
        for (Field field : instance.getFields()) {
            Object value = field.getValue();
            values.add(new ObjectFieldValue(field.getName(), field.getVerboseSignature(), value, null));
        }
        Collection<FieldDescriptor> descriptors = instance.getClazz().getFieldDescriptors();
        for (FieldDescriptor descriptor : descriptors) {
            boolean alreadyPresent = values.stream().anyMatch(value -> value.name().equals(descriptor.getName()));
            if (!alreadyPresent) {
                values.add(new ObjectFieldValue(descriptor.getName(), descriptor.getVerboseSignature(), null, null));
            }
        }
        return values;
    }

    private Map<String, Object> snapshotSummary(ISnapshot snapshot) {
        var info = snapshot.getSnapshotInfo();
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("path", info.getPath());
        summary.put("prefix", info.getPrefix());
        summary.put("jvmInfo", info.getJvmInfo());
        summary.put("identifierSize", info.getIdentifierSize());
        summary.put("creationDate", info.getCreationDate());
        summary.put("numberOfObjects", info.getNumberOfObjects());
        summary.put("numberOfClasses", info.getNumberOfClasses());
        summary.put("numberOfClassLoaders", info.getNumberOfClassLoaders());
        summary.put("usedHeapSize", info.getUsedHeapSize());
        return summary;
    }

    private String safeDisplayName(IObject object) {
        try {
            return object.getDisplayName();
        } catch (RuntimeException ignored) {
            return object.getTechnicalName();
        }
    }

    private ISnapshot snapshot(LoadedHeap heap) {
        return (ISnapshot) heap.nativeSnapshot();
    }

    private IProgressListener progress() {
        return new VoidProgressListener();
    }

    private HeapOperationException classifySnapshotException(String message, SnapshotException exception) {
        String lowerMessage = exception.getMessage() == null ? "" : exception.getMessage().toLowerCase();
        if (lowerMessage.contains("hprof") || lowerMessage.contains("format")) {
            return new HeapOperationException(HeapErrorCode.UNSUPPORTED_FORMAT, message, exception);
        }
        if (lowerMessage.contains("index")) {
            return new HeapOperationException(HeapErrorCode.CORRUPT_INDEX, message, exception);
        }
        return new HeapOperationException(HeapErrorCode.INTERNAL, message, exception);
    }

    @Override
    public void close() {
        // No global resources to close beyond per-snapshot disposal.
    }
}
