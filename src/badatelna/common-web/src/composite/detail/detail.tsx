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

// eslint-disable-next-line react/display-name
export const Detail = memo(
  forwardRef(function Detail<OBJECT extends DomainObject>(
    options: DetailProps<OBJECT>,
    ref: Ref<DetailHandle<OBJECT>>
  ) {
    const { props, context, formRef, editing } = useDetail(options, ref);
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
          <div className={classes.wrapper}>
            <Form<OBJECT>
              ref={formRef}
              initialValues={{} as any}
              onSubmit={noop}
              editing={editing}
              validationSchema={validationSchema}
            >
              <ContainerComponent>
                <FieldsComponent />
                {GeneralFieldsComponent !== undefined && (
                  <FormPanel label="Obecné">
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
