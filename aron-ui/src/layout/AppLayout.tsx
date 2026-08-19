import { makeStyles, Text, tokens } from "@fluentui/react-components";
import { useQuery } from "@tanstack/react-query";
import { useEffect, useRef } from "react";
import { useTranslation } from "react-i18next";
import { Outlet, useLocation } from "react-router-dom";
import { systemApi } from "../api/client";
import { useUiConfig } from "../api/useUiConfig";
import ApiErrorBar from "../errors/ApiErrorBar";
import AppHeader from "./AppHeader";
import Breadcrumbs from "./Breadcrumbs";

/** Target of the skip link; also the element focused after a route change. */
const MAIN_ID = "main-content";

const useStyles = makeStyles({
  root: {
    display: "flex",
    flexDirection: "column",
    minHeight: "100%",
    backgroundColor: tokens.colorNeutralBackground1,
  },
  // off-screen until focused: the first Tab of the page reveals it
  skipLink: {
    position: "absolute",
    left: "-9999px",
    top: "0",
    zIndex: 100,
    padding: `${tokens.spacingVerticalS} ${tokens.spacingHorizontalL}`,
    backgroundColor: tokens.colorNeutralBackground1,
    color: tokens.colorNeutralForeground1,
    borderRadius: tokens.borderRadiusMedium,
    ":focus": {
      left: tokens.spacingHorizontalM,
      top: tokens.spacingVerticalM,
      outline: `2px solid ${tokens.colorStrokeFocus2}`,
    },
  },
  main: {
    flexGrow: 1,
    display: "flex",
    flexDirection: "column",
    // focused programmatically after a route change - no ring for a region
    ":focus": {
      outline: "none",
    },
  },
  footer: {
    display: "flex",
    flexWrap: "wrap",
    alignItems: "center",
    gap: `${tokens.spacingVerticalXS} ${tokens.spacingHorizontalL}`,
    padding: `${tokens.spacingVerticalS} ${tokens.spacingHorizontalXXL}`,
    color: tokens.colorNeutralForeground3,
    borderTop: `1px solid ${tokens.colorNeutralStroke2}`,
    "@media (max-width: 860px)": {
      padding: `${tokens.spacingVerticalS} ${tokens.spacingHorizontalM}`,
    },
  },
  footerLinks: {
    display: "flex",
    flexWrap: "wrap",
    gap: `${tokens.spacingVerticalXS} ${tokens.spacingHorizontalL}`,
  },
  footerLink: {
    color: tokens.colorNeutralForeground2,
    fontSize: tokens.fontSizeBase200,
  },
  version: {
    marginLeft: "auto",
  },
});

/**
 * Application frame: configuration-driven header, breadcrumb strip, routed
 * content, footer with the deployment's published links and the running
 * backend's version.
 *
 * Accessibility (doc/accessibility.md): the skip link lets a keyboard user pass
 * the repeated header, and a route change parks focus on the main region so a
 * screen reader reads the new page instead of staying in the old context.
 */
export default function AppLayout() {
  const styles = useStyles();
  const { t } = useTranslation();
  const location = useLocation();
  const mainRef = useRef<HTMLElement>(null);
  const initialRender = useRef(true);
  const { data: info } = useQuery({
    queryKey: ["system-info"],
    queryFn: () => systemApi.systemGetInfo(),
  });
  const { data: config } = useUiConfig();

  useEffect(() => {
    if (initialRender.current) {
      initialRender.current = false;
      return;
    }
    mainRef.current?.focus();
  }, [location.pathname]);

  const focusMain = () => mainRef.current?.focus();
  const footerLinks = config?.footerLinks ?? [];

  return (
    <div className={styles.root}>
      {/* the fragment href documents the target; focus is moved by hand because
          the SPA shell carries a <base href>, against which a bare #fragment
          would resolve to the portal root instead of the current page */}
      <a
        className={styles.skipLink}
        href={`#${MAIN_ID}`}
        onClick={(event) => {
          event.preventDefault();
          focusMain();
        }}
      >
        {t("nav.skipToContent")}
      </a>
      <AppHeader />
      <ApiErrorBar />
      <Breadcrumbs />
      <main id={MAIN_ID} ref={mainRef} tabIndex={-1} className={styles.main}>
        <Outlet />
      </main>
      <footer className={styles.footer}>
        {footerLinks.length > 0 && (
          <nav aria-label={t("app.footer.label")} className={styles.footerLinks}>
            {footerLinks.map((link) => (
              <a key={link.url} className={styles.footerLink} href={link.url}>
                {/* a well-known link is labelled by the UI in the reader's
                    language; a free link brings its own label */}
                {link.label ?? (link.code ? t(`app.footer.link.${link.code}`) : link.url)}
              </a>
            ))}
          </nav>
        )}
        <Text size={200} className={styles.version}>
          {info ? t("app.footer.version", { name: info.name, version: info.version }) : " "}
        </Text>
      </footer>
    </div>
  );
}
