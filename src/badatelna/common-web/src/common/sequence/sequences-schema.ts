import * as Yup from 'yup';
import { Sequence } from './sequences-types';

export function useValidationSchema() {
  return Yup.object<Sequence>().shape({
    name: Yup.string().nullable().required(),
    format: Yup.string().nullable().required(),
    counter: Yup.number().nullable().required(),
  });
}
