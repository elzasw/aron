import React, { ReactElement, useState } from 'react'
import { Autocomplete as EasAutocomplete } from "@eas/common-web";
import { Option } from "../../types";

interface Props {
    value: Option | Option[] | null,
    onChange: (value: Option | Option[] | null) => void,
    getOptions: (input: string) => Option[],
}

export function Autocomplete({getOptions, value, onChange}: Props): ReactElement {

    const [options, setOptions] = useState<Option[]>(getOptions(""));
    const [loading, setLoading] = useState(false);

    
    const handleInputChange = async (query: string) => {
        setLoading(true)
        const newOptions = getOptions(query);
        setOptions(newOptions);
        setLoading(false)
    }

    const source = {
        setSearchQuery: handleInputChange,
        hasNextPage: () => false,
        isDataValid: () => true,
        setParams: () => null,
        loadMore: () => new Promise<void>(() => null),
        loading,
        reset: () => null,
        items: options,
        count: options.length
    }
    return (
        <EasAutocomplete
        {...{value, onChange, source}}>

        </EasAutocomplete>
    )
}
