export const API_URL = `${process.env.URL_PREFIX || ''}/api/aron`;

export enum ApiUrl {
  APU = '/apu',
  APU_PART_TYPE = '/apuPartType',
  APU_PART_ITEM_TYPE = '/apuPartItemType',
  FACETS = '/facets',
  FILE = '/file',
  ME = '/me',
  NEWS = '/news',
  RELATION = '/relation',
  FAVORITE_QUERY = '/favoriteQuery',
}
