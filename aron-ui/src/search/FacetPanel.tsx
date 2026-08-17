import { Button, Checkbox, Input, Link, makeStyles, Spinner, Text, tokens } from "@fluentui/react-components";
import { useQuery } from "@tanstack/react-query";
import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import { searchApi } from "../api/client";
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
    fontWeight: tokens.fontWeightSemibold,
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
  onFilters: (filters: SearchFilter[]) => void;
}

/**
 * One facet of the sidebar; the widget follows the facet type. Bucket counts
 * and dating bounds come server-computed with multi-select semantics.
 */
export default function FacetPanel({ def, filters, result, apuType, query, onFilters }: Props) {
  const styles = useStyles();

  return (
    <div className={styles.facet} title={def.tooltip}>
      <Text className={styles.title}>{def.label}</Text>
      {def.type === FacetType.Enum && (
        <EnumFacet def={def} filters={filters} result={result} onFilters={onFilters} />
      )}
      {def.type === FacetType.MultiRef && (
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
  useEffect(() => setValue(applied), [applied]);

  // the filter applies while typing (old-portal behavior, same 700 ms
  // debounce); Enter just applies immediately
  const debounced = useDebouncedValue(value, 700);
  useEffect(() => {
    // fires on the debounced keystroke only; the guard keeps the apply-echo
    // (applied catching up with debounced) from re-applying
    if (debounced !== applied) {
      onFilters(setText(filters, def.code, debounced));
    }
  }, [debounced]); // deliberately not on applied/filters - see the guard

  return (
    <Input
      value={value}
      placeholder={t("facets.textPlaceholder")}
      onChange={(_, data) => setValue(data.value)}
      onKeyDown={(e) => e.key === "Enter" && onFilters(setText(filters, def.code, value))}
    />
  );
}

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
  useEffect(() => {
    setFrom(applied.from ?? "");
    setTo(applied.to ?? "");
  }, [applied.from, applied.to]);
  const apply = () => onFilters(setRange(filters, def.code, from, to));

  return (
    <>
      <div className={styles.rangeRow}>
        <Text size={200}>{t("facets.from")}</Text>
        <Input
          className={styles.rangeInput}
          value={from}
          placeholder={bounds !== undefined ? String(bounds.minYear) : undefined}
          onChange={(_, data) => setFrom(data.value)}
          onKeyDown={(e) => e.key === "Enter" && apply()}
        />
        <Text size={200}>{t("facets.to")}</Text>
        <Input
          className={styles.rangeInput}
          value={to}
          placeholder={bounds !== undefined ? String(bounds.maxYear) : undefined}
          onChange={(_, data) => setTo(data.value)}
          onKeyDown={(e) => e.key === "Enter" && apply()}
        />
        <Button size="small" onClick={apply}>
          {t("facets.apply")}
        </Button>
      </div>
      {bounds !== undefined && (
        <Text size={200} className={styles.count}>
          {t("facets.availableRange", { min: bounds.minYear, max: bounds.maxYear })}
        </Text>
      )}
    </>
  );
}
