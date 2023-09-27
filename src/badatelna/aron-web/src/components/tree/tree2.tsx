import React, { useState, useEffect } from 'react';
import classNames from 'classnames';
import TreeView from '@material-ui/lab/TreeView';
import TreeItem from '@material-ui/lab/TreeItem';
import ExpandMoreIcon from '@material-ui/icons/ExpandMore';
import ChevronRightIcon from '@material-ui/icons/ChevronRight';
import RemoveIcon from '@material-ui/icons/Remove';

import { useStyles } from './styles';
// import { useConfiguration } from '../configuration';
import { createUrl } from '../../common-utils';
// import { useParams } from 'react-router-dom';

interface Props {
  apuId: string;
  // items: TreeLevelWithChildren[];
  className?: string;
  disableClick?: any;
  expanded?: string[];
  selected?: string[];
  initialItems?: TreeLevel[];
  labelMapper?: (item: TreeLevel) => string;
  idMapper?: (item: TreeLevel) => string;
  onLabelClick?: (item: any) => void;
  onNodeToggle?: (expanded: string[]) => void;
}

interface TreeLevel {
  id: string;
  parentId: string;
  pos: number;
  depth: number;
  childCnt: number;
  description: string;
  name: string;
}

enum NodeDirection {
  AFTER = 'after',
  BEFORE = 'before',
  UNDER = 'under',
}

async function fetchNodes(apuId: string, direction: NodeDirection): Promise<TreeLevel[]> {
  const nodesResponse = await fetch(createUrl(`/apu/${apuId}/related/${direction}`));
  return await nodesResponse.json();
}

// export function findNode(nodeId: string, nodes: TreeLevel[]): [TreeLevel | undefined, string[], TreeLevel[]] {
//   const idPath: string[] = [];
//   const nodePath: TreeLevel[] = [];
//   let foundNode: TreeLevel | undefined = undefined;
//
//   nodes.find((node) => {
//     if (node.id === nodeId) {
//       foundNode = node;
//       idPath.push(node.id);
//       nodePath.push(node);
//       return true
//     } else if (node.children && node.children.length > 0) {
//       const [_foundNode, foundIdPath, foundNodePath] = findNode(nodeId, node.children);
//       if (_foundNode != undefined) {
//         foundNode = _foundNode;
//         idPath.push(node.id, ...foundIdPath);
//         nodePath.push(node, ...foundNodePath)
//         return true;
//       }
//     }
//     return false;
//   })
//
//   return [foundNode, idPath, nodePath]
// }
//
// export function modifyNode(nodeId: string, nodes: TreeLevel[]) {
//   const newNodes = [...nodes];
//   const [node] = findNode(nodeId, newNodes);
//
//   if (node) {
//     node.name = "test";
//     node.description = "test 2";
//   }
//   // console.log(nodes, newNodes, node)
//   return newNodes;
// }

interface BatchNodeResult {
  levels: TreeLevel[];
  id: string;
  direction: NodeDirection;
}

function useTree(apuId: string, initialItems: TreeLevel[] = []): [TreeLevel[], (apuId: string, direction: NodeDirection) => Promise<void>, boolean] {
  console.log("initial items", initialItems);
  const [isLoading, setIsLoading] = useState(false);
  const [treeLevels, setTreeLevels] = useState<TreeLevel[]>(initialItems);

  const initialDataLoad = async () => {
    const promiseQueue: (() => Promise<BatchNodeResult>)[] = [];

    initialItems.forEach(({ id, depth, pos }, index) => {
      const previousLevel = initialItems[index - 1]?.depth === depth ? initialItems[index - 1] : undefined;
      console.log("previous level", previousLevel)
      if (!previousLevel && pos > 1) {
        promiseQueue.push(async () => {
          const direction = NodeDirection.BEFORE;
          const levels = await fetchNodes(id, direction);
          return { levels, id, direction }
        });
      }
      promiseQueue.push(async () => {
        const direction = NodeDirection.AFTER;
        const levels = await fetchNodes(id, direction);
        return { levels, id, direction }
      });
    })
    const results = await Promise.all(promiseQueue.map((promise) => promise()));
    console.log("initial load result", results);

    const newTreeLevels = [...initialItems];
    results.forEach(({ id, levels, direction }) => {
      const index = newTreeLevels.findIndex((level) => level.id === id);
      let indexToPlace: number | undefined = undefined;

      if (direction === NodeDirection.BEFORE) { indexToPlace = index; }
      else if (direction === NodeDirection.AFTER) {
        const { depth } = newTreeLevels[index];

        for (let i = index + 1; i <= newTreeLevels.length; i++) {
          const nextLevel = newTreeLevels[i];

          if (!nextLevel || nextLevel.depth < depth) {
            indexToPlace = i;
            break;
          }

          if (nextLevel.depth > depth) {
            continue;
          }
        }
      }
      // const possibleAfterIndex = newTreeLevels.findIndex((level, _index) => _index > index && level.depth >= newTreeLevels[index].depth);
      // const indexToPlace = direction === NodeDirection.BEFORE ? possibleBeforeIndex : possibleAfterIndex >= 0 ? possibleAfterIndex : newTreeLevels.length;
      if (indexToPlace == undefined) { return; }

      newTreeLevels.splice(indexToPlace, 0, ...levels);
      console.log("tree levels", [...levels], direction, "source index:", index, "target index:", indexToPlace, [...newTreeLevels]);

    })
    setTreeLevels(newTreeLevels);
  }

  const getRelatedNodes = async (nodeId: string, direction: NodeDirection) => {
    console.log(`loading nodes for: ${nodeId}/${direction}, currentNodes:`, treeLevels);
    const nodeIndex = treeLevels.findIndex((item) => item.id === nodeId);
    const node = treeLevels[nodeIndex];

    // skip fetch when children are already loaded
    if (direction === NodeDirection.UNDER && node) {
      const potentialChildNode = treeLevels[nodeIndex + 1];
      if (potentialChildNode?.depth > node.depth) {
        return;
      }
    }

    setIsLoading(true);

    const nodes = await fetchNodes(nodeId, direction);

    if (direction === NodeDirection.UNDER) {
      const newTreeLevels = [...treeLevels];
      newTreeLevels.splice(nodeIndex + 1, 0, ...nodes)
      setTreeLevels(newTreeLevels);
    }
    else if (direction === NodeDirection.AFTER) {
      const newTreeLevels = [...treeLevels];
      let nextPossibleIndex = nodeIndex + 1;

      while (newTreeLevels[nextPossibleIndex]?.depth > node.depth) {
        nextPossibleIndex++;
      }

      newTreeLevels.splice(nextPossibleIndex, 0, ...nodes)
      setTreeLevels(newTreeLevels);
    } else if (direction === NodeDirection.BEFORE) {
      const newTreeLevels = [...treeLevels];
      const previousPossibleIndex = nodeIndex;

      newTreeLevels.splice(previousPossibleIndex, 0, ...nodes)
      setTreeLevels(newTreeLevels);
    }
    else {
      setTreeLevels(nodes);
    }

    setIsLoading(false);

    return;
  }

  useEffect(() => {
    // const promiseQueue: (() => Promise<void>)[] = [];
    //
    // const callNextPromise = async (nextIndex: number, queue: (() => Promise<void>)[]) => {
    //   const promise = queue[nextIndex];
    //   if (promise != undefined) {
    //     await queue[nextIndex]?.();
    //     if (queue.length > nextIndex) {
    //       setTimeout(() => {
    //         callNextPromise(nextIndex + 1, queue);
    //       }, 1000)
    //     }
    //   }
    // }
    //
    // initialItems.forEach((level, index) => {
    //   const previousLevel = initialItems[index - 1]?.depth === level.depth ? initialItems[index - 1] : undefined;
    //   console.log("previous level", previousLevel)
    //   if (!previousLevel && level.pos > 1) {
    //     promiseQueue.push(async () => await getRelatedNodes(level.id, NodeDirection.BEFORE));
    //   }
    //   promiseQueue.push(async () => await getRelatedNodes(level.id, NodeDirection.AFTER));
    // })
    //
    // callNextPromise(0, promiseQueue);
    initialDataLoad();
  }, [apuId])

  return [treeLevels, getRelatedNodes, isLoading]
}

export function Tree2({
  apuId,
  className,
  labelMapper = (item) => item.name,
  idMapper = (item) => item.id,
  onNodeToggle = () => null,
  onLabelClick,
  disableClick,
  initialItems,
  ...props
}: Props) {
  const classes = useStyles();
  const [treeItems, getRelatedNodes] = useTree(apuId, initialItems);

  console.log("expanded", props.expanded);

  function renderItemsFromFlat(items: TreeLevel[], parent?: TreeLevel, parentId?: string) {
    return <>{[...items].map((item, index, array) => {
      const childNodes: TreeLevel[] = [];
      for (let i = index + 1; array[i]?.depth > item.depth; i++) {
        childNodes.push(array[i]);
      }

      if (childNodes.length > 0) {
        array.splice(index + 1, childNodes.length);
      }

      // if (childNodes.length === 0 && item.childCnt > 0) {
      //   childNodes.push({
      //     id: "_",
      //     parentId: item.parentId,
      //     pos: 0,
      //     depth: item.depth + 1,
      //     childCnt: 0,
      //     name: "loading...",
      //     description: "loading...",
      //   })
      // }

      const optionalStyle = {
        overflow: "hidden",
        textOverflow: "ellipsis",
        width: "calc( 100% - 20px )",
      };

      const label = <div title={labelMapper(item)} style={{
        whiteSpace: "nowrap",
        paddingRight: "10px",
        ...optionalStyle
      }}>{`${item.pos}/${parent?.childCnt} ${parent?.id.split("-")[0]}/${item.id.split("-")[0]}`} - {labelMapper(item)}</div>;

      const hasChildren = item.childCnt > 0;

      // const nodeId = `${item.parentId ? `${item.parentId}__` : ''}${idMapper(item)}`;
      const nodeId = `${parentId ? `${parentId}__` : ''}${idMapper(item)}`;
      // const nodeId = item.id;

      console.log("node id", nodeId, item);
      const handleLabelClick = (_event: any) => {
        if (
          onLabelClick
          && (!disableClick
            || idMapper(disableClick) !== idMapper(item)
          )
        ) {
          onLabelClick(item)
        }
      }

      const handleLoadBefore = (e: React.MouseEvent) => {
        e.preventDefault();
        e.stopPropagation();
        getRelatedNodes(childNodes[0].id, NodeDirection.BEFORE);
      }

      const handleLoadAfter = (e: React.MouseEvent) => {
        e.preventDefault();
        e.stopPropagation();
        getRelatedNodes(getLastDirectChild(item, childNodes)?.id, NodeDirection.AFTER)
      }

      const getLastDirectChild = (item: TreeLevel, items: TreeLevel[]) => {
        const directChildren = getDirectChildren(item, items);
        return directChildren[directChildren.length - 1];
      }

      const getDirectChildren = (item: TreeLevel, items: TreeLevel[]) => {
        const directChildren = items.filter((_item) => _item.depth === item.depth + 1);
        return directChildren;
      }

      return <TreeItem
        {...{
          key: nodeId,
          id: item.id,
          nodeId,
          label,
          icon: hasChildren ? undefined : (
            <RemoveIcon className={classes.endItem} />
          ),
          onLabelClick: handleLabelClick,
          onIconClick: handleLabelClick,
        }}
      >
        {childNodes.length > 0
          && getDirectChildren(item, childNodes).length < item.childCnt
          && childNodes[0].pos > 1
          && <TreeItem
            nodeId={`${item.id}_loadBefore`}
            onLabelClick={handleLoadBefore}
            label={`load more before - ${childNodes[0].id}`}
          ></TreeItem>
        }

        {childNodes.length > 0
          && renderItemsFromFlat(childNodes, item, nodeId)}

        {childNodes.length === 0
          && item.childCnt > 0
          && <TreeItem
            nodeId={`${nodeId}_loading`}
            label="loading..."
            onLabelClick={(e: React.MouseEvent) => { e.preventDefault(); e.stopPropagation(); }}
          ></TreeItem>
        }

        {childNodes.length > 0
          && getDirectChildren(item, childNodes).length < item.childCnt
          && getLastDirectChild(item, childNodes).pos < item.childCnt
          && <TreeItem
            nodeId={`${item.id}_loadAfter`}
            onLabelClick={handleLoadAfter}
            label={`load more after - ${getLastDirectChild(item, childNodes).pos}/${item.childCnt}  ${getLastDirectChild(item, childNodes)?.id.split("-")[0]}`}
          ></TreeItem>
        }
      </TreeItem>
    })}</>
  }

  // const renderItems = (items: TreeLevelWithChildren[], parentId?: string) => { return <></> }
  // const renderItems = (items: TreeLevel[], parentId?: string) =>
  //   items?.map((item) => {
  //     // console.log("render item", item)
  //
  //     const optionalStyle = {
  //       overflow: "hidden",
  //       textOverflow: "ellipsis",
  //       width: "calc( 100% - 20px )",
  //     };
  //
  //     const label = <div title={labelMapper(item)} style={{
  //       whiteSpace: "nowrap",
  //       paddingRight: "10px",
  //       ...optionalStyle
  //     }}>{labelMapper(item)}</div>;
  //
  //     const nodeId = `${parentId ? `${parentId}__` : ''}${idMapper(item)}`;
  //     const { children } = item;
  //
  //     const hasLoadedChildren = children && children.length;
  //     const hasChildren = item.childCnt > 0;
  //
  //     const emptyLoadingItems: TreeLevel[] = [{
  //       id: "_",
  //       parentId: item.parentId,
  //       pos: 0,
  //       depth: item.depth + 1,
  //       childCnt: 0,
  //       name: "loading...",
  //       description: "loading...",
  //       children: [],
  //     }]
  //
  //     function handleLabelClick(event: any) {
  //       // console.log("handleLabelClick", item, event);
  //       if (
  //         onLabelClick
  //         && (!disableClick
  //           || idMapper(disableClick) !== idMapper(item)
  //         )
  //       ) {
  //         onLabelClick(item)
  //       }
  //     }
  //
  //     return (
  //       <TreeItem
  //         {...{
  //           key: nodeId,
  //           id: item.id,
  //           nodeId,
  //           label,
  //           icon: hasChildren ? undefined : (
  //             <RemoveIcon className={classes.endItem} />
  //           ),
  //           onLabelClick: handleLabelClick,
  //           onIconClick: handleLabelClick,
  //         }}
  //       >
  //         {hasChildren ? renderItems(hasLoadedChildren ? children : emptyLoadingItems, nodeId) : <></>}
  //       </TreeItem>
  //     );
  //   });

  function _onNodeToggle(event: any, expanded: string[]) {
    const lastExpandedId = expanded[0].split("__").reverse()[0];
    // const lastExpandedNode = findNode(lastExpandedId, treeItems)

    getRelatedNodes(lastExpandedId, NodeDirection.UNDER);
    onNodeToggle(expanded);
  }

  return (
    <TreeView
      {...{
        ...props,
        items: treeItems,
        className: classNames(classes.tree, className),
        defaultCollapseIcon: <ExpandMoreIcon />,
        defaultExpandIcon: <ChevronRightIcon />,
        onNodeToggle: _onNodeToggle,
        onNodeSelect: (_event: any, nodePath: any) => {
          const nodeId = nodePath.split("__").pop();
          onLabelClick?.({
            id: nodeId
          });
        },
      }}
    >
      {renderItemsFromFlat(treeItems)}
    </TreeView>
  );
}
