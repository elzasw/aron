import React from 'react';
import Paper from '@material-ui/core/Paper';
import LinearProgress from '@material-ui/core/LinearProgress';

import { useStyles } from './styles';
import { useGet } from '../../common-utils';
import { ApiUrl } from '../../enums';
import { NewsEntity } from '../../types';
import { useSpacingStyles } from '../../styles';

export const News: React.FC = () => {
  const classes = useStyles();
  const spacingClasses = useSpacingStyles();

  const [items, loading] = useGet<NewsEntity[]>(ApiUrl.NEWS);

  return (
    <div className={classes.news}>
      {loading ? (
        <LinearProgress />
      ) : items ? (
        items.map(({ date, name, text, attachments }) => (
          <Paper elevation={8} key={date} className={classes.newsItem}>
            <div className={classes.itemHeading}>
              {date}
              <span className={classes.itemTitle}>{name}</span>
            </div>
            <div className={classes.itemText}>{text}</div>
            {attachments ? (
              <div className={classes.itemLinks}>
                {attachments.map(({ link, name }) => (
                  <div className={spacingClasses.marginTopSmall}>
                    <a href={link}>{name}</a>
                  </div>
                ))}
              </div>
            ) : (
              <></>
            )}
          </Paper>
        ))
      ) : (
        <></>
      )}
    </div>
  );
};
