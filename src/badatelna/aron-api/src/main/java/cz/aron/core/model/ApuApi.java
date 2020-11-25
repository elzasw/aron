package cz.aron.core.model;

import cz.inqool.eas.common.domain.DomainApi;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.inject.Inject;

/**
 * @author Lukas Jane (inQool) 03.11.2020.
 */
@RestController
@RequestMapping("/apu")
public class ApuApi extends DomainApi<
        ApuEntity,
        ApuEntity,
        ApuEntity,
        ApuEntity,
        ApuEntity,
        ApuService> {

    @Inject private ApuTreeViewStore apuTreeViewStore;

    @GetMapping("/{id}/tree")
    public ApuEntityTreeView getSimpleTree(@PathVariable("id") String apuId) {
        ApuEntityTreeView apuEntityTreeView = apuTreeViewStore.find(apuId);
        return apuEntityTreeView;
    }
}

