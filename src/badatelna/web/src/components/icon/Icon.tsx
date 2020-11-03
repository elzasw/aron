import React from "react";

import { Props } from "./_types";
import { IconType } from "./_enums";
import { Icons } from "../../assets";

const getComponent = (type: IconType) => {
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

export const Icon: React.FC<Props> = ({ type, size = 24, color, ...props }) => {
  let Component = getComponent(type);

  return (
    <Component style={{ width: size, height: size }} fill={color} {...props} />
  );
};
