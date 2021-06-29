import { ReactChild } from 'react';

import { PageTemplate } from '../../types';

export interface Props {
  children: ReactChild;
  pageTemplate?: PageTemplate;
  appLogo?: string;
  appTopImage?: string;
}
