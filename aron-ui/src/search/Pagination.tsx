import { Button, Dropdown, makeStyles, mergeClasses, Option, Text, tokens } from "@fluentui/react-components";
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
}

/** Pagination bar: compact circular pager with a window around the current page + page-size select. */
export default function Pagination({ page, size, total, onPage, onSize }: Props) {
  const styles = useStyles();
  const { t } = useTranslation();
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
    <nav className={styles.bar} aria-label={t("search.pagination")}>
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
      <div className={styles.pageSize}>
        <Text size={200}>{t("search.pageSize")}</Text>
        <Dropdown
          className={styles.sizeDropdown}
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
