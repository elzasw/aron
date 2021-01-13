import React from 'react';

import { Props } from './types';
import { IconType } from '../../enums';
import { Icons } from '../../assets';

const getSrc = (type: IconType) => {
  switch (type) {
    case IconType.BOOK:
      return Icons.Book;
    case IconType.BOOK_SOLID:
      return Icons.BookSolid;
    case IconType.BUILDING_SOLID:
      return Icons.BuildingSolid;
    case IconType.FOLDER_OPEN_SOLID:
      return Icons.FolderOpenSolid;
    case IconType.GRADUATION_CAP_SOLID:
      return Icons.GraduationCapSolid;
    case IconType.ID_BADGE_SOLID:
      return Icons.IdBadgeSolid;
    case IconType.MAP_SOLID:
      return Icons.MapSolid;
    case IconType.USERS_SOLID:
      return Icons.UsersSolis;
    default:
      return Icons.Book;
  }
};

export function Icon({ type, size = 24, color, ...props }: Props) {
  return (
    <img
      {...props}
      src={getSrc(type)}
      style={{ width: size, height: size, color }}
    />
  );
}
