package cz.aron.transfagent.peva.jsoncomponent;

import java.util.List;

public class Table {
	
	private List<Column> columns;
	
	private List<List<String>> rows;
	
	public Table(List<Column> columns, List<List<String>> rows) {
		this.columns = columns;
		this.rows = rows;
	}

	public List<Column> getColumns() {
		return columns;
	}

	public List<List<String>> getRows() {
		return rows;
	}		

}
