import { Button, Dropdown, makeStyles, Option, Text, tokens } from "@fluentui/react-components";
import { useTranslation } from "react-i18next";

const PAGE_SIZES = [10, 20, 50];

const useStyles = makeStyles({
  bar: {
    display: "flex",
    alignItems: "center",
    gap: tokens.spacingHorizontalXS,
  },
  spacer: {
    flexGrow: 1,
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

/** Top pagination bar: page numbers with a window around the current page + page-size select. */
export default function Pagination({ page, size, total, onPage, onSize }: Props) {
  const styles = useStyles();
  const { t } = useTranslation();
  const pageCount = Math.max(1, Math.ceil(total / size));

  const pages: number[] = [];
  for (let p = Math.max(1, page - 2); p <= Math.min(pageCount, page + 2); p++) {
    pages.push(p);
  }

  return (
    <div className={styles.bar}>
      <Button appearance="subtle" disabled={page <= 1} onClick={() => onPage(1)}>
        «
      </Button>
      <Button appearance="subtle" disabled={page <= 1} onClick={() => onPage(page - 1)}>
        ‹
      </Button>
      {pages[0] > 1 && <Text>…</Text>}
      {pages.map((p) => (
        <Button
          key={p}
          appearance={p === page ? "primary" : "subtle"}
          onClick={() => onPage(p)}
        >
          {p}
        </Button>
      ))}
      {pages[pages.length - 1] < pageCount && <Text>…</Text>}
      <Button appearance="subtle" disabled={page >= pageCount} onClick={() => onPage(page + 1)}>
        ›
      </Button>
      <Button appearance="subtle" disabled={page >= pageCount} onClick={() => onPage(pageCount)}>
        »
      </Button>
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
    </div>
  );
}
