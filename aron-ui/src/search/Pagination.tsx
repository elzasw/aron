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
import type { ReactNode } from "react";
import { useTranslation } from "react-i18next";
import { PRIMARY_DARK, PRIMARY_MAIN } from "../layout/AppHeader";

const PAGE_SIZES = [10, 20, 50];

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

  const pager = (label: string, target: number, disabled: boolean, symbol: string) => (
    <Button
      className={styles.pageButton}
      appearance="subtle"
      shape="circular"
      size="small"
      aria-label={label}
      title={label}
      disabled={disabled}
      onClick={() => onPage(target)}
    >
      {symbol}
    </Button>
  );

  return (
    <nav className={styles.bar} aria-label={label ?? t("search.pagination")}>
      {pager(t("search.firstPage"), 1, page <= 1, "«")}
      {pager(t("search.prevPage"), page - 1, page <= 1, "‹")}
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
      {pager(t("search.nextPage"), page + 1, page >= pageCount, "›")}
      {pager(t("search.lastPage"), pageCount, page >= pageCount, "»")}
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
