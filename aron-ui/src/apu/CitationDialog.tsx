import {
  Button,
  Dialog,
  DialogActions,
  DialogBody,
  DialogContent,
  DialogSurface,
  DialogTitle,
  makeStyles,
  Spinner,
  Subtitle2,
  Text,
  tokens,
} from "@fluentui/react-components";
import { Copy20Regular } from "@fluentui/react-icons";
import { useState } from "react";
import { useTranslation } from "react-i18next";
import { useCitations } from "./useCitations";

const useStyles = makeStyles({
  content: {
    display: "flex",
    flexDirection: "column",
    gap: tokens.spacingVerticalM,
  },
  form: {
    display: "flex",
    flexDirection: "column",
    alignItems: "flex-start",
    gap: tokens.spacingVerticalXS,
  },
  // the citation is one long line of prose: it must wrap, and stay selectable
  // for a reader whose browser has no clipboard access
  text: {
    userSelect: "text",
  },
});

export interface Props {
  /** The record to cite. */
  uuid: string;

  onClose: () => void;
}

/**
 * The record's citations, as the deployment's citation forms produce them. The
 * text is server-rendered and copied verbatim - this dialog neither composes nor
 * reformats one (see doc: a citation's wording is the archivists' agreement).
 *
 * Several forms are shown one below the other, each with its own copy button and
 * its own name; a single form needs no name, since the dialog's title already
 * says what this is. The copy result is announced in the dialog rather than as a
 * toast: it belongs where the reader is looking, and a live region reaches a
 * screen reader without a provider of its own.
 */
export default function CitationDialog({ uuid, onClose }: Props) {
  const { t } = useTranslation();
  const styles = useStyles();
  const { data, isPending, isError } = useCitations(uuid);
  const [status, setStatus] = useState<string>();

  async function copy(text: string): Promise<void> {
    try {
      await navigator.clipboard.writeText(text);
      setStatus(t("citation.copied"));
    } catch {
      setStatus(t("citation.copyFailed"));
    }
  }

  return (
    <Dialog
      open
      onOpenChange={(_event, dialogData) => {
        if (!dialogData.open) {
          onClose();
        }
      }}
    >
      <DialogSurface>
        <DialogBody>
          <DialogTitle>{t("citation.title")}</DialogTitle>
          <DialogContent className={styles.content}>
            {isPending && <Spinner size="small" label={t("citation.loading")} />}
            {isError && <Text role="alert">{t("citation.error")}</Text>}
            {data?.map((citation) => (
              <div key={citation.code} className={styles.form}>
                {data.length > 1 && <Subtitle2 as="h3">{citation.label}</Subtitle2>}
                <Text className={styles.text}>{citation.text}</Text>
                <Button
                  size="small"
                  icon={<Copy20Regular />}
                  onClick={() => void copy(citation.text)}
                >
                  {t("citation.copy")}
                </Button>
              </div>
            ))}
            <div role="status" aria-live="polite">
              {status && <Text size={200}>{status}</Text>}
            </div>
          </DialogContent>
          <DialogActions>
            <Button appearance="secondary" onClick={onClose}>
              {t("citation.close")}
            </Button>
          </DialogActions>
        </DialogBody>
      </DialogSurface>
    </Dialog>
  );
}
