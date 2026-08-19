import { makeStyles, Title1, tokens } from "@fluentui/react-components";
import { useTranslation } from "react-i18next";
import { Link } from "react-router-dom";

const useStyles = makeStyles({
  root: {
    display: "flex",
    flexDirection: "column",
    gap: tokens.spacingVerticalL,
  },
  homeLink: {
    color: tokens.colorBrandForegroundLink,
  },
});

/**
 * Client-side fallback for in-app navigation to an unknown route. Direct hits on
 * unknown URLs never reach the SPA - the server enumerates the route families
 * and returns 404 itself.
 */
export default function NotFoundPage() {
  const styles = useStyles();
  const { t } = useTranslation();

  return (
    <div className={styles.root}>
      <Title1 as="h1">{t("notFound.heading")}</Title1>
      <Link to="/" className={styles.homeLink}>
        {t("notFound.home")}
      </Link>
    </div>
  );
}
