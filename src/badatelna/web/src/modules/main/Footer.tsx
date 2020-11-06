import React from "react";

import { AppTitle } from "../../components";

export const Footer: React.FC = () => {
  return (
    <div className="main-footer">
      <div className="padding">
        <div className="flex flex-wrap">
          <div className="main-footer-left flex-align-top padding">
            <AppTitle />
          </div>
          <div className="flex">
            {[
              {
                title: "Základní informace",
                content: [
                  "Lorem ipsum dolor sit amet, consectetuer adipiscing elit. Nam quis nulla. Aliquam erat volutpat. Lorem ipsum dolor sit amet, consectetuer adipiscing elit. Nulla non lectus sed nisl molestie malesuada. Donec ipsum massa, ullamcorper in, auctor et, scelerisque sed, est.",
                ],
              },
              {
                title: "Kontakt",
                content: ["info@archivonline.cz", "+420 777 888 999"],
              },
            ].map(({ title, content }) => (
              <div key={title} className="main-footer-section padding">
                <p className="main-footer-title margin-tiny">{title}</p>
                {content.map((c) => (
                  <p key={c} className="main-footer-text margin-none">
                    {c}
                  </p>
                ))}
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
};
