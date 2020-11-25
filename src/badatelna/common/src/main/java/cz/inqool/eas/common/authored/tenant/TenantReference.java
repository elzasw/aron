package cz.inqool.eas.common.authored.tenant;

import lombok.*;

import javax.persistence.Embeddable;

@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Getter
@Setter
@Embeddable
public class TenantReference {
    protected String id;
    protected String name;
}
