import {
  Spinner,
  Text,
  Title2,
  makeStyles,
  tokens,
} from "@fluentui/react-components";
import { useId, useRef, useState } from "react";
import { useTranslation } from "react-i18next";
import { Link, useParams, useSearchParams } from "react-router-dom";
import { ResponseError } from "../api/generated";
import { useApuDetail } from "../apu/useApuDetail";
import OsdViewport, { type OsdViewportHandle, type OsdSource } from "../dao/OsdViewport";
import ThumbnailRail from "../dao/ThumbnailRail";
import ViewerToolbar from "../dao/ViewerToolbar";
import { buildPages, initialPageIndex, pageFileId, type DaoPage } from "../dao/pages";

const useStyles = makeStyles({
  // fills the frame the ApuPage way: the page contributes no height, the
  // document never scrolls, canvas and rail scroll (or zoom) on their own
  layout: {
    position: "absolute",
    inset: 0,
    display: "flex",
    flexDirection: "column",
    gap: tokens.spacingVerticalXS,
    padding: `${tokens.spacingVerticalM} ${tokens.spacingHorizontalXXL}`,
    "@media (max-width: 860px)": {
      padding: `${tokens.spacingVerticalS} ${tokens.spacingHorizontalM}`,
    },
  },
  header: {
    display: "flex",
    flexWrap: "wrap",
    alignItems: "baseline",
    columnGap: tokens.spacingHorizontalL,
    rowGap: tokens.spacingVerticalXXS,
  },
  license: {
    color: tokens.colorNeutralForeground3,
  },
  link: {
    color: tokens.colorBrandForegroundLink,
    textDecorationLine: "none",
    ":hover": { textDecorationLine: "underline" },
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

/**
 * The digital-object viewer: a routed page (deep-linkable, back-button-closable)
 * filling the frame. The current page travels as the `file` search parameter -
 * the pathname stays stable, so AppLayout's route-change focus does not steal
 * focus on every page turn, and Back returns to the record, not through the
 * pages (`replace`).
 */
export default function DaoViewerPage() {
  const styles = useStyles();
  const { t } = useTranslation();
  const { uuid, daoUuid } = useParams<{ uuid: string; daoUuid: string }>();
  const [searchParams, setSearchParams] = useSearchParams();
  const detail = useApuDetail(uuid);
  const viewportRef = useRef<OsdViewportHandle>(null);
  const hintId = useId();

  const dao = detail.data?.digitalObjects.find((candidate) => candidate.uuid === daoUuid);
  const pages = dao === undefined ? [] : buildPages(dao.files);

  const [currentIndex, setCurrentIndex] = useState<number | null>(null);
  const [status, setStatus] = useState("");
  const [openFailed, setOpenFailed] = useState(false);

  // the deep link decides the starting page once the data is there
  const resolvedIndex =
    currentIndex ?? (pages.length > 0 ? initialPageIndex(pages, searchParams.get("file")) : 0);

  const goTo = (index: number) => {
    const page = pages[index];
    if (page === undefined) {
      return;
    }
    setCurrentIndex(index);
    setOpenFailed(false);
    setStatus(t("dao.pageStatus", { page: index + 1, total: pages.length }));
    const fileId = pageFileId(page);
    if (fileId !== undefined) {
      setSearchParams({ file: fileId }, { replace: true });
    }
  };

  // page-turn keys work wherever focus is on this page, except inside the
  // canvas (OpenSeadragon's own arrows pan there) and the page-number input
  const onKeyDown = (event: React.KeyboardEvent) => {
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

  const copyLink = async () => {
    try {
      await navigator.clipboard.writeText(window.location.href);
      setStatus(t("dao.linkCopied"));
    } catch {
      setStatus(t("dao.linkCopyFailed"));
    }
  };

  if (detail.isPending) {
    return <Spinner className={styles.layout} />;
  }
  if (detail.isError) {
    const notFound = detail.error instanceof ResponseError && detail.error.response.status === 404;
    return (
      <div className={styles.layout}>
        <Text>{t(notFound ? "apu.notFound" : "apu.error")}</Text>
      </div>
    );
  }
  if (dao === undefined || pages.length === 0) {
    return (
      <div className={styles.layout}>
        <Text>{t("dao.notFound")}</Text>
        <Link to={`/apu/${uuid}`} className={styles.link}>
          {t("dao.backToRecord")}
        </Link>
      </div>
    );
  }

  const page = pages[Math.min(resolvedIndex, pages.length - 1)];
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
    <div className={styles.layout} onKeyDown={onKeyDown}>
      <header className={styles.header}>
        <Title2 as="h1">{dao.name ?? detail.data.name}</Title2>
        <Link to={`/apu/${uuid}`} className={styles.link}>
          {t("dao.backToRecord")}
        </Link>
        {dao.license !== undefined && (
          <Text size={200} className={styles.license}>
            {t("dao.license", { code: dao.license })}
          </Text>
        )}
      </header>
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
      />
      <div className={styles.content}>
        <div className={styles.rail}>
          <ThumbnailRail pages={pages} currentIndex={page.index} onSelect={goTo} />
        </div>
        <div className={styles.canvas}>
          {source !== null ? (
            <OsdViewport
              ref={viewportRef}
              source={source}
              label={pageLabel}
              describedBy={hintId}
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
