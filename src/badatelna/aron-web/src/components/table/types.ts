export interface ColumnDefinition {
    name: string;
}

export interface TableData {
    columns: ColumnDefinition[];
    rows: string[][]
}

export interface TableProps {
    data: TableData;
}

export interface TableRowProps {
    data: string[];
    columns: ColumnDefinition[];
}
