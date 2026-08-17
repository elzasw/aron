import { Button, Card, Input, makeStyles, tokens } from "@fluentui/react-components";
import { useState } from "react";
import { useTranslation } from "react-i18next";
import { useNavigate } from "react-router-dom";
import { PRIMARY_DARK, PRIMARY_MAIN } from "../layout/AppHeader";

const useStyles = makeStyles({
  root: {
    display: "flex",
    flexDirection: "column",
    alignItems: "center",
    padding: `${tokens.spacingVerticalXXXL} ${tokens.spacingHorizontalXXL}`,
  },
  searchCard: {
    width: "100%",
    maxWidth: "1100px",
    padding: tokens.spacingHorizontalXXL,
  },
  searchRow: {
    display: "flex",
    width: "100%",
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
 * Portal home page: the central search entry. Favorite queries, news and the
 * theme image arrive with later slices.
 */
export default function HomePage() {
  const styles = useStyles();
  const { t } = useTranslation();
  const navigate = useNavigate();
  const [query, setQuery] = useState("");

  const search = () => {
    navigate(query.trim() ? `/apu?q=${encodeURIComponent(query.trim())}` : "/apu");
  };

  return (
    <div className={styles.root}>
      <Card className={styles.searchCard}>
        <div className={styles.searchRow}>
          <Input
            className={styles.searchInput}
            appearance="outline"
            size="large"
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
    </div>
  );
}
