import { makeStyles, Text, Title1, tokens } from "@fluentui/react-components";
import { useTranslation } from "react-i18next";
import { useParams } from "react-router-dom";

const useStyles = makeStyles({
  root: {
    display: "flex",
    flexDirection: "column",
    gap: tokens.spacingVerticalL,
  },
});

/** Placeholder for the APU detail page (route family /apu/{uuid}). */
export default function ApuPage() {
  const styles = useStyles();
  const { t } = useTranslation();
  const { uuid } = useParams<{ uuid: string }>();

  return (
    <div className={styles.root}>
      <Title1>{t("apu.heading")}</Title1>
      <Text>{t("apu.placeholder", { uuid })}</Text>
    </div>
  );
}
