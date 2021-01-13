export interface Tenant {
  id: string;
  name: string;
}

export interface Authority {
  authority: string;
}

export interface User {
  id: string | null;
  username: string;
  name: string;
  email: string;
  authorities: Authority[];
  tenant?: Tenant;
}

export interface UserProviderProps {
  meUrl: string;
  logoutUrl: string;
  checkPermission?: (user: any, permission: string) => boolean;
}

export interface AuthorizedProps {
  permission: string;
  shouldReload?: boolean;
}

export interface LoggedInProps {
  shouldReload?: boolean;
  redirectUrl?: string;
}
