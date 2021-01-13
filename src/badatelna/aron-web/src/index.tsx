import 'core-js/stable';
import 'regenerator-runtime/runtime';
import React from 'react';
import { render } from 'react-dom';

import { App } from './app';

import { initHotreload } from '@eas/common-web';

if (process.env.NODE_ENV === 'development') {
  initHotreload('ws://localhost:8090');
}

render(<App />, document.getElementById('app'));
