import * as Yup from 'yup';
import { Action } from './actions-types';

export function useValidationSchema() {
  return Yup.object<Action>().shape({
    name: Yup.string().nullable().required(),
    script: Yup.string().nullable().required(),
  });
}
