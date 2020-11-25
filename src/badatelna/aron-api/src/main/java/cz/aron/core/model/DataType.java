package cz.aron.core.model;

/**
 * @author Lukas Jane (inQool) 27.10.2020.
 */
public enum DataType {
    ENUM,           //výčtový typ
    APU_REF,        //odkaz na jinou jednotku publikace
    INTEGER,        //číselná hodnota
    STRING,         //textová hodnota
    UNITDATE,       //datace
    LINK;           //webový odkaz
}
