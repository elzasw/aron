import { createContext } from 'react';
import { User } from './user-types';

export interface UserContext {
  user: User | undefined;
  hasPermission: (permission: string) => boolean;
  isLogedIn: () => boolean;
  reload: () => Promise<void>;
}

export const UserContext = createContext<UserContext>(undefined as any);
