import { useStaticListSource, useListSource } from 'utils/list-source-hook';
import { DictionaryAutocomplete } from 'common/common-types';

export function useReportTypes() {
  return useStaticListSource<DictionaryAutocomplete>([
    //{ id: 'DOCX', name: 'Dokument Word (docx)' },
    { id: 'XLSX', name: 'Sešit (xlsx)' },
    { id: 'PDF', name: 'Dokument PDF (pdf)' },
    { id: 'HTML', name: 'Stránka (html)' },
    { id: 'XML', name: 'Datový soubor (xml)' },
    { id: 'CSV', name: 'Datový soubor (csv)' },
  ]);
}

export function useTemplates(tag: string) {
  return useListSource<DictionaryAutocomplete>({
    url: `/api/report/templates/tagged/${tag}`,
  });
}
