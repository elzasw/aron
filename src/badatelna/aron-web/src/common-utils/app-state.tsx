import React, {
  createContext,
  useState,
  useCallback,
  useMemo,
  useContext,
  ReactNode,
} from 'react';

import { ModulePath } from '../enums';

interface AppState {
  evidencePath: ModulePath | null;
  evidenceDetailTreeHeight: number;
  evidenceDetailTreeExpandedItems: string[];
}

type AppStateOptional = Partial<AppState>;

interface IAppStateContext {
  appState: AppState;
  updateAppState: (appState: AppStateOptional) => void;
}

const AppStateContext = createContext<IAppStateContext>(undefined as any);

function useAppStateContext() {
  const [appState, setAppState] = useState<AppState>({
    evidencePath: null,
    evidenceDetailTreeHeight: 150,
    evidenceDetailTreeExpandedItems: [],
  });

  const updateAppState = useCallback(
    (newAppState: AppStateOptional) => {
      setAppState({ ...appState, ...newAppState });
    },
    [appState]
  );

  const context: IAppStateContext = useMemo(
    () => ({
      appState,
      updateAppState,
    }),
    [appState, updateAppState]
  );

  return { context };
}

export function AppStateProvider({ children }: { children: ReactNode }) {
  const { context } = useAppStateContext();

  return (
    <AppStateContext.Provider value={context}>
      {children}
    </AppStateContext.Provider>
  );
}

export function useAppState() {
  return useContext(AppStateContext);
}
