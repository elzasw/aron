import { makeStyles, mergeClasses, tokens } from "@fluentui/react-components";
import { useEffect, useMemo } from "react";
import { useTranslation } from "react-i18next";
import { languageOf, offeredLanguages, setLanguage } from "../i18n";
import { PRIMARY_DARK, PRIMARY_MAIN } from "./palette";

const useStyles = makeStyles({
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
 * Language choice of the reader. Offered languages are those the deployment
 * declares (`localizations` of /api/v1/ui/config) that this build also has
 * strings for - a deployment can therefore add a language only once both sides
 * have it. A single offered language needs no control.
 *
 * Switching re-renders the UI strings and, because the language is part of
 * their react-query keys, refetches the server-rendered text (labels, datings).
 */
export default function LanguageSwitcher({ localizations }: { localizations: string[] }) {
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
