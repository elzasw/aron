import {
  Button,
  Dialog,
  DialogActions,
  DialogBody,
  DialogContent,
  DialogSurface,
  DialogTitle,
  makeStyles,
  Select,
  Subtitle2,
  Text,
  tokens,
  useRestoreFocusTarget,
} from "@fluentui/react-components";
import { Cart24Regular, Delete20Regular } from "@fluentui/react-icons";
import { useRef, useState } from "react";
import { useTranslation } from "react-i18next";
import { Link } from "react-router-dom";
import { useUiConfig } from "../api/useUiConfig";
import { PRIMARY_MAIN } from "../layout/palette";
import {
  ACCESS_TYPES,
  type AccessType,
  type BasketGroup,
  groupLabel,
  groupsOf,
  itemName,
} from "./basket";
import ConfirmDialog from "./ConfirmDialog";
import OrderDialog from "./OrderDialog";
import { useOrders } from "./OrdersProvider";
import { useReturnFocus } from "./useReturnFocus";

const useStyles = makeStyles({
  wrapper: {
    display: "flex",
    alignItems: "center",
  },
  trigger: {
    color: "#ffffff",
    fontWeight: tokens.fontWeightSemibold,
    minWidth: "40px",
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
  content: {
    display: "flex",
    flexDirection: "column",
    gap: tokens.spacingVerticalL,
  },
  group: {
    display: "flex",
    flexDirection: "column",
    gap: tokens.spacingVerticalS,
    padding: tokens.spacingHorizontalM,
    borderRadius: tokens.borderRadiusMedium,
    backgroundColor: tokens.colorNeutralBackground2,
  },
  groupHeader: {
    display: "flex",
    alignItems: "center",
    justifyContent: "space-between",
    gap: tokens.spacingHorizontalS,
  },
  items: {
    listStyleType: "none",
    margin: 0,
    padding: 0,
  },
  // name | access type | remove; the name takes a line of its own where the
  // three do not fit beside each other (a phone), rather than being cut short
  item: {
    display: "flex",
    flexWrap: "wrap",
    alignItems: "center",
    gap: tokens.spacingHorizontalS,
    padding: `${tokens.spacingVerticalXS} 0`,
    borderTop: `1px solid ${tokens.colorNeutralStroke2}`,
  },
  itemName: {
    flexGrow: 1,
    flexBasis: "12em",
    minWidth: 0,
    overflowWrap: "anywhere",
    color: tokens.colorBrandForegroundLink,
  },
  groupActions: {
    display: "flex",
    justifyContent: "flex-end",
  },
});

/** What is waiting for the reader's yes or no. */
type Question = { kind: "group"; group: BasketGroup } | { kind: "all" };

/**
 * The reader's basket in the header: a button carrying the number of records,
 * opening the basket - one block per order, i.e. per archive and reading room,
 * each sent on its own through the order form.
 *
 * Shown only where the deployment takes orders and the basket holds something:
 * a reader who never orders is not shown an empty basket on every page (the
 * old portal's behaviour). Unlike there it is present on a narrow screen too.
 *
 * A dialog rather than a panel hung under the button: it holds links, selects
 * and buttons, needs the focus kept inside and returned afterwards, and has to
 * fit a phone - all of which a modal dialog already does.
 */
export default function BasketButton() {
  const styles = useStyles();
  const { t } = useTranslation();
  const { data: config } = useUiConfig();
  const orders = useOrders();
  const [open, setOpen] = useState(false);
  const [ordering, setOrdering] = useState<BasketGroup>();
  const [question, setQuestion] = useState<Question>();
  const [status, setStatus] = useState("");
  const count = orders.items.length;
  // the basket is opened by a button that is not its trigger in Fluent's sense,
  // so the button to return the focus to has to be marked by hand
  const restoreFocusTarget = useRestoreFocusTarget();
  // always present in the open basket: where the focus goes when the control
  // the reader used has just removed itself
  const closeRef = useRef<HTMLButtonElement>(null);
  const openedBy = useReturnFocus(ordering !== undefined || question !== undefined, closeRef);

  // an open basket stays until the reader closes it, even once emptied - the
  // button taking the dialog away with it would drop the focus nowhere and the
  // confirmation of a sent order with it
  if (config?.orders !== true || (count === 0 && !open)) {
    return null;
  }

  const groups = groupsOf(orders.items);

  function answerYes(): void {
    if (question?.kind === "group") {
      orders.removeGroup(question.group.key);
      setStatus(t("orders.basket.groupRemoved", { addressee: groupLabel(question.group) }));
    } else if (question?.kind === "all") {
      orders.clear();
      setStatus(t("orders.basket.emptied"));
    }
    setQuestion(undefined);
  }

  return (
    <div className={styles.wrapper}>
      <Button
        appearance="transparent"
        icon={<Cart24Regular />}
        className={styles.trigger}
        // the name contains the visible number, so speech input can address it
        aria-label={t("orders.basket.button", { count })}
        {...restoreFocusTarget}
        onClick={() => {
          setStatus("");
          setOpen(true);
        }}
      >
        {count}
      </Button>
      {/* mounted only while open, like the page's other dialogs */}
      {open && (
      <Dialog
        open
        onOpenChange={(_event, data) => {
          if (!data.open) {
            setOpen(false);
          }
        }}
      >
        <DialogSurface>
          <DialogBody>
            <DialogTitle>{t("orders.basket.title")}</DialogTitle>
            <DialogContent className={styles.content}>
              {/* what just happened - an order sent, a group removed - is said
                  here, where the reader is, and reaches a screen reader */}
              <div role="status" aria-live="polite">
                {status && <Text weight="semibold">{status}</Text>}
              </div>
              {count === 0 && <Text>{t("orders.basket.empty")}</Text>}
              {groups.map((group) => (
                <section key={group.key} className={styles.group} aria-label={groupLabel(group)}>
                  <div className={styles.groupHeader}>
                    <Subtitle2 as="h3">{groupLabel(group)}</Subtitle2>
                    <Button
                      appearance="subtle"
                      icon={<Delete20Regular />}
                      aria-label={t("orders.basket.removeGroup", { addressee: groupLabel(group) })}
                      onClick={(event) => {
                        openedBy(event.currentTarget);
                        setQuestion({ kind: "group", group });
                      }}
                    />
                  </div>
                  <ul className={styles.items}>
                    {group.items.map((item) => (
                      <li key={item.uuid} className={styles.item}>
                        <Link
                          to={`/apu/${item.uuid}`}
                          className={styles.itemName}
                          onClick={() => setOpen(false)}
                        >
                          {itemName(item)}
                        </Link>
                        <Select
                          size="small"
                          value={item.accessType}
                          aria-label={t("orders.basket.accessTypeOf", { name: itemName(item) })}
                          onChange={(_event, data) =>
                            orders.setAccessType(item.uuid, data.value as AccessType)
                          }
                        >
                          {ACCESS_TYPES.map((type) => (
                            <option key={type} value={type}>
                              {t(`orders.accessType.${type}`)}
                            </option>
                          ))}
                        </Select>
                        <Button
                          appearance="subtle"
                          size="small"
                          icon={<Delete20Regular />}
                          aria-label={t("orders.removeFor", { name: itemName(item) })}
                          onClick={() => {
                            // this button is about to disappear; without a new
                            // home the focus would fall out of the basket
                            closeRef.current?.focus();
                            orders.remove(item.uuid);
                            setStatus(t("orders.basket.itemRemoved", { name: itemName(item) }));
                          }}
                        />
                      </li>
                    ))}
                  </ul>
                  <div className={styles.groupActions}>
                    <Button
                      appearance="primary"
                      aria-label={t("orders.basket.orderFrom", { addressee: groupLabel(group) })}
                      onClick={(event) => {
                        openedBy(event.currentTarget);
                        setStatus("");
                        setOrdering(group);
                      }}
                    >
                      {t("orders.basket.order")}
                    </Button>
                  </div>
                </section>
              ))}
            </DialogContent>
            <DialogActions>
              {count > 0 && (
                <Button
                  appearance="secondary"
                  onClick={(event) => {
                    openedBy(event.currentTarget);
                    setQuestion({ kind: "all" });
                  }}
                >
                  {t("orders.basket.clear")}
                </Button>
              )}
              <Button ref={closeRef} appearance="secondary" onClick={() => setOpen(false)}>
                {t("orders.basket.close")}
              </Button>
            </DialogActions>
          </DialogBody>
        </DialogSurface>
      </Dialog>
      )}
      {ordering && (
        <OrderDialog
          group={ordering}
          onClose={() => setOrdering(undefined)}
          onSent={(sent) => {
            // the sent records leave the basket; the others wait for their own order
            orders.removeGroup(sent.key);
            setOrdering(undefined);
            setStatus(t("orders.basket.sent", { addressee: groupLabel(sent) }));
          }}
        />
      )}
      {question && (
        <ConfirmDialog
          message={
            question.kind === "group"
              ? t("orders.confirm.removeGroup", { addressee: groupLabel(question.group) })
              : t("orders.confirm.clear")
          }
          onConfirm={answerYes}
          onCancel={() => setQuestion(undefined)}
        />
      )}
    </div>
  );
}
