import { makeStyles, tokens } from "@fluentui/react-components";
import { useTranslation } from "react-i18next";
import { Link, useLocation } from "react-router-dom";
import { SECTIONS } from "../sections";

const useStyles = makeStyles({
  bar: {
    display: "flex",
    alignItems: "center",
    gap: tokens.spacingHorizontalS,
    padding: `${tokens.spacingVerticalM} ${tokens.spacingHorizontalXXL}`,
    backgroundColor: tokens.colorNeutralBackground1,
    borderBottom: `1px solid ${tokens.colorNeutralStroke2}`,
    fontSize: tokens.fontSizeBase300,
  },
  home: {
    color: tokens.colorBrandForegroundLink,
    textDecorationLine: "none",
    ":hover": {
      textDecorationLine: "underline",
    },
  },
  current: {
    color: tokens.colorNeutralForeground2,
  },
});

/**
 * Breadcrumb strip under the header: "Úvod / <section>". Hidden on the home
 * page. Detail pages get their hierarchy breadcrumb with a later slice.
 */
export default function Breadcrumbs() {
  const styles = useStyles();
  const { t } = useTranslation();
  const { pathname } = useLocation();

  if (pathname === "/") {
    return null;
  }
  const firstSegment = "/" + pathname.split("/")[1];
  const section = Object.values(SECTIONS).find((s) => s.route === firstSegment);
  const currentKey =
    section?.labelKey ?? (firstSegment === "/apu" ? "nav.search" : "notFound.heading");

  return (
    <div className={styles.bar}>
      <Link to="/" className={styles.home}>
        {t("nav.home")}
      </Link>
      <span className={styles.current}>/</span>
      <span className={styles.current}>{t(currentKey)}</span>
    </div>
  );
}
