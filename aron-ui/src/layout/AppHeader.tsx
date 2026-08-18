import { makeStyles, mergeClasses, tokens } from "@fluentui/react-components";
import { useQuery } from "@tanstack/react-query";
import { useEffect, useMemo, useState } from "react";
import { useTranslation } from "react-i18next";
import { Link, NavLink } from "react-router-dom";
import { logoUrl, uiApi } from "../api/client";
import { MenuItem } from "../api/generated";
import { languageOf, offeredLanguages, setLanguage } from "../i18n";
import { SECTIONS } from "../sections";

/**
 * Default primary palette of the original portal (its theme's primary.dark and
 * primary.main): the slate of the header and primary buttons. Section accents
 * are configured separately (menu colors of /api/v1/ui/config).
 */
export const PRIMARY_DARK = "hsl(210, 20%, 20%)";
export const PRIMARY_MAIN = "hsl(210, 20%, 30%)";

const useStyles = makeStyles({
  header: {
    display: "flex",
    alignItems: "stretch",
    flexWrap: "wrap",
    gap: tokens.spacingHorizontalXXL,
    padding: `0 ${tokens.spacingHorizontalXXL}`,
    backgroundColor: PRIMARY_DARK,
    color: "#ffffff",
    minHeight: "72px",
    "@media (max-width: 860px)": {
      padding: `0 ${tokens.spacingHorizontalM}`,
      gap: tokens.spacingHorizontalM,
    },
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
    // deployments supply arbitrary SVG/PNG logos - constrain both dimensions
    height: "48px",
    maxWidth: "300px",
    width: "auto",
    objectFit: "contain",
  },
  title: {
    fontSize: tokens.fontSizeHero800,
    fontWeight: tokens.fontWeightBold,
    lineHeight: "1.1",
  },
  screenReaderOnly: {
    position: "absolute",
    width: "1px",
    height: "1px",
    overflow: "hidden",
    clipPath: "inset(50%)",
    whiteSpace: "nowrap",
  },
  nav: {
    display: "flex",
    alignItems: "stretch",
    flexWrap: "wrap",
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
      backgroundColor: PRIMARY_MAIN,
    },
  },
  navItemActive: {
    backgroundColor: PRIMARY_MAIN,
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
    backgroundColor: "transparent",
    color: "#ffffff",
    fontFamily: "inherit",
    fontSize: tokens.fontSizeBase200,
    fontWeight: tokens.fontWeightSemibold,
    cursor: "pointer",
    ":hover": {
      backgroundColor: PRIMARY_MAIN,
    },
    // the focus ring must stay visible against the dark header
    ":focus-visible": {
      outline: "2px solid #ffffff",
      outlineOffset: "2px",
    },
  },
  languageBadgeActive: {
    backgroundColor: "#ffffff",
    color: PRIMARY_DARK,
    cursor: "default",
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
  const name = config?.name ?? t("app.title");

  useEffect(() => {
    if (config) {
      document.title = config.name;
    }
  }, [config]);

  return (
    <header className={styles.header}>
      <Link to="/" className={styles.brand}>
        {/* the logo carries the visual brand; the name is shown only without it
            (old-portal behavior - deployment logos typically contain the wordmark) */}
        {!logoFailed && (
          <img className={styles.logo} src={logoUrl} alt="" onError={() => setLogoFailed(true)} />
        )}
        {logoFailed && <span className={styles.title}>{name}</span>}
        <h1 className={styles.screenReaderOnly}>{name}</h1>
      </Link>
      <nav className={styles.nav} aria-label={t("nav.main")}>
        {(config?.menuItems ?? []).map((item) => (
          <HeaderMenuItem key={item.code} item={item} />
        ))}
      </nav>
      <LanguageSwitcher localizations={config?.localizations ?? []} />
    </header>
  );
}

/**
 * Language choice of the reader. Offered languages are those the deployment
 * declares (`localizations` of /api/v1/ui/config) that this build also has
 * strings for - a deployment can therefore add a language only once both sides
 * have it. A single offered language needs no control.
 *
 * Switching re-renders the UI strings and, because the language is part of
 * their react-query keys, refetches the server-rendered text (labels, datings).
 */
function LanguageSwitcher({ localizations }: { localizations: string[] }) {
  const styles = useStyles();
  const { t, i18n } = useTranslation();
  const offered = useMemo(() => offeredLanguages(localizations), [localizations]);
  const active = languageOf(i18n.language);

  useEffect(() => {
    // the reader's remembered (or browser-preferred) language may not be one
    // this deployment offers - fall back to its first rather than show a
    // language the server will not render text in
    if (offered.length > 0 && !offered.some((language) => language === active)) {
      setLanguage(offered[0]);
    }
  }, [offered, active]);

  if (offered.length < 2) {
    return null;
  }
  return (
    <div className={styles.language} role="group" aria-label={t("nav.language")}>
      {offered.map((language) => (
        <button
          key={language}
          type="button"
          lang={language}
          className={
            language === active
              ? mergeClasses(styles.languageBadge, styles.languageBadgeActive)
              : styles.languageBadge
          }
          aria-pressed={language === active}
          aria-label={t(`nav.languageName.${language}`)}
          title={t(`nav.languageName.${language}`)}
          onClick={() => setLanguage(language)}
        >
          {language.toUpperCase()}
        </button>
      ))}
    </div>
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
