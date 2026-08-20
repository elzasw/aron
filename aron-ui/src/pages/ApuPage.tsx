import {
  makeStyles,
  Spinner,
  Subtitle2,
  Text,
  Title2,
  tokens,
} from "@fluentui/react-components";
import { useQuery } from "@tanstack/react-query";
import { useApiLanguage } from "../i18n/useApiLanguage";
import { type CSSProperties, Fragment, useState } from "react";
import { useTranslation } from "react-i18next";
import { Link, useParams } from "react-router-dom";
import { apuApi } from "../api/client";
import {
  ApuType,
  type DetailItem,
  DetailItemKind,
  JsonItem,
  LinkItem,
  RefItem,
  TextItem,
  UnitDateItem,
  type DetailPart,
  PartViewType,
  ResponseError,
} from "../api/generated";
import ApuTree from "../apu/ApuTree";
import Splitter from "../layout/Splitter";

/**
 * The fund's reference to its archival-description tree root - rendered as a
 * standalone link above the parts, never as an ordinary item row (the old
 * portal's behavior; the render model keeps type codes exactly for such cases).
 */
const ARCHDESC_ROOT_REF = "ARCHDESC~ROOT~REF";

/**
 * Width of the tree pane: the reader drags the splitter, and the value travels
 * to the stylesheet as a custom property (see the `tree` rule) so the narrow
 * layout can still drop it. It is remembered because the fitting width is a
 * property of the reader's screen and their material, not of one record.
 */
const TREE_WIDTH_VAR = "--aron-tree-width";
const DEFAULT_TREE_WIDTH = 320;
const MIN_TREE_WIDTH = 200;
const MAX_TREE_WIDTH = 640;
const TREE_WIDTH_KEY = "aron.treeWidth";

function storedTreeWidth(): number {
  try {
    const stored = Number(window.localStorage.getItem(TREE_WIDTH_KEY));
    return stored >= MIN_TREE_WIDTH && stored <= MAX_TREE_WIDTH ? stored : DEFAULT_TREE_WIDTH;
  } catch {
    // storage can be unavailable (private mode, blocked cookies) - not fatal
    return DEFAULT_TREE_WIDTH;
  }
}

function rememberTreeWidth(width: number): void {
  try {
    window.localStorage.setItem(TREE_WIDTH_KEY, String(width));
  } catch {
    // a reader without storage simply starts from the default width again
  }
}

const useStyles = makeStyles({
  layout: {
    display: "flex",
    alignItems: "flex-start",
    // the gutter is split by the separator, so each half stays modest
    gap: tokens.spacingHorizontalL,
    padding: `${tokens.spacingVerticalXL} ${tokens.spacingHorizontalXXL}`,
    // narrow viewports stack the tree above the description
    "@media (max-width: 860px)": {
      flexDirection: "column",
      alignItems: "stretch",
      gap: tokens.spacingVerticalXL,
      padding: `${tokens.spacingVerticalL} ${tokens.spacingHorizontalM}`,
    },
  },
  tree: {
    // the reader's width, kept as a custom property so the stacked layout below
    // can still override it - an inline width could not be overridden at all
    width: `var(${TREE_WIDTH_VAR}, ${DEFAULT_TREE_WIDTH}px)`,
    flexShrink: 0,
    minWidth: 0,
    display: "flex",
    flexDirection: "column",
    // the tree stays beside the description while the description scrolls
    position: "sticky",
    top: tokens.spacingVerticalM,
    maxHeight: `calc(100vh - 2 * ${tokens.spacingVerticalM})`,
    "@media (max-width: 860px)": {
      width: "100%",
      position: "static",
      maxHeight: "60vh",
    },
  },
  // stacked, the panes sit above each other and there is no width to drag
  splitter: {
    "@media (max-width: 860px)": {
      display: "none",
    },
  },
  root: {
    display: "flex",
    flexDirection: "column",
    gap: tokens.spacingVerticalL,
    maxWidth: "1000px",
    minWidth: 0,
    flexGrow: 1,
  },
  breadcrumbs: {
    display: "flex",
    flexWrap: "wrap",
    alignItems: "center",
    gap: tokens.spacingHorizontalXS,
    color: tokens.colorNeutralForeground3,
  },
  link: {
    color: tokens.colorBrandForegroundLink,
    textDecorationLine: "none",
    ":hover": { textDecorationLine: "underline" },
  },
  header: {
    display: "flex",
    flexDirection: "column",
    gap: tokens.spacingVerticalXS,
  },
  part: {
    display: "flex",
    flexDirection: "column",
    gap: tokens.spacingVerticalS,
    paddingTop: tokens.spacingVerticalM,
    borderTop: `1px solid ${tokens.colorNeutralStroke2}`,
  },
  items: {
    display: "grid",
    gridTemplateColumns: "minmax(160px, 240px) 1fr",
    columnGap: tokens.spacingHorizontalL,
    rowGap: tokens.spacingVerticalXS,
    margin: 0,
    "@media (max-width: 640px)": {
      gridTemplateColumns: "1fr",
      rowGap: tokens.spacingVerticalXXS,
    },
  },
  // collapsed GROUPED part: one line of part label + item-value summary,
  // aligned with the item grid so labels form one column
  groupedHeader: {
    display: "grid",
    gridTemplateColumns: "minmax(160px, 240px) 1fr",
    columnGap: tokens.spacingHorizontalL,
    alignItems: "baseline",
    background: "none",
    border: "none",
    padding: 0,
    cursor: "pointer",
    textAlign: "left",
    fontSize: tokens.fontSizeBase300,
    "@media (max-width: 640px)": {
      gridTemplateColumns: "1fr",
    },
  },
  groupedSummary: {
    fontWeight: tokens.fontWeightSemibold,
    overflowWrap: "anywhere",
  },
  groupedChevron: {
    marginLeft: tokens.spacingHorizontalS,
    color: tokens.colorNeutralForeground3,
  },
  itemLabel: {
    color: tokens.colorNeutralForeground3,
    margin: 0,
  },
  itemValue: {
    margin: 0,
    overflowWrap: "anywhere",
  },
  json: {
    margin: 0,
    padding: tokens.spacingVerticalS,
    backgroundColor: tokens.colorNeutralBackground2,
    overflowX: "auto",
    fontSize: tokens.fontSizeBase200,
  },
  fileList: {
    margin: 0,
    paddingLeft: tokens.spacingHorizontalXL,
  },
});

/**
 * The contract discriminates items by `kind`, each kind being its own model.
 * The generated subtypes inherit `kind` from the base (it is the discriminator,
 * not a field of theirs), so these predicates are what lets TypeScript narrow -
 * after them every branch sees only the fields its kind actually has.
 */
const isText = (item: DetailItem): item is TextItem => item.kind === DetailItemKind.Text;
const isUnitDate = (item: DetailItem): item is UnitDateItem => item.kind === DetailItemKind.Unitdate;
const isLink = (item: DetailItem): item is LinkItem => item.kind === DetailItemKind.Link;
const isRef = (item: DetailItem): item is RefItem => item.kind === DetailItemKind.Ref;
const isJson = (item: DetailItem): item is JsonItem => item.kind === DetailItemKind.Json;

/** Plain display text of an item - what a summary or a tooltip shows. */
function itemText(item: DetailItem): string {
  if (isText(item) || isUnitDate(item)) {
    return item.value;
  }
  if (isLink(item)) {
    return item.caption;
  }
  if (isRef(item)) {
    return item.ref.name;
  }
  return isJson(item) ? item.json : "";
}

function ItemValue({ item }: { item: DetailItem }) {
  const styles = useStyles();
  if (isRef(item)) {
    return (
      <Link to={`/apu/${item.ref.uuid}`} className={styles.link}>
        {item.ref.name}
      </Link>
    );
  }
  if (isLink(item)) {
    return (
      <a href={item.href} target="_blank" rel="noreferrer" className={styles.link}>
        {item.caption}
      </a>
    );
  }
  if (isJson(item)) {
    return <pre className={styles.json}>{item.json}</pre>;
  }
  if (isUnitDate(item)) {
    // a single-valued dating carries its machine-readable form into the markup;
    // <time> has no range form, so an interval stays plain text
    return item.from !== undefined && (item.to === undefined || item.to === item.from) ? (
      <time dateTime={item.from}>{item.value}</time>
    ) : (
      <>{item.value}</>
    );
  }
  return <>{itemText(item)}</>;
}

function ItemRows({ items }: { items: DetailItem[] }) {
  const styles = useStyles();
  return (
    <dl className={styles.items}>
      {items.map((item, index) => (
        <Fragment key={`${item.code}-${index}`}>
          <dt className={styles.itemLabel}>{item.label}</dt>
          <dd className={styles.itemValue}>
            <ItemValue item={item} />
          </dd>
        </Fragment>
      ))}
    </dl>
  );
}

/**
 * One part of the detail. Follows the old portal's display rules: the part's
 * own textual value is never rendered (it only duplicates the items); a
 * GROUPED part collapses to one line - part label plus a summary of its item
 * values (references excluded) - expandable to the full rows; a STANDALONE
 * part with a single item collapses to one label/value row without the part
 * header.
 */
function Part({ part }: { part: DetailPart }) {
  const styles = useStyles();
  const [open, setOpen] = useState(false);
  const items = part.items.filter((item) => item.code !== ARCHDESC_ROOT_REF);
  if (items.length === 0) {
    return null;
  }

  if (part.viewType === PartViewType.Grouped) {
    const summary = items
      .filter((item) => item.kind !== DetailItemKind.Ref)
      .map(itemText)
      .join(" ");
    return (
      <section className={styles.part} aria-label={part.label}>
        <button
          type="button"
          className={styles.groupedHeader}
          aria-expanded={open}
          onClick={() => setOpen(!open)}
        >
          <span className={styles.itemLabel}>{part.label}</span>
          <span className={styles.groupedSummary}>
            {summary}
            <span aria-hidden="true" className={styles.groupedChevron}>
              {open ? "▾" : "▸"}
            </span>
          </span>
        </button>
        {open && <ItemRows items={items} />}
      </section>
    );
  }

  const single = items.length === 1;
  return (
    <section className={styles.part} aria-label={single ? items[0].label : part.label}>
      {!single && <Subtitle2 as="h2">{part.label}</Subtitle2>}
      <ItemRows items={items} />
    </section>
  );
}

/** APU detail: server-assembled render model (breadcrumbs, parts, metadata sections). */
export default function ApuPage() {
  const styles = useStyles();
  const { t } = useTranslation();
  const { uuid } = useParams<{ uuid: string }>();
  const lang = useApiLanguage();
  const [treeWidth, setTreeWidth] = useState(storedTreeWidth);

  const detail = useQuery({
    queryKey: ["apu-detail", uuid, lang],
    queryFn: () => apuApi.apuGetDetail({ uuid: uuid!, lang }),
    enabled: uuid !== undefined,
    staleTime: 5 * 60 * 1000,
    retry: (failureCount, error) =>
      !(error instanceof ResponseError && error.response.status === 404) && failureCount < 2,
  });

  if (detail.isPending) {
    return <Spinner className={styles.layout} />;
  }
  if (detail.isError) {
    const notFound =
      detail.error instanceof ResponseError && detail.error.response.status === 404;
    return (
      <div className={styles.layout}>
        <Text>{t(notFound ? "apu.notFound" : "apu.error")}</Text>
      </div>
    );
  }

  const data = detail.data;
  const ancestors = data.treePath.slice(0, -1);
  const archdescRoot = data.parts
    .flatMap((part) => part.items)
    .filter(isRef)
    .find((item) => item.code === ARCHDESC_ROOT_REF);

  return (
    <div
      className={styles.layout}
      style={{ [TREE_WIDTH_VAR]: `${treeWidth}px` } as CSSProperties}
    >
      {data.apuType === ApuType.ArchDesc && (
        <>
          <aside className={styles.tree}>
            <ApuTree treePath={data.treePath} currentUuid={data.uuid} />
          </aside>
          <Splitter
            label={t("apu.treeWidth")}
            value={treeWidth}
            min={MIN_TREE_WIDTH}
            max={MAX_TREE_WIDTH}
            onChange={setTreeWidth}
            onCommit={rememberTreeWidth}
            className={styles.splitter}
          />
        </>
      )}
      <div className={styles.root}>
      {ancestors.length > 0 && (
        <nav aria-label={t("apu.breadcrumbs")} className={styles.breadcrumbs}>
          {ancestors.map((ancestor) => (
            <Fragment key={ancestor.uuid}>
              <Link to={`/apu/${ancestor.uuid}`} className={styles.link}>
                {ancestor.name}
              </Link>
              <span aria-hidden="true">›</span>
            </Fragment>
          ))}
          <span aria-current="page">{data.name}</span>
        </nav>
      )}
      <header className={styles.header}>
        <Title2 as="h1">{data.name}</Title2>
        {data.description && <Text size={400}>{data.description}</Text>}
        {archdescRoot && (
          <Link to={`/apu/${archdescRoot.ref.uuid}`} className={styles.link}>
            {archdescRoot.label}
          </Link>
        )}
      </header>
      {data.parts.map((part, index) => (
        <Part key={`${part.code}-${index}`} part={part} />
      ))}
      {data.attachments.length > 0 && (
        <section className={styles.part} aria-label={t("apu.attachments")}>
          <Subtitle2 as="h2">{t("apu.attachments")}</Subtitle2>
          <ul className={styles.fileList}>
            {data.attachments.map((attachment, index) => (
              <li key={index}>
                <Text>{attachment.name}</Text>
              </li>
            ))}
          </ul>
          <Text size={200}>{t("apu.binariesLater")}</Text>
        </section>
      )}
      {data.digitalObjects.length > 0 && (
        <section className={styles.part} aria-label={t("apu.digitalObjects")}>
          <Subtitle2 as="h2">{t("apu.digitalObjects")}</Subtitle2>
          <ul className={styles.fileList}>
            {data.digitalObjects.map((digitalObject) => (
              <li key={digitalObject.uuid}>
                <Text>
                  {digitalObject.name ?? digitalObject.uuid}{" "}
                  ({t("apu.digitalObjectFiles", { count: digitalObject.files.length })})
                </Text>
              </li>
            ))}
          </ul>
          <Text size={200}>{t("apu.binariesLater")}</Text>
        </section>
      )}
      </div>
    </div>
  );
}
