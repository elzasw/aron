import { useStaticListSource } from 'utils/list-source-hook';
import { DictionaryAutocomplete } from 'common/common-types';

export function useReportRequestStates() {
  return useStaticListSource<DictionaryAutocomplete>([
    { id: 'PENDING', name: 'Čekající' },
    { id: 'PROCESSING', name: 'Zpracováva se' },
    { id: 'PROCESSED', name: 'Ukončen' },
    { id: 'FAILED', name: 'Ukončen s chybu' },
  ]);
}

export function useReportTypes() {
  return useStaticListSource<DictionaryAutocomplete>([
    { id: 'DOCX', name: 'Dokument Word (docx)' },
    { id: 'XLSX', name: 'Sešit (xlsx)' },
    { id: 'PDF', name: 'Dokument PDF (pdf)' },
    { id: 'HTML', name: 'Stránka (html)' },
    { id: 'XML', name: 'Datový soubor (xml)' },
    { id: 'CSV', name: 'Datový soubor (csv)' },
  ]);
}
