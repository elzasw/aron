import React, { useState } from "react";
import classNames from "classnames";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { faCaretLeft } from "@fortawesome/free-solid-svg-icons";

import { Search } from "../search";
import { SidebarProps } from "./_types";

export const Sidebar: React.FC<SidebarProps> = (props) => {
  const [visible, setVisible] = useState(true);

  return (
    <div className={classNames("evidence-sidebar", visible && "visible")}>
      <div
        className="visible-button flex-centered padding-top-small padding-bottom-small"
        onClick={() => setVisible(!visible)}
      >
        <FontAwesomeIcon icon={faCaretLeft} style={{ fontSize: 26 }} />
      </div>
      <div className="sidebar-content">
        <div className="padding">
          <div className="padding-top padding-bottom">
            <Search onClick={() => null} />
          </div>
          {[
            {
              label: "Archiv",
              checkboxes: [
                { label: "SOA Zámrsk", info: "1200" },
                { label: "SOkA Hradec Králové", info: "320" },
                { label: "SOkA Chrudim", info: "12" },
                { label: "SOkA Jičín", info: "54" },
                { label: "SOkA Náchod", info: "33" },
              ],
              showNext: "23",
            },
            {
              label: "Druh",
              select: true,
            },
            {
              label: "Časový rozsah",
              checkboxes: [
                { label: "1201 - 1400", info: "8" },
                { label: "1401 - 1500", info: "23" },
                { label: "1501 - 1600", info: "23" },
                { label: "1601 1700", info: "39" },
              ],
            },
            {
              label: "Archivní soubor",
              select: true,
            },
          ].map(({ label, select, checkboxes, showNext }) => (
            <div key={label} className="sidebar-item">
              <h5>{label}</h5>
              {select ? (
                <select className="select"></select>
              ) : checkboxes ? (
                <div>
                  {checkboxes.map(({ label, info }) => (
                    <div className="flex-align-center full-width margin-bottom-tiny">
                      <input type="checkbox" key={label}></input>&nbsp;&nbsp;
                      {label}&nbsp;<span>({info})</span>
                    </div>
                  ))}
                  {showNext ? (
                    <p>Zobrazit další ({showNext})</p>
                  ) : (
                    <p>Zadat rozpětí</p>
                  )}
                </div>
              ) : (
                <></>
              )}
            </div>
          ))}
          <div className="padding-top margin-top-big">
            <button
              {...{
                className: "button-all",
              }}
            >
              Všechny filtry
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};
