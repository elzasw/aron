import { makeStyles, Text, Title1, tokens } from "@fluentui/react-components";
import { useTranslation } from "react-i18next";
import { useSearchParams } from "react-router-dom";

const useStyles = makeStyles({
  root: {
    display: "flex",
    flexDirection: "column",
    gap: tokens.spacingVerticalL,
    padding: `${tokens.spacingVerticalXXL} ${tokens.spacingHorizontalXXL}`,
  },
});

/**
 * Placeholder of the general search page (/apu?q=...); the real result list and
 * facets arrive with the search slice.
 */
export default function SearchPage() {
  const styles = useStyles();
  const { t } = useTranslation();
  const [params] = useSearchParams();
  const query = params.get("q");

  return (
    <div className={styles.root}>
      <Title1>{t("nav.search")}</Title1>
      <Text>
        {query ? t("search.placeholderWithQuery", { query }) : t("search.placeholder")}
      </Text>
    </div>
  );
}
