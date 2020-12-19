package cz.aron.transfagent.elza.archentities;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 * List of other codes
 * 
 * Typically used to list deleted or compatible codes
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "other-codes")
public class OtherCodes {

    @XmlElement(name = "other-code", required = true)
    List<OtherCode> otherCodes;

    public List<OtherCode> getOtherCodes() {
        return otherCodes;
    }

    public void setOtherCodes(List<OtherCode> otherCodes) {
        this.otherCodes = otherCodes;
    }

}
