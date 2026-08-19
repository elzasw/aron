import {
  Menu,
  MenuButton,
  MenuItemRadio,
  MenuList,
  MenuPopover,
  MenuTrigger,
  makeStyles,
  tokens,
  type MenuProps,
} from "@fluentui/react-components";
import { useEffect, useMemo } from "react";
import { useTranslation } from "react-i18next";
import { languageOf, offeredLanguages, setLanguage } from "../i18n";
import { PRIMARY_MAIN } from "./palette";

const useStyles = makeStyles({
  wrapper: {
    display: "flex",
    alignItems: "center",
    paddingLeft: tokens.spacingHorizontalL,
  },
  trigger: {
    border: "1px solid rgba(255, 255, 255, 0.6)",
    backgroundColor: "transparent",
    color: "#ffffff",
    fontWeight: tokens.fontWeightSemibold,
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
  item: {
    display: "flex",
    alignItems: "baseline",
    gap: tokens.spacingHorizontalS,
  },
  code: {
    color: tokens.colorNeutralForeground3,
    fontSize: tokens.fontSizeBase200,
  },
});

/**
 * Language choice of the reader. Offered languages are those the deployment
 * declares (`localizations` of /api/v1/ui/config) that this build also has
 * strings for - a deployment can therefore add a language only once both sides
 * have it. A single offered language needs no control.
 *
 * A menu rather than a row of badges: it stays the same size for two languages
 * or ten, and every option can say which language it is instead of leaving the
 * reader to decode a two-letter code. Each name is written in its own language
 * and marked with `lang`, so a screen reader pronounces "Deutsch" as German
 * rather than as Czech (WCAG 3.1.2). Deliberately no flags - a flag is a
 * country, not a language (whose flag would English get?), and the old portal's
 * flag-only switcher is exactly what a reader cannot decipher.
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

  const onCheckedValueChange: MenuProps["onCheckedValueChange"] = (_event, data) => {
    const chosen = data.checkedItems[0];
    if (chosen !== undefined) {
      setLanguage(chosen);
    }
  };

  return (
    <div className={styles.wrapper}>
      <Menu checkedValues={{ language: [active] }} onCheckedValueChange={onCheckedValueChange}>
        <MenuTrigger disableButtonEnhancement>
          <MenuButton
            className={styles.trigger}
            appearance="transparent"
            size="small"
            // the name spells the current language out but must still contain
            // the visible code, or speech input ("click CS") cannot address the
            // control - WCAG 2.5.3 Label in Name
            aria-label={t("nav.languageCurrent", {
              language: t(`nav.languageName.${active}`),
              code: active.toUpperCase(),
            })}
          >
            {active.toUpperCase()}
          </MenuButton>
        </MenuTrigger>
        <MenuPopover>
          {/* radio items, not plain ones: exactly one language is in effect and
              the menu says which (role=menuitemradio + aria-checked) */}
          <MenuList>
            {offered.map((language) => (
              <MenuItemRadio key={language} name="language" value={language}>
                <span className={styles.item}>
                  <span lang={language}>{t(`nav.languageName.${language}`)}</span>
                  <span className={styles.code}>{language.toUpperCase()}</span>
                </span>
              </MenuItemRadio>
            ))}
          </MenuList>
        </MenuPopover>
      </Menu>
    </div>
  );
}
