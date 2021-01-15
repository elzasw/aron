import React, { useContext, useEffect, useState } from 'react';
import classNames from 'classnames';
import { get, uniq } from 'lodash';
import { Resizable } from 're-resizable';
import CircularProgress from '@material-ui/core/CircularProgress';
import MoreHorizIcon from '@material-ui/icons/MoreHoriz';
import UnfoldLessIcon from '@material-ui/icons/UnfoldLess';

import { NavigationContext } from '@eas/common-web';

import { useStyles } from './styles';
import { useLayoutStyles, useSpacingStyles } from '../../styles';
import { DetailTreeProps } from './types';
import { useAppState, useGet } from '../../common-utils';
import { Tree } from '../../components';
import { ModulePath, ApiUrl } from '../../enums';
import { ApuEntity, ApuTree } from '../../types';

const WRAPPER_ID = 'evidence-detail-tree-wrapper';

const getParentId = (item: ApuEntity): string => {
  return `${item.parent ? `${getParentId(item.parent)}__` : ''}${item.id}`;
};

export function EvidenceDetailTree({ item, id }: DetailTreeProps) {
  const classes = useStyles();
  const layoutClasses = useLayoutStyles();
  const spacingClasses = useSpacingStyles();

  const { navigate } = useContext(NavigationContext);

  const { appState, updateAppState } = useAppState();

  const [treeItem, loading] = useGet<ApuTree>(`${ApiUrl.APU}/${id}/tree`);

  const [selected, setSelected] = useState<string[]>([]);

  useEffect(() => {
    setSelected([getParentId(item)]);
  }, [item]);

  useEffect(() => {
    if (treeItem && !loading) {
      const init = () => {
        const itemExists = (
          item: ApuTree | undefined,
          id: string,
          parentId?: string
        ): boolean => {
          if (item) {
            const itemId = `${parentId ? `${parentId}__` : ''}${item.id}`;

            return !!(
              itemId === id ||
              (id.match(itemId) &&
                item.children &&
                item.children.some((item) => itemExists(item, id, itemId)))
            );
          }

          return false;
        };

        const items = (
          appState.evidenceDetailTreeExpandedItems || []
        ).filter((id) => itemExists(treeItem, id));

        let current: ApuEntity | undefined = item;

        while (current) {
          items.push(getParentId(current));
          current = current.parent;
        }

        updateAppState({ evidenceDetailTreeExpandedItems: uniq(items) });

        const treeItemContent = get(
          document.getElementsByClassName('MuiTreeItem-content'),
          '[0]'
        );

        if (treeItemContent) {
          const itemHeight = treeItemContent.getBoundingClientRect().height;

          const isParent = (parent: ApuTree) => {
            let current: ApuEntity | undefined = item;

            while (current) {
              if (current.id === parent.id) {
                return true;
              }

              current = current.parent;
            }

            return false;
          };

          const getScrollTop = (current: ApuTree, parentId?: string) => {
            let scrollTop = 0;

            if (current.id !== item.id) {
              scrollTop += itemHeight;

              const itemId = `${parentId ? `${parentId}__` : ''}${current.id}`;

              current.children.some((child) => {
                if (child.id === item.id) {
                  return true;
                }

                if (isParent(child)) {
                  scrollTop += getScrollTop(child, itemId);
                  return true;
                }

                if (
                  appState.evidenceDetailTreeExpandedItems.includes(
                    `${itemId}__${child.id}`
                  )
                ) {
                  scrollTop += getScrollTop(child, itemId);
                } else {
                  scrollTop += itemHeight;
                }

                return false;
              });
            }

            return scrollTop;
          };

          const scrollTop = getScrollTop(treeItem);

          setTimeout(() => {
            const wrapper = document.getElementById(WRAPPER_ID);

            if (wrapper) {
              wrapper.scrollTop = scrollTop;
            }
          }, 500);
        }
      };

      setTimeout(init);
    }

    // eslint-disable-next-line
  }, [treeItem, loading]);

  return treeItem ? (
    <Resizable
      size={{
        width: '100%',
        height: appState.evidenceDetailTreeHeight,
      }}
      minHeight={50}
      enable={{
        top: false,
        right: false,
        bottom: true,
        left: false,
        topRight: false,
        bottomRight: false,
        bottomLeft: false,
        topLeft: false,
      }}
      bounds="parent"
      onResizeStop={(_, __, ___, { height }) =>
        updateAppState({
          evidenceDetailTreeHeight: appState.evidenceDetailTreeHeight + height,
        })
      }
      handleComponent={{
        bottom: (
          <div
            className={classNames(
              classes.treeResizeHandle,
              layoutClasses.flexCentered
            )}
          >
            <MoreHorizIcon className={classes.treeResizeHandleIcon} />
          </div>
        ),
      }}
      className={spacingClasses.marginBottomBig}
    >
      <div
        id={WRAPPER_ID}
        className={classNames(
          classes.treeWrapper,
          spacingClasses.paddingBottomSmall
        )}
      >
        <div className={layoutClasses.flex}>
          <UnfoldLessIcon
            className={classes.treeCollapseIcon}
            onClick={() =>
              updateAppState({ evidenceDetailTreeExpandedItems: [] })
            }
          />
          <Tree
            {...{
              items: [treeItem],
              selected,
              expanded: appState.evidenceDetailTreeExpandedItems,
              disableClick: item,
              labelMapper: (item) => item.name || 'Neznámé',
              onLabelClick: (newItem) =>
                navigate(`${ModulePath.APU}/${newItem.id}`),
              onNodeToggle: (evidenceDetailTreeExpandedItems) =>
                updateAppState({ evidenceDetailTreeExpandedItems }),
            }}
          />
        </div>
      </div>
    </Resizable>
  ) : loading ? (
    <div
      className={classNames(layoutClasses.flexCentered, spacingClasses.padding)}
    >
      <CircularProgress />
    </div>
  ) : (
    <></>
  );
}