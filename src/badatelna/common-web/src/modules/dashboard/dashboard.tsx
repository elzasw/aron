import React, {
  useContext,
  useState,
  useEffect,
  useCallback,
  useRef,
} from 'react';
import { useEventCallback } from 'utils/event-callback-hook';
import { DialogHandle } from 'components/dialog/dialog-types';
import { DndGrid } from 'components/dnd-grid/dnd-grid';
import { UserSettingsContext } from 'common/settings/user/user-settings-context';
import { ReportingContext } from 'modules/reporting/reporting-context';
import { ReportDefinition } from 'modules/reporting/reporting-types';
import { DashboardContext } from './dashboard-context';
import { DashboardUserSettings, DashboardProps } from './dashboard-types';
import { DashboardCard } from './dashboard-card';
import { DashboardAddDialog } from './dashboard-add-dialog';
import { CardUniversal } from './cards/universal/card-universal';

function defaultCardFactory() {
  return CardUniversal;
}

export function Dashboard({
  cardFactory = defaultCardFactory,
  classes,
}: DashboardProps) {
  const { loadDefinitions } = useContext(ReportingContext);

  const [definitions, setDefinitions] = useState<ReportDefinition[]>([]);

  const addDialogRef = useRef<DialogHandle>(null);

  const { getCustomSettings, setCustomSettings } = useContext(
    UserSettingsContext
  );

  const loadDashboard = useCallback(() => {
    const settings = getCustomSettings('dashboard', 1) as
      | DashboardUserSettings
      | undefined;

    const items = settings?.items ?? [];
    const supportedDefinitions = definitions.filter(
      (definition) => definition.dashboardSupport
    );

    if (items.length !== supportedDefinitions.length) {
      return [...items, 'ADD'];
    }
    return [...items];
  }, [getCustomSettings, definitions]);

  const saveDashboard = useEventCallback((items: string[]) => {
    items = items.filter((item) => item !== 'ADD');

    const settings: DashboardUserSettings = {
      items,
      version: 1,
    };

    setCustomSettings('dashboard', settings);
  });

  const openAddDialog = useEventCallback(() => {
    addDialogRef.current?.open();
  });

  const remove = useEventCallback((id: string) => {
    saveDashboard(loadDashboard().filter((value) => value !== id));
  });

  useEffect(() => {
    loadDefinitions().then((definitions) => setDefinitions(definitions));
  }, [loadDefinitions]);

  return (
    <DashboardContext.Provider
      value={{
        definitions: definitions,
        classes,
        cardFactory,
        loadDashboard,
        saveDashboard,
        openAddDialog,
        remove,
      }}
    >
      <>
        <DndGrid
          columns={4}
          value={[...loadDashboard()]}
          onChange={saveDashboard}
          ItemComponent={DashboardCard}
        />
        <DashboardAddDialog ref={addDialogRef} />
      </>
    </DashboardContext.Provider>
  );
}
