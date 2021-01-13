import * as Yup from 'yup';
import { ReportTemplate } from 'common/common-types';

export function useValidationSchema() {
  return Yup.object<ReportTemplate>().shape({
    name: Yup.string().nullable().required(),
  });
}
