import React, { useState } from "react";
import { Link, useLocation } from "react-router-dom";
import classNames from "classnames";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { faBars } from "@fortawesome/free-solid-svg-icons";

import { Props } from "./_types";
import { appHeaderItems } from "../../enums";
import { AppTitle } from "../appTitle";

export const AppHeader: React.FC<Props> = () => {
  const location = useLocation();
  const [open, setOpen] = useState(false);

  return (
    <div className="app-header">
      <div className="app-header-inner flex-space-between full-height">
        <AppTitle />
        <div className="app-header-items full-height">
          {appHeaderItems.map(({ path, label }) => (
            <Link
              key={path}
              to={{
                pathname: path,
              }}
              className={classNames(
                "app-header-item flex-centered padding-left padding-right",
                location.pathname === path && "active"
              )}
            >
              {label}
            </Link>
          ))}
        </div>
        <>
          <FontAwesomeIcon
            icon={faBars}
            style={{ fontSize: 24 }}
            className="icon"
            onClick={() => setOpen(!open)}
          />
          {open ? (
            <div
              className="app-header-items-mobile"
              onClick={() => setOpen(false)}
            >
              {appHeaderItems.map(({ path, label }) => (
                <Link
                  key={path}
                  to={{
                    pathname: path,
                  }}
                  className="app-header-item-mobile"
                >
                  {label}
                </Link>
              ))}
            </div>
          ) : (
            <></>
          )}
        </>
      </div>
    </div>
  );
};
