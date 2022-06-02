import React from 'react';
import classNames from 'classnames';
import TreeView from '@material-ui/lab/TreeView';
import TreeItem from '@material-ui/lab/TreeItem';
import ExpandMoreIcon from '@material-ui/icons/ExpandMore';
import ChevronRightIcon from '@material-ui/icons/ChevronRight';
import RemoveIcon from '@material-ui/icons/Remove';

import { Props } from './types';
import { useStyles } from './styles';

export function Tree({
  items,
  className,
  labelMapper = (item) => item.label,
  idMapper = (item) => item.id,
  onNodeToggle = () => null,
  onLabelClick,
  disableClick,
  ...props
}: Props) {
  const classes = useStyles();

  const renderItems = (items: any[], parentId?: string) =>
    items.map((item) => {
      const label = <div style={{
        whiteSpace: "nowrap",
        overflow: "hidden",
        textOverflow: "ellipsis",
        width: "calc( 100% - 20px )",
        paddingRight: "10px",
      }}>{labelMapper(item)}</div>;
      const nodeId = `${parentId ? `${parentId}__` : ''}${idMapper(item)}`;
      const { children } = item;

      const hasChildren = children && children.length;

      return (
        <TreeItem
          {...{
            key: nodeId,
            id: item.id,
            nodeId,
            label,
            icon: hasChildren ? undefined : (
              <RemoveIcon className={classes.endItem} />
            ),
            onLabelClick:
              onLabelClick &&
              (!disableClick || idMapper(disableClick) !== idMapper(item))
                ? () => onLabelClick(item)
                : undefined,
          }}
        >
          {hasChildren ? renderItems(children, nodeId) : <></>}
        </TreeItem>
      );
    });

  return (
    <TreeView
      {...{
        ...props,
        className: classNames(classes.tree, className),
        defaultCollapseIcon: <ExpandMoreIcon />,
        defaultExpandIcon: <ChevronRightIcon />,
        onNodeToggle: (_, expanded) => onNodeToggle(expanded),
      }}
    >
      {renderItems(items)}
    </TreeView>
  );
}
