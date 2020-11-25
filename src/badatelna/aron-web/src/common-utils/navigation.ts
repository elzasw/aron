export const getUrlWithQuery = (path: string, query?: string) =>
  `${path}${query ? `?query=${encodeURI(query)}` : ''}`;
