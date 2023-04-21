package cz.aron.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cz.aron.domain.types.TypesHolder;
import cz.aron.domain.types.dto.ApuPartType;

import java.util.Collection;

@RestController
@RequestMapping("/apuPartType")
public class ApuPartTypeApi {
    private final TypesHolder typesHolder;
    
    public ApuPartTypeApi(TypesHolder typesHolder) {
		this.typesHolder = typesHolder;
	}

	@GetMapping
    public Collection<ApuPartType> list() {
        return typesHolder.getAllApuPartTypes();
    }
}
