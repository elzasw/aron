export function isNotNullish<T>(value: T): value is NonNullable<T> {
  // (A)
  return value !== undefined && value !== null;
}
