package cz.aron.domain;

public enum DataType {
    ENUM,           //výčtový typ
    APU_REF,        //odkaz na jinou jednotku publikace
    INTEGER,        //číselná hodnota
    STRING,         //textová hodnota
    UNITDATE,       //datace
    LINK,           //webový odkaz
    ITEM_AGGREG,    //virtual item for aggregating all APU_REF relations to one field
    JSON;           //json data
}
