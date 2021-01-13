import React from 'react';
import classNames from 'classnames';
import { FormattedMessage } from 'react-intl';

import { AppTitle } from '../../components';
import { useStyles } from './styles';
import { useLayoutStyles, useSpacingStyles } from '../../styles';
import { Message } from '../../enums';

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
                title: Message.BASIC_INFORMATION,
                content: [
                  'Archiv Online je webová aplikace Státního oblastního archivu v Zámrsku sloužící ke zpřístupnění popisu archiválií a jejich digitalizátů. Do tohoto systému bude třeba převést přes deset tisíc starších archivních pomůcek a zhruba šest milionů již dříve pořízených snímků, a to včetně ošetření ochrany osobních údajů a dalších práv. Postupně budou tvořeny nové pomůcky, nové digitalizáty a přístupové body (zjednodušeně řečeno „rejstříková hesla“).',
                  'Vzhledem k tomu, že na tuto činnost nemá archiv žádné specializované pracoviště ani pracovní síly, musí ji vykonávat archiváři vedle svých dalších povinností v podobě kontrolní činnosti u původců, skartačních řízení, výběru archiválií, obsluhy badatelen, odpovědí na badatelské dotazy atd. Věc tedy nepůjde tak rychle, jak bychom sami chtěli, budeme se však snažit maximálně zúročit naše nové technické prostředky a postupně zlepšovat služby veřejnosti.',
                ],
              },
              {
                title: Message.CONTACT,
                content: ['webmaster@ahapa.cz'],
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
                    spacingClasses.marginSmall
                  )}
                >
                  <FormattedMessage id={title} />
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
