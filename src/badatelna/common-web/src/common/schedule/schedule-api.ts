import { useStaticListSource, useListSource } from 'utils/list-source-hook';
import { DictionaryAutocomplete } from 'common/common-types';
import { useContext } from 'react';
import { ScheduleRunsContext } from './run/runs-context';

export function useScriptTypes() {
  return useStaticListSource<DictionaryAutocomplete>([
    { id: 'GROOVY', name: 'Groovy' },
    { id: 'JAVASCRIPT', name: 'Javascript' },
  ]);
}

export function useRunStates() {
  return useStaticListSource<DictionaryAutocomplete>([
    { id: 'STARTED', name: 'Běží' },
    { id: 'ERROR', name: 'Chyba' },
    { id: 'FINISHED', name: 'Skončil' },
  ]);
}

export function useJobs() {
  const { jobsUrl } = useContext(ScheduleRunsContext);

  return useListSource<DictionaryAutocomplete>({
    url: `${jobsUrl}/autocomplete/full`,
  });
}

export function useJobsAll() {
  const { jobsUrl } = useContext(ScheduleRunsContext);

  return useListSource<DictionaryAutocomplete>({
    url: `${jobsUrl}/autocomplete/full/all`,
  });
}
