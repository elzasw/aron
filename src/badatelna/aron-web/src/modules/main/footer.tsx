import React from 'react';
import classNames from 'classnames';

import { AppTitle } from '../../components';
import { useStyles } from './styles';
import { useLayoutStyles, useSpacingStyles } from '../../styles';

export const Footer: React.FC = () => {
  const classes = useStyles();
  const layoutClasses = useLayoutStyles();
  const spacingClasses = useSpacingStyles();

  return (
    <div className={classes.mainFooter}>
      <div className={spacingClasses.padding}>
        <div className={classNames(layoutClasses.flex, layoutClasses.flexWrap)}>
          <div
            className={classNames(
              classes.mainFooterLeft,
              layoutClasses.flexAlignTop,
              spacingClasses.padding
            )}
          >
            <AppTitle />
          </div>
          <div className={layoutClasses.flex}>
            {[
              {
                title: 'Základní informace',
                content: [
                  'Lorem ipsum dolor sit amet, consectetuer adipiscing elit. Nam quis nulla. Aliquam erat volutpat. Lorem ipsum dolor sit amet, consectetuer adipiscing elit. Nulla non lectus sed nisl molestie malesuada. Donec ipsum massa, ullamcorper in, auctor et, scelerisque sed, est.',
                ],
              },
              {
                title: 'Kontakt',
                content: ['info@archivonline.cz', '+420 777 888 999'],
              },
            ].map(({ title, content }) => (
              <div
                key={title}
                className={classNames(
                  classes.mainFooterSection,
                  spacingClasses.padding
                )}
              >
                <p
                  className={classNames(
                    classes.mainFooterTitle,
                    spacingClasses.marginSmall,
                  )}
                >
                  {title}
                </p>
                {content.map((c) => (
                  <p
                    key={c}
                    className={classNames(
                      classes.mainFooterText,
                      spacingClasses.marginNone
                    )}
                  >
                    {c}
                  </p>
                ))}
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
};
