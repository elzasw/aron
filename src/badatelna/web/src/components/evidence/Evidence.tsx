import React from "react";
import { get, find } from "lodash";
import { useLocation } from "react-router-dom";

import { dataItems } from "../../enums";
import { Module } from "../module";
import { EvidenceDetail } from "./EvidenceDetail";
import { EvidenceMain } from "./EvidenceMain";
import { Props } from "./_types";

export const Evidence: React.FC<Props> = (props) => {
  const { path, label } = props;

  const location = useLocation();

  let id: string = "";
  if (location.pathname !== path) {
    id = location.pathname.replace(/^\/[^/]+\//, "");
  }

  const Component = id ? EvidenceDetail : EvidenceMain;

  return (
    <Module
      {...{
        ...props,
        items: id
          ? [
              { path, label },
              {
                label: get(
                  find(dataItems, (item) => item.id === id),
                  "name",
                  "Neznámé"
                ),
              },
            ]
          : [{ label }],
      }}
    >
      <div className="evidence">
        <Component {...props} />
      </div>
    </Module>
  );
};
