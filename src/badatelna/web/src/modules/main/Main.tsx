import React from "react";
import { Icon, IconType } from "../../components";

import { Body } from "./Body";
import { Footer } from "./Footer";

export const Main: React.FC = () => {
  return (
    <div className="main">
      <Icon type={IconType.BOOK} className="main-background-icon" />
      <div className="main-inner flex-column">
        <Body />
        <Footer />
      </div>
    </div>
  );
};
