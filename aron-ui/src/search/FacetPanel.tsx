import {
  Checkbox,
  Input,
  Link,
  makeStyles,
  Spinner,
  Text,
  tokens,
  Tooltip,
  useId,
} from "@fluentui/react-components";
import { useQuery } from "@tanstack/react-query";
import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import { searchApi } from "../api/client";
import { PRIMARY_MAIN } from "../layout/AppHeader";
import {
  type ApuType,
  type DatingFacetResult,
  type EnumFacetResult,
  type FacetDef,
  type FacetResult,
  FacetResultKind,
  FacetType,
  type RefFacetResult,
  type SearchFilter,
} from "../api/generated";
import {
  addRelated,
  rangeOf,
  RELATED_FACET,
  relatedOf,
  removeRelated,
  serializeFilters,
  setIncludeUndated,
  setRange,
  setText,
  textOf,
  toggleValue,
  TYPE_FACET,
  valuesOf,
} from "./filters";
import { RelatedChip } from "./RelatedChips";
import { useDebouncedValue } from "./useDebouncedValue";

/** Records offered per keystroke in the relation picker - a shortlist, not a page. */
const RELATED_OPTIONS = 10;

const useStyles = makeStyles({
  facet: {
    display: "flex",
    flexDirection: "column",
    gap: tokens.spacingVerticalXS,
    paddingBottom: tokens.spacingVerticalM,
    borderBottom: `1px solid ${tokens.colorNeutralStroke2}`,
  },
  title: {
    margin: "0",
    fontSize: tokens.fontSizeBase300,
    lineHeight: tokens.lineHeightBase300,
    fontWeight: tokens.fontWeightSemibold,
  },
  // read by a screen reader, out of the visual flow (the option's explanation,
  // which is also shown as a tooltip on hover and focus)
  srOnly: {
    position: "absolute",
    width: "1px",
    height: "1px",
    margin: "-1px",
    padding: 0,
    overflow: "hidden",
    clip: "rect(0 0 0 0)",
    whiteSpace: "nowrap",
    border: 0,
  },
  // one offered record of the relation picker: a full-width row, so the whole
  // line is the target the way a bucket's checkbox label is
  relatedOption: {
    display: "block",
    width: "100%",
    textAlign: "left",
    border: "none",
    background: "none",
    cursor: "pointer",
    padding: `${tokens.spacingVerticalXXS} 0`,
    fontSize: tokens.fontSizeBase200,
    color: tokens.colorNeutralForeground1,
    ":hover": {
      textDecorationLine: "underline",
    },
  },
  // per-condition hit count next to the facet name (muted, like bucket counts)
  titleCount: {
    marginLeft: tokens.spacingHorizontalXS,
    color: tokens.colorNeutralForeground3,
    fontWeight: tokens.fontWeightRegular,
  },
  count: {
    color: tokens.colorNeutralForeground3,
  },
  bucketLabel: {
    display: "flex",
    alignItems: "baseline",
    gap: tokens.spacingHorizontalXS,
  },
  rangeRow: {
    display: "flex",
    alignItems: "center",
    gap: tokens.spacingHorizontalS,
  },
  rangeInput: {
    width: "90px",
  },
  sliderWrap: {
    position: "relative",
    height: "24px",
    marginTop: tokens.spacingVerticalXS,
  },
  sliderTrack: {
    position: "absolute",
    left: "8px",
    right: "8px",
    top: "10px",
    height: "4px",
    borderRadius: tokens.borderRadiusCircular,
    backgroundColor: tokens.colorNeutralStroke1,
  },
  sliderFill: {
    position: "absolute",
    top: "10px",
    height: "4px",
    borderRadius: tokens.borderRadiusCircular,
    backgroundColor: PRIMARY_MAIN,
  },
  // two overlaid native range inputs form the dual-thumb slider: tracks are
  // transparent and ignore the pointer, only the thumbs are interactive
  sliderInput: {
    position: "absolute",
    left: "0",
    top: "0",
    width: "100%",
    height: "24px",
    margin: "0",
    appearance: "none",
    backgroundColor: "transparent",
    pointerEvents: "none",
    "::-webkit-slider-thumb": {
      appearance: "none",
      pointerEvents: "auto",
      width: "16px",
      height: "16px",
      borderRadius: "50%",
      backgroundColor: PRIMARY_MAIN,
      border: `2px solid ${tokens.colorNeutralBackground1}`,
      boxShadow: tokens.shadow2,
      cursor: "pointer",
      marginTop: "4px",
    },
    "::-moz-range-thumb": {
      pointerEvents: "auto",
      width: "12px",
      height: "12px",
      borderRadius: "50%",
      backgroundColor: PRIMARY_MAIN,
      border: `2px solid ${tokens.colorNeutralBackground1}`,
      boxShadow: tokens.shadow2,
      cursor: "pointer",
    },
    "::-webkit-slider-runnable-track": {
      backgroundColor: "transparent",
    },
    "::-moz-range-track": {
      backgroundColor: "transparent",
    },
    // the thumb suppresses the browser's own ring - put it back for keyboard use
    ":focus-visible": {
      outline: `2px solid ${tokens.colorStrokeFocus2}`,
      outlineOffset: "2px",
      borderRadius: tokens.borderRadiusMedium,
    },
  },
  sliderBounds: {
    display: "flex",
    justifyContent: "space-between",
    color: tokens.colorNeutralForeground3,
  },
});

interface Props {
  def: FacetDef;
  filters: SearchFilter[];
  /** This facet's slice of the search response (result kind follows the facet type). */
  result?: FacetResult;
  /**
   * Section of the search - facet options are section-scoped. Absent in the
   * general search, whose built-in facets need none: the dating and type facets
   * come with the search response, and the relation picker searches records.
   */
  apuType?: ApuType;
  /** Fulltext query of the current search (scopes the reference type-ahead). */
  query: string;
  /** Total of the current search - the per-condition count badge of active text/range facets. */
  total?: number;
  onFilters: (filters: SearchFilter[]) => void;
}

/**
 * One facet of the sidebar; the widget follows the facet type. Bucket counts
 * and dating bounds come server-computed with multi-select semantics.
 */
export default function FacetPanel({ def, filters, result, apuType, query, total, onFilters }: Props) {
  const styles = useStyles();

  // the old portal highlights how many records satisfy an entered condition;
  // text/range filters auto-apply, so the current total IS that count
  const conditionEntered =
    (def.type === FacetType.Fulltext || def.type === FacetType.Unitdate) &&
    filters.some((f) => f.facet === def.code);

  // the heading names the group, so a screen reader can jump between facets
  const headingId = `facet-${def.code}`;

  return (
    <div className={styles.facet} title={def.tooltip} role="group" aria-labelledby={headingId}>
      <h2 id={headingId} className={styles.title}>
        {def.label}
        {conditionEntered && total !== undefined && (
          <span className={styles.titleCount}>({total})</span>
        )}
      </h2>
      {def.type === FacetType.Enum && (
        <EnumFacet def={def} filters={filters} result={result} onFilters={onFilters} />
      )}
      {/* the built-in relation facet has no buckets to enumerate: the reader
          picks a record by name instead (see BuiltInFacets on the server) */}
      {def.type === FacetType.Ref && def.code === RELATED_FACET && (
        <RelatedFacet def={def} filters={filters} onFilters={onFilters} />
      )}
      {/* a configured reference facet exists only inside a section, so its
          section-scoped options always have one */}
      {def.type === FacetType.Ref && def.code !== RELATED_FACET && apuType !== undefined && (
        <RefFacet
          def={def}
          filters={filters}
          result={result}
          apuType={apuType}
          query={query}
          onFilters={onFilters}
        />
      )}
      {def.type === FacetType.Fulltext && (
        <TextFacet def={def} filters={filters} onFilters={onFilters} />
      )}
      {def.type === FacetType.Unitdate && (
        <RangeFacet def={def} filters={filters} result={result} onFilters={onFilters} />
      )}
    </div>
  );
}

/** One selectable facet value; count is absent for a selection unknown to the current buckets. */
interface OptionRow {
  value: string;
  label?: string;
  count?: number;
}

/**
 * The deployment's explanation of one option, where its value does not say what
 * it covers. An entry names the option by its value or by its displayed label,
 * so a deployment writing about a reference facet can use the name it thinks in.
 */
function tooltipFor(def: FacetDef, row: OptionRow): string | undefined {
  return (def.optionTooltips ?? []).find(
    (entry) => entry.value === row.value || entry.value === row.label,
  )?.tooltip;
}

/**
 * One option. Its explanation is both shown on hover/focus and attached as the
 * checkbox's accessible description - the old portal put it on a hover-only
 * div, which never reached a keyboard or a screen reader.
 */
function FacetCheckbox({
  row,
  checked,
  tooltip,
  onToggle,
}: {
  row: OptionRow;
  checked: boolean;
  tooltip?: string;
  onToggle: () => void;
}) {
  const styles = useStyles();
  const descriptionId = useId("facet-option-");
  const checkbox = (
    <Checkbox
      checked={checked}
      onChange={onToggle}
      input={tooltip ? { "aria-describedby": descriptionId } : undefined}
      label={
        <span className={styles.bucketLabel}>
          <span>{row.label ?? row.value}</span>
          {row.count !== undefined && (
            <Text size={200} className={styles.count}>
              ({row.count})
            </Text>
          )}
        </span>
      }
    />
  );
  if (!tooltip) {
    return checkbox;
  }
  return (
    <>
      <Tooltip content={tooltip} relationship="description" withArrow>
        {checkbox}
      </Tooltip>
      {/* the description a screen reader reads: the tooltip's own text lives in
          a portal that is only mounted while it is open */}
      <span id={descriptionId} className={styles.srOnly}>
        {tooltip}
      </span>
    </>
  );
}

function EnumFacet({
  def,
  filters,
  result,
  onFilters,
}: Pick<Props, "def" | "filters" | "result" | "onFilters">) {
  const { t } = useTranslation();
  const [expanded, setExpanded] = useState(false);
  const selected = valuesOf(filters, def.code);
  const raw = result?.kind === FacetResultKind.Enum ? (result as EnumFacetResult).buckets : [];
  // the built-in type facet's values are ApuType members, which the response
  // cannot label (search takes no lang) and this UI already names - the same
  // strings the section menu and the type-count chips use
  const buckets: OptionRow[] =
    def.code === TYPE_FACET
      ? raw.map((bucket) => ({ ...bucket, label: t(`sections.${bucket.value}`) }))
      : raw;
  const collapsedCount = def.displayedItems && def.displayedItems > 0 ? def.displayedItems : 10;
  const visible = expanded ? buckets : buckets.slice(0, collapsedCount);

  return (
    <>
      {visible.map((bucket) => (
        <FacetCheckbox
          key={bucket.value}
          row={bucket}
          checked={selected.includes(bucket.value)}
          tooltip={tooltipFor(def, bucket)}
          onToggle={() => onFilters(toggleValue(filters, def.code, bucket.value))}
        />
      ))}
      {buckets.length > collapsedCount && (
        <Link onClick={() => setExpanded(!expanded)}>
          {expanded ? t("facets.showLess") : t("facets.showMore", { count: buckets.length })}
        </Link>
      )}
    </>
  );
}

/**
 * Reference facet: selected values pinned on top, below them a type-ahead over
 * the facet's options (server-side, multi-select semantics - see the
 * /facets/{code}/options contract). Without a typed query the search response's
 * own buckets serve as the top options, no extra request needed.
 */
function RefFacet({
  def,
  filters,
  result,
  apuType,
  query,
  onFilters,
}: Omit<Props, "apuType" | "total"> & { apuType: ApuType }) {
  const { t } = useTranslation();
  const [q, setQ] = useState("");
  const debouncedQ = useDebouncedValue(q.trim(), 300);
  const selected = valuesOf(filters, def.code);
  const buckets = result?.kind === FacetResultKind.Ref ? (result as RefFacetResult).buckets : [];

  const options = useQuery({
    queryKey: ["facet-options", apuType, def.code, debouncedQ, query, serializeFilters(filters)],
    queryFn: () =>
      searchApi.searchGetFacetOptions({
        code: def.code,
        facetOptionsRequest: {
          apuType,
          q: debouncedQ,
          query: query || undefined,
          filters,
          size: 50,
        },
      }),
    enabled: debouncedQ.length > 0,
    placeholderData: (previous) => previous,
  });

  const shown = debouncedQ ? (options.data?.options ?? []) : buckets;
  const collapsedCount = def.displayedItems && def.displayedItems > 0 ? def.displayedItems : 10;
  const unselected = shown.filter((b) => !selected.includes(b.value)).slice(0, collapsedCount);
  // selected values resolve their label from the multi-select buckets (which
  // always include them); a value beyond the bucket limit falls back to itself
  const selectedRow = (value: string): OptionRow =>
    buckets.find((b) => b.value === value) ??
    shown.find((b) => b.value === value) ?? { value };

  return (
    <>
      {selected.map((value) => (
        <FacetCheckbox
          key={value}
          row={selectedRow(value)}
          checked
          tooltip={tooltipFor(def, selectedRow(value))}
          onToggle={() => onFilters(toggleValue(filters, def.code, value))}
        />
      ))}
      <Input
        value={q}
        aria-label={t("facets.optionsFor", { facet: def.label })}
        placeholder={t("facets.optionsPlaceholder")}
        onChange={(_, data) => setQ(data.value)}
        contentAfter={options.isFetching ? <Spinner size="extra-tiny" /> : undefined}
      />
      {unselected.map((bucket) => (
        <FacetCheckbox
          key={bucket.value}
          row={bucket}
          checked={false}
          tooltip={tooltipFor(def, bucket)}
          onToggle={() => onFilters(toggleValue(filters, def.code, bucket.value))}
        />
      ))}
      {debouncedQ.length > 0 && options.isSuccess && unselected.length === 0 && (
        <Text size={200}>{t("facets.noOptions")}</Text>
      )}
    </>
  );
}

function TextFacet({ def, filters, onFilters }: Pick<Props, "def" | "filters" | "onFilters">) {
  const { t } = useTranslation();
  const applied = textOf(filters, def.code);
  const [value, setValue] = useState(applied);
  // follow the filter when it changes elsewhere (shared link, reset). Adjusting
  // state while rendering rather than in an effect keeps it to one render pass.
  const [lastApplied, setLastApplied] = useState(applied);
  if (applied !== lastApplied) {
    setLastApplied(applied);
    setValue(applied);
  }

  // the filter applies while typing (old-portal behavior, same 700 ms
  // debounce); Enter just applies immediately
  const debounced = useDebouncedValue(value, 700);
  useEffect(() => {
    // fires on the debounced keystroke only; the guard keeps the apply-echo
    // (applied catching up with debounced) from re-applying
    if (debounced !== applied) {
      onFilters(setText(filters, def.code, debounced));
    }
    // only the debounced keystroke may trigger this; the guard above makes the
    // echo of our own apply a no-op
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [debounced]);

  return (
    <Input
      value={value}
      aria-label={def.label}
      placeholder={t("facets.textPlaceholder")}
      onChange={(_, data) => setValue(data.value)}
      onKeyDown={(e) => e.key === "Enter" && onFilters(setText(filters, def.code, value))}
    />
  );
}

/**
 * Dating facet: a dual-thumb year slider over the available bounds plus the
 * paired integer fields. Both auto-apply with the same debounce as text
 * filters (old-portal behavior, no apply button); typed years clamp to the
 * available bounds.
 */
function RangeFacet({
  def,
  filters,
  result,
  onFilters,
}: Pick<Props, "def" | "filters" | "result" | "onFilters">) {
  const styles = useStyles();
  const { t } = useTranslation();
  const dating = result?.kind === FacetResultKind.Dating ? (result as DatingFacetResult) : undefined;
  const bounds = dating?.bounds;
  const applied = rangeOf(filters, def.code);
  const includeUndated = applied.includeUndated === true;
  const [from, setFrom] = useState(applied.from ?? "");
  const [to, setTo] = useState(applied.to ?? "");
  // follow the applied range (shared link, reset, our own clamped apply) during
  // render rather than through an effect - see TextFacet
  const appliedKey = `${applied.from ?? ""}|${applied.to ?? ""}`;
  const [lastAppliedKey, setLastAppliedKey] = useState(appliedKey);
  if (appliedKey !== lastAppliedKey) {
    setLastAppliedKey(appliedKey);
    setFrom(applied.from ?? "");
    setTo(applied.to ?? "");
  }

  const clampYear = (value: string) => {
    if (value === "" || bounds === undefined) {
      return value;
    }
    const year = parseInt(value, 10);
    if (Number.isNaN(year)) {
      return "";
    }
    return String(Math.min(Math.max(year, bounds.minYear), bounds.maxYear));
  };

  const debouncedFrom = useDebouncedValue(from, 700);
  const debouncedTo = useDebouncedValue(to, 700);
  useEffect(() => {
    // let the user finish typing a year (the old portal's >= 3 digits rule)
    const settled = (value: string) => value === "" || value.length >= 3;
    if (!settled(debouncedFrom) || !settled(debouncedTo)) {
      return;
    }
    const nextFrom = clampYear(debouncedFrom);
    const nextTo = clampYear(debouncedTo);
    if (nextFrom !== (applied.from ?? "") || nextTo !== (applied.to ?? "")) {
      // the fields follow the clamped values through the resync above once the
      // filter is applied, so the effect only has to publish it
      onFilters(setRange(filters, def.code, nextFrom, nextTo, includeUndated));
    }
    // only a debounced edit may trigger this; the guard above absorbs the echo
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [debouncedFrom, debouncedTo]);

  // slider thumbs live inside the available bounds; unset fields sit at the edges
  const slider = bounds !== undefined && bounds.maxYear > bounds.minYear
    ? {
        min: bounds.minYear,
        max: bounds.maxYear,
        lo: Math.min(Math.max(parseInt(from, 10) || bounds.minYear, bounds.minYear), bounds.maxYear),
        hi: Math.min(Math.max(parseInt(to, 10) || bounds.maxYear, bounds.minYear), bounds.maxYear),
      }
    : undefined;
  const percent = (value: number) =>
    slider ? ((value - slider.min) / (slider.max - slider.min)) * 100 : 0;
  // how many records a dating filter would leave out for having no date at all,
  // shown only while such a filter is on and there are any
  const undatedOffer =
    (applied.from !== undefined || applied.to !== undefined) &&
    dating?.undatedCount !== undefined &&
    dating.undatedCount > 0
      ? Number(dating.undatedCount)
      : undefined;

  return (
    <>
      {slider && (
        <>
          <div className={styles.sliderWrap}>
            <div className={styles.sliderTrack} />
            <div
              className={styles.sliderFill}
              style={{
                left: `calc(8px + ${percent(slider.lo)} * (100% - 16px) / 100)`,
                width: `calc(${percent(slider.hi) - percent(slider.lo)} * (100% - 16px) / 100)`,
              }}
            />
            <input
              type="range"
              className={styles.sliderInput}
              aria-label={t("facets.sliderFromYear", { facet: def.label })}
              min={slider.min}
              max={slider.max}
              value={slider.lo}
              onChange={(e) => setFrom(String(Math.min(Number(e.target.value), slider.hi)))}
            />
            <input
              type="range"
              className={styles.sliderInput}
              aria-label={t("facets.sliderToYear", { facet: def.label })}
              min={slider.min}
              max={slider.max}
              value={slider.hi}
              onChange={(e) => setTo(String(Math.max(Number(e.target.value), slider.lo)))}
            />
          </div>
          <div className={styles.sliderBounds}>
            <Text size={200}>{slider.min}</Text>
            <Text size={200}>{slider.max}</Text>
          </div>
        </>
      )}
      <div className={styles.rangeRow}>
        <Text size={200} aria-hidden="true">
          {t("facets.from")}
        </Text>
        <Input
          className={styles.rangeInput}
          type="number"
          value={from}
          aria-label={t("facets.fromYear", { facet: def.label })}
          placeholder={bounds !== undefined ? String(bounds.minYear) : undefined}
          onChange={(_, data) => setFrom(data.value)}
        />
        <Text size={200} aria-hidden="true">
          {t("facets.to")}
        </Text>
        <Input
          className={styles.rangeInput}
          type="number"
          value={to}
          aria-label={t("facets.toYear", { facet: def.label })}
          placeholder={bounds !== undefined ? String(bounds.maxYear) : undefined}
          onChange={(_, data) => setTo(data.value)}
        />
      </div>
      {/* Only where the choice exists: undated records are dropped by a dating
          filter, so without one they are already in the results, and where every
          record is dated there is nothing to add back. Whether it is worth
          offering is a property of the data, which is why the count decides. */}
      {undatedOffer !== undefined && (
        <Checkbox
          checked={includeUndated}
          onChange={() => onFilters(setIncludeUndated(filters, def.code, !includeUndated))}
          label={t("facets.includeUndated", { count: undatedOffer })}
        />
      )}
    </>
  );
}

/**
 * Reference facet without buckets: the reader names the record a result must be
 * related to. Its options come from an ordinary record search rather than from
 * facet options, because this facet spans every reference item type - a union of
 * their terms is not something an engine enumerates cheaply, and the reader is
 * looking for one record anyway.
 *
 * The current search's own filters deliberately do not narrow the picker: the
 * record to relate to need not be among the results being narrowed. The cost is
 * that each (debounced) keystroke also computes the general search's facets,
 * which this widget throws away - the search endpoint always answers with the
 * facets of the scope it was asked about.
 */
function RelatedFacet({
  def,
  filters,
  onFilters,
}: Pick<Props, "def" | "filters" | "onFilters">) {
  const styles = useStyles();
  const { t } = useTranslation();
  const [q, setQ] = useState("");
  const debouncedQ = useDebouncedValue(q.trim(), 300);
  const selected = relatedOf(filters).flatMap((filter) => filter.apus);

  const options = useQuery({
    queryKey: ["related-options", debouncedQ],
    queryFn: () =>
      searchApi.searchSearch({
        apuSearchRequest: { query: debouncedQ, size: RELATED_OPTIONS },
      }),
    enabled: debouncedQ.length > 0,
    placeholderData: (previous) => previous,
  });
  const hits = (options.data?.items ?? []).filter((hit) => !selected.includes(hit.uuid));

  return (
    <>
      {selected.map((apu) => (
        <RelatedChip
          key={apu}
          apu={apu}
          onRemove={() => onFilters(removeRelated(filters, apu))}
        />
      ))}
      <Input
        size="small"
        value={q}
        aria-label={t("facets.relatedFor", { facet: def.label })}
        placeholder={t("facets.relatedPlaceholder")}
        onChange={(_, data) => setQ(data.value)}
      />
      {debouncedQ.length > 0 &&
        (hits.length > 0 ? (
          hits.map((hit) => (
            <button
              key={hit.uuid}
              type="button"
              className={styles.relatedOption}
              onClick={() => onFilters(addRelated(filters, hit.uuid))}
            >
              <span className={styles.bucketLabel}>
                <span>{hit.name}</span>
                <Text size={200} className={styles.count}>
                  {t(`sections.${hit.apuType}`)}
                </Text>
              </span>
            </button>
          ))
        ) : (
          <Text size={200}>{t(options.isPending ? "search.loading" : "facets.relatedNoMatch")}</Text>
        ))}
    </>
  );
}
