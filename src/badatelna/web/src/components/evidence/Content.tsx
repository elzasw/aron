import React from "react";

import { ContentProps } from "./_types";

export const Content: React.FC<ContentProps> = ({ onClick, items }) => {
  return (
    <div className="evidence-content">
      <div className="padding">
        {items.map(({ id, name, sub, evidenceNumber, time, archive }) => (
          <div
            key={id}
            onClick={() => onClick(id)}
            className="evidence-content-item margin-bottom"
          >
            <h4>
              {evidenceNumber}/{sub}
            </h4>
            <p>
              {evidenceNumber} {name} {time} (1949 - 1951) ({archive})
            </p>
          </div>
        ))}
      </div>
    </div>
  );
};
