import React from "react";
import { Link } from "react-router-dom";

import { ModulePath } from "../../enums";
import { Props } from "./_types";

export const Module: React.FC<Props> = ({ children, items }) => {
  const allItems = [{ path: ModulePath.MAIN, label: "Úvod" }, ...items];

  return (
    <div className="module">
      <div className="breadcrumbs flex-align-center padding-left-big">
        {allItems.map(({ path, label }, i) => (
          <div key={label} className="flex-centered">
            {i ? <div>&nbsp;&nbsp;&nbsp;/&nbsp;&nbsp;&nbsp;</div> : ""}
            {path && i < allItems.length - 1 ? (
              <Link to={{ pathname: path }} className="breadcrumbs-link">
                {label}
              </Link>
            ) : (
              label
            )}
          </div>
        ))}
      </div>
      {children}
    </div>
  );
};
