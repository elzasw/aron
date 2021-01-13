import { get } from 'lodash';

export function sortByArray<TItem, TSortItem>(
  array: TItem[],
  sortingArray: TSortItem[],
  field: string
) {
  let tmpArray = [...array];
  const result: TItem[] = [];

  sortingArray.forEach((sortItem) => {
    const key = get(sortItem, field);

    let found = false;

    tmpArray = tmpArray.filter((item) => {
      if (!found && get(item, field) == key) {
        result.push(item);
        found = true;
        return false;
      }

      return true;
    });
  });

  return result;
}
