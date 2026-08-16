import { makeStyles, mergeClasses, Text, tokens } from "@fluentui/react-components";
import { useQuery } from "@tanstack/react-query";
import { useState } from "react";
import { useTranslation } from "react-i18next";
import { Link, NavLink } from "react-router-dom";
import { logoUrl, uiApi } from "../api/client";
import { MenuItem } from "../api/generated";
import { SECTIONS } from "../sections";

/** Navy of the original portal header. */
export const HEADER_BACKGROUND = "#1d3c60";

const useStyles = makeStyles({
  header: {
    display: "flex",
    alignItems: "stretch",
    gap: tokens.spacingHorizontalXXL,
    padding: `0 ${tokens.spacingHorizontalXXL}`,
    backgroundColor: HEADER_BACKGROUND,
    color: "#ffffff",
    minHeight: "72px",
  },
  brand: {
    display: "flex",
    alignItems: "center",
    gap: tokens.spacingHorizontalM,
    color: "#ffffff",
    textDecorationLine: "none",
    paddingTop: tokens.spacingVerticalS,
    paddingBottom: tokens.spacingVerticalS,
  },
  logo: {
    height: "44px",
    width: "auto",
  },
  title: {
    fontSize: tokens.fontSizeHero800,
    fontWeight: tokens.fontWeightBold,
    lineHeight: "1.1",
  },
  nav: {
    display: "flex",
    alignItems: "stretch",
    marginLeft: "auto",
    gap: tokens.spacingHorizontalXS,
  },
  navItem: {
    display: "flex",
    alignItems: "center",
    padding: `0 ${tokens.spacingHorizontalL}`,
    color: "#ffffff",
    textDecorationLine: "none",
    fontWeight: tokens.fontWeightSemibold,
    borderBottom: "4px solid transparent",
    whiteSpace: "nowrap",
    ":hover": {
      backgroundColor: "rgba(255, 255, 255, 0.08)",
    },
  },
  navItemActive: {
    backgroundColor: "rgba(255, 255, 255, 0.14)",
  },
  language: {
    display: "flex",
    alignItems: "center",
    paddingLeft: tokens.spacingHorizontalL,
  },
  languageBadge: {
    border: "1px solid rgba(255, 255, 255, 0.6)",
    borderRadius: tokens.borderRadiusMedium,
    padding: `${tokens.spacingVerticalXXS} ${tokens.spacingHorizontalS}`,
    color: "#ffffff",
    fontWeight: tokens.fontWeightSemibold,
  },
});

/**
 * Portal header: logo + deployment name on the left, the configuration-driven
 * top-level menu on the right (sections and accent colors come from
 * /api/v1/ui/config; routes and labels from {@link SECTIONS}).
 */
export default function AppHeader() {
  const styles = useStyles();
  const { t } = useTranslation();
  const [logoFailed, setLogoFailed] = useState(false);
  const { data: config } = useQuery({
    queryKey: ["ui-config"],
    queryFn: () => uiApi.uiGetConfig(),
  });

  return (
    <header className={styles.header}>
      <Link to="/" className={styles.brand}>
        {!logoFailed && (
          <img className={styles.logo} src={logoUrl} alt="" onError={() => setLogoFailed(true)} />
        )}
        <span className={styles.title}>{config?.name ?? t("app.title")}</span>
      </Link>
      <nav className={styles.nav}>
        {(config?.menuItems ?? []).map((item) => (
          <HeaderMenuItem key={item.code} item={item} />
        ))}
        {config && config.localizations.length > 1 && (
          <div className={styles.language}>
            {/* language switcher arrives with the EN localization */}
            <Text className={styles.languageBadge} size={200} title="Čeština">
              CS
            </Text>
          </div>
        )}
      </nav>
    </header>
  );
}

function HeaderMenuItem({ item }: { item: MenuItem }) {
  const styles = useStyles();
  const { t } = useTranslation();
  const section = SECTIONS[item.code];
  const accent = { borderBottomColor: item.color ?? section.defaultColor };
  const label = t(section.labelKey);

  if (item.url !== undefined || section.route === undefined) {
    return (
      <a className={styles.navItem} style={accent} href={item.url}>
        {label}
      </a>
    );
  }
  return (
    <NavLink
      to={section.route}
      style={accent}
      className={({ isActive }) =>
        isActive ? mergeClasses(styles.navItem, styles.navItemActive) : styles.navItem
      }
    >
      {label}
    </NavLink>
  );
}
