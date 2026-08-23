import { makeStyles, tokens } from "@fluentui/react-components";
import { useEffect, useRef } from "react";
import { useTranslation } from "react-i18next";
import type { DaoPage } from "./pages";

const useStyles = makeStyles({
  rail: {
    listStyleType: "none",
    margin: 0,
    padding: tokens.spacingVerticalXS,
    display: "flex",
    flexDirection: "column",
    gap: tokens.spacingVerticalXS,
    overflowY: "auto",
  },
  // long documents: rows outside the viewport skip layout and paint, which is
  // the virtualization this rail needs without a dependency
  row: {
    contentVisibility: "auto",
    containIntrinsicSize: "auto 132px",
  },
  thumb: {
    display: "flex",
    flexDirection: "column",
    alignItems: "center",
    gap: tokens.spacingVerticalXXS,
    width: "112px",
    padding: tokens.spacingVerticalXXS,
    background: "none",
    border: "2px solid transparent",
    borderRadius: tokens.borderRadiusMedium,
    cursor: "pointer",
    color: tokens.colorNeutralForeground2,
    fontSize: tokens.fontSizeBase200,
  },
  current: {
    border: `2px solid ${tokens.colorBrandStroke1}`,
    color: tokens.colorNeutralForeground1,
  },
  image: {
    width: "96px",
    height: "96px",
    objectFit: "contain",
  },
  // a page without a thumbnail keeps its slot, so numbering stays visible
  placeholder: {
    width: "96px",
    height: "96px",
    display: "flex",
    alignItems: "center",
    justifyContent: "center",
    backgroundColor: tokens.colorNeutralBackground3,
    borderRadius: tokens.borderRadiusSmall,
  },
});

interface ThumbnailRailProps {
  pages: DaoPage[];
  currentIndex: number;
  onSelect: (index: number) => void;
}

/**
 * The vertical strip of page thumbnails. The current page scrolls into the
 * rail's middle by setting the rail's own scrollTop - never scrollIntoView,
 * which scrolls the document too and takes the header with it (the ApuTree
 * rule).
 */
export default function ThumbnailRail({ pages, currentIndex, onSelect }: ThumbnailRailProps) {
  const styles = useStyles();
  const { t } = useTranslation();
  const railRef = useRef<HTMLUListElement>(null);

  useEffect(() => {
    const rail = railRef.current;
    const row = rail?.children[currentIndex] as HTMLElement | undefined;
    if (rail && row) {
      rail.scrollTop = row.offsetTop - rail.clientHeight / 2 + row.clientHeight / 2;
    }
  }, [currentIndex]);

  return (
    <ul ref={railRef} className={styles.rail} aria-label={t("dao.thumbnails")}>
      {pages.map((page) => (
        <li key={page.index} className={styles.row}>
          <button
            type="button"
            className={`${styles.thumb} ${page.index === currentIndex ? styles.current : ""}`}
            aria-current={page.index === currentIndex ? "true" : undefined}
            onClick={() => onSelect(page.index)}
          >
            {page.thumbnail?.url !== undefined ? (
              // the visible page number below is the accessible name; the picture only repeats it
              <img src={page.thumbnail.url} alt="" loading="lazy" className={styles.image} />
            ) : (
              <span aria-hidden="true" className={styles.placeholder}>
                {page.index + 1}
              </span>
            )}
            <span>{t("dao.pageNumber", { page: page.index + 1 })}</span>
          </button>
        </li>
      ))}
    </ul>
  );
}
