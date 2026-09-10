const rowsFor = result => {
  if (Array.isArray(result)) return result;
  for (const candidate of [result, result?.structuredContent, result?.result]) {
    for (const key of ['rows', 'entries', 'results', 'data']) {
      if (Array.isArray(candidate?.[key])) return candidate[key];
    }
  }
  return [];
};

const valueFor = (row, names) => {
  const key = Object.keys(row || {}).find(candidate => names.includes(candidate.toLowerCase()));
  return key === undefined ? undefined : row[key];
};

const numericValue = value => {
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : null;
};

export function planToolVisuals(question, name, args, result) {
  const source = `${question || ''} ${args?.query || ''}`.toLowerCase();
  const rows = rowsFor(result);

  // A grouped value/count result is a frequency distribution. String frequency
  // queries are the primary pie-chart use case and do not depend on model JSON.
  if (name === 'heap_run_oql' && rows.length) {
    const data = rows.map(row => ({
      label: valueFor(row, ['val', 'value', 'label', 'name']),
      value: numericValue(valueFor(row, ['cnt', 'count', 'objectcount', 'frequency']))
    })).filter(point => point.label !== undefined && point.value !== null && point.value > 0).slice(0, 8);
    const looksLikeFrequency = data.length > 0 && (
      /string|frequency|common|duplicate|pie/.test(source)
      || rows.some(row => valueFor(row, ['val']) !== undefined && valueFor(row, ['cnt', 'count']) !== undefined)
    );
    if (looksLikeFrequency) {
      return [{ type: 'chart', title: 'Most common values', subtitle: 'Object-count distribution', chartType: 'pie', data }];
    }
  }

  return [];
}
