import React from "react";
import { useHistory } from "react-router-dom";

import { Sidebar } from "./Sidebar";
import { Content } from "./Content";
import { Props } from "./_types";
import { dataItems } from "../../enums";

export const EvidenceMain: React.FC<Props> = ({ path }) => {
  const history = useHistory();

  return (
    <div className="evidence-main flex">
      <Sidebar />
      <Content
        {...{
          onClick: (id) => history.push(`${path}/${id}`),
          items: dataItems,
        }}
      />
    </div>
  );
};
