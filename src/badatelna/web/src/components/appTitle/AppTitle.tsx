import React from "react";
import { useHistory, useLocation } from "react-router-dom";
import classNames from "classnames";

import { Props } from "./_types";
import { ModulePath } from "../../enums";
import { Icon, IconType } from "..";

export const AppTitle: React.FC<Props> = () => {
  const history = useHistory();
  const location = useLocation();

  const isClickable = location.pathname !== ModulePath.MAIN;

  return (
    <div
      className={classNames(
        "app-title flex-centered",
        isClickable && "clickable"
      )}
      onClick={() => isClickable && history.push(ModulePath.MAIN)}
    >
      <Icon type={IconType.BOOK} size={42} color="#fff" />
      &nbsp;&nbsp;&nbsp;
      <span className="first">Archiv</span>
      &nbsp;Online
    </div>
  );
};
