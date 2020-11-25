import { StateAction } from 'composite/navigation/navigation-context';

export interface LinkProps {
  to: string;
  replace?: boolean;
  state?: StateAction;
}
