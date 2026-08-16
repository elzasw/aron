import { makeStyles, Text, tokens } from "@fluentui/react-components";
import { useQuery } from "@tanstack/react-query";
import { useTranslation } from "react-i18next";
import { Outlet } from "react-router-dom";
import { systemApi } from "../api/client";
import AppHeader from "./AppHeader";
import Breadcrumbs from "./Breadcrumbs";

const useStyles = makeStyles({
  root: {
    display: "flex",
    flexDirection: "column",
    minHeight: "100%",
    backgroundColor: tokens.colorNeutralBackground1,
  },
  main: {
    flexGrow: 1,
    display: "flex",
    flexDirection: "column",
  },
  footer: {
    padding: `${tokens.spacingVerticalS} ${tokens.spacingHorizontalXXL}`,
    color: tokens.colorNeutralForeground3,
    borderTop: `1px solid ${tokens.colorNeutralStroke2}`,
  },
});

/**
 * Application frame: configuration-driven header, breadcrumb strip, routed
 * content, footer with the running backend's name and version (the first
 * consumer of the generated /api/v1 client).
 */
export default function AppLayout() {
  const styles = useStyles();
  const { t } = useTranslation();
  const { data: info } = useQuery({
    queryKey: ["system-info"],
    queryFn: () => systemApi.systemGetInfo(),
  });

  return (
    <div className={styles.root}>
      <AppHeader />
      <Breadcrumbs />
      <main className={styles.main}>
        <Outlet />
      </main>
      <footer className={styles.footer}>
        <Text size={200}>
          {info ? t("app.footer.version", { name: info.name, version: info.version }) : " "}
        </Text>
      </footer>
    </div>
  );
}
