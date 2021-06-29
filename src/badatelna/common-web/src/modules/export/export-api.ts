import { useStaticListSource, useListSource } from 'utils/list-source-hook';
import { DictionaryAutocomplete } from 'common/common-types';
import { ExportType } from './export-types';
import { useContext } from 'react';
import { ExportContext } from './export-context';

export function useExportRequestStates() {
  return useStaticListSource<DictionaryAutocomplete>([
    { id: 'PENDING', name: 'Čekající' },
    { id: 'PROCESSING', name: 'Zpracováva se' },
    { id: 'PROCESSED', name: 'Ukončen' },
    { id: 'FAILED', name: 'Ukončen s chybu' },
  ]);
}

export function useAllowdExportTypes(allowedTypes?: ExportType[]) {
  const items = [
    { id: 'DOCX', name: 'Dokument Word (docx)' },
    { id: 'XLSX', name: 'Sešit (xlsx)' },
    { id: 'PDF', name: 'Dokument PDF (pdf)' },
    { id: 'HTML', name: 'Stránka (html)' },
    { id: 'XML', name: 'Datový soubor (xml)' },
    { id: 'CSV', name: 'Datový soubor (csv)' },
  ];

  return useStaticListSource<DictionaryAutocomplete>(
    items.filter((item) => allowedTypes?.includes(item.id as ExportType))
  );
}

export function useExportTypes() {
  return useStaticListSource<DictionaryAutocomplete>([
    { id: 'DOCX', name: 'Dokument Word (docx)' },
    { id: 'XLSX', name: 'Sešit (xlsx)' },
    { id: 'PDF', name: 'Dokument PDF (pdf)' },
    { id: 'HTML', name: 'Stránka (html)' },
    { id: 'XML', name: 'Datový soubor (xml)' },
    { id: 'CSV', name: 'Datový soubor (csv)' },
  ]);
}

export function useExportDataProviders() {
  return useStaticListSource<DictionaryAutocomplete>([
    { id: 'CONFIGURATION_PROVIDER', name: 'EAS Configuration data provider' },
    { id: 'NOOP_PROVIDER', name: 'No-op provider' },
    { id: 'PARAMS_PROVIDER', name: 'EAS Params provider' },
    { id: 'REPORTING_PROVIDER', name: 'EAS Reporting provider' },
    { id: 'SINGLE_ENTITY_PROVIDER', name: 'EAS Single entity provider' },
  ]);
}

export function useExportDesignProviders() {
  return useStaticListSource<DictionaryAutocomplete>([
    { id: 'DYNAMIC_PROVIDER', name: 'Dynamic grid design provider' },
    { id: 'SIMPLE_PROVIDER', name: 'Simple design provider' },
  ]);
}

export function useTemplates(tag: string) {
  const { url } = useContext(ExportContext);

  return useListSource<DictionaryAutocomplete>({
    url: `${url}/templates/tagged/${tag}`,
  });
}
