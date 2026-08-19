import { makeStyles, Text, Title1, tokens } from "@fluentui/react-components";
import { useTranslation } from "react-i18next";
import { MenuItemCode } from "../api/generated";
import { SECTIONS } from "../sections";

const useStyles = makeStyles({
  root: {
    display: "flex",
    flexDirection: "column",
    gap: tokens.spacingVerticalL,
    padding: `${tokens.spacingVerticalXXL} ${tokens.spacingHorizontalXXL}`,
  },
});

/** Placeholder page of one portal section; real content arrives slice by slice. */
export default function SectionPage({ code }: { code: MenuItemCode }) {
  const styles = useStyles();
  const { t } = useTranslation();

  return (
    <div className={styles.root}>
      <Title1 as="h1">{t(SECTIONS[code].labelKey)}</Title1>
      <Text>{t("section.placeholder")}</Text>
    </div>
  );
}
