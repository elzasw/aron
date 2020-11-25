package cz.inqool.eas.common.authored.user;

import lombok.*;

import javax.persistence.Embeddable;

@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Getter
@Setter
@Embeddable
public class UserReference {
    protected String id;
    protected String name;
}
