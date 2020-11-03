import React from "react";
import { useHistory } from "react-router-dom";

import { Icon, Search } from "../../components";
import { favouriteQueries, ModulePath } from "../../enums";

export const Body: React.FC = () => {
  const history = useHistory();

  return (
    <div className="main-body flex-centered padding-top-big padding-bottom-big">
      <div className="main-body-inner">
        <h1>Zadejte hledaný dotaz</h1>
        <Search
          big={true}
          onClick={() => history.push(ModulePath.ARCHIVAL_FILES)}
        />
        <h4 className="margin-top-big">Oblíbené dotazy</h4>
        <div className="main-body-favourite flex flex-wrap padding-bottom-big">
          {favouriteQueries.map(({ icon, label }) => (
            <div key={label} className="margin-bottom-small flex-align-center">
              <Icon type={icon} className="icon" />
              &nbsp;&nbsp;&nbsp;{label}
            </div>
          ))}
        </div>
      </div>
    </div>
  );
};
