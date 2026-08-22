import { makeStyles, Text, tokens, useId } from "@fluentui/react-components";
import { useTranslation } from "react-i18next";
import { Link } from "react-router-dom";
import { ApuSearchItem } from "../api/generated";
import StructuredResultCard from "./StructuredResultCard";
import { useResultLayout } from "./useResultLayout";

const useStyles = makeStyles({
  // the results are a section of the page and each card a subsection of it; the
  // heading that says so is read, not seen - the count is already on screen in
  // the status line above, and a second visible one would only repeat it
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
  list: {
    display: "flex",
    flexDirection: "column",
    gap: tokens.spacingVerticalM,
    listStyleType: "none",
    margin: "0",
    padding: "0",
  },
  // one row: the icon strip and, beside it, everything else. It must not wrap -
  // a strip pushed onto a line of its own is only as tall as that line instead
  // of the whole card. What stacks on a narrow screen stacks inside the content.
  card: {
    display: "flex",
    backgroundColor: tokens.colorNeutralBackground1,
    border: `1px solid ${tokens.colorNeutralStroke2}`,
    borderRadius: tokens.borderRadiusMedium,
    boxShadow: tokens.shadow2,
    overflow: "hidden",
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
  const headingId = useId("search-results-");

  if (items.length === 0) {
    return (
      <Text className={styles.empty} as="p">
        {t("search.noResults")}
      </Text>
    );
  }
  return (
    <>
      <h2 id={headingId} className={styles.srOnly}>
        {t("search.resultsHeading")}
      </h2>
      <ul className={styles.list} aria-labelledby={headingId}>
        {items.map((item) => (
          <li key={item.uuid} className={styles.card}>
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
    </>
  );
}
