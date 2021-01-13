import * as Yup from 'yup';
import { Translation, Langugage } from './translations-types';

export function useValidationSchema() {
  return Yup.object<Translation>().shape({
    name: Yup.string().nullable().required(),
    language: Yup.mixed<Langugage>().nullable().required(),
  });
}
