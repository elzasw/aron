import {
  Button,
  Dialog,
  DialogActions,
  DialogBody,
  DialogContent,
  DialogSurface,
  DialogTitle,
} from "@fluentui/react-components";
import { useTranslation } from "react-i18next";

export interface Props {
  /** The question, e.g. "Send the order?". */
  message: string;

  onConfirm: () => void;

  onCancel: () => void;
}

/**
 * A yes/no question before a step the reader cannot take back - emptying the
 * basket, sending an order. An alert dialog: it cannot be dismissed by a click
 * beside it, only answered, and Escape answers "no".
 */
export default function ConfirmDialog({ message, onConfirm, onCancel }: Props) {
  const { t } = useTranslation();
  return (
    <Dialog
      open
      modalType="alert"
      onOpenChange={(_event, data) => {
        if (!data.open) {
          onCancel();
        }
      }}
    >
      <DialogSurface>
        <DialogBody>
          <DialogTitle>{t("orders.confirm.title")}</DialogTitle>
          <DialogContent>{message}</DialogContent>
          <DialogActions>
            <Button appearance="primary" onClick={onConfirm}>
              {t("orders.confirm.yes")}
            </Button>
            <Button appearance="secondary" onClick={onCancel}>
              {t("orders.confirm.no")}
            </Button>
          </DialogActions>
        </DialogBody>
      </DialogSurface>
    </Dialog>
  );
}
