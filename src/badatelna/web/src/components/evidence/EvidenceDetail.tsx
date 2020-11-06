import React from "react";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { faFilePdf } from "@fortawesome/free-solid-svg-icons";

import { Icon, IconType } from "../icon";
import { Props } from "./_types";
import { data } from "../../enums";
import { get } from "lodash";

export const EvidenceDetail: React.FC<Props> = () => {
  return (
    <div className="evidence-detail">
      <div className="evidence-detail-left">
        <div className="padding">
          {[
            {
              name: "name",
              label: "Archivní soubor",
            },
            {
              name: "archive",
              label: "Archiv",
            },
            {
              name: "evidenceNumber",
              label: "Evidenční číslo",
            },
            {
              name: "time",
              label: "Časový rozsah",
            },
            {
              name: "year",
              label: "Rok vzniku",
            },
            {
              name: "note",
              label: "Poznámka",
            },
            {
              label: "Přílohy",
            },
          ].map(({ name, label }) => (
            <div key={name} className="evidence-detail-item flex">
              <p className="evidence-detail-item-label bold">{label}</p>
              {name ? (
                <p className={name !== "note" ? "bold" : "padding-bottom"}>
                  {get(data, name, "")}
                </p>
              ) : (
                <div className="flex-col">
                  {["parni-mlekarna-609.pdf", "soa-zamrsk-609.pdf"].map(
                    (label) => (
                      <div
                        key={label}
                        className="evidence-detail-item-file flex-align-center margin-bottom-small"
                      >
                        <FontAwesomeIcon
                          icon={faFilePdf}
                          style={{ fontSize: 24 }}
                          className="icon"
                        />
                        &nbsp;&nbsp;<span>{label}</span>
                      </div>
                    )
                  )}
                </div>
              )}
            </div>
          ))}
        </div>
      </div>
      <div className="evidence-detail-right padding">
        <div className="evidence-detail-button padding padding-top-big padding-bottom-big">
          <Icon type={IconType.BOOK_SOLID} />
          <div className="margin-top-small">Zobrazit detail</div>
        </div>
      </div>
    </div>
  );
};
