package cz.aron.transfagent.elza.archentities;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;



/**
 * Access point type (XML)
 *
 * @since 21.11.2016
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ap-type")
public class APTypeXml {

    @XmlAttribute(name = "code", required = true)
    private String code;

    @XmlAttribute(name = "party-type")
    private String partyType;

    @XmlAttribute(name = "parent-ap-type")
    private String parentType;

    @XmlElement(name = "name", required = true)
    private String name;

    @XmlElement(name = "read-only", required = true)
    private boolean readOnly;

    @XmlElement(name = "other-codes")
    private OtherCodes otherCodes;

    public OtherCodes getOtherCodes() {
        return otherCodes;
    }

    public void setOtherCodes(OtherCodes otherCodes) {
        this.otherCodes = otherCodes;
    }

    public String getCode() {
        return code;
    }

    public void setCode(final String code) {
        this.code = code;
    }

    public String getPartyType() {
        return partyType;
    }

    public void setPartyType(final String partyType) {
        this.partyType = partyType;
    }

    public String getParentType() {
        return parentType;
    }

    public void setParentType(final String parentRegisterType) {
        this.parentType = parentRegisterType;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public boolean isReadOnly() {
        return readOnly;
    }

    public void setReadOnly(final boolean readOnly) {
        this.readOnly = readOnly;
    }
}
