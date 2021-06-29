import * as Yup from 'yup';
import { ExportTemplate } from '../export-types';

export function useValidationSchema() {
  return Yup.object<ExportTemplate>().shape({
    name: Yup.string().nullable().required(),
  });
}
