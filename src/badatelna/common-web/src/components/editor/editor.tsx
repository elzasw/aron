import React from 'react';
import { noop } from 'lodash';
import { ControlledEditor } from '@monaco-editor/react';
import { EditorProps } from './editor-types';
import { useEventCallback } from 'utils/event-callback-hook';

export function Editor({
  value,
  onChange = noop,
  height = 200,
  language,
  disabled,
}: EditorProps) {
  // fix undefined value
  value = value ?? null;

  // prepare value for MUI component
  value = value ?? '';

  language = language ?? 'text';

  const handleChange = useEventCallback((e, value: string | undefined) => {
    onChange(value ?? null);
  });

  return (
    <>
      <ControlledEditor
        height={height}
        language={language}
        value={value}
        theme="vs-light"
        options={{
          selectOnLineNumbers: true,
          readOnly: disabled,
          automaticLayout: true,
          minimap: {
            enabled: false,
          },
          wordWrap: 'on',
          wrappingIndent: 'indent',
          folding: true,
        }}
        onChange={handleChange}
      />
    </>
  );
}
