import {
  Button,
  Dropdown,
  makeStyles,
  mergeClasses,
  Option,
  Text,
  tokens,
  useId,
} from "@fluentui/react-components";
import {
  ArrowNext16Regular,
  ArrowPrevious16Regular,
  ChevronDoubleLeft16Regular,
  ChevronDoubleRight16Regular,
  ChevronLeft16Regular,
  ChevronRight16Regular,
} from "@fluentui/react-icons";
import type { ReactElement, ReactNode } from "react";
import { useTranslation } from "react-i18next";
import { PRIMARY_DARK, PRIMARY_MAIN } from "../layout/AppHeader";

const PAGE_SIZES = [10, 20, 50];
// The window around the current page moves two pages a click and a capped
// result is a thousand pages long: the old portal's ten-page jump covers the
// distance between a neighbour and the ends.
const PAGE_JUMP = 10;

const useStyles = makeStyles({
  bar: {
    display: "flex",
    alignItems: "center",
    flexWrap: "wrap",
    gap: tokens.spacingHorizontalXS,
  },
  spacer: {
    flexGrow: 1,
  },
  // compact circular controls (the old portal's pager look)
  pageButton: {
    minWidth: "32px",
    height: "32px",
    padding: "0",
    borderRadius: tokens.borderRadiusCircular,
  },
  pageCurrent: {
    backgroundColor: PRIMARY_DARK,
    color: "#ffffff",
    ":hover": {
      backgroundColor: PRIMARY_MAIN,
      color: "#ffffff",
    },
    ":hover:active": {
      backgroundColor: PRIMARY_MAIN,
      color: "#ffffff",
    },
  },
  pageSize: {
    display: "flex",
    alignItems: "center",
    gap: tokens.spacingHorizontalS,
  },
  controls: {
    display: "flex",
    alignItems: "center",
    marginRight: tokens.spacingHorizontalL,
  },
  sizeDropdown: {
    minWidth: "72px",
  },
});

interface Props {
  page: number; // 1-based
  size: number;
  total: number;
  onPage: (page: number) => void;
  onSize: (size: number) => void;
  /** Extra controls (e.g. the sort select) rendered next to the page-size select. */
  controls?: ReactNode;
  /**
   * Name of the landmark, for a page that shows the bar more than once: two
   * navigation regions of one name are the same entry twice in a screen
   * reader's landmark list.
   */
  label?: string;
}

/** Pagination bar: compact circular pager with a window around the current page + page-size select. */
export default function Pagination({ page, size, total, onPage, onSize, controls, label }: Props) {
  const styles = useStyles();
  const { t } = useTranslation();
  // per instance: the bar stands above and below the results, and with a fixed
  // id the second dropdown would be named by the first one's label
  const pageSizeLabelId = useId("page-size-label-");
  const pageCount = Math.max(1, Math.ceil(total / size));

  const pages: number[] = [];
  for (let p = Math.max(1, page - 2); p <= Math.min(pageCount, page + 2); p++) {
    pages.push(p);
  }

  const pager = (label: string, target: number, disabled: boolean, icon: ReactElement) => (
    <Button
      className={styles.pageButton}
      appearance="subtle"
      shape="circular"
      size="small"
      icon={icon}
      aria-label={label}
      title={label}
      disabled={disabled}
      onClick={() => onPage(target)}
    />
  );

  return (
    <nav className={styles.bar} aria-label={label ?? t("search.pagination")}>
      {/* the icons are the scan viewer's set (ViewerToolbar), so the portal's two
          page navigations read alike: bar-and-arrow for an end, double chevron
          for the jump - the meaning it had in the old portal. Nearer than a
          jump to an end it lands on the end, as the old one did */}
      {pager(t("search.firstPage"), 1, page <= 1, <ArrowPrevious16Regular />)}
      {pager(
        t("search.jumpBack", { pages: PAGE_JUMP }),
        Math.max(1, page - PAGE_JUMP),
        page <= 1,
        <ChevronDoubleLeft16Regular />,
      )}
      {pager(t("search.prevPage"), page - 1, page <= 1, <ChevronLeft16Regular />)}
      {pages[0] > 1 && <Text size={200}>…</Text>}
      {pages.map((p) => (
        <Button
          key={p}
          className={
            p === page ? mergeClasses(styles.pageButton, styles.pageCurrent) : styles.pageButton
          }
          appearance="subtle"
          shape="circular"
          size="small"
          aria-label={t("search.page", { page: p })}
          aria-current={p === page ? "page" : undefined}
          onClick={() => onPage(p)}
        >
          {p}
        </Button>
      ))}
      {pages[pages.length - 1] < pageCount && <Text size={200}>…</Text>}
      {pager(t("search.nextPage"), page + 1, page >= pageCount, <ChevronRight16Regular />)}
      {pager(
        t("search.jumpForward", { pages: PAGE_JUMP }),
        Math.min(pageCount, page + PAGE_JUMP),
        page >= pageCount,
        <ChevronDoubleRight16Regular />,
      )}
      {pager(t("search.lastPage"), pageCount, page >= pageCount, <ArrowNext16Regular />)}
      <div className={styles.spacer} />
      {controls !== undefined && <div className={styles.controls}>{controls}</div>}
      <div className={styles.pageSize}>
        {/* the visible text is the dropdown's label; Fluent's trigger button has
            no name of its own (Fluent's documented aria-labelledby pattern) */}
        <Text size={200} id={pageSizeLabelId}>
          {t("search.pageSize")}
        </Text>
        <Dropdown
          className={styles.sizeDropdown}
          aria-labelledby={pageSizeLabelId}
          value={String(size)}
          selectedOptions={[String(size)]}
          onOptionSelect={(_, data) => data.optionValue && onSize(Number(data.optionValue))}
        >
          {PAGE_SIZES.map((s) => (
            <Option key={s} value={String(s)} text={String(s)}>
              {s}
            </Option>
          ))}
        </Dropdown>
      </div>
    </nav>
  );
}
