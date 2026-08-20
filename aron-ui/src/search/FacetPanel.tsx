import { Checkbox, Input, Link, makeStyles, Spinner, Text, tokens } from "@fluentui/react-components";
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
import { rangeOf, serializeFilters, setRange, setText, textOf, toggleValue, valuesOf } from "./filters";
import { useDebouncedValue } from "./useDebouncedValue";

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
  /** Section of the search - facet options are section-scoped. */
  apuType: ApuType;
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
      {def.type === FacetType.Ref && (
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

function FacetCheckbox({
  row,
  checked,
  onToggle,
}: {
  row: OptionRow;
  checked: boolean;
  onToggle: () => void;
}) {
  const styles = useStyles();
  return (
    <Checkbox
      checked={checked}
      onChange={onToggle}
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
  const buckets = result?.kind === FacetResultKind.Enum ? (result as EnumFacetResult).buckets : [];
  const collapsedCount = def.displayedItems && def.displayedItems > 0 ? def.displayedItems : 10;
  const visible = expanded ? buckets : buckets.slice(0, collapsedCount);

  return (
    <>
      {visible.map((bucket) => (
        <FacetCheckbox
          key={bucket.value}
          row={bucket}
          checked={selected.includes(bucket.value)}
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
function RefFacet({ def, filters, result, apuType, query, onFilters }: Props) {
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
  const bounds = result?.kind === FacetResultKind.Dating
    ? (result as DatingFacetResult).bounds
    : undefined;
  const applied = rangeOf(filters, def.code);
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
      onFilters(setRange(filters, def.code, nextFrom, nextTo));
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
    </>
  );
}
