import * as Yup from 'yup';
import { Translation, Langugage } from './translations-types';

export function useValidationSchema() {
  return Yup.object<Translation>().shape({
    name: Yup.string().nullable().required('Název @@@ Musí být vyplněné'),
    language: Yup.mixed<Langugage>()
      .nullable()
      .required('Jazyk @@@ Musí být vyplněné'),
  });
}
