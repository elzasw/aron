import { createContext } from 'react';

export interface UserContext<USER> {
  user: USER | undefined;
  hasPermission: (permission: string) => boolean;
  isLogedIn: () => boolean;
  reload: () => Promise<void>;
  logout: (automatic?: boolean) => void;
}

export const UserContext = createContext<UserContext<any>>(undefined as any);
