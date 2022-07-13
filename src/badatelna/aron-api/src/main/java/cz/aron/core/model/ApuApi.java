package cz.aron.core.model;

import cz.inqool.eas.common.domain.DomainApi;
import cz.inqool.eas.common.domain.index.dto.Result;
import cz.inqool.eas.common.domain.index.dto.params.Params;
import cz.inqool.eas.common.exception.MissingObject;

import org.springframework.web.bind.annotation.*;

import javax.inject.Inject;
import javax.validation.Valid;

import java.util.List;

import static cz.inqool.eas.common.utils.AssertionUtils.coalesce;
import static cz.inqool.eas.common.utils.AssertionUtils.notNull;

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
    @Inject private ApuEntityViewStore apuEntityViewStore;
    @Inject private ApuRepository apuRepository;

    @GetMapping("/{id}/tree")
    public ApuEntityTreeView getSimpleTree(@PathVariable("id") String apuId) {
        ApuEntityTreeView apuEntityTreeView = apuTreeViewStore.find(apuId);
        return apuEntityTreeView;
    }

    @GetMapping("/labels")
    public List<IdLabelDto> mapNames(@RequestBody List<String> ids) {
        return service.mapNames(ids);
    }
    
    @PostMapping("/listview")
    public Result<ApuEntityView> listView(@Valid @RequestBody(required = false) Params params) {
        
        params = coalesce(params, Params::new);
        
        Result<String> idsResult = apuRepository.getIndex().listIdsByParams(params);
        List<ApuEntityView> items = apuEntityViewStore.listByIds(idsResult.getItems());
        return new Result<>(items, idsResult.getCount(), idsResult.getSearchAfter(), idsResult.getAggregations());
    }
    
    @GetMapping(value = "/{id}/view")
    public ApuEntityView getView(@PathVariable("id") String id) {                        
        ApuEntityView view = apuEntityViewStore.find(id);        
        notNull(view, () -> new MissingObject(ApuEntityView.class, id));
        return view;
    }

}

