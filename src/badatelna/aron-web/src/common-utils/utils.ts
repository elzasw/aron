import { get } from 'lodash';
import jsyaml from 'js-yaml';

export function sortByArray<TItem, TSortItem>(
  array: TItem[],
  sortingArray: TSortItem[],
  field: string
) {
  let tmpArray = [...array];
  const result: TItem[] = [];

  sortingArray.forEach((sortItem) => {
    const key = get(sortItem, field);

    tmpArray = tmpArray.filter((item) => {
      if (get(item, field) == key) {
        result.push(item);
        return false;
      }

      return true;
    });
  });

  return result;
}

export function parseYaml(text?: string) {
  if (text) {
    try {
      return jsyaml.load(text) as any;
    } catch (e) {
      console.log(e);
    }
  }

  return null;
}

export function openUrl(url: string) {
  window.open(url, '_blank');
}

export function iOS() {
  return (
    [
      'iPad Simulator',
      'iPhone Simulator',
      'iPod Simulator',
      'iPad',
      'iPhone',
      'iPod',
    ].includes(navigator.platform) ||
    // iPad on iOS 13 detection
    (navigator.userAgent.includes('Mac') && 'ontouchend' in document)
  );
}

export const isUUID = (value: string) =>
  /\b[0-9a-fA-F]{8}\b-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-\b[0-9a-fA-F]{12}\b/.test(
    value
  );
