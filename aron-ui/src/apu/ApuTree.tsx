import { Button, makeStyles, mergeClasses, Spinner, tokens } from "@fluentui/react-components";
import { useEffect, useLayoutEffect, useRef, useState } from "react";
import { useTranslation } from "react-i18next";
import { useNavigate } from "react-router-dom";
import { apuApi } from "../api/client";
import { TreeDirection, type TreeNode } from "../api/generated";

/**
 * Incremental archival-description tree (the old portal's mechanism, typed):
 * a flat node list in tree order rendered by depth. Seeded from the detail's
 * treePath; sibling windows and children load on demand through
 * `GET /apu/{uuid}/tree?direction=BEFORE|AFTER|UNDER`.
 *
 * A node is one line. Descriptions run to whole sentences, so wrapping labels
 * would turn the pane into a wall of text and drown the indentation that
 * carries the structure. What a line may not do is grow without end either, so
 * a label stops at LABEL_LIMIT and the rest is reachable three ways: the pane
 * scrolls sideways (the old portal's `treeHorizontalScroll`, here always on),
 * the splitter widens it, and the tooltip carries the whole text.
 */

/** Loaded node; parentUuid is derived client-side (the seed path is a chain). */
type LoadedNode = TreeNode & { parentUuid?: string };

/**
 * How far a node label may run before it is clipped. Wide enough for a title to
 * identify the record, narrow enough that one sentence cannot stretch the pane
 * to a width no scrollbar can navigate.
 */
const LABEL_LIMIT = "60ch";

const useStyles = makeStyles({
  root: {
    display: "flex",
    flexDirection: "column",
    gap: tokens.spacingVerticalXS,
    // the pane is the tree's own viewport: it scrolls, the page stays put -
    // sideways too, which is how a deep row reaches its end
    overflowY: "auto",
    overflowX: "auto",
    // the tree keeps the reader's place itself when rows arrive above them, so
    // the browser must not also correct the scroll position - both would apply
    overflowAnchor: "none",
    flexGrow: 1,
    minHeight: 0,
    minWidth: 0,
  },
  list: {
    listStyleType: "none",
    margin: 0,
    padding: 0,
    display: "flex",
    flexDirection: "column",
  },
  row: {
    display: "flex",
    alignItems: "center",
    gap: tokens.spacingHorizontalXS,
    minHeight: "24px",
    borderRadius: tokens.borderRadiusSmall,
    // as wide as its content, so a long row makes the pane scroll instead of
    // shrinking the label - but never narrower than the pane, or the current
    // row's highlight would stop short of the edge
    width: "max-content",
    minWidth: "100%",
  },
  // the open record keeps a visible place in the tree, not only a bold label
  currentRow: {
    backgroundColor: tokens.colorNeutralBackground1Selected,
  },
  toggle: {
    minWidth: "24px",
    maxWidth: "24px",
    padding: 0,
  },
  togglePlaceholder: {
    width: "24px",
    flexShrink: 0,
  },
  // the old portal's leaf marker: a node without children reads as an end point
  leaf: {
    width: "24px",
    flexShrink: 0,
    textAlign: "center",
    color: tokens.colorNeutralForeground4,
  },
  label: {
    background: "none",
    border: "none",
    padding: 0,
    cursor: "pointer",
    textAlign: "left",
    fontSize: tokens.fontSizeBase300,
    color: tokens.colorNeutralForeground1,
    whiteSpace: "nowrap",
    maxWidth: LABEL_LIMIT,
    overflow: "hidden",
    textOverflow: "ellipsis",
    // room for the ellipsis to sit clear of the pane's right edge
    paddingRight: tokens.spacingHorizontalS,
    ":hover": { textDecorationLine: "underline" },
  },
  current: {
    fontWeight: tokens.fontWeightSemibold,
    cursor: "default",
    ":hover": { textDecorationLine: "none" },
  },
  more: {
    fontSize: tokens.fontSizeBase200,
  },
});

/** Insert after the node's whole subtree (skip deeper nodes). */
function afterSubtree(nodes: LoadedNode[], index: number): number {
  const depth = nodes[index].depth;
  let i = index + 1;
  while (i < nodes.length && nodes[i].depth > depth) {
    i++;
  }
  return i;
}

function insertNodes(
  nodes: LoadedNode[],
  aroundUuid: string,
  direction: TreeDirection,
  fetched: TreeNode[],
): LoadedNode[] {
  const index = nodes.findIndex((node) => node.uuid === aroundUuid);
  if (index < 0) {
    return nodes;
  }
  const around = nodes[index];
  const parentUuid = direction === TreeDirection.Under ? around.uuid : around.parentUuid;
  const known = new Set(nodes.map((node) => node.uuid));
  const inserted = fetched
    .filter((node) => !known.has(node.uuid))
    .map((node) => ({ ...node, parentUuid }));
  const position =
    direction === TreeDirection.Before
      ? index
      : direction === TreeDirection.Under
        ? index + 1
        : afterSubtree(nodes, index);
  const next = [...nodes];
  next.splice(position, 0, ...inserted);
  return next;
}

export default function ApuTree({ treePath, currentUuid }: { treePath: TreeNode[]; currentUuid: string }) {
  const styles = useStyles();
  const { t } = useTranslation();
  const navigate = useNavigate();

  const [nodes, setNodes] = useState<LoadedNode[]>([]);
  const [collapsed, setCollapsed] = useState<Set<string>>(new Set());
  const [loading, setLoading] = useState(false);
  /** APU whose fan-out has landed - the moment the tree can be positioned. */
  const [seeded, setSeeded] = useState<string>();
  const paneRef = useRef<HTMLDivElement>(null);
  const currentRowRef = useRef<HTMLDivElement>(null);
  /** Where the reader was before rows were inserted above them. */
  const anchor = useRef<{ scrollTop: number; scrollHeight: number } | null>(null);

  // a different record means a different tree: drop what the reader had folded.
  // Adjusting during render rather than in the loading effect below keeps the
  // stale collapse from surviving one paint.
  const [lastUuid, setLastUuid] = useState(currentUuid);
  if (currentUuid !== lastUuid) {
    setLastUuid(currentUuid);
    setCollapsed(new Set());
  }

  const fetchNodes = (uuid: string, direction: TreeDirection) =>
    apuApi.apuGetTreeNodes({ uuid, direction });

  // seed from the tree path and fan out around it (the old portal's rule):
  // earlier/later siblings of every path node, children of the APU itself
  useEffect(() => {
    let cancelled = false;
    const seed: LoadedNode[] = treePath.map((node, index) => ({
      ...node,
      parentUuid: index > 0 ? treePath[index - 1].uuid : undefined,
    }));
    const load = async () => {
      setLoading(true);
      const requests: { uuid: string; direction: TreeDirection }[] = [];
      seed.forEach((node, index) => {
        const parent = index > 0 ? seed[index - 1] : undefined;
        if (node.pos > 1) {
          requests.push({ uuid: node.uuid, direction: TreeDirection.Before });
        }
        if (parent === undefined || node.pos !== parent.childCount) {
          requests.push({ uuid: node.uuid, direction: TreeDirection.After });
        }
        if (index === seed.length - 1 && node.childCount > 0) {
          requests.push({ uuid: node.uuid, direction: TreeDirection.Under });
        }
      });
      try {
        const results = await Promise.all(
          requests.map(async (request) => ({
            ...request,
            fetched: await fetchNodes(request.uuid, request.direction),
          })),
        );
        if (!cancelled) {
          let next = seed;
          for (const result of results) {
            next = insertNodes(next, result.uuid, result.direction, result.fetched);
          }
          setNodes(next);
        }
      } catch {
        // the tree is a navigation aid - a failed fan-out keeps the plain path
        if (!cancelled) {
          setNodes(seed);
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
          setSeeded(currentUuid);
        }
      }
    };
    load();
    return () => {
      cancelled = true;
    };
    // treePath always changes together with the record it leads to, and its
    // array identity is new on every render - reloading on it would loop
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [currentUuid]);

  // A record opened by its own URL lands the reader anywhere inside the fund, so
  // the tree brings the open record into view itself - once per record, as soon
  // as its surroundings have arrived. The pane is scrolled directly instead of
  // through scrollIntoView(), which would scroll the page as well and take the
  // header out of view.
  useEffect(() => {
    const pane = paneRef.current;
    const row = currentRowRef.current;
    if (seeded !== currentUuid || pane === null || row === null) {
      return;
    }
    const rowBox = row.getBoundingClientRect();
    pane.scrollTop +=
      rowBox.top - pane.getBoundingClientRect().top - (pane.clientHeight - rowBox.height) / 2;
  }, [seeded, currentUuid]);

  // Earlier siblings arrive above what the reader is reading, so the pane is
  // moved down by exactly what appeared: the rows grow upwards and the node
  // they were asked for stays under the same pixel. Runs before the paint, or
  // the jump would be visible.
  useLayoutEffect(() => {
    const pane = paneRef.current;
    const kept = anchor.current;
    anchor.current = null;
    if (pane === null || kept === null) {
      return;
    }
    pane.scrollTop = kept.scrollTop + pane.scrollHeight - kept.scrollHeight;
  }, [nodes]);

  const childrenLoaded = (node: LoadedNode) =>
    nodes.some((candidate) => candidate.parentUuid === node.uuid);

  const toggle = async (node: LoadedNode) => {
    if (childrenLoaded(node)) {
      const next = new Set(collapsed);
      if (next.has(node.uuid)) {
        next.delete(node.uuid);
      } else {
        next.add(node.uuid);
      }
      setCollapsed(next);
      return;
    }
    setLoading(true);
    try {
      const fetched = await fetchNodes(node.uuid, TreeDirection.Under);
      setNodes((current) => insertNodes(current, node.uuid, TreeDirection.Under, fetched));
    } finally {
      setLoading(false);
    }
  };

  const loadSiblings = async (edge: LoadedNode, direction: TreeDirection) => {
    setLoading(true);
    try {
      const fetched = await fetchNodes(edge.uuid, direction);
      if (direction === TreeDirection.Before && paneRef.current !== null) {
        // rows about to appear above the reader would otherwise push everything
        // down by their height; remember the place to restore (see below)
        anchor.current = {
          scrollTop: paneRef.current.scrollTop,
          scrollHeight: paneRef.current.scrollHeight,
        };
      }
      setNodes((current) => insertNodes(current, edge.uuid, direction, fetched));
    } finally {
      setLoading(false);
    }
  };

  // a node is hidden when any loaded ancestor is collapsed
  const hidden = (node: LoadedNode): boolean => {
    let parentUuid = node.parentUuid;
    while (parentUuid !== undefined) {
      if (collapsed.has(parentUuid)) {
        return true;
      }
      parentUuid = nodes.find((candidate) => candidate.uuid === parentUuid)?.parentUuid;
    }
    return false;
  };

  const minDepth = nodes.length > 0 ? Math.min(...nodes.map((node) => node.depth)) : 0;
  const siblingsOf = (parentUuid: string | undefined) =>
    nodes.filter((node) => node.parentUuid === parentUuid);

  return (
    <div className={styles.root} ref={paneRef}>
      <ul role="tree" aria-label={t("apu.tree")} className={styles.list}>
        {nodes.map((node) => {
          if (hidden(node)) {
            return null;
          }
          const isCurrent = node.uuid === currentUuid;
          const expandable = node.childCount > 0;
          const expanded = childrenLoaded(node) && !collapsed.has(node.uuid);
          const label = node.description || node.name;
          const parent = nodes.find((candidate) => candidate.uuid === node.parentUuid);
          const siblings = siblingsOf(node.parentUuid);
          const firstLoaded = siblings[0]?.uuid === node.uuid && node.pos > 1;
          const lastLoaded =
            siblings[siblings.length - 1]?.uuid === node.uuid &&
            parent !== undefined &&
            node.pos < parent.childCount;
          return (
            <li
              key={node.uuid}
              role="treeitem"
              aria-level={node.depth - minDepth + 1}
              aria-selected={isCurrent}
              aria-expanded={expandable ? expanded : undefined}
            >
              {firstLoaded && (
                <div className={styles.row} style={{ paddingLeft: `${(node.depth - minDepth) * 16}px` }}>
                  <span className={styles.togglePlaceholder} />
                  <Button
                    appearance="transparent"
                    size="small"
                    className={styles.more}
                    onClick={() => loadSiblings(node, TreeDirection.Before)}
                  >
                    {t("apu.treeMoreBefore")}
                  </Button>
                </div>
              )}
              <div
                className={mergeClasses(styles.row, isCurrent && styles.currentRow)}
                style={{ paddingLeft: `${(node.depth - minDepth) * 16}px` }}
                ref={isCurrent ? currentRowRef : undefined}
              >
                {expandable ? (
                  <Button
                    appearance="transparent"
                    size="small"
                    className={styles.toggle}
                    aria-label={t(expanded ? "apu.treeCollapse" : "apu.treeExpand", { name: label })}
                    onClick={() => toggle(node)}
                  >
                    <span aria-hidden="true">{expanded ? "▾" : "▸"}</span>
                  </Button>
                ) : (
                  <span aria-hidden="true" className={styles.leaf}>
                    &ndash;
                  </span>
                )}
                {/* the drawn label may be clipped - the tooltip is the whole text */}
                <button
                  type="button"
                  title={label}
                  className={mergeClasses(styles.label, isCurrent && styles.current)}
                  aria-current={isCurrent ? "page" : undefined}
                  onClick={() => !isCurrent && navigate(`/apu/${node.uuid}`)}
                >
                  {label}
                </button>
              </div>
              {lastLoaded && (
                <div className={styles.row} style={{ paddingLeft: `${(node.depth - minDepth) * 16}px` }}>
                  <span className={styles.togglePlaceholder} />
                  <Button
                    appearance="transparent"
                    size="small"
                    className={styles.more}
                    onClick={() => loadSiblings(node, TreeDirection.After)}
                  >
                    {t("apu.treeMoreAfter")}
                  </Button>
                </div>
              )}
            </li>
          );
        })}
      </ul>
      {loading && <Spinner size="tiny" />}
    </div>
  );
}
