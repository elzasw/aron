import {
  makeStyles,
  Spinner,
  Subtitle2,
  Text,
  Title2,
  tokens,
} from "@fluentui/react-components";
import { useQuery } from "@tanstack/react-query";
import { Fragment, useState } from "react";
import { useTranslation } from "react-i18next";
import { Link, useParams } from "react-router-dom";
import { apuApi } from "../api/client";
import {
  ApuType,
  type DetailItem,
  DetailItemKind,
  type DetailPart,
  PartViewType,
  ResponseError,
} from "../api/generated";
import ApuTree from "../apu/ApuTree";

/**
 * The fund's reference to its archival-description tree root - rendered as a
 * standalone link above the parts, never as an ordinary item row (the old
 * portal's behavior; the render model keeps type codes exactly for such cases).
 */
const ARCHDESC_ROOT_REF = "ARCHDESC~ROOT~REF";

const useStyles = makeStyles({
  layout: {
    display: "flex",
    alignItems: "flex-start",
    gap: tokens.spacingHorizontalXXL,
    padding: `${tokens.spacingVerticalXL} ${tokens.spacingHorizontalXXL}`,
    // narrow viewports stack the tree above the description
    "@media (max-width: 860px)": {
      flexDirection: "column",
      alignItems: "stretch",
      padding: `${tokens.spacingVerticalL} ${tokens.spacingHorizontalM}`,
    },
  },
  tree: {
    width: "320px",
    flexShrink: 0,
    overflowX: "auto",
    "@media (max-width: 860px)": {
      width: "100%",
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

function ItemValue({ item }: { item: DetailItem }) {
  const styles = useStyles();
  switch (item.kind) {
    case DetailItemKind.Ref:
      return item.ref ? (
        <Link to={`/apu/${item.ref.uuid}`} className={styles.link}>
          {item.ref.name}
        </Link>
      ) : (
        <>{item.value}</>
      );
    case DetailItemKind.Link:
      return (
        <a href={item.href ?? item.value} target="_blank" rel="noreferrer" className={styles.link}>
          {item.value}
        </a>
      );
    case DetailItemKind.Json:
      return <pre className={styles.json}>{item.value}</pre>;
    default:
      return <>{item.value}</>;
  }
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
      .map((item) => item.value)
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
      {!single && <Subtitle2>{part.label}</Subtitle2>}
      <ItemRows items={items} />
    </section>
  );
}

/** APU detail: server-assembled render model (breadcrumbs, parts, metadata sections). */
export default function ApuPage() {
  const styles = useStyles();
  const { t } = useTranslation();
  const { uuid } = useParams<{ uuid: string }>();

  const detail = useQuery({
    queryKey: ["apu-detail", uuid],
    queryFn: () => apuApi.apuGetDetail({ uuid: uuid! }),
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
    .find((item) => item.code === ARCHDESC_ROOT_REF);

  return (
    <div className={styles.layout}>
      {data.apuType === ApuType.ArchDesc && (
        <aside className={styles.tree}>
          <ApuTree treePath={data.treePath} currentUuid={data.uuid} />
        </aside>
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
        <Title2 as="h2">{data.name}</Title2>
        {data.description && <Text size={400}>{data.description}</Text>}
        {archdescRoot?.ref && (
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
          <Subtitle2>{t("apu.attachments")}</Subtitle2>
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
          <Subtitle2>{t("apu.digitalObjects")}</Subtitle2>
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
