import { Button, makeStyles, mergeClasses, Spinner, tokens } from "@fluentui/react-components";
import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import { useNavigate } from "react-router-dom";
import { apuApi } from "../api/client";
import { TreeDirection, type TreeNode } from "../api/generated";

/**
 * Incremental archival-description tree (the old portal's mechanism, typed):
 * a flat node list in tree order rendered by depth. Seeded from the detail's
 * treePath; sibling windows and children load on demand through
 * `GET /apu/{uuid}/tree?direction=BEFORE|AFTER|UNDER`.
 */

/** Loaded node; parentUuid is derived client-side (the seed path is a chain). */
type LoadedNode = TreeNode & { parentUuid?: string };

const useStyles = makeStyles({
  root: {
    display: "flex",
    flexDirection: "column",
    gap: tokens.spacingVerticalXS,
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
    minHeight: "28px",
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
  label: {
    background: "none",
    border: "none",
    padding: 0,
    cursor: "pointer",
    textAlign: "left",
    fontSize: tokens.fontSizeBase300,
    color: tokens.colorBrandForegroundLink,
    ":hover": { textDecorationLine: "underline" },
  },
  current: {
    color: tokens.colorNeutralForeground1,
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
        }
      }
    };
    setCollapsed(new Set());
    load();
    return () => {
      cancelled = true;
    };
  }, [currentUuid]);

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
    <div className={styles.root}>
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
              <div className={styles.row} style={{ paddingLeft: `${(node.depth - minDepth) * 16}px` }}>
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
                  <span className={styles.togglePlaceholder} />
                )}
                <button
                  type="button"
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
