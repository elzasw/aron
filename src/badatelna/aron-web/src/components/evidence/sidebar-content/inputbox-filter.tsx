import React, { ReactElement, useState, useCallback } from 'react'
import { Option } from "../../../types";
import { FilterObject, FilterChangeCallBack } from '../types';
import { Autocomplete } from '../../autocomplete';
import { isEmpty } from 'lodash';
import { useStyles } from './styles';

interface Props {
    name: string,
    title: string,
    getOptions: (query: string) => Option[],
    onChange: FilterChangeCallBack;
    value: Option;
}

export default function InputboxFilter({name, title, getOptions, onChange, value}: Props): ReactElement {
    const [inputValue, setInputValue] = useState<Option | null>(value);
    
    const updateFilterValue = useCallback(
        (newFilterValue: FilterObject) => {
          onChange(name, !isEmpty(newFilterValue) ? newFilterValue : null);
        },
        [name, onChange]
      );
    const handleInputChange = (option: Option | any) => {
    setInputValue(option);
    updateFilterValue(option === null ? {} : {[option.id]: option.name});
    }

    const classes = useStyles();
    return (
        <>
        <div className={classes.filterTitle}>{title}</div>
        <Autocomplete
        {...{getOptions, value: inputValue, onChange: handleInputChange}}
        ></Autocomplete>
        </>
    )
}
