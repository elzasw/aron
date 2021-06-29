import React from 'react';
import { noop } from 'lodash';
import MonacoEditor from '@monaco-editor/react';
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

  const handleChange = useEventCallback((value: string | undefined) => {
    onChange(value ?? null);
  });

  return (
    <>
      <MonacoEditor
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
