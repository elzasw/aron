import React, { ReactNode, useContext } from 'react';
import Box from '@material-ui/core/Box';
import { FormPanel } from 'composite/form/fields/form-panel';
import { FormCustomField } from 'composite/form/fields/form-custom-field';
import { DetailContext } from './detail-context';

export function DetailContainer({ children }: { children?: ReactNode }) {
  const { errors } = useContext(DetailContext);

  return (
    <>
      {errors.length > 0 && (
        <FormPanel
          label={
            <Box color="#CD5360" fontWeight="500">
              Kontrola správnosti údajů
            </Box>
          }
        >
          {errors.map((error, i) => (
            <FormCustomField key={i} label={error.label}>
              <Box color="#CD5360" fontWeight="500">
                {error.value}
              </Box>
            </FormCustomField>
          ))}
        </FormPanel>
      )}
      {children}
    </>
  );
}
