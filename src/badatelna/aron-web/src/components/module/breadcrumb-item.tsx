import React, { useState, ReactChild } from 'react';
import { Link } from 'react-router-dom';
import classNames from 'classnames';
import MoreHorizIcon from '@material-ui/icons/MoreHoriz';

import { Breadcrumb } from './types';
import { useStyles } from './styles';
import { useLayoutStyles } from '../../styles';

export function BreadcrumbItem({
  path,
  label,
  items,
  index,
  allItems,
  lastItemWidth,
}: Breadcrumb) {
  const classes = useStyles();
  const layoutClasses = useLayoutStyles();

  const [open, setOpen] = useState(false);

  const renderLink = (path: string | undefined, label: string | ReactChild) =>
    items || (path && index < allItems.length - 1) ? (
      <Link
        to={{ pathname: path }}
        className={classNames(
          classes.breadcrumbLink,
          items && classes.breadcrumbLinkResponsive
        )}
      >
        {label}
      </Link>
    ) : (
      <div
        style={
          index === allItems.length - 1 && lastItemWidth
            ? {
                maxWidth: lastItemWidth,
                overflow: 'hidden',
                textOverflow: 'ellipsis',
              }
            : undefined
        }
      >
        {label}
      </div>
    );

  return (
    <div className={classNames(classes.breadcrumb, layoutClasses.flexCentered)}>
      {index ? <div>&nbsp;&nbsp;&nbsp;/&nbsp;&nbsp;&nbsp;</div> : ''}
      {items ? (
        <MoreHorizIcon
          className={classes.breadcrumbIcon}
          onClick={() => setOpen(!open)}
        />
      ) : (
        renderLink(path, label)
      )}
      {items && open ? (
        <div className={classes.breadcrumbMenu} onClick={() => setOpen(false)}>
          {items.map(({ path, label }) => renderLink(path, label))}
        </div>
      ) : (
        <></>
      )}
    </div>
  );
}
