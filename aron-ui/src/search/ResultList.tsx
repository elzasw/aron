import { makeStyles, Text, tokens } from "@fluentui/react-components";
import { useTranslation } from "react-i18next";
import { Link } from "react-router-dom";
import { ApuSearchItem, MenuItemCode } from "../api/generated";
import { SECTIONS } from "../sections";
import StructuredResultCard from "./StructuredResultCard";
import { useResultLayout } from "./useResultLayout";

const useStyles = makeStyles({
  list: {
    display: "flex",
    flexDirection: "column",
    gap: tokens.spacingVerticalM,
    listStyleType: "none",
    margin: "0",
    padding: "0",
  },
  card: {
    display: "flex",
    backgroundColor: tokens.colorNeutralBackground1,
    border: `1px solid ${tokens.colorNeutralStroke2}`,
    borderRadius: tokens.borderRadiusMedium,
    boxShadow: tokens.shadow2,
    overflow: "hidden",
    // a card with a thumbnail stacks instead of squeezing the text on narrow screens
    flexWrap: "wrap",
  },
  accent: {
    width: "56px",
    flexShrink: 0,
  },
  body: {
    display: "flex",
    flexDirection: "column",
    gap: tokens.spacingVerticalXS,
    padding: `${tokens.spacingVerticalM} ${tokens.spacingHorizontalL}`,
    flexGrow: 1,
    minWidth: "0",
  },
  heading: {
    margin: "0",
    fontSize: tokens.fontSizeBase400,
    fontWeight: tokens.fontWeightSemibold,
  },
  name: {
    color: tokens.colorBrandForegroundLink,
    textDecorationLine: "none",
    ":hover": {
      textDecorationLine: "underline",
    },
  },
  empty: {
    padding: tokens.spacingVerticalXXL,
  },
});

/** Accent color of a result card: the section palette by APU type. */
const TYPE_ACCENTS: Record<string, string> = {
  INSTITUTION: SECTIONS[MenuItemCode.Institution].defaultColor,
  FUND: SECTIONS[MenuItemCode.Fund].defaultColor,
  FINDING_AID: SECTIONS[MenuItemCode.FindingAid].defaultColor,
  ARCH_DESC: SECTIONS[MenuItemCode.ArchDesc].defaultColor,
  ENTITY: SECTIONS[MenuItemCode.Entity].defaultColor,
};

/**
 * The result page as a list of record cards.
 *
 * A record whose source system delivered a structured presentation gets it; a
 * record without one keeps the name-and-description card. That decision is per
 * record and needs no deployment flag - the data already answers it, and a
 * partially reimported corpus stays readable.
 */
export default function ResultList({ items }: { items: ApuSearchItem[] }) {
  const styles = useStyles();
  const { t } = useTranslation();
  const layout = useResultLayout();

  if (items.length === 0) {
    return (
      <Text className={styles.empty} as="p">
        {t("search.noResults")}
      </Text>
    );
  }
  return (
    <ul className={styles.list}>
      {items.map((item) => (
        <li key={item.uuid} className={styles.card}>
          <div
            className={styles.accent}
            style={{ backgroundColor: TYPE_ACCENTS[item.apuType] ?? "#8ea3bd" }}
          />
          {item.structured ? (
            <StructuredResultCard
              uuid={item.uuid}
              name={item.name}
              structured={item.structured}
              layout={layout}
            />
          ) : (
            <div className={styles.body}>
              <h3 className={styles.heading}>
                <Link to={`/apu/${item.uuid}`} className={styles.name}>
                  {item.name}
                </Link>
              </h3>
              {item.description && <Text size={300}>{item.description}</Text>}
            </div>
          )}
        </li>
      ))}
    </ul>
  );
}
