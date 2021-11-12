import React from 'react';
import MuiTable from '@material-ui/core/Table'
import MuiTableRow from '@material-ui/core/TableRow'
import TableCell from '@material-ui/core/TableCell'
import TableHead from '@material-ui/core/TableHead'
import TableBody from '@material-ui/core/TableBody'
import TableContainer from '@material-ui/core/TableContainer'
import {
    TableRowProps,
    TableProps,
} from './types';

const TableRow = ({
    data, 
    columns = [], 
}: TableRowProps) => {
    return <MuiTableRow>
        {columns.map((column, index) =>
            <TableCell key={column.name}>{data[index]}</TableCell>
        )}
    </MuiTableRow>
}

export const Table = ({
    data
}: TableProps) => {
    const {columns} = data;

    return (
        <TableContainer>
            <MuiTable size='small'>
                <TableHead>
                    <MuiTableRow>
                        {columns.map(({name})=>
                            <TableCell key={name}>
                                {name.toUpperCase()}
                            </TableCell>
                        )}
                    </MuiTableRow>
                </TableHead>
                <TableBody>
                    {data.rows.map((row, index)=>
                        <TableRow key={index} data={row} columns={columns}/>
                    )}
                </TableBody>
            </MuiTable>
        </TableContainer>

    );
}
