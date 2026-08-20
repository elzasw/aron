import { makeStyles, tokens } from "@fluentui/react-components";
import { useTranslation } from "react-i18next";
import { useApuDetail } from "../apu/useApuDetail";
import type { SearchFilter } from "../api/generated";
import { relatedOf, removeRelated } from "./filters";

const useStyles = makeStyles({
  group: {
    display: "flex",
    flexDirection: "column",
    gap: tokens.spacingVerticalXS,
  },
  title: {
    fontSize: tokens.fontSizeBase300,
    fontWeight: tokens.fontWeightSemibold,
    margin: 0,
  },
  chip: {
    display: "flex",
    alignItems: "center",
    justifyContent: "space-between",
    gap: tokens.spacingHorizontalXS,
    padding: `${tokens.spacingVerticalXXS} ${tokens.spacingHorizontalS}`,
    borderRadius: tokens.borderRadiusMedium,
    backgroundColor: tokens.colorNeutralBackground3,
    fontSize: tokens.fontSizeBase200,
  },
  remove: {
    border: "none",
    background: "none",
    cursor: "pointer",
    padding: tokens.spacingHorizontalXXS,
    color: tokens.colorNeutralForeground2,
    fontSize: tokens.fontSizeBase300,
    lineHeight: tokens.lineHeightBase300,
    ":hover": {
      color: tokens.colorNeutralForeground1,
    },
  },
});

/**
 * The active relation constraints, shown in the sidebar even where no facets
 * exist (the general search): a constraint the reader did not set from the
 * sidebar must still be visible there, and undoable.
 */
export default function RelatedChips({
  filters,
  onFilters,
}: {
  filters: SearchFilter[];
  onFilters: (next: SearchFilter[]) => void;
}) {
  const styles = useStyles();
  const { t } = useTranslation();
  const apus = relatedOf(filters).flatMap((filter) => filter.apus);

  if (apus.length === 0) {
    return null;
  }
  return (
    <div className={styles.group} role="group" aria-label={t("search.activeFilters")}>
      <h2 className={styles.title}>{t("search.relatedTo")}</h2>
      {apus.map((apu) => (
        <RelatedChip
          key={apu}
          apu={apu}
          onRemove={() => onFilters(removeRelated(filters, apu))}
        />
      ))}
    </div>
  );
}

/**
 * One constraint. The record's name is resolved from the record itself rather
 * than carried in the URL, so it is right even after a rename; the uuid stands
 * in until it arrives (and stays if the record is gone).
 */
function RelatedChip({ apu, onRemove }: { apu: string; onRemove: () => void }) {
  const styles = useStyles();
  const { t } = useTranslation();
  const { data } = useApuDetail(apu);
  const label = data?.name ?? apu;

  return (
    <div className={styles.chip}>
      <span>{label}</span>
      <button
        type="button"
        className={styles.remove}
        onClick={onRemove}
        aria-label={`${t("search.relatedRemove")}: ${label}`}
      >
        ✕
      </button>
    </div>
  );
}
