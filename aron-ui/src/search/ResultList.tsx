import { makeStyles, Text, tokens } from "@fluentui/react-components";
import { useTranslation } from "react-i18next";
import { Link } from "react-router-dom";
import { ApuSearchItem, MenuItemCode } from "../api/generated";
import { SECTIONS } from "../sections";

const useStyles = makeStyles({
  list: {
    display: "flex",
    flexDirection: "column",
    gap: tokens.spacingVerticalM,
  },
  card: {
    display: "flex",
    backgroundColor: tokens.colorNeutralBackground1,
    border: `1px solid ${tokens.colorNeutralStroke2}`,
    borderRadius: tokens.borderRadiusMedium,
    boxShadow: tokens.shadow2,
    overflow: "hidden",
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
  },
  name: {
    color: tokens.colorBrandForegroundLink,
    fontWeight: tokens.fontWeightSemibold,
    fontSize: tokens.fontSizeBase400,
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

export default function ResultList({ items }: { items: ApuSearchItem[] }) {
  const styles = useStyles();
  const { t } = useTranslation();

  if (items.length === 0) {
    return (
      <Text className={styles.empty} as="p">
        {t("search.noResults")}
      </Text>
    );
  }
  return (
    <div className={styles.list}>
      {items.map((item) => (
        <div key={item.uuid} className={styles.card}>
          <div
            className={styles.accent}
            style={{ backgroundColor: TYPE_ACCENTS[item.apuType] ?? "#8ea3bd" }}
          />
          <div className={styles.body}>
            <Link to={`/apu/${item.uuid}`} className={styles.name}>
              {item.name}
            </Link>
            {item.description && <Text size={300}>{item.description}</Text>}
          </div>
        </div>
      ))}
    </div>
  );
}
