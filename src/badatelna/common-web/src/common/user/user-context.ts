import { createContext } from 'react';

export interface UserContext<USER, STATE = any> {
  user: USER | undefined;
  hasPermission: (permission: string, state?: STATE) => boolean;
  isLogedIn: () => boolean;
  reload: () => Promise<void>;
  logout: (automatic?: boolean) => void;
  logoutWithoutRedirect: () => void;
}

export const UserContext = createContext<UserContext<any, any>>(
  undefined as any
);
