import React, { useRef } from 'react';
import { noop } from 'lodash';

import { ConfirmDialog, DialogHandle, useEventCallback } from '@eas/common-web';

import { Props } from './types';
import { Button } from '../button';

export function ConfirmButton({
  title = 'Potvrdit',
  text = 'Opravdu chcete provést akci?',
  onConfirm = noop,
  ...props
}: Props) {
  const ref = useRef<DialogHandle>(null);

  const handleClick = useEventCallback(() => {
    ref.current?.open();
  });

  return (
    <>
      <Button {...props} onClick={handleClick} />
      <ConfirmDialog
        ref={ref}
        onConfirm={onConfirm}
        onCancel={noop}
        title={title}
        text={text}
      />
    </>
  );
}
