import { makeStyles, Text, Title1, tokens } from "@fluentui/react-components";
import { useTranslation } from "react-i18next";

const useStyles = makeStyles({
  root: {
    display: "flex",
    flexDirection: "column",
    gap: tokens.spacingVerticalL,
  },
});

/** Placeholder for the portal home page (search entry, favorite queries, news). */
export default function HomePage() {
  const styles = useStyles();
  const { t } = useTranslation();

  return (
    <div className={styles.root}>
      <Title1>{t("home.heading")}</Title1>
      <Text>{t("home.placeholder")}</Text>
    </div>
  );
}
