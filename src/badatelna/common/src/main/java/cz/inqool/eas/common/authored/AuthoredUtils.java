package cz.inqool.eas.common.authored;

import cz.inqool.eas.common.authored.tenant.TenantGenerator;
import cz.inqool.eas.common.authored.tenant.TenantReference;
import cz.inqool.eas.common.authored.user.UserGenerator;
import cz.inqool.eas.common.authored.user.UserReference;
import cz.inqool.eas.common.domain.index.dto.filter.EqFilter;
import cz.inqool.eas.common.domain.index.dto.filter.Filter;
import cz.inqool.eas.common.domain.index.dto.filter.IdsFilter;


import static java.util.Collections.emptySet;

public class AuthoredUtils {
    public static Filter userFilter() {
        UserReference user = UserGenerator.generateValue();

        if (user != null) {
            return EqFilter.
                    builder().
                    field("createdBy.id").
                    value(user.getId()).
                    build();
        } else {
            return IdsFilter.
                    builder().
                    ids(emptySet()).
                    build();
        }
    }

    public static Filter tenantFilter() {
        TenantReference tenant = TenantGenerator.generateValue();

        if (tenant != null) {
            return EqFilter.
                    builder().
                    field("createdByTenant.id").
                    value(tenant.getId()).
                    build();
        } else {
            return IdsFilter.
                    builder().
                    ids(emptySet()).
                    build();
        }
    }
}
