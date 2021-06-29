import React, { forwardRef } from 'react';
import { noop } from 'lodash';
import { Form } from 'composite/form/form';
import { DictionaryAutocomplete } from 'common/common-types';
import { useStaticListSource } from 'utils/list-source-hook';
import { FormTextField } from 'composite/form/fields/form-text-field';
import { FormNumberField } from 'composite/form/fields/form-number-field';
import { FormCheckbox } from 'composite/form/fields/form-checkbox';
import { FormDateField } from 'composite/form/fields/form-date-field';
import { FormDateTimeField } from 'composite/form/fields/form-date-time-field';
import { FormTimeField } from 'composite/form/fields/form-time-field';
import { FormSelect } from 'composite/form/fields/form-select';
import { FormAutocomplete } from 'composite/form/fields/form-autocomplete';
import { useAutocompleteSource } from 'components/autocomplete/autocomplete-source-hook';
import { ReportInputField, ReportInputFieldType } from '../reporting-types';
import { FormHandle } from 'composite/form/form-types';
import { ReportSettingsFormProps } from '../reporting-types';

export const ReportSettingsForm = forwardRef<
  FormHandle,
  ReportSettingsFormProps
>(
  // eslint-disable-next-line no-empty-pattern
  function ReportSettingsForm({ definition }, ref) {
    const inputFields = definition?.inputFields ?? [];

    return (
      <Form<any>
        ref={ref}
        initialValues={{} as any}
        onSubmit={noop}
        editing={true}
      >
        {inputFields.map((field) => (
          <InputField key={field.name} field={field} />
        ))}
      </Form>
    );
  }
);

function InputField({ field }: { field: ReportInputField }) {
  switch (field.type) {
    case ReportInputFieldType.TEXT:
      return <FormTextField name={field.name} label={field.label} />;
    case ReportInputFieldType.NUMBER:
      return <FormNumberField name={field.name} label={field.label} />;
    case ReportInputFieldType.BOOLEAN:
      return <FormCheckbox name={field.name} label={field.label} />;
    case ReportInputFieldType.DATE:
      return <FormDateField name={field.name} label={field.label} />;
    case ReportInputFieldType.DATETIME:
      return <FormDateTimeField name={field.name} label={field.label} />;
    case ReportInputFieldType.TIME:
      return <FormTimeField name={field.name} label={field.label} />;
    case ReportInputFieldType.SELECT:
      return <SelectInputField field={field} />;
    case ReportInputFieldType.AUTOCOMPLETE:
      return <AutocompleteInputField field={field} />;
    default:
      return <></>;
  }
}

function SelectInputField({ field }: { field: ReportInputField }) {
  const source = useStaticListSource<DictionaryAutocomplete>(
    field.selectItems ?? []
  );

  return (
    <FormSelect
      name={field.name}
      label={field.label}
      source={source}
      valueIsId
    />
  );
}

function AutocompleteInputField({ field }: { field: ReportInputField }) {
  const source = useAutocompleteSource<any>({
    url: field.autocompleteUrl!,
    apiUrl: field.autocompleteApiUrl,
    params: field.autocompleteParams,
  });

  return (
    <FormAutocomplete
      name={field.name}
      label={field.label}
      multiple={true}
      source={source}
      labelMapper={(a) => a.label}
      tooltipMapper={(a) => a.label}
    />
  );
}
