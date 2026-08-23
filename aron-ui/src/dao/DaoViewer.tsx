import { Text, makeStyles, tokens } from "@fluentui/react-components";
import { useEffect, useId, useRef, useState } from "react";
import { useTranslation } from "react-i18next";
import type { DigitalObjectInfo } from "../api/generated";
import { serverContextPath } from "../serverContext";
import OsdViewport, { type OsdViewportHandle, type OsdSource } from "./OsdViewport";
import ThumbnailRail from "./ThumbnailRail";
import ViewerToolbar, {
  NEUTRAL_ADJUSTMENTS,
  type ImageAdjustments,
} from "./ViewerToolbar";
import { buildPages, initialPageIndex, pageFileId, type DaoPage } from "./pages";

const useStyles = makeStyles({
  // fills whatever pane hosts it - the record page's middle column or the
  // fullscreen page's frame; the host owns the height
  root: {
    height: "100%",
    display: "flex",
    flexDirection: "column",
    gap: tokens.spacingVerticalXS,
    minHeight: 0,
    minWidth: 0,
  },
  content: {
    display: "flex",
    flexGrow: 1,
    minHeight: 0,
    gap: tokens.spacingHorizontalS,
  },
  rail: {
    flexShrink: 0,
    display: "flex",
    minHeight: 0,
    // on a phone the toolbar's page controls carry the navigation; a second
    // column would squeeze the image into a stamp
    "@media (max-width: 860px)": {
      display: "none",
    },
  },
  canvas: {
    position: "relative",
    flexGrow: 1,
    minWidth: 0,
    backgroundColor: tokens.colorNeutralBackground3,
    borderRadius: tokens.borderRadiusMedium,
  },
  // the slot of a page with nothing to render: named download instead of a canvas
  downloadCard: {
    height: "100%",
    display: "flex",
    flexDirection: "column",
    alignItems: "center",
    justifyContent: "center",
    gap: tokens.spacingVerticalS,
    textAlign: "center",
    padding: tokens.spacingHorizontalXL,
  },
  link: {
    color: tokens.colorBrandForegroundLink,
    textDecorationLine: "none",
    ":hover": { textDecorationLine: "underline" },
  },
  srOnly: {
    position: "absolute",
    width: "1px",
    height: "1px",
    margin: "-1px",
    padding: 0,
    overflow: "hidden",
    clip: "rect(0 0 0 0)",
    whiteSpace: "nowrap",
    border: 0,
  },
});

/** The current page's OpenSeadragon source, or null when there is nothing to render. */
function pageSource(page: DaoPage): OsdSource | null {
  if (page.kind === "deepZoom" && page.tile?.dziUrl !== undefined) {
    return page.tile.dziUrl;
  }
  if (page.kind === "image" && page.published?.url !== undefined) {
    return { type: "image", url: page.published.url };
  }
  return null;
}

/** The routed (fullscreen) viewer URL of one page, relative to the router. */
export function viewerUrl(apuUuid: string, daoUuid: string, fileId: string | undefined): string {
  return `/apu/${apuUuid}/dao/${daoUuid}` + (fileId !== undefined ? `?file=${fileId}` : "");
}

interface DaoViewerProps {
  apuUuid: string;
  dao: DigitalObjectInfo;
  /** The deep-linked file to open first, when the host's URL names one. */
  initialFileId?: string | null;
  /** Page turns report the current file, so a routed host can keep its URL truthful. */
  onPageChange?: (fileId: string | undefined) => void;
  /** Embedded hosts link into the fullscreen (routed) viewer; the routed page itself does not. */
  showFullscreenLink?: boolean;
  /**
   * Page-turn keys work document-wide - for the fullscreen page, whose whole
   * surface is the viewer. Embedded viewers keep them scoped to themselves, so
   * arrows still scroll the description or walk the tree beside them.
   */
  globalKeyboard?: boolean;
}

/**
 * The digital-object viewer itself - toolbar, thumbnail rail, deep-zoom canvas,
 * keyboard model and the polite live region - independent of where it is
 * hosted: embedded as the record page's centerpiece (the old portal's
 * principle: a digitized record shows its scan immediately), or filling the
 * routed fullscreen page. It fills its host's height; the host owns the frame.
 */
export default function DaoViewer({
  apuUuid,
  dao,
  initialFileId = null,
  onPageChange,
  showFullscreenLink = false,
  globalKeyboard = false,
}: DaoViewerProps) {
  const styles = useStyles();
  const { t } = useTranslation();
  const viewportRef = useRef<OsdViewportHandle>(null);
  const hintId = useId();

  const pages = buildPages(dao.files);
  const [currentIndex, setCurrentIndex] = useState<number | null>(null);
  const [status, setStatus] = useState("");
  const [openFailed, setOpenFailed] = useState(false);
  const [navigatorShown, setNavigatorShown] = useState(false);
  const [viewportLocked, setViewportLocked] = useState(false);
  const [adjustments, setAdjustments] = useState<ImageAdjustments>(NEUTRAL_ADJUSTMENTS);

  // the deep link decides the starting page once the data is there
  const resolvedIndex = currentIndex ?? (pages.length > 0 ? initialPageIndex(pages, initialFileId) : 0);

  const goTo = (index: number) => {
    const page = pages[index];
    if (page === undefined) {
      return;
    }
    setCurrentIndex(index);
    setOpenFailed(false);
    setStatus(t("dao.pageStatus", { page: index + 1, total: pages.length }));
    onPageChange?.(pageFileId(page));
  };

  // page-turn keys work wherever focus is within the viewer, except inside the
  // canvas (OpenSeadragon's own arrows pan there) and the page-number input
  const onKeyDown = (event: { key: string; target: EventTarget | null; preventDefault: () => void }) => {
    const target = event.target as HTMLElement;
    if (target.closest('[role="application"], input') !== null) {
      return;
    }
    const step =
      event.key === "ArrowRight" ? 1
      : event.key === "ArrowLeft" ? -1
      : event.key === "PageDown" ? 10
      : event.key === "PageUp" ? -10
      : null;
    if (step !== null) {
      event.preventDefault();
      goTo(Math.min(Math.max(resolvedIndex + step, 0), pages.length - 1));
    } else if (event.key === "Home") {
      event.preventDefault();
      goTo(0);
    } else if (event.key === "End") {
      event.preventDefault();
      goTo(pages.length - 1);
    }
  };

  // the fullscreen page's whole surface is the viewer, so its keys are too
  const keyHandlerRef = useRef(onKeyDown);
  useEffect(() => {
    keyHandlerRef.current = onKeyDown;
  });
  useEffect(() => {
    if (!globalKeyboard) {
      return;
    }
    const listener = (event: KeyboardEvent) => keyHandlerRef.current(event);
    document.addEventListener("keydown", listener);
    return () => document.removeEventListener("keydown", listener);
  }, [globalKeyboard]);

  if (pages.length === 0) {
    return <Text>{t("dao.notFound")}</Text>;
  }

  const page = pages[Math.min(resolvedIndex, pages.length - 1)];
  const currentFileId = pageFileId(page);

  const copyLink = async () => {
    try {
      // the fullscreen viewer URL names the page exactly, wherever the viewer is hosted
      const url = window.location.origin + serverContextPath + viewerUrl(apuUuid, dao.uuid, currentFileId);
      await navigator.clipboard.writeText(url);
      setStatus(t("dao.linkCopied"));
    } catch {
      setStatus(t("dao.linkCopyFailed"));
    }
  };

  const source = openFailed ? null : pageSource(page);
  const pageLabel = t("dao.canvasLabel", {
    name: page.published?.name ?? t("dao.pageNumber", { page: page.index + 1 }),
  });
  const downloadUrl =
    page.published?.url !== undefined
      ? page.published.url + (page.published.url.includes("?") ? "&" : "?") + "download=true"
      : undefined;

  return (
    // the handler only reacts to page-turn keys bubbling from the controls below
    // eslint-disable-next-line jsx-a11y/no-static-element-interactions
    <div className={styles.root} onKeyDown={onKeyDown}>
      <ViewerToolbar
        pageCount={pages.length}
        currentIndex={page.index}
        onGoTo={goTo}
        onZoomIn={() => viewportRef.current?.zoomIn()}
        onZoomOut={() => viewportRef.current?.zoomOut()}
        onZoomFit={() => viewportRef.current?.home()}
        onRotateLeft={() => viewportRef.current?.rotateBy(-90)}
        onRotateRight={() => viewportRef.current?.rotateBy(90)}
        downloadUrl={downloadUrl}
        downloadName={page.published?.name}
        onCopyLink={() => void copyLink()}
        fullscreenUrl={showFullscreenLink ? viewerUrl(apuUuid, dao.uuid, currentFileId) : undefined}
        navigatorShown={navigatorShown}
        onToggleNavigator={() => setNavigatorShown(!navigatorShown)}
        viewportLocked={viewportLocked}
        onToggleViewportLock={() => setViewportLocked(!viewportLocked)}
        adjustments={adjustments}
        onAdjust={setAdjustments}
      />
      <div className={styles.content}>
        <div className={styles.rail}>
          <ThumbnailRail pages={pages} currentIndex={page.index} onSelect={goTo} />
        </div>
        <div
          className={styles.canvas}
          // the reader's brightness/contrast, a plain CSS filter over the canvas
          style={
            adjustments === NEUTRAL_ADJUSTMENTS
              ? undefined
              : { filter: `brightness(${adjustments.brightness}%) contrast(${adjustments.contrast}%)` }
          }
        >
          {source !== null ? (
            <OsdViewport
              ref={viewportRef}
              source={source}
              label={pageLabel}
              describedBy={hintId}
              navigatorVisible={navigatorShown}
              preserveViewport={viewportLocked}
              onOpenFailed={() => {
                setOpenFailed(true);
                setStatus(t("dao.loadError"));
              }}
            />
          ) : (
            <div className={styles.downloadCard}>
              <Text>{openFailed ? t("dao.loadError") : t("dao.noPreview")}</Text>
              {downloadUrl !== undefined && (
                <a href={downloadUrl} download={page.published?.name} className={styles.link}>
                  {t("dao.downloadNamed", {
                    name: page.published?.name ?? t("dao.pageNumber", { page: page.index + 1 }),
                  })}
                </a>
              )}
            </div>
          )}
        </div>
      </div>
      <p id={hintId} className={styles.srOnly}>
        {t("dao.keyboardHint")}
      </p>
      <div role="status" aria-live="polite" className={styles.srOnly}>
        {status}
      </div>
    </div>
  );
}
