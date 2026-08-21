import { Button, Card, Input, makeStyles, tokens } from "@fluentui/react-components";
import { useState } from "react";
import { useTranslation } from "react-i18next";
import { useNavigate } from "react-router-dom";
import { useUiConfig } from "../api/useUiConfig";
import HomeFooterBand from "../home/HomeFooterBand";
import HomeTileGroups from "../home/HomeTileGroups";
import { PRIMARY_DARK, PRIMARY_MAIN } from "../layout/AppHeader";

const useStyles = makeStyles({
  // the band is full-bleed, so the page pads its own content instead of itself
  root: {
    display: "flex",
    flexDirection: "column",
    alignItems: "center",
  },
  content: {
    display: "flex",
    flexDirection: "column",
    alignItems: "center",
    gap: tokens.spacingVerticalXXXL,
    width: "100%",
    maxWidth: "1100px",
    padding: `${tokens.spacingVerticalXXXL} ${tokens.spacingHorizontalXXL}`,
    boxSizing: "border-box",
  },
  searchCard: {
    width: "100%",
    padding: tokens.spacingHorizontalXXL,
  },
  searchRow: {
    display: "flex",
    width: "100%",
  },
  // the portal name is shown by the header logo; the page still needs a heading
  screenReaderOnly: {
    position: "absolute",
    width: "1px",
    height: "1px",
    overflow: "hidden",
    clipPath: "inset(50%)",
    whiteSpace: "nowrap",
  },
  searchInput: {
    flexGrow: 1,
    height: "56px",
    fontSize: tokens.fontSizeBase500,
    borderTopRightRadius: "0",
    borderBottomRightRadius: "0",
  },
  searchButton: {
    height: "56px",
    minWidth: "110px",
    backgroundColor: PRIMARY_DARK,
    color: "#ffffff",
    borderTopLeftRadius: "0",
    borderBottomLeftRadius: "0",
    textTransform: "uppercase",
    ":hover": {
      backgroundColor: PRIMARY_MAIN,
      color: "#ffffff",
    },
    ":hover:active": {
      backgroundColor: PRIMARY_MAIN,
      color: "#ffffff",
    },
  },
});

/**
 * Portal home page: the central search entry, then whatever the deployment
 * publishes below it - its curated entry points and its own footer band, both
 * from /api/v1/ui/config. A deployment that configures neither gets the search
 * box alone, which is a configuration and not an omission.
 */
export default function HomePage() {
  const styles = useStyles();
  const { t } = useTranslation();
  const navigate = useNavigate();
  const [query, setQuery] = useState("");
  const { data: config } = useUiConfig();
  const name = config?.name ?? t("app.title");

  const search = () => {
    navigate(query.trim() ? `/apu?q=${encodeURIComponent(query.trim())}` : "/apu");
  };

  const homePage = config?.homePage;

  return (
    <div className={styles.root}>
      <div className={styles.content}>
        <h1 className={styles.screenReaderOnly}>{name}</h1>
        <Card className={styles.searchCard}>
          <div className={styles.searchRow}>
            <Input
              className={styles.searchInput}
              appearance="outline"
              size="large"
              aria-label={t("home.searchPlaceholder")}
              placeholder={t("home.searchPlaceholder")}
              value={query}
              onChange={(_, data) => setQuery(data.value)}
              onKeyDown={(e) => e.key === "Enter" && search()}
            />
            <Button className={styles.searchButton} appearance="primary" onClick={search}>
              {t("home.searchButton")}
            </Button>
          </div>
        </Card>
        {homePage && homePage.groups.length > 0 && <HomeTileGroups groups={homePage.groups} />}
      </div>
      {homePage?.footer && <HomeFooterBand footer={homePage.footer} />}
    </div>
  );
}
