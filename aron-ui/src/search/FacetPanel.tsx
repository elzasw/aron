import { Button, Checkbox, Input, Link, makeStyles, Text, tokens } from "@fluentui/react-components";
import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import { FacetBucket, FacetDef, FacetType, SearchFilter } from "../api/generated";
import { rangeOf, setRange, setText, textOf, toggleValue, valuesOf } from "./filters";

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
  buckets: FacetBucket[];
  onFilters: (filters: SearchFilter[]) => void;
}

/**
 * One facet of the sidebar; the widget follows the facet type. ENUM buckets
 * carry live counts with multi-select semantics (server-computed).
 */
export default function FacetPanel({ def, filters, buckets, onFilters }: Props) {
  const styles = useStyles();

  return (
    <div className={styles.facet} title={def.tooltip}>
      <Text className={styles.title}>{def.label}</Text>
      {def.type === FacetType.Enum && (
        <EnumFacet def={def} filters={filters} buckets={buckets} onFilters={onFilters} />
      )}
      {def.type === FacetType.Fulltext && (
        <TextFacet def={def} filters={filters} onFilters={onFilters} />
      )}
      {def.type === FacetType.Unitdate && (
        <RangeFacet def={def} filters={filters} onFilters={onFilters} />
      )}
    </div>
  );
}

function EnumFacet({ def, filters, buckets, onFilters }: Props) {
  const styles = useStyles();
  const { t } = useTranslation();
  const [expanded, setExpanded] = useState(false);
  const selected = valuesOf(filters, def.code);
  const collapsedCount = def.displayedItems && def.displayedItems > 0 ? def.displayedItems : 10;
  const visible = expanded ? buckets : buckets.slice(0, collapsedCount);

  return (
    <>
      {visible.map((bucket) => (
        <Checkbox
          key={bucket.value}
          checked={selected.includes(bucket.value)}
          onChange={() => onFilters(toggleValue(filters, def.code, bucket.value))}
          label={
            <span className={styles.bucketLabel}>
              <span>{bucket.label ?? bucket.value}</span>
              <Text size={200} className={styles.count}>
                ({bucket.count})
              </Text>
            </span>
          }
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

function TextFacet({ def, filters, onFilters }: Omit<Props, "buckets">) {
  const { t } = useTranslation();
  const applied = textOf(filters, def.code);
  const [value, setValue] = useState(applied);
  useEffect(() => setValue(applied), [applied]);

  return (
    <Input
      value={value}
      placeholder={t("facets.textPlaceholder")}
      onChange={(_, data) => setValue(data.value)}
      onBlur={() => onFilters(setText(filters, def.code, value))}
      onKeyDown={(e) => e.key === "Enter" && onFilters(setText(filters, def.code, value))}
    />
  );
}

function RangeFacet({ def, filters, onFilters }: Omit<Props, "buckets">) {
  const styles = useStyles();
  const { t } = useTranslation();
  const applied = rangeOf(filters, def.code);
  const [from, setFrom] = useState(applied.from ?? "");
  const [to, setTo] = useState(applied.to ?? "");
  useEffect(() => {
    setFrom(applied.from ?? "");
    setTo(applied.to ?? "");
  }, [applied.from, applied.to]);
  const apply = () => onFilters(setRange(filters, def.code, from, to));

  return (
    <div className={styles.rangeRow}>
      <Text size={200}>{t("facets.from")}</Text>
      <Input
        className={styles.rangeInput}
        value={from}
        onChange={(_, data) => setFrom(data.value)}
        onKeyDown={(e) => e.key === "Enter" && apply()}
      />
      <Text size={200}>{t("facets.to")}</Text>
      <Input
        className={styles.rangeInput}
        value={to}
        onChange={(_, data) => setTo(data.value)}
        onKeyDown={(e) => e.key === "Enter" && apply()}
      />
      <Button size="small" onClick={apply}>
        {t("facets.apply")}
      </Button>
    </div>
  );
}
