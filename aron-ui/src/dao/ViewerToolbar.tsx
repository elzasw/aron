import {
  Button,
  Input,
  Text,
  Tooltip,
  makeStyles,
  tokens,
} from "@fluentui/react-components";
import { useState } from "react";
import { useTranslation } from "react-i18next";
import { useNavigate } from "react-router-dom";

const useStyles = makeStyles({
  bar: {
    display: "flex",
    flexWrap: "wrap",
    alignItems: "center",
    columnGap: "2px",
    rowGap: tokens.spacingVerticalXXS,
    padding: `${tokens.spacingVerticalXXS} 0`,
  },
  group: {
    display: "flex",
    alignItems: "center",
    columnGap: "2px",
  },
  pageInput: {
    width: "56px",
  },
  pageTotal: {
    whiteSpace: "nowrap",
    paddingRight: tokens.spacingHorizontalXS,
    paddingLeft: tokens.spacingHorizontalXXS,
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
  /** In-app route of the fullscreen viewer; absent when the viewer already fills the frame. */
  fullscreenUrl?: string;
}

/** One compact control: an icon glyph whose accessible name is its tooltip. */
function IconButton({
  label,
  glyph,
  onClick,
  disabled,
}: {
  label: string;
  glyph: string;
  onClick: () => void;
  disabled?: boolean;
}) {
  return (
    <Tooltip content={label} relationship="label">
      <Button appearance="subtle" size="small" disabled={disabled} onClick={onClick}>
        {glyph}
      </Button>
    </Tooltip>
  );
}

/**
 * The viewer's own controls, one compact line - real buttons whose accessible
 * names double as tooltips, no library chrome. Zoom and rotation drive the
 * canvas through the page's handle; page turns go through the host, which may
 * keep them in its URL.
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
  fullscreenUrl,
}: ViewerToolbarProps) {
  const styles = useStyles();
  const { t } = useTranslation();
  const navigate = useNavigate();
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
        <IconButton
          label={t("dao.firstPage")}
          glyph="⇤"
          disabled={currentIndex === 0}
          onClick={() => onGoTo(0)}
        />
        <IconButton
          label={t("dao.prevPage")}
          glyph="←"
          disabled={currentIndex === 0}
          onClick={() => onGoTo(currentIndex - 1)}
        />
        <Input
          aria-label={t("dao.goToPage")}
          className={styles.pageInput}
          size="small"
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
        <Text size={200} className={styles.pageTotal}>
          {t("dao.pageOf", { total: pageCount })}
        </Text>
        <IconButton
          label={t("dao.nextPage")}
          glyph="→"
          disabled={currentIndex >= pageCount - 1}
          onClick={() => onGoTo(currentIndex + 1)}
        />
        <IconButton
          label={t("dao.lastPage")}
          glyph="⇥"
          disabled={currentIndex >= pageCount - 1}
          onClick={() => onGoTo(pageCount - 1)}
        />
      </div>
      <div className={styles.divider} aria-hidden="true" />
      <div className={styles.group}>
        <IconButton label={t("dao.zoomIn")} glyph="＋" onClick={onZoomIn} />
        <IconButton label={t("dao.zoomOut")} glyph="−" onClick={onZoomOut} />
        <IconButton label={t("dao.zoomFit")} glyph="▣" onClick={onZoomFit} />
        <IconButton label={t("dao.rotateLeft")} glyph="⟲" onClick={onRotateLeft} />
        <IconButton label={t("dao.rotateRight")} glyph="⟳" onClick={onRotateRight} />
      </div>
      <div className={styles.divider} aria-hidden="true" />
      <div className={styles.group}>
        {downloadUrl !== undefined && (
          <Tooltip content={t("dao.download")} relationship="label">
            <Button as="a" appearance="subtle" size="small" href={downloadUrl} download={downloadName}>
              ⤓
            </Button>
          </Tooltip>
        )}
        <IconButton label={t("dao.copyLink")} glyph="⧉" onClick={onCopyLink} />
        {fullscreenUrl !== undefined && (
          <Tooltip content={t("dao.fullscreen")} relationship="label">
            <Button
              as="a"
              appearance="subtle"
              size="small"
              href={fullscreenUrl}
              onClick={(event) => {
                // an in-app route: the router navigates, the browser must not reload
                event.preventDefault();
                navigate(fullscreenUrl);
              }}
            >
              ⛶
            </Button>
          </Tooltip>
        )}
      </div>
    </div>
  );
}
