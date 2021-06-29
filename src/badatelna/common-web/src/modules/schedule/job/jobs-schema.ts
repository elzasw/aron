import * as Yup from 'yup';
import { Job } from '../schedule-types';

export function useValidationSchema() {
  return Yup.object<Job>().shape({
    name: Yup.string().nullable().required(),
    timer: Yup.string().nullable().required(),
    script: Yup.string().nullable().required(),
  });
}
