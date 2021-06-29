import React from 'react';
import Paper from '@material-ui/core/Paper';
import LinearProgress from '@material-ui/core/LinearProgress';

import { useGet, parseYaml } from '../../common-utils';
import { ApiUrl } from '../../enums';
import { NewsEntity } from '../../types';
import { useSpacingStyles } from '../../styles';
import { Module } from '../../components';
import { useStyles } from './styles';
import { Props } from './types';

export function News({ path, label }: Props) {
  const classes = useStyles();
  const spacingClasses = useSpacingStyles();

  const [news, loading] = useGet<string>(ApiUrl.NEWS, { textResponse: true });

  const items: NewsEntity[] | null = parseYaml(news);

  return (
    <Module
      {...{
        path,
        items: [{ label }],
      }}
    >
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
    </Module>
  );
}
