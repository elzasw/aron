import i18n from "i18next";
import { initReactI18next } from "react-i18next";
import cs from "./cs.json";

// Czech-first: cs is the default and the fallback; further languages are added
// as resource bundles here.
i18n.use(initReactI18next).init({
  lng: "cs",
  fallbackLng: "cs",
  resources: {
    cs: { translation: cs },
  },
  interpolation: {
    // React already escapes rendered values
    escapeValue: false,
  },
});

export default i18n;
