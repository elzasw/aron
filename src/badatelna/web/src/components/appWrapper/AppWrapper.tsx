import React from "react";

import { Props } from "./_types";
import { AppHeader } from "..";

export const AppWrapper: React.FC<Props> = ({ children }) => {
  return (
    <div className="app-wrapper">
      <AppHeader />
      {children}
    </div>
  );
};
