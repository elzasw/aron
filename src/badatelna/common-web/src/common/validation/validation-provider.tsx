import * as React from 'react';
import * as Yup from 'yup';
import { useDefaultValidationMessages } from './validation-messages';

export function ValidationProvider({
  children,
}: React.PropsWithChildren<unknown>) {
  const msgs = useDefaultValidationMessages();

  React.useEffect(() => {
    Yup.setLocale({
      mixed: {
        required: msgs.requiredField,
      },
      number: {
        max: msgs.maxLength,
      },
    });
  }, [msgs]);

  return <>{children}</>;
}
