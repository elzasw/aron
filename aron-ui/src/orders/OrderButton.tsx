import { makeStyles, Text, tokens } from "@fluentui/react-components";
import { useMutation } from "@tanstack/react-query";
import { useState } from "react";
import { useTranslation } from "react-i18next";
import { apuApi } from "../api/client";
import { useApiLanguage } from "../i18n/useApiLanguage";
import { serverContextPath } from "../serverContext";
import { DEFAULT_ACCESS_TYPE } from "./basket";
import { useOrders } from "./OrdersProvider";

const useStyles = makeStyles({
  // announced, not shown: the button's own label already changed for the eye
  status: {
    position: "absolute",
    width: "1px",
    height: "1px",
    overflow: "hidden",
    clipPath: "inset(50%)",
    whiteSpace: "nowrap",
  },
  error: {
    flexBasis: "100%",
    color: tokens.colorPaletteRedForeground1,
  },
});

export interface Props {
  /** The record to order. */
  uuid: string;

  /** The record's name, for the button's accessible name. */
  name: string;

  /** The surface of the neighbouring actions, so the row reads as one. */
  className?: string;
}

/**
 * Puts the record into the reader's basket, or takes it out again. Rendered
 * only for a record the server marked orderable.
 *
 * Adding asks the server what an order says about the record
 * (`/apu/{uuid}/order-item`) and stores the answer; the record's public
 * address is added here, because only the browser knows where the portal is
 * published. A record the server cannot describe is refused with a status, so
 * nothing half-described ever reaches the basket - the reader is told on the
 * spot instead of at the moment of sending.
 */
export default function OrderButton({ uuid, name, className }: Props) {
  const { t } = useTranslation();
  const styles = useStyles();
  const lang = useApiLanguage();
  const orders = useOrders();
  const [status, setStatus] = useState("");
  const inBasket = orders.items.some((item) => item.uuid === uuid);

  const add = useMutation({
    mutationFn: () => apuApi.apuGetOrderItem({ uuid, lang }),
    onSuccess: (item) => {
      orders.add({
        ...item,
        archivalie: {
          ...item.archivalie,
          url: `${window.location.origin}${serverContextPath}/apu/${uuid}`,
        },
        uuid,
        accessType: DEFAULT_ACCESS_TYPE,
      });
      setStatus(t("orders.added"));
    },
  });

  function toggle(): void {
    if (inBasket) {
      orders.remove(uuid);
      add.reset();
      setStatus(t("orders.removed"));
    } else {
      setStatus("");
      add.mutate();
    }
  }

  return (
    <>
      <button
        type="button"
        className={className}
        aria-label={t(inBasket ? "orders.removeFor" : "orders.addFor", { name })}
        disabled={add.isPending}
        onClick={toggle}
      >
        {t(inBasket ? "orders.remove" : "orders.add")}
      </button>
      <span role="status" aria-live="polite" className={styles.status}>
        {status}
      </span>
      {add.isError && (
        <Text role="alert" size={200} className={styles.error}>
          {t("orders.addFailed")}
        </Text>
      )}
    </>
  );
}
