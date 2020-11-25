import { useEventCallback } from './event-callback-hook';

export function useNumberAdapter(
  value: number | null,
  onChange: (value: number | null) => void
) {
  const stringValue = value != null ? String(value) : '';
  const handleChange = useEventCallback((value: string) => {
    onChange(value != '' ? Number(value) : null);
  });

  return { value: stringValue, onChange: handleChange };
}
