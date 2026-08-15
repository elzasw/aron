import { makeStyles, Text, Title3, tokens } from "@fluentui/react-components";
import { useQuery } from "@tanstack/react-query";
import { useTranslation } from "react-i18next";
import { Link, Outlet } from "react-router-dom";
import { systemApi } from "../api/client";

const useStyles = makeStyles({
  root: {
    display: "flex",
    flexDirection: "column",
    minHeight: "100%",
    backgroundColor: tokens.colorNeutralBackground2,
  },
  header: {
    display: "flex",
    alignItems: "center",
    gap: tokens.spacingHorizontalL,
    padding: `${tokens.spacingVerticalM} ${tokens.spacingHorizontalXXL}`,
    backgroundColor: tokens.colorNeutralBackground1,
    borderBottom: `1px solid ${tokens.colorNeutralStroke2}`,
  },
  titleLink: {
    color: tokens.colorNeutralForeground1,
    textDecorationLine: "none",
  },
  main: {
    flexGrow: 1,
    padding: `${tokens.spacingVerticalXXL} ${tokens.spacingHorizontalXXL}`,
  },
  footer: {
    padding: `${tokens.spacingVerticalS} ${tokens.spacingHorizontalXXL}`,
    color: tokens.colorNeutralForeground3,
    borderTop: `1px solid ${tokens.colorNeutralStroke2}`,
  },
});

/**
 * Application frame: header with the portal title, routed content, footer with
 * the running backend's name and version (the first consumer of the generated
 * /api/v1 client).
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
      <header className={styles.header}>
        <Link to="/" className={styles.titleLink}>
          <Title3>{t("app.title")}</Title3>
        </Link>
      </header>
      <main className={styles.main}>
        <Outlet />
      </main>
      <footer className={styles.footer}>
        <Text size={200}>
          {info ? t("app.footer.version", { name: info.name, version: info.version }) : " "}
        </Text>
      </footer>
    </div>
  );
}
