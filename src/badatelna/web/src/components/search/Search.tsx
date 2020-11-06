import React from "react";
import classNames from "classnames";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { faSearch } from "@fortawesome/free-solid-svg-icons";

import { Props } from "./_types";

export const Search: React.FC<Props> = ({ big, onClick }) => {
  return (
    <div className={classNames("search", big && "big")}>
      <div className="search-inner flex-centered">
        <FontAwesomeIcon icon={faSearch} className="icon" />
        <input placeholder="Napište, co hledáte..." />
        <button {...{ onClick: () => onClick() }}>Hledat</button>
      </div>
      {big ? (
        <div className="flex-row-end padding-top">
          <div className="search-advanced">Pokročilé vyhledávání</div>
        </div>
      ) : (
        <></>
      )}
    </div>
  );
};
