import {
  Button,
  Input,
  Label,
  Text,
  makeStyles,
  tokens,
} from "@fluentui/react-components";
import { useId, useState } from "react";
import { useTranslation } from "react-i18next";

const useStyles = makeStyles({
  bar: {
    display: "flex",
    flexWrap: "wrap",
    alignItems: "center",
    gap: tokens.spacingHorizontalXS,
    padding: `${tokens.spacingVerticalXS} 0`,
  },
  group: {
    display: "flex",
    alignItems: "center",
    gap: tokens.spacingHorizontalXS,
  },
  pageInput: {
    width: "64px",
  },
  divider: {
    width: "1px",
    alignSelf: "stretch",
    backgroundColor: tokens.colorNeutralStroke2,
    marginLeft: tokens.spacingHorizontalXS,
    marginRight: tokens.spacingHorizontalXS,
  },
});

interface ViewerToolbarProps {
  pageCount: number;
  currentIndex: number;
  onGoTo: (index: number) => void;
  onZoomIn: () => void;
  onZoomOut: () => void;
  onZoomFit: () => void;
  onRotateLeft: () => void;
  onRotateRight: () => void;
  /** Download of the current page's published file; absent = nothing to download. */
  downloadUrl?: string;
  downloadName?: string;
  onCopyLink: () => void;
}

/**
 * The viewer's own controls - real buttons with accessible names, no library
 * chrome. Zoom and rotation drive the canvas through the page's handle; page
 * turns go through the URL state so a deep link always names what is on
 * screen.
 */
export default function ViewerToolbar({
  pageCount,
  currentIndex,
  onGoTo,
  onZoomIn,
  onZoomOut,
  onZoomFit,
  onRotateLeft,
  onRotateRight,
  downloadUrl,
  downloadName,
  onCopyLink,
}: ViewerToolbarProps) {
  const styles = useStyles();
  const { t } = useTranslation();
  const pageInputId = useId();
  // the draft exists only while the reader is typing; otherwise the input
  // simply shows the current page, so turns from anywhere refresh it
  const [pageDraft, setPageDraft] = useState<string | null>(null);
  const pageValue = pageDraft ?? String(currentIndex + 1);

  const commitDraft = () => {
    setPageDraft(null);
    const page = Number(pageDraft);
    if (Number.isInteger(page) && page >= 1 && page <= pageCount) {
      onGoTo(page - 1);
    }
  };

  return (
    <div role="toolbar" aria-label={t("dao.viewerControls")} className={styles.bar}>
      <div className={styles.group}>
        <Button
          appearance="subtle"
          disabled={currentIndex === 0}
          onClick={() => onGoTo(0)}
          aria-label={t("dao.firstPage")}
        >
          ⇤
        </Button>
        <Button
          appearance="subtle"
          disabled={currentIndex === 0}
          onClick={() => onGoTo(currentIndex - 1)}
          aria-label={t("dao.prevPage")}
        >
          ←
        </Button>
        <Label htmlFor={pageInputId}>{t("dao.goToPage")}</Label>
        <Input
          id={pageInputId}
          className={styles.pageInput}
          type="number"
          min={1}
          max={pageCount}
          value={pageValue}
          onChange={(_, data) => setPageDraft(data.value)}
          onBlur={commitDraft}
          onKeyDown={(event) => {
            if (event.key === "Enter") {
              commitDraft();
            }
          }}
        />
        <Text>{t("dao.pageOf", { total: pageCount })}</Text>
        <Button
          appearance="subtle"
          disabled={currentIndex >= pageCount - 1}
          onClick={() => onGoTo(currentIndex + 1)}
          aria-label={t("dao.nextPage")}
        >
          →
        </Button>
        <Button
          appearance="subtle"
          disabled={currentIndex >= pageCount - 1}
          onClick={() => onGoTo(pageCount - 1)}
          aria-label={t("dao.lastPage")}
        >
          ⇥
        </Button>
      </div>
      <div className={styles.divider} aria-hidden="true" />
      <div className={styles.group}>
        <Button appearance="subtle" onClick={onZoomIn} aria-label={t("dao.zoomIn")}>
          ＋
        </Button>
        <Button appearance="subtle" onClick={onZoomOut} aria-label={t("dao.zoomOut")}>
          −
        </Button>
        <Button appearance="subtle" onClick={onZoomFit} aria-label={t("dao.zoomFit")}>
          ▣
        </Button>
        <Button appearance="subtle" onClick={onRotateLeft} aria-label={t("dao.rotateLeft")}>
          ⟲
        </Button>
        <Button appearance="subtle" onClick={onRotateRight} aria-label={t("dao.rotateRight")}>
          ⟳
        </Button>
      </div>
      <div className={styles.divider} aria-hidden="true" />
      <div className={styles.group}>
        {downloadUrl !== undefined && (
          <Button
            as="a"
            appearance="subtle"
            href={downloadUrl}
            download={downloadName}
          >
            {t("dao.download")}
          </Button>
        )}
        <Button appearance="subtle" onClick={onCopyLink}>
          {t("dao.copyLink")}
        </Button>
      </div>
    </div>
  );
}
