import React from 'react';
import { useStyles } from './styles';
import Paper from '@material-ui/core/Paper';

export const News: React.FC = () => {
  const classes = useStyles();

  return (
    <div className={classes.newsWrapper}>
      {[
        {
          date: '1.2.2020',
          label: 'Nadpis aktuality 3',
          text:
            'Lorem ipsum dolor sit amet, consectetuer adipiscing elit. Fusce suscipit libero eget elit. Fusce tellus odio, dapibus id fermentum quis, suscipit id erat. Vivamus porttitor turpis ac leo. Maecenas libero. Duis pulvinar. Suspendisse sagittis ultrices augue. Duis pulvinar. Nullam justo enim, consectetuer nec, ullamcorper ac, vestibulum in, elit. Etiam dictum tincidunt diam. Nulla quis diam. Duis pulvinar. Proin mattis lacinia justo. Fusce tellus. Aliquam ornare wisi eu metus.',
          link: true,
        },
        {
          date: '5.1.2020',
          label: 'Nadpis aktuality 2',
          text:
            'Lorem ipsum dolor sit amet, consectetuer adipiscing elit. Fusce suscipit libero eget elit. Fusce tellus odio, dapibus id fermentum quis, suscipit id erat. Vivamus porttitor turpis ac leo. Maecenas libero. Duis pulvinar. Suspendisse sagittis ultrices augue. Duis pulvinar. Nullam justo enim, consectetuer nec, ullamcorper ac, vestibulum in, elit. Etiam dictum tincidunt diam. Nulla quis diam. Duis pulvinar. Proin mattis lacinia justo. Fusce tellus. Aliquam ornare wisi eu metus.',
        },
        {
          date: '3.12.2020',
          label: 'Nadpis aktuality 1',
          text:
            'Lorem ipsum dolor sit amet, consectetuer adipiscing elit. Fusce suscipit libero eget elit. Fusce tellus odio, dapibus id fermentum quis, suscipit id erat. Vivamus porttitor turpis ac leo. Maecenas libero. Duis pulvinar. Suspendisse sagittis ultrices augue. Duis pulvinar. Nullam justo enim, consectetuer nec, ullamcorper ac, vestibulum in, elit. Etiam dictum tincidunt diam. Nulla quis diam. Duis pulvinar. Proin mattis lacinia justo. Fusce tellus. Aliquam ornare wisi eu metus.',
        },
      ].map(({ date, label, text, link }) => (
        <Paper elevation={8} key={date} className={classes.newsSingleItem}>
          <div className={classes.itemHeading}>
            {date}
            <span className={classes.itemTitle} style={{ fontWeight: 600 }}>
              {label}
            </span>
          </div>
          <div className={classes.itemText}>{text}</div>
          {link ? (
            <div className={classes.itemLinks}>
              <span style={{ fontWeight: 600 }}>Přílohy k aktualitě</span>
              <div className="margin-bottom-tiny">
                <a href="#odkaz1">Odkaz na soubor 1</a>
              </div>
              <div>
                <a href="#odkaz2">Odkaz na soubor 2</a>
              </div>
            </div>
          ) : (
            <></>
          )}
        </Paper>
      ))}
    </div>
  );
};
