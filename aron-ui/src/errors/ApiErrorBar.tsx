import {
  Button,
  makeStyles,
  MessageBar,
  MessageBarActions,
  MessageBarBody,
  MessageBarTitle,
  tokens,
} from "@fluentui/react-components";
import { useSyncExternalStore } from "react";
import { useTranslation } from "react-i18next";
import { dismissApiError, getApiErrors, subscribeApiErrors } from "./apiErrors";

const useStyles = makeStyles({
  root: {
    display: "flex",
    flexDirection: "column",
    gap: tokens.spacingVerticalXS,
    padding: `${tokens.spacingVerticalXS} ${tokens.spacingHorizontalXXL}`,
  },
});

/**
 * Visible list of failed API requests (see apiErrors.ts): each failure shows
 * the endpoint, status and server message, dismissible by the user. Rendered
 * on every page (AppLayout) - errors must be seen, not swallowed.
 */
export default function ApiErrorBar() {
  const styles = useStyles();
  const { t } = useTranslation();
  const errors = useSyncExternalStore(subscribeApiErrors, getApiErrors);

  // the container stays mounted even while empty: a live region announces only
  // what changes inside an element that already exists
  return (
    <div className={errors.length > 0 ? styles.root : undefined} role="alert">
      {errors.map((error) => (
        <MessageBar key={error.id} intent="error">
          <MessageBarBody>
            <MessageBarTitle>{t("errors.requestFailed")}</MessageBarTitle>
            {error.request ? `${error.request}: ` : ""}
            {error.detail}
          </MessageBarBody>
          <MessageBarActions>
            <Button appearance="transparent" size="small" onClick={() => dismissApiError(error.id)}>
              {t("errors.dismiss")}
            </Button>
          </MessageBarActions>
        </MessageBar>
      ))}
    </div>
  );
}
