package cz.inqool.eas.common.authored.store;

import cz.inqool.eas.common.authored.Authored;
import cz.inqool.eas.common.authored.tenant.CreatedByTenantGenerator;
import cz.inqool.eas.common.authored.tenant.TenantReference;
import cz.inqool.eas.common.authored.tenant.UpdatedByTenantGenerator;
import cz.inqool.eas.common.authored.user.CreatedByGenerator;
import cz.inqool.eas.common.authored.user.UpdatedByGenerator;
import cz.inqool.eas.common.authored.user.UserReference;
import cz.inqool.eas.common.dated.store.DatedObject;
import cz.inqool.entityviews.ViewableClass;
import cz.inqool.entityviews.ViewableImplement;
import cz.inqool.entityviews.ViewableMapping;
import cz.inqool.entityviews.ViewableProperty;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenerationTime;
import org.hibernate.annotations.GeneratorType;

import javax.persistence.AttributeOverride;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.MappedSuperclass;

import static cz.inqool.eas.common.domain.DomainViews.*;

@ViewableClass(views = {DEFAULT, CREATE, UPDATE, IDENTIFIED})
@ViewableMapping(views = DEFAULT, mappedTo = DEFAULT)
@ViewableMapping(views = CREATE, mappedTo = CREATE)
@ViewableMapping(views = UPDATE, mappedTo = UPDATE)
@ViewableMapping(views = IDENTIFIED, mappedTo = IDENTIFIED)
@ViewableImplement(value = Authored.class, views = DEFAULT)
@Getter
@Setter
@MappedSuperclass
abstract public class AuthoredObject<ROOT> extends DatedObject<ROOT> implements Authored<ROOT> {
    /**
     * Field should be assigned with logged in {@link UserReference} when entity is first created.
     */
    @ViewableProperty(views = DEFAULT)
    @GeneratorType( type = CreatedByGenerator.class, when = GenerationTime.INSERT)
    @Embedded
    @AttributeOverride(name = "id", column = @Column(name = "created_by_id"))
    @AttributeOverride(name = "name", column = @Column(name = "created_by_name"))
    protected UserReference createdBy;

    /**
     * Field should be assigned with logged in {@link TenantReference} when entity is first created.
     */
    @ViewableProperty(views = DEFAULT)
    @GeneratorType( type = CreatedByTenantGenerator.class, when = GenerationTime.INSERT)
    @Embedded
    @AttributeOverride(name = "id", column = @Column(name = "created_by_tenant_id"))
    @AttributeOverride(name = "name", column = @Column(name = "created_by_tenant_name"))
    protected TenantReference createdByTenant;

    /**
     * Field should be assigned with logged in {@link UserReference} when entity is updated.
     */
    @ViewableProperty(views = DEFAULT)
    @GeneratorType( type = UpdatedByGenerator.class, when = GenerationTime.ALWAYS)
    @Embedded
    @AttributeOverride(name = "id", column = @Column(name = "updated_by_id"))
    @AttributeOverride(name = "name", column = @Column(name = "updated_by_name"))
    protected UserReference updatedBy;

    /**
     * Field should be assigned with logged in {@link TenantReference} when entity is updated.
     */
    @ViewableProperty(views = DEFAULT)
    @GeneratorType( type = UpdatedByTenantGenerator.class, when = GenerationTime.ALWAYS)
    @Embedded
    @AttributeOverride(name = "id", column = @Column(name = "updated_by_tenant_id"))
    @AttributeOverride(name = "name", column = @Column(name = "updated_by_tenant_name"))
    protected TenantReference updatedByTenant;

    /**
     * Field should be assigned with logged in {@link UserReference} in Store.
     */
    @ViewableProperty(views = DEFAULT)
    @Embedded
    @AttributeOverride(name = "id", column = @Column(name = "deleted_by_id"))
    @AttributeOverride(name = "name", column = @Column(name = "deleted_by_name"))
    protected UserReference deletedBy;

    /**
     * Field should be assigned with logged in {@link TenantReference} in Store.
     */
    @ViewableProperty(views = DEFAULT)
    @Embedded
    @AttributeOverride(name = "id", column = @Column(name = "deleted_by_tenant_id"))
    @AttributeOverride(name = "name", column = @Column(name = "deleted_by_tenant_name"))
    protected TenantReference deletedByTenant;
}
