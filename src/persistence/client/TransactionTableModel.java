package persistence.client;

import java.util.List;
import javax.swing.table.AbstractTableModel;
import persistence.Transaction;

class TransactionTableModel extends AbstractTableModel {
	List list;

	public TransactionTableModel(List list) {
		this.list=list;
	}

	void reload() {
		fireTableDataChanged();
	}

	public int getColumnCount() {
		return 2;
	}

	public int getRowCount() {
		return list.size();
	}

	public String getColumnName(int column) {
		switch(column) {
			case 0:
				return "transaction";
			case 1:
				return "rollbackOnly";
			default:
				return null;
		}
	}

	public Object getValueAt(int rowIndex, int columnIndex) {
		switch(columnIndex) {
			case 0:
				return get(rowIndex);
			case 1:
				return new Boolean(get(rowIndex).isRollbackOnly());
			default:
				return null;
		}
	}

	Transaction get(int n) {
		return (Transaction)list.get(n);
	}
}
