import React, {
  memo,
  forwardRef,
  Ref,
  ReactElement,
  RefAttributes,
} from 'react';
import CircularProgress from '@material-ui/core/CircularProgress';
import { noop } from 'lodash';
import { DomainObject } from 'common/common-types';
import { DetailProps, DetailHandle } from './detail-types';
import { useDetail } from './detail-hook';
import { DetailContext } from './detail-context';
import { useStyles } from './detail-styles';
import { Form } from 'composite/form/form';
import { FormPanel } from 'composite/form/fields/form-panel';
import { FormattedMessage } from 'react-intl';
import { EmptyComponent } from 'utils/empty-component';

// eslint-disable-next-line react/display-name
export const Detail = memo(
  forwardRef(function Detail<OBJECT extends DomainObject>(
    options: DetailProps<OBJECT>,
    ref: Ref<DetailHandle<OBJECT>>
  ) {
    const { props, context, formRef, detailContainerRef, editing } = useDetail(
      options,
      ref
    );
    const {
      ToolbarComponent,
      ContainerComponent,
      FieldsComponent,
      GeneralFieldsComponent,
      toolbarProps,
      validationSchema,
    } = props;

    const classes = useStyles();

    return (
      <div className={classes.outerWrapper}>
        <DetailContext.Provider value={context}>
          {options.source.loading && (
            <div className={classes.loaderWrapper}>
              <CircularProgress disableShrink className={classes.loader} />
            </div>
          )}
          <ToolbarComponent {...toolbarProps} />
          <div ref={detailContainerRef} className={classes.wrapper}>
            <Form<OBJECT>
              ref={formRef}
              initialValues={{} as any}
              onSubmit={noop}
              editing={editing}
              validationSchema={validationSchema}
            >
              <ContainerComponent>
                <FieldsComponent />
                {!(
                  GeneralFieldsComponent === EmptyComponent ||
                  GeneralFieldsComponent === undefined
                ) && (
                  <FormPanel
                    label={
                      <FormattedMessage
                        id="EAS_DETAIL_TITLE"
                        defaultMessage="Obecné"
                      />
                    }
                  >
                    <GeneralFieldsComponent />
                  </FormPanel>
                )}
              </ContainerComponent>
            </Form>
          </div>
        </DetailContext.Provider>
      </div>
    );
  })
) as <OBJECT extends DomainObject>(
  p: DetailProps<OBJECT> & RefAttributes<DetailHandle<OBJECT>>
) => ReactElement;
