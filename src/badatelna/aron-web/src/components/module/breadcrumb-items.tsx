import React, { useEffect, useState, useCallback } from 'react';
import { useLocation } from 'react-router-dom';
import classNames from 'classnames';
import { FormattedMessage } from 'react-intl';

import { ModulePath, Message } from '../../enums';
import { Breadcrumbs } from './types';
import { useStyles } from './styles';
import { useLayoutStyles, useSpacingStyles } from '../../styles';
import { BreadcrumbItem } from './breadcrumb-item';
import { useConfiguration } from '../configuration';

const BREADCRUMBS_ID = 'module-breadcrumbs';
const TOOLBAR_ID = 'module-toolbar';

export function BreadcrumbItems({ items, toolbar }: Breadcrumbs) {
  const classes = useStyles();
  const layoutClasses = useLayoutStyles();
  const spacingClasses = useSpacingStyles();
  const configuration = useConfiguration();

  const location = useLocation();

  const [responsive, setResponsive] = useState(false);
  const [breadcrumbsWidth, setBreadcrumbsWidth] = useState(0);
  const [availableWidth, setAvailableWidth] = useState(0);
  const [lastItemWidth, setLastItemWidth] = useState(0);

  const itemsWithMain = [
    ...items,
  ];
  if(configuration.showMainPageBreadcrumb){
    itemsWithMain.unshift({
      path: ModulePath.MAIN,
      label: <FormattedMessage id={Message.INTRODUCTION} />,
    })
  }

  const allItems =
    responsive && itemsWithMain.length > 2
      ? [
          itemsWithMain[0],
          {
            label: '...',
            items: itemsWithMain.slice(1, itemsWithMain.length - 1),
          },
          itemsWithMain[itemsWithMain.length - 1],
        ]
      : itemsWithMain;

  const onResize = useCallback(
    (currentBreadcrumbsWidth?: number) => {
      const toolbar = document.getElementById(TOOLBAR_ID);

      const availableWidth =
        window.innerWidth -
        25 -
        (toolbar ? toolbar.getBoundingClientRect().width : 0);

      setAvailableWidth(availableWidth);
      setResponsive(
        availableWidth < (currentBreadcrumbsWidth || breadcrumbsWidth)
      );
    },
    [breadcrumbsWidth]
  );

  useEffect(() => {
    const updateLastItemWidth = () => {
      const breadcrumbs = document.getElementById(BREADCRUMBS_ID);

      if (breadcrumbs) {
        let width = 0;

        for (let i = 0; i < breadcrumbs.children.length - 1; i++) {
          width += breadcrumbs.children[i].getBoundingClientRect().width;
        }

        const newLastItemWidth = availableWidth - width - 50;

        if (newLastItemWidth > 0 && newLastItemWidth !== lastItemWidth) {
          setLastItemWidth(newLastItemWidth);
        }
      }
    };

    setTimeout(updateLastItemWidth);
    // eslint-disable-next-line
  }, [availableWidth]);

  useEffect(() => {
    const breadcrumbs = document.getElementById(BREADCRUMBS_ID);

    const breadcrumbsWidth = breadcrumbs
      ? breadcrumbs.getBoundingClientRect().width
      : 0;

    setBreadcrumbsWidth(breadcrumbsWidth);

    onResize(breadcrumbsWidth);
    // eslint-disable-next-line
  }, []);

  useEffect(() => {
    window.addEventListener('resize', () => onResize());

    return () => {
      window.removeEventListener('resize', () => onResize());
    };
  }, [onResize, location]);

  return (
    <div
      className={classNames(
        classes.breadcrumbsWrapper,
        layoutClasses.flexSpaceBetween
      )}
    >
      <div
        id={BREADCRUMBS_ID}
        className={classNames(
          classes.breadcrumbs,
          layoutClasses.flex,
          spacingClasses.paddingLeftBig
        )}
      >
        {allItems.map((item, index) => (
          <BreadcrumbItem
            {...{
              key: `${index}-${item.label}`,
              ...item,
              index,
              allItems,
              lastItemWidth,
            }}
          />
        ))}
      </div>
      {toolbar ? (
        <div id={TOOLBAR_ID} className={classes.toolbar}>
          {toolbar}
        </div>
      ) : (
        <div id={TOOLBAR_ID} className={spacingClasses.paddingRightBig} />
      )}
    </div>
  );
}
