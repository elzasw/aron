import * as Yup from 'yup';
import { Sequence } from './sequences-types';

export function useValidationSchema() {
  return Yup.object<Sequence>().shape({
    name: Yup.string().nullable().required('Název @@@ Musí být vyplněné'),
    format: Yup.string().nullable().required('Formát @@@ Musí být vyplněné'),
    counter: Yup.number()
      .nullable()
      .required('Počítadlo @@@ Musí být vyplněné'),
  });
}
