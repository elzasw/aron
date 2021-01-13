package cz.aron.core.relation;

import cz.inqool.eas.common.domain.DomainApi;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Lukas Jane (inQool) 03.11.2020.
 */
@RestController
@RequestMapping("/relation")
public class RelationApi extends DomainApi<
        Relation,
        Relation,
        Relation,
        Relation,
        Relation,
        RelationService> {
}

