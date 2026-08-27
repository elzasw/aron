import {
  Button,
  Menu,
  MenuItemLink,
  MenuList,
  MenuPopover,
  MenuTrigger,
  makeStyles,
  mergeClasses,
  tokens,
} from "@fluentui/react-components";
import { Navigation24Regular } from "@fluentui/react-icons";
import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import { Link, NavLink, useHref, useLocation, useNavigate } from "react-router-dom";
import { logoUrl } from "../api/client";
import { useUiConfig } from "../api/useUiConfig";
import { MenuItem } from "../api/generated";
import { SECTIONS } from "../sections";
import LanguageSwitcher from "./LanguageSwitcher";
import { PRIMARY_DARK, PRIMARY_MAIN } from "./palette";
import useMediaQuery from "./useMediaQuery";

export { PRIMARY_DARK, PRIMARY_MAIN } from "./palette";

/**
 * Below this the section tabs give way to one menu button (the old portal's
 * hamburger): a row of tabs wrapped into two or three lines would take a phone
 * screen's height from the content. Matches the stacking breakpoint used by
 * the pages. A JS decision, because it changes what is rendered.
 */
const COMPACT_QUERY = "(max-width: 860px)";

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
    // the frame is a one-viewport flex column; a page that overflows it would
    // otherwise squeeze the header down to that minimum, and a wrapped second
    // row of tabs would then be painted underneath the breadcrumb strip
    flexShrink: 0,
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
  compactNav: {
    display: "flex",
    alignItems: "center",
    marginLeft: "auto",
  },
  compactTrigger: {
    color: "#ffffff",
    minWidth: "40px",
    ":hover": {
      backgroundColor: PRIMARY_MAIN,
      color: "#ffffff",
    },
    ":hover:active": {
      backgroundColor: PRIMARY_MAIN,
      color: "#ffffff",
    },
    // the focus ring must stay visible against the dark header
    ":focus-visible": {
      outline: "2px solid #ffffff",
      outlineOffset: "2px",
    },
  },
  // the section's accent colour, which the tabs carry as their underline
  swatch: {
    display: "inline-block",
    width: "12px",
    height: "12px",
    borderRadius: tokens.borderRadiusSmall,
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
  const { data: config } = useUiConfig();
  const name = config?.name ?? t("app.title");
  const compact = useMediaQuery(COMPACT_QUERY);
  const menuItems = config?.menuItems ?? [];

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
        {/* names the link when the logo carries the wordmark; the page's own
            heading is its h1, so the portal name must not take that role */}
        {!logoFailed && <span className={styles.screenReaderOnly}>{name}</span>}
      </Link>
      {compact ? (
        menuItems.length > 0 && <CompactMenu items={menuItems} />
      ) : (
        <nav className={styles.nav} aria-label={t("nav.main")}>
          {menuItems.map((item) => (
            <HeaderMenuItem key={item.code} item={item} />
          ))}
        </nav>
      )}
      <LanguageSwitcher localizations={config?.localizations ?? []} />
    </header>
  );
}

/**
 * The same menu as one button on a narrow viewport: the sections as menu
 * links, each with its accent colour as a swatch where the tab had an
 * underline. Still the page's main navigation landmark.
 */
function CompactMenu({ items }: { items: MenuItem[] }) {
  const styles = useStyles();
  const { t } = useTranslation();
  return (
    <nav className={styles.compactNav} aria-label={t("nav.main")}>
      <Menu>
        <MenuTrigger disableButtonEnhancement>
          <Button
            appearance="transparent"
            icon={<Navigation24Regular />}
            aria-label={t("nav.main")}
            className={styles.compactTrigger}
          />
        </MenuTrigger>
        <MenuPopover>
          <MenuList>
            {items.map((item) => (
              <CompactMenuItem key={item.code} item={item} />
            ))}
          </MenuList>
        </MenuPopover>
      </Menu>
    </nav>
  );
}

function CompactMenuItem({ item }: { item: MenuItem }) {
  const styles = useStyles();
  const { t } = useTranslation();
  const navigate = useNavigate();
  const { pathname } = useLocation();
  const section = SECTIONS[item.code];
  const route = item.url === undefined ? section.route : undefined;
  // the router's own href, so the link is right under a deployment prefix too
  const href = useHref(route ?? "/");
  const swatch = (
    <span
      aria-hidden="true"
      className={styles.swatch}
      style={{ backgroundColor: item.color ?? section.defaultColor }}
    />
  );
  if (route === undefined) {
    return (
      <MenuItemLink href={item.url ?? ""} icon={swatch}>
        {t(section.labelKey)}
      </MenuItemLink>
    );
  }
  const active = pathname === route || pathname.startsWith(`${route}/`);
  return (
    <MenuItemLink
      href={href}
      icon={swatch}
      aria-current={active ? "page" : undefined}
      onClick={(event) => {
        // an in-app destination goes through the router, like the tabs
        event.preventDefault();
        void navigate(route);
      }}
    >
      {t(section.labelKey)}
    </MenuItemLink>
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
