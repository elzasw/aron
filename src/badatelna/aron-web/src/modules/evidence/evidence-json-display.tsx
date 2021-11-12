import React, {FC} from 'react';
import { Table } from '../../components';
import { JsonData } from './types';
import { JsonType } from '../../enums';

export const EvidenceJSONDisplay:FC<{
    jsonString: string;
}> = ({
    jsonString
}) => {
    try {
        const jsonData: JsonData = JSON.parse(jsonString);
        switch(jsonData.type){
            case JsonType.TABLE:
                return <Table data={jsonData.data}/>;
            default:
                return <>{jsonString.toString()}</>;
        }
    } catch (error){
        return <>{jsonString.toString()}</>;
    }
}
