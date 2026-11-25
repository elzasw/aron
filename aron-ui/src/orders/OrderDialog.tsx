import {
  Button,
  Checkbox,
  Dialog,
  DialogActions,
  DialogBody,
  DialogContent,
  DialogSurface,
  DialogTitle,
  Field,
  Input,
  makeStyles,
  Spinner,
  Text,
  Textarea,
  tokens,
} from "@fluentui/react-components";
import { useMutation } from "@tanstack/react-query";
import type { TFunction } from "i18next";
import { type FormEvent, useRef, useState } from "react";
import { useTranslation } from "react-i18next";
import { type BasketGroup, type BasketItem, groupLabel } from "./basket";
import ConfirmDialog from "./ConfirmDialog";
import { OrderError, type OrderRequest, submitOrder } from "./orderApi";
import { useReturnFocus } from "./useReturnFocus";

/** The reader's contact, remembered between orders when they ask for it. */
const CONTACT_KEY = "aron.orderContact";

/** The old portal's rule; the order service validates the address properly. */
const EMAIL = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

/**
 * Introduces the wished date inside the free text the archive receives. Not
 * translated: it is read by the archive's staff in the mail, which the order
 * service writes in Czech whatever language the reader uses.
 */
const RESERVE_DATE_PREFIX = "Na kdy rezervovat: ";

interface Contact {
  name: string;
  email: string;
  phoneNumber: string;
}

function storedContact(): Contact | undefined {
  try {
    const parsed = JSON.parse(window.localStorage.getItem(CONTACT_KEY) ?? "null") as
      | Partial<Contact>
      | null;
    if (parsed === null || typeof parsed !== "object") {
      return undefined;
    }
    return {
      name: typeof parsed.name === "string" ? parsed.name : "",
      email: typeof parsed.email === "string" ? parsed.email : "",
      phoneNumber: typeof parsed.phoneNumber === "string" ? parsed.phoneNumber : "",
    };
  } catch {
    // unreadable or unavailable storage: an empty form
    return undefined;
  }
}

function rememberContact(contact: Contact | undefined): void {
  try {
    if (contact) {
      window.localStorage.setItem(CONTACT_KEY, JSON.stringify(contact));
    } else {
      window.localStorage.removeItem(CONTACT_KEY);
    }
  } catch {
    // remembering is a convenience; the order goes out either way
  }
}

/**
 * The ordered records as the reader checks them before sending: one numbered
 * block per record. Shown only - the order carries the items themselves.
 */
function summaryOf(items: BasketItem[], t: TFunction): string {
  return items
    .map(({ archivalie, institution }, index) => {
      const fund = [t("orders.form.summary.nad", { number: archivalie.cisloNad })];
      if (archivalie.dilciList != null) {
        fund.push(t("orders.form.summary.subNumber", { number: archivalie.dilciList }));
      }
      if (archivalie.archivniIdentifikator) {
        fund.push(archivalie.archivniIdentifikator);
      }
      const lines = [`--- ${index + 1} ---`];
      if (archivalie.formNazev) {
        lines.push(`${archivalie.formNazev} | ${institution.name}`);
      }
      lines.push(fund.join(" | "));
      const description = archivalie.uplnyPopis ?? archivalie.popis;
      if (description) {
        lines.push(description);
      }
      if (archivalie.url) {
        lines.push(t("orders.form.summary.link", { url: archivalie.url }));
      }
      return lines.join("\n");
    })
    .join("\n\n");
}

const useStyles = makeStyles({
  content: {
    display: "flex",
    flexDirection: "column",
    gap: tokens.spacingVerticalM,
  },
  error: {
    color: tokens.colorPaletteRedForeground1,
  },
});

export interface Props {
  /** The items of one order: one archive, one reading room. */
  group: BasketGroup;

  onClose: () => void;

  /** The order was accepted; the dialog has nothing more to do. */
  onSent: (group: BasketGroup) => void;
}

/**
 * The order form of one basket group: who orders, an optional wished date and
 * message, and the list of records for a last check. The order goes to the
 * deployment's order service (`orderApi.ts`), which mails it to the reading
 * room or hands it to the archive's own system.
 *
 * Mistakes are shown once the reader has tried to send - a form that reddens
 * while it is being filled in reads as a reproach - and the first faulty field
 * then takes the focus. Sending is confirmed, as in the old portal: an order
 * reaches people, and cannot be called back from here. Every failure is said
 * in the dialog, in the service's own words where it has any.
 */
export default function OrderDialog({ group, onClose, onSent }: Props) {
  const { t } = useTranslation();
  const styles = useStyles();
  const [contact] = useState(storedContact);
  const [name, setName] = useState(contact?.name ?? "");
  const [email, setEmail] = useState(contact?.email ?? "");
  const [phoneNumber, setPhoneNumber] = useState(contact?.phoneNumber ?? "");
  const [remember, setRemember] = useState(contact !== undefined);
  // prefilled in the reader's language, as it is the subject of the mail they get too
  const [subject, setSubject] = useState(() => t("orders.form.defaultSubject"));
  const [reserveDate, setReserveDate] = useState("");
  const [message, setMessage] = useState("");
  const [attempted, setAttempted] = useState(false);
  const [confirming, setConfirming] = useState(false);
  const nameRef = useRef<HTMLInputElement>(null);
  const emailRef = useRef<HTMLInputElement>(null);
  // the confirmation returns the focus here, whichever way it was answered
  const sendRef = useRef<HTMLButtonElement>(null);
  const openedBy = useReturnFocus(confirming, sendRef);
  const addressee = groupLabel(group);

  const nameError = name.trim() === "" ? t("orders.form.required") : undefined;
  const emailError =
    email.trim() === ""
      ? t("orders.form.required")
      : EMAIL.test(email.trim())
        ? undefined
        : t("orders.form.invalidEmail");

  const send = useMutation({
    mutationFn: submitOrder,
    onSuccess: () => onSent(group),
  });

  function submit(event: FormEvent): void {
    event.preventDefault();
    if (send.isPending) {
      // Enter in a field while the order is on its way must not send a second one
      return;
    }
    setAttempted(true);
    if (nameError) {
      nameRef.current?.focus();
    } else if (emailError) {
      emailRef.current?.focus();
    } else {
      if (sendRef.current) {
        openedBy(sendRef.current);
      }
      setConfirming(true);
    }
  }

  function sendOrder(): void {
    setConfirming(false);
    const trimmed = { name: name.trim(), email: email.trim(), phoneNumber: phoneNumber.trim() };
    rememberContact(remember ? trimmed : undefined);
    const text = [
      reserveDate.trim() && RESERVE_DATE_PREFIX + reserveDate.trim(),
      message.trim(),
    ].filter(Boolean);
    const order: OrderRequest = {
      name: trimmed.name,
      email: trimmed.email,
      phoneNumber: trimmed.phoneNumber || undefined,
      subjects: [subject.trim()],
      returnUrl: window.location.href,
      archiv: group.institution.code,
      badatelna: group.badatelna?.code,
      popis: text.length > 0 ? text.join("\n") : undefined,
      polozky: group.items.map((item) => ({
        archivalie: item.archivalie,
        zpusobZpristupneni: item.accessType,
      })),
    };
    send.mutate(order);
  }

  return (
    <>
    <Dialog
      open
      onOpenChange={(_event, data) => {
        // an order on its way is not abandoned by a stray click or Escape
        if (!data.open && !send.isPending) {
          onClose();
        }
      }}
    >
      <DialogSurface>
        <form onSubmit={submit} noValidate>
          <DialogBody>
            <DialogTitle>{t("orders.form.title")}</DialogTitle>
            <DialogContent className={styles.content}>
              <Field
                label={t("orders.form.name")}
                required
                validationMessage={attempted ? nameError : undefined}
              >
                <Input
                  ref={nameRef}
                  value={name}
                  autoComplete="name"
                  onChange={(_event, data) => setName(data.value)}
                />
              </Field>
              <Field
                label={t("orders.form.email")}
                required
                validationMessage={attempted ? emailError : undefined}
              >
                <Input
                  ref={emailRef}
                  type="email"
                  value={email}
                  autoComplete="email"
                  onChange={(_event, data) => setEmail(data.value)}
                />
              </Field>
              <Field label={t("orders.form.phone")}>
                <Input
                  type="tel"
                  value={phoneNumber}
                  autoComplete="tel"
                  onChange={(_event, data) => setPhoneNumber(data.value)}
                />
              </Field>
              <Checkbox
                label={t("orders.form.remember")}
                checked={remember}
                onChange={(_event, data) => setRemember(data.checked === true)}
              />
              <Field label={t("orders.form.subject")}>
                <Input value={subject} onChange={(_event, data) => setSubject(data.value)} />
              </Field>
              <Field label={t("orders.form.reserveDate")}>
                <Input
                  value={reserveDate}
                  onChange={(_event, data) => setReserveDate(data.value)}
                />
              </Field>
              <Field label={t("orders.form.summaryFor", { addressee })}>
                <Textarea readOnly rows={6} value={summaryOf(group.items, t)} />
              </Field>
              <Field label={t("orders.form.messageFor", { addressee })}>
                <Textarea
                  rows={4}
                  value={message}
                  onChange={(_event, data) => setMessage(data.value)}
                />
              </Field>
              {send.isPending && <Spinner size="small" label={t("orders.form.sending")} />}
              {send.isError && (
                <Text role="alert" className={styles.error}>
                  {send.error instanceof OrderError && send.error.serverMessage
                    ? send.error.serverMessage
                    : t("orders.form.sendFailed")}
                </Text>
              )}
            </DialogContent>
            <DialogActions>
              {/* not focusable-disabled: the focus rests here while the order is
                  on its way, and a disabled button would drop it out of the dialog */}
              <Button
                ref={sendRef}
                type="submit"
                appearance="primary"
                disabledFocusable={send.isPending}
              >
                {t("orders.form.send")}
              </Button>
              <Button appearance="secondary" disabledFocusable={send.isPending} onClick={onClose}>
                {t("orders.form.cancel")}
              </Button>
            </DialogActions>
          </DialogBody>
        </form>
      </DialogSurface>
    </Dialog>
    {confirming && (
      <ConfirmDialog
        message={t("orders.confirm.send")}
        onConfirm={sendOrder}
        onCancel={() => setConfirming(false)}
      />
    )}
    </>
  );
}
