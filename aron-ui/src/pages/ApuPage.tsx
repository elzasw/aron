import {
  makeStyles,
  mergeClasses,
  Spinner,
  Subtitle2,
  Text,
  Title2,
  tokens,
} from "@fluentui/react-components";
import { type CSSProperties, Fragment, useState } from "react";
import { useTranslation } from "react-i18next";
import { Link, useParams } from "react-router-dom";
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
import { useUiConfig } from "../api/useUiConfig";
import ApuTree from "../apu/ApuTree";
import CitationDialog from "../apu/CitationDialog";
import { citationIsOffered } from "../apu/citations";
import { useApuDetail } from "../apu/useApuDetail";
import DaoGallery from "../dao/DaoGallery";
import DaoViewer from "../dao/DaoViewer";
import { MEDIUM, NARROW_TALL, SHORT, SIDE_BY_SIDE, STACKED, THREE_PANES, media } from "../layout/breakpoints";
import Splitter from "../layout/Splitter";
import useMediaQuery from "../layout/useMediaQuery";
import { relatedSearchUrl } from "../search/filters";

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

/** Width of the description column beside an embedded viewer - the reader's, like the tree's. */
const DESCRIPTION_WIDTH_VAR = "--aron-description-width";
const DEFAULT_DESCRIPTION_WIDTH = 420;
const MIN_DESCRIPTION_WIDTH = 280;
const MAX_DESCRIPTION_WIDTH = 760;
const DESCRIPTION_WIDTH_KEY = "aron.descriptionWidth";

function storedWidth(key: string, min: number, max: number, fallback: number): number {
  try {
    const stored = Number(window.localStorage.getItem(key));
    return stored >= min && stored <= max ? stored : fallback;
  } catch {
    // storage can be unavailable (private mode, blocked cookies) - not fatal
    return fallback;
  }
}

function rememberWidth(key: string, width: number): void {
  try {
    window.localStorage.setItem(key, String(width));
  } catch {
    // a reader without storage simply starts from the default width again
  }
}

/**
 * A digitized record's three arrangements (the old portal's, on its md/lg
 * breakpoints; see layout/breakpoints.ts): stacked on a small screen; three
 * panes tree | viewer | description where they fit; between the two, tree
 * beside one right column with the viewer above the description - three panes
 * across would squeeze the viewer into a sliver, and a link instead of the scan
 * would break the rule that a digitized record shows its scan immediately.
 * Only the wide arrangement has a description column of its own, so this is
 * the one query the component needs; the others live in the styles alone.
 */
const THREE_PANE_QUERY = THREE_PANES;

/** Whether a pane is folded away (the old portal's triangles) - remembered like its width. */
const TREE_COLLAPSED_KEY = "aron.treeCollapsed";
const DESCRIPTION_COLLAPSED_KEY = "aron.descriptionCollapsed";

function storedCollapsed(key: string): boolean {
  try {
    return window.localStorage.getItem(key) === "true";
  } catch {
    return false;
  }
}

function rememberCollapsed(key: string, collapsed: boolean): void {
  try {
    window.localStorage.setItem(key, String(collapsed));
  } catch {
    // a reader without storage simply starts expanded again
  }
}

const useStyles = makeStyles({
  // Two panes filling the frame, each scrolling on its own (the old portal's
  // arrangement): the tree keeps its place while the description is read, and
  // the page itself never scrolls - a second, outer scrollbar would move the
  // tree's own horizontal one out of reach.
  //
  // Positioned against the main region rather than flowing inside it. In flow,
  // the region is sized by this content and would have to be allowed to shrink
  // back below it, which is the same permission a long page needs in order to
  // grow - one rule cannot serve both. Positioned, this page contributes no
  // height at all, so the region keeps exactly the frame's leftover and every
  // page that flows normally is left alone.
  layout: {
    position: "absolute",
    inset: 0,
    display: "flex",
    alignItems: "stretch",
    // the gutter is split by the separator, so each half stays modest
    gap: tokens.spacingHorizontalL,
    padding: `${tokens.spacingVerticalXL} ${tokens.spacingHorizontalXXL}`,
    // small screens - narrow, or short as a phone held sideways - stack the
    // tree above the description and the document scrolls again: two nested
    // scroll areas on a phone are worse than one long page, and a frame-tall
    // pane leaves nothing for the text once header and footer have their rows
    [media(STACKED)]: {
      position: "static",
      flexDirection: "column",
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
    // stretched to the pane row; the tree scrolls inside it
    minHeight: 0,
    [media(STACKED)]: {
      width: "100%",
      maxHeight: "60vh",
    },
  },
  // stacked, the panes sit above each other and there is no width to drag
  splitter: {
    [media(STACKED)]: {
      display: "none",
    },
  },
  // a folded pane - only where the fold-away chevron exists to bring it back;
  // the stacked layout hides the splitters, so it always shows every pane
  collapsedPane: {
    [media(SIDE_BY_SIDE)]: {
      display: "none",
    },
  },
  // the description: the pane that scrolls when a record is long
  root: {
    display: "flex",
    flexDirection: "column",
    gap: tokens.spacingVerticalL,
    maxWidth: "1000px",
    minWidth: 0,
    flexGrow: 1,
    minHeight: 0,
    overflowY: "auto",
    // room for the scrollbar so it does not sit on the text
    paddingRight: tokens.spacingHorizontalM,
    [media(STACKED)]: {
      overflowY: "visible",
      paddingRight: 0,
    },
  },
  // a digitized record shows its scan immediately (the old portal's principle):
  // the viewer is the page's centerpiece and absorbs the free width...
  viewerPane: {
    flexGrow: 1,
    minWidth: 0,
    minHeight: 0,
    display: "flex",
    flexDirection: "column",
    gap: tokens.spacingVerticalXS,
    // stacked, the viewer needs a height of its own - the document scrolls.
    // Upright, most of the screen with the description's start still in view;
    // sideways, the whole of it: scrolling the header away then gives the scan
    // every row there is, and the description follows below
    [media(NARROW_TALL)]: {
      height: "70vh",
      flexGrow: 0,
    },
    [media(SHORT)]: {
      height: "100vh",
      flexGrow: 0,
    },
    // in the middle arrangement it heads the right column, which scrolls as a
    // whole: the scan is on screen at once and the description starts below it
    [media(MEDIUM)]: {
      height: "70%",
      flexGrow: 0,
      flexShrink: 0,
    },
  },
  // viewer and description together: a flat part of the pane row (the three
  // panes and the stacked layout need no grouping), one scrolling column only
  // in the middle arrangement, where the viewer sits above the description
  rightColumn: {
    display: "contents",
    [media(MEDIUM)]: {
      display: "flex",
      flexDirection: "column",
      gap: tokens.spacingVerticalL,
      flexGrow: 1,
      minWidth: 0,
      minHeight: 0,
      overflowY: "auto",
    },
  },
  // ...and the description becomes the right-hand column, scrolling on its own,
  // as wide as the reader drags its splitter (a custom property, like the tree)
  rootBesideViewer: {
    flexGrow: 0,
    flexShrink: 0,
    width: `var(${DESCRIPTION_WIDTH_VAR}, ${DEFAULT_DESCRIPTION_WIDTH}px)`,
  },
  // below the viewer, the description is part of the column's scroll, not a
  // scroller of its own
  rootBelowViewer: {
    flexShrink: 0,
    overflowY: "visible",
    paddingRight: 0,
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
  actions: {
    display: "flex",
    flexWrap: "wrap",
    gap: tokens.spacingHorizontalS,
    paddingTop: tokens.spacingVerticalXS,
  },
  // a navigation, so a real link - styled as the outlined button it reads as
  action: {
    padding: `${tokens.spacingVerticalXS} ${tokens.spacingHorizontalM}`,
    borderRadius: tokens.borderRadiusMedium,
    border: `1px solid ${tokens.colorNeutralStroke1}`,
    color: tokens.colorBrandForegroundLink,
    fontSize: tokens.fontSizeBase200,
    textDecorationLine: "none",
    ":hover": {
      backgroundColor: tokens.colorNeutralBackground1Hover,
      textDecorationLine: "underline",
    },
  },
  // the same surface for the action that opens a dialog: a real button, with
  // the browser's own button styling reset away
  actionButton: {
    backgroundColor: "transparent",
    fontFamily: "inherit",
    cursor: "pointer",
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
  // in the narrow column beside a viewer, rows flow instead of aligning into
  // grid columns: a short value stays on its label's line, a long one wraps
  // below it and takes the full width - two rigid columns would squeeze the
  // values into a sliver
  itemsNarrow: {
    display: "flex",
    flexDirection: "column",
    rowGap: tokens.spacingVerticalXS,
    margin: 0,
  },
  itemRowNarrow: {
    display: "flex",
    flexWrap: "wrap",
    alignItems: "baseline",
    columnGap: tokens.spacingHorizontalL,
    rowGap: tokens.spacingVerticalXXS,
  },
  // the basis decides when a value deserves the label's line: shorter than
  // this fits beside it, anything needing more wraps under it full-width
  itemValueNarrow: {
    margin: 0,
    flexGrow: 1,
    flexBasis: "14rem",
    minWidth: 0,
    overflowWrap: "anywhere",
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
  groupedHeaderNarrow: {
    gridTemplateColumns: "1fr",
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

function ItemRows({ items, narrow }: { items: DetailItem[]; narrow: boolean }) {
  const styles = useStyles();
  if (narrow) {
    return (
      <dl className={styles.itemsNarrow}>
        {items.map((item, index) => (
          <div key={`${item.code}-${index}`} className={styles.itemRowNarrow}>
            <dt className={styles.itemLabel}>{item.label}</dt>
            <dd className={styles.itemValueNarrow}>
              <ItemValue item={item} />
            </dd>
          </div>
        ))}
      </dl>
    );
  }
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
function Part({ part, narrow }: { part: DetailPart; narrow: boolean }) {
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
          className={mergeClasses(styles.groupedHeader, narrow && styles.groupedHeaderNarrow)}
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
        {open && <ItemRows items={items} narrow={narrow} />}
      </section>
    );
  }

  const single = items.length === 1;
  return (
    <section className={styles.part} aria-label={single ? items[0].label : part.label}>
      {!single && <Subtitle2 as="h2">{part.label}</Subtitle2>}
      <ItemRows items={items} narrow={narrow} />
    </section>
  );
}

/** APU detail: server-assembled render model (breadcrumbs, parts, metadata sections). */
export default function ApuPage() {
  const styles = useStyles();
  const { t } = useTranslation();
  const { uuid } = useParams<{ uuid: string }>();
  const [treeWidth, setTreeWidth] = useState(() =>
    storedWidth(TREE_WIDTH_KEY, MIN_TREE_WIDTH, MAX_TREE_WIDTH, DEFAULT_TREE_WIDTH),
  );
  const [descriptionWidth, setDescriptionWidth] = useState(() =>
    storedWidth(DESCRIPTION_WIDTH_KEY, MIN_DESCRIPTION_WIDTH, MAX_DESCRIPTION_WIDTH,
      DEFAULT_DESCRIPTION_WIDTH),
  );
  const [treeCollapsed, setTreeCollapsed] = useState(() => storedCollapsed(TREE_COLLAPSED_KEY));
  const [descriptionCollapsed, setDescriptionCollapsed] = useState(() =>
    storedCollapsed(DESCRIPTION_COLLAPSED_KEY),
  );
  const collapseTree = (collapsed: boolean) => {
    setTreeCollapsed(collapsed);
    rememberCollapsed(TREE_COLLAPSED_KEY, collapsed);
  };
  const collapseDescription = (collapsed: boolean) => {
    setDescriptionCollapsed(collapsed);
    rememberCollapsed(DESCRIPTION_COLLAPSED_KEY, collapsed);
  };
  const [citationOpen, setCitationOpen] = useState(false);
  // whether the description is a column of its own beside the viewer - a JS
  // decision, because it changes what is rendered (its splitter, the row
  // layout of its items), not only how it is laid out
  const threePanes = useMediaQuery(THREE_PANE_QUERY);
  // the same query the breadcrumb strip reads - one request, one truth
  const detail = useApuDetail(uuid);
  // which record types this deployment can cite (cached with the rest of the config)
  const { data: uiConfig } = useUiConfig();

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
  const archdescRoot = data.parts
    .flatMap((part) => part.items)
    .filter(isRef)
    .find((item) => item.code === ARCHDESC_ROOT_REF);

  // the first digital object with content is embedded; any further ones stay
  // reachable through the gallery links in the description
  const embeddedDao = data.digitalObjects.find((dao) => dao.files.length > 0);
  const galleryDaos = data.digitalObjects.filter((dao) => dao !== embeddedDao);
  // the description is a column of its own only in the wide arrangement
  const descriptionColumn = embeddedDao !== undefined && threePanes;

  return (
    <div
      className={styles.layout}
      style={
        {
          [TREE_WIDTH_VAR]: `${treeWidth}px`,
          [DESCRIPTION_WIDTH_VAR]: `${descriptionWidth}px`,
        } as CSSProperties
      }
    >
      {data.apuType === ApuType.ArchDesc && (
        <>
          <aside
            className={mergeClasses(styles.tree, treeCollapsed && styles.collapsedPane)}
          >
            <ApuTree treePath={data.treePath} currentUuid={data.uuid} />
          </aside>
          <Splitter
            label={t("apu.treeWidth")}
            value={treeWidth}
            min={MIN_TREE_WIDTH}
            max={MAX_TREE_WIDTH}
            onChange={setTreeWidth}
            onCommit={(width) => rememberWidth(TREE_WIDTH_KEY, width)}
            collapsed={treeCollapsed}
            onCollapsedChange={collapseTree}
            collapseLabel={t("apu.collapseTree")}
            expandLabel={t("apu.expandTree")}
            className={styles.splitter}
          />
        </>
      )}
      <div className={styles.rightColumn}>
      {embeddedDao !== undefined && (
        <div className={styles.viewerPane}>
          <DaoViewer apuUuid={data.uuid} dao={embeddedDao} showFullscreenLink />
        </div>
      )}
      {/* the description sits right of this separator, so the value grows
          leftwards; only a column of its own has a width to drag or a fold */}
      {descriptionColumn && (
        <Splitter
          reverse
          label={t("apu.descriptionWidth")}
          value={descriptionWidth}
          min={MIN_DESCRIPTION_WIDTH}
          max={MAX_DESCRIPTION_WIDTH}
          onChange={setDescriptionWidth}
          onCommit={(width) => rememberWidth(DESCRIPTION_WIDTH_KEY, width)}
          collapsed={descriptionCollapsed}
          onCollapsedChange={collapseDescription}
          collapseLabel={t("apu.collapseDescription")}
          expandLabel={t("apu.expandDescription")}
          className={styles.splitter}
        />
      )}
      {/* keyed by the record: the description is its own scroll area, and a new
          element starts at its top - the reader never opens a record halfway
          down because the previous one was scrolled */}
      <div
        className={mergeClasses(
          styles.root,
          descriptionColumn && styles.rootBesideViewer,
          descriptionColumn && descriptionCollapsed && styles.collapsedPane,
          embeddedDao !== undefined && !threePanes && styles.rootBelowViewer,
        )}
        key={data.uuid}
      >
      <header className={styles.header}>
        <Title2 as="h1">{data.name}</Title2>
        {data.description && <Text size={400}>{data.description}</Text>}
        <div className={styles.actions}>
          <Link
            to={relatedSearchUrl(data.uuid)}
            className={styles.action}
            aria-label={t("apu.findRelatedFor", { name: data.name })}
          >
            {t("apu.findRelated")}
          </Link>
          {/* offered only where the deployment has a citation form for this
              record type, so the reader is never offered one that cannot be
              produced; it opens a dialog, so a button rather than a link */}
          {citationIsOffered(uiConfig?.citations, data.apuType) && (
            <button
              type="button"
              className={mergeClasses(styles.action, styles.actionButton)}
              aria-label={t("citation.createFor", { name: data.name })}
              onClick={() => setCitationOpen(true)}
            >
              {t("citation.create")}
            </button>
          )}
        </div>
        {citationOpen && (
          <CitationDialog uuid={data.uuid} onClose={() => setCitationOpen(false)} />
        )}
        {archdescRoot && (
          <Link to={`/apu/${archdescRoot.ref.uuid}`} className={styles.link}>
            {archdescRoot.label}
          </Link>
        )}
      </header>
      {data.parts.map((part, index) => (
        <Part key={`${part.code}-${index}`} part={part} narrow={descriptionColumn} />
      ))}
      {data.attachments.length > 0 && (
        <section className={styles.part} aria-label={t("apu.attachments")}>
          <Subtitle2 as="h2">{t("apu.attachments")}</Subtitle2>
          <ul className={styles.fileList}>
            {data.attachments.map((attachment, index) => (
              <li key={index}>
                {/* the server built the URL (or withheld it); ?download=true asks for attachment disposition */}
                {attachment.file?.url !== undefined ? (
                  <a
                    href={`${attachment.file.url}?download=true`}
                    download={attachment.name}
                    className={styles.link}
                  >
                    {attachment.name}
                  </a>
                ) : (
                  <Text>{attachment.name}</Text>
                )}
              </li>
            ))}
          </ul>
        </section>
      )}
      {galleryDaos.length > 0 && (
        <section className={styles.part} aria-label={t("apu.digitalObjects")}>
          <Subtitle2 as="h2">{t("apu.digitalObjects")}</Subtitle2>
          <DaoGallery apuUuid={data.uuid} objects={galleryDaos} />
        </section>
      )}
      </div>
      </div>
    </div>
  );
}
