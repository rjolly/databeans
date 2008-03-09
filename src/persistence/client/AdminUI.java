/*
 * AdminUI.java
 *
 * Created on February 10, 2008, 12:35 PM
 */

package persistence.client;

import bsh.Interpreter;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.ExpandVetoException;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import persistence.AdminConnection;
import persistence.Connection;
import persistence.Connections;
import persistence.PersistentObject;
import persistence.PersistentSystem;
import persistence.Transaction;

/**
 *
 * @author  raphael
 */
public class AdminUI extends javax.swing.JFrame {
	Connection conn;
	DefaultTreeModel model=new DefaultTreeModel(null);
	TransactionTableModel transactionModel;
	Interpreter interpreter;

	/** Creates new form AdminUI */
	public AdminUI() {
		initComponents();
		jDialog1.pack();
		jDialog2.pack();
		jTree1.setModel(model);
		jTree1.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		jTree1.addTreeSelectionListener(new TreeSelectionListener() {
			public void valueChanged(TreeSelectionEvent e) {
				ObjectTreeNode node = (ObjectTreeNode)jTree1.getLastSelectedPathComponent();
				Object object = node==null?null:node.object;
				int n = jSplitPane1.getDividerLocation();
				if(object instanceof PersistentObject) {
					jTable1.setModel(ObjectTableModel.model(object));
					jSplitPane1.setRightComponent(jScrollPane3);
				} else {
					jTextArea1.setText(String.valueOf(object));
					jSplitPane1.setRightComponent(jScrollPane2);
				}
				jSplitPane1.setDividerLocation(n);
			}
		});
		jTree1.addTreeWillExpandListener(new TreeWillExpandListener() {
			public void treeWillCollapse(TreeExpansionEvent event) throws ExpandVetoException {}

			public void treeWillExpand(TreeExpansionEvent event) throws ExpandVetoException {
				TreePath path=event.getPath();
				model.reload((TreeNode)path.getLastPathComponent());
				if(path.getPathCount()==1) jTree1.setSelectionRow(0);
			}
		});
		jTable2.addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				maybeShowPopup(e);
			}

			public void mouseReleased(MouseEvent e) {
				maybeShowPopup(e);
			}

			private void maybeShowPopup(MouseEvent e) {
				if (e.isPopupTrigger()) {
					int m=jTable2.getSelectedRowCount();
					if(m<2) {
						int n=jTable2.rowAtPoint(e.getPoint());
						jTable2.setRowSelectionInterval(n,n);
					}
					jPopupMenu1.show(e.getComponent(),
					e.getX(), e.getY());
				}
			}
		});
		interpreter = new Interpreter( jConsole1 );
		new Thread( interpreter ).start(); // start a thread to call the run() method
		enableTabs(new boolean[] {false,false,false,false,false,false,true});
	}

	void setDialogLocation(Component dialog) {
		Point location=this.getLocation();
		Dimension size=this.getSize();
		Dimension s=dialog.getSize();
		location.translate((size.width-s.width)>>1,(size.height-s.height)>>1);
		dialog.setLocation(location);
	}

	void enableTabs(boolean tab[]) {
		for(int i=0;i<tab.length;i++) jTabbedPane1.setEnabledAt(i, tab[i]);
		for(int i=0;i<tab.length;i++) if(tab[i]) {
			jTabbedPane1.setSelectedIndex(i);
			break;
		}
	}

	void open() {
		new Thread() {
			public void run() {
				try {
					open0();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}.start();
	}

	void open0() throws Exception {
		try {
			open1();
			SwingUtilities.invokeAndWait(new Runnable() {
				public void run() {
					open2();
				}
			});
		} catch (final Exception e) {
			SwingUtilities.invokeAndWait(new Runnable() {
				public void run() {
					error(e);
				}
			});
		}
	}

	void open1() throws Exception {
		boolean admin=jCheckBox1.isSelected();
		String location=jTextField1.getText();
		conn=admin?Connections.getAdminConnection(location):Connections.getConnection(location);
	}

	void open2() {
		boolean admin=jCheckBox1.isSelected();
		try {
			PersistentSystem system=conn.system();
			setSystem(system);
			setTransactions(system.getTransactions());
		} catch (Exception e) {
			error(e);
		}
		try {
			interpreter.set("conn",conn);
		} catch (bsh.EvalError e) {
			error(e);
		}
		enableTabs(new boolean[] {true,admin,admin,admin,admin,admin,true});
	}

	void setSystem(PersistentSystem system) {
		model.setRoot(ObjectTreeNode.node(system,"system"));
		model.reload();
		jTree1.setSelectionRow(0);
	}

	void close() {
		try {
			interpreter.set("conn",null);
		} catch (bsh.EvalError e) {
			error(e);
		}
		try {
			conn.close();
		} catch (Exception e) {
			error(e);
		}
		enableTabs(new boolean[] {false,false,false,false,false,false,true});
	}

	void setTransactions(List list) {
		transactionModel=new TransactionTableModel(list);
		jTable2.setModel(transactionModel);
	}

	void refreshTransactions() {
		transactionModel.fireTableDataChanged();
	}

	void abort() {
		AdminConnection conn=(AdminConnection)this.conn;
		int row[]=jTable2.getSelectedRows();
		try {
			for(int i=0;i<row.length;i++) {
				conn.abortTransaction(transaction(row[i]));
			}
			refreshTransactions();
		} catch (Exception e) {
			error(e);
		}
	}

	Transaction transaction(int n) {
		return (Transaction)jTable2.getValueAt(n, 0);
	}

	void export() {
		AdminConnection conn=(AdminConnection)this.conn;
		boolean overwrite=jCheckBox4.isSelected();
		String name=jTextField2.getText();
		if(overwrite || !new File(name).exists()) try {
			conn.export(name);
			jCheckBox4.setSelected(false);
		} catch (Exception e) {
			error(e);
		}
	}

	void inport() {
		AdminConnection conn=(AdminConnection)this.conn;
		boolean confirm=jCheckBox5.isSelected();
		String name=jTextField2.getText();
		if(confirm) try {
			conn.inport(name);
			model.reload();
			jTree1.setSelectionRow(0);
			jCheckBox5.setSelected(false);
		} catch (Exception e) {
			error(e);
		}
	}

	void addUser() {
		AdminConnection conn=(AdminConnection)this.conn;
		String name=jTextField3.getText();
		char pwd[]=jPasswordField1.getPassword();
		char conf[]=jPasswordField2.getPassword();
		if(Arrays.equals(pwd, conf)) try {
			conn.addUser(name, String.valueOf(pwd));
		} catch (Exception e) {
			error(e);
		}
	}

	void deleteUser() {
		AdminConnection conn=(AdminConnection)this.conn;
		String name=jTextField3.getText();
		boolean confirm=jCheckBox3.isSelected();
		if(confirm) try {
			conn.deleteUser(name);
			jCheckBox3.setSelected(false);
		} catch (Exception e) {
			error(e);
		}
	}

	void changePassword() {
		AdminConnection conn=(AdminConnection)this.conn;
		String name=jTextField3.getText();
		char pwd[]=jPasswordField1.getPassword();
		char conf[]=jPasswordField2.getPassword();
		char old[]=jPasswordField3.getPassword();
		boolean enable=jCheckBox6.isSelected();
		if(Arrays.equals(pwd, conf)) try {
			if(enable) conn.changePassword(name, String.valueOf(old), String.valueOf(pwd));
			else conn.changePassword(name, String.valueOf(pwd));
		} catch (Exception e) {
			error(e);
		}
	}

	void refresh() {
		AdminConnection conn=(AdminConnection)this.conn;
		try {
			long max=conn.maxSpace();
			long value=conn.allocatedSpace();
			jProgressBar1.setMaximum((int)max);
			jProgressBar1.setValue((int)value);
			jProgressBar1.setString(String.valueOf(value)+"/"+String.valueOf(max));
		} catch (Exception e) {
			error(e);
		}
	}

	void gc() {
		AdminConnection conn=(AdminConnection)this.conn;
		try {
			conn.gc();
			refresh();
		} catch (Exception e) {
			error(e);
		}
	}

	void shutdown() {
		AdminConnection conn=(AdminConnection)this.conn;
		boolean confirm=jCheckBox2.isSelected();
		if(confirm) try {
			conn.shutdown();
			jCheckBox2.setSelected(false);
			close();
		} catch (Exception e) {
			error(e);
		}
	}

	void error(Exception e) {
		JOptionPane.showMessageDialog(this, e.toString(), "Error", JOptionPane.ERROR_MESSAGE);
	}

	/** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
        // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
        private void initComponents() {

                jScrollPane3 = new javax.swing.JScrollPane();
                jTable1 = new javax.swing.JTable();
                jDialog1 = new javax.swing.JDialog();
                jLabel1 = new javax.swing.JLabel();
                jTextField1 = new javax.swing.JTextField();
                jButton1 = new javax.swing.JButton();
                jButton2 = new javax.swing.JButton();
                jCheckBox1 = new javax.swing.JCheckBox();
                jDialog2 = new javax.swing.JDialog();
                jLabel2 = new javax.swing.JLabel();
                jButton3 = new javax.swing.JButton();
                jPopupMenu1 = new javax.swing.JPopupMenu();
                jMenuItem1 = new javax.swing.JMenuItem();
                jTabbedPane1 = new javax.swing.JTabbedPane();
                jSplitPane1 = new javax.swing.JSplitPane();
                jScrollPane1 = new javax.swing.JScrollPane();
                jTree1 = new javax.swing.JTree();
                jScrollPane2 = new javax.swing.JScrollPane();
                jTextArea1 = new javax.swing.JTextArea();
                jScrollPane4 = new javax.swing.JScrollPane();
                jTable2 = new javax.swing.JTable();
                jPanel2 = new javax.swing.JPanel();
                jLabel4 = new javax.swing.JLabel();
                jTextField3 = new javax.swing.JTextField();
                jLabel5 = new javax.swing.JLabel();
                jLabel6 = new javax.swing.JLabel();
                jButton6 = new javax.swing.JButton();
                jButton7 = new javax.swing.JButton();
                jButton8 = new javax.swing.JButton();
                jCheckBox3 = new javax.swing.JCheckBox();
                jPasswordField1 = new javax.swing.JPasswordField();
                jPasswordField2 = new javax.swing.JPasswordField();
                jPasswordField3 = new javax.swing.JPasswordField();
                jLabel11 = new javax.swing.JLabel();
                jCheckBox6 = new javax.swing.JCheckBox();
                jPanel1 = new javax.swing.JPanel();
                jLabel3 = new javax.swing.JLabel();
                jTextField2 = new javax.swing.JTextField();
                jButton4 = new javax.swing.JButton();
                jButton5 = new javax.swing.JButton();
                jCheckBox4 = new javax.swing.JCheckBox();
                jCheckBox5 = new javax.swing.JCheckBox();
                jPanel3 = new javax.swing.JPanel();
                jButton10 = new javax.swing.JButton();
                jProgressBar1 = new javax.swing.JProgressBar();
                jPanel4 = new javax.swing.JPanel();
                jButton11 = new javax.swing.JButton();
                jCheckBox2 = new javax.swing.JCheckBox();
                jConsole1 = new bsh.util.JConsole();
                menuBar = new javax.swing.JMenuBar();
                fileMenu = new javax.swing.JMenu();
                openMenuItem = new javax.swing.JMenuItem();
                closeMenuItem = new javax.swing.JMenuItem();
                exitMenuItem = new javax.swing.JMenuItem();
                helpMenu = new javax.swing.JMenu();
                aboutMenuItem = new javax.swing.JMenuItem();

                jTable1.setModel(new javax.swing.table.DefaultTableModel(
                        new Object [][] {
                                {null, null},
                                {null, null},
                                {null, null},
                                {null, null}
                        },
                        new String [] {
                                "Title 1", "Title 2"
                        }
                ));
                jScrollPane3.setViewportView(jTable1);

                jDialog1.setTitle("Open connection");
                jDialog1.setModal(true);

                jLabel1.setText("Location :");

                jTextField1.setText("//localhost/store");

                jButton1.setText("Ok");
                jButton1.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                jButton1ActionPerformed(evt);
                        }
                });

                jButton2.setText("Cancel");
                jButton2.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                jButton2ActionPerformed(evt);
                        }
                });

                jCheckBox1.setSelected(true);
                jCheckBox1.setText("Admin");

                org.jdesktop.layout.GroupLayout jDialog1Layout = new org.jdesktop.layout.GroupLayout(jDialog1.getContentPane());
                jDialog1.getContentPane().setLayout(jDialog1Layout);
                jDialog1Layout.setHorizontalGroup(
                        jDialog1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                        .add(jDialog1Layout.createSequentialGroup()
                                .addContainerGap()
                                .add(jDialog1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                                        .add(jDialog1Layout.createSequentialGroup()
                                                .add(jLabel1)
                                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                                .add(jTextField1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 302, Short.MAX_VALUE))
                                        .add(org.jdesktop.layout.GroupLayout.TRAILING, jDialog1Layout.createSequentialGroup()
                                                .add(jButton1)
                                                .add(18, 18, 18)
                                                .add(jButton2))
                                        .add(jCheckBox1))
                                .addContainerGap())
                );
                jDialog1Layout.setVerticalGroup(
                        jDialog1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                        .add(jDialog1Layout.createSequentialGroup()
                                .addContainerGap()
                                .add(jDialog1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                                        .add(jLabel1)
                                        .add(jTextField1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(jCheckBox1)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .add(jDialog1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                                        .add(jButton2)
                                        .add(jButton1))
                                .addContainerGap())
                );

                jDialog2.setModal(true);

                jLabel2.setText("<html>databeans : a new, fully object oriented persistence framework for java<br/>Copyright (C) 2007-2008 Databeans<br/><br/>Version 2.0rc16</html>");

                jButton3.setText("Ok");
                jButton3.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                jButton3ActionPerformed(evt);
                        }
                });

                org.jdesktop.layout.GroupLayout jDialog2Layout = new org.jdesktop.layout.GroupLayout(jDialog2.getContentPane());
                jDialog2.getContentPane().setLayout(jDialog2Layout);
                jDialog2Layout.setHorizontalGroup(
                        jDialog2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                        .add(jDialog2Layout.createSequentialGroup()
                                .addContainerGap()
                                .add(jLabel2)
                                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .add(org.jdesktop.layout.GroupLayout.TRAILING, jDialog2Layout.createSequentialGroup()
                                .addContainerGap(433, Short.MAX_VALUE)
                                .add(jButton3)
                                .addContainerGap())
                );
                jDialog2Layout.setVerticalGroup(
                        jDialog2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                        .add(jDialog2Layout.createSequentialGroup()
                                .addContainerGap()
                                .add(jLabel2)
                                .add(18, 18, 18)
                                .add(jButton3)
                                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                );

                jMenuItem1.setText("Abort");
                jMenuItem1.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                jMenuItem1ActionPerformed(evt);
                        }
                });
                jPopupMenu1.add(jMenuItem1);

                setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

                jScrollPane1.setViewportView(jTree1);

                jSplitPane1.setLeftComponent(jScrollPane1);

                jTextArea1.setColumns(20);
                jTextArea1.setEditable(false);
                jTextArea1.setLineWrap(true);
                jTextArea1.setRows(5);
                jScrollPane2.setViewportView(jTextArea1);

                jSplitPane1.setRightComponent(jScrollPane2);

                jTabbedPane1.addTab("System", jSplitPane1);

                jTable2.setModel(new javax.swing.table.DefaultTableModel(
                        new Object [][] {
                                {null, null},
                                {null, null},
                                {null, null},
                                {null, null}
                        },
                        new String [] {
                                "Title 1", "Title 2"
                        }
                ));
                jTable2.addComponentListener(new java.awt.event.ComponentAdapter() {
                        public void componentShown(java.awt.event.ComponentEvent evt) {
                                jTable2ComponentShown(evt);
                        }
                });
                jScrollPane4.setViewportView(jTable2);
                jTable2.getColumnModel().getSelectionModel().setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);

                jTabbedPane1.addTab("Transactions", jScrollPane4);

                jLabel4.setText("Username:");

                jLabel5.setText("Password:");

                jLabel6.setText("Confirm password:");

                jButton6.setText("Add user");
                jButton6.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                jButton6ActionPerformed(evt);
                        }
                });

                jButton7.setText("Change password");
                jButton7.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                jButton7ActionPerformed(evt);
                        }
                });

                jButton8.setText("Delete user");
                jButton8.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                jButton8ActionPerformed(evt);
                        }
                });

                jCheckBox3.setText("Confirm");

                jLabel11.setText("Old password:");

                jCheckBox6.setText("Enable");

                org.jdesktop.layout.GroupLayout jPanel2Layout = new org.jdesktop.layout.GroupLayout(jPanel2);
                jPanel2.setLayout(jPanel2Layout);
                jPanel2Layout.setHorizontalGroup(
                        jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                        .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel2Layout.createSequentialGroup()
                                .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                                        .add(jPanel2Layout.createSequentialGroup()
                                                .addContainerGap()
                                                .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                                                        .add(jLabel6)
                                                        .add(jLabel5)
                                                        .add(jLabel4)
                                                        .add(jLabel11))
                                                .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                                                        .add(jPanel2Layout.createSequentialGroup()
                                                                .add(12, 12, 12)
                                                                .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                                                                        .add(jTextField3, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 200, Short.MAX_VALUE)
                                                                        .add(jPasswordField1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 200, Short.MAX_VALUE)))
                                                        .add(jPanel2Layout.createSequentialGroup()
                                                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                                                .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                                                                        .add(jPasswordField3, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 200, Short.MAX_VALUE)
                                                                        .add(jPasswordField2, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 200, Short.MAX_VALUE)))))
                                        .add(org.jdesktop.layout.GroupLayout.LEADING, jPanel2Layout.createSequentialGroup()
                                                .add(142, 142, 142)
                                                .add(jButton6)
                                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                                .add(jButton7)))
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                                        .add(jPanel2Layout.createSequentialGroup()
                                                .add(jButton8)
                                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                                .add(jCheckBox3))
                                        .add(jCheckBox6))
                                .add(52, 52, 52))
                );
                jPanel2Layout.setVerticalGroup(
                        jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                        .add(jPanel2Layout.createSequentialGroup()
                                .addContainerGap()
                                .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                                        .add(jPanel2Layout.createSequentialGroup()
                                                .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                                                        .add(jLabel4)
                                                        .add(jTextField3, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                                .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                                                        .add(jLabel5)
                                                        .add(jPasswordField1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                                                .add(8, 8, 8)
                                                .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                                                        .add(jLabel6)
                                                        .add(jPasswordField2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                                .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                                                        .add(jLabel11)
                                                        .add(jPasswordField3, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
                                        .add(jCheckBox6))
                                .add(8, 8, 8)
                                .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                                        .add(jButton6)
                                        .add(jButton7)
                                        .add(jButton8)
                                        .add(jCheckBox3))
                                .addContainerGap(189, Short.MAX_VALUE))
                );

                jTabbedPane1.addTab("Users", jPanel2);

                jLabel3.setText("Filename:");

                jTextField2.setText("data.xml");

                jButton4.setText("Export");
                jButton4.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                jButton4ActionPerformed(evt);
                        }
                });

                jButton5.setText("Import");
                jButton5.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                jButton5ActionPerformed(evt);
                        }
                });

                jCheckBox4.setText("Overwrite");

                jCheckBox5.setText("Confirm");

                org.jdesktop.layout.GroupLayout jPanel1Layout = new org.jdesktop.layout.GroupLayout(jPanel1);
                jPanel1.setLayout(jPanel1Layout);
                jPanel1Layout.setHorizontalGroup(
                        jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                        .add(jPanel1Layout.createSequentialGroup()
                                .addContainerGap()
                                .add(jLabel3)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                                        .add(jPanel1Layout.createSequentialGroup()
                                                .add(jButton4)
                                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                                .add(jCheckBox4)
                                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                                .add(jButton5)
                                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                                .add(jCheckBox5))
                                        .add(jTextField2, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 468, Short.MAX_VALUE))
                                .addContainerGap())
                );
                jPanel1Layout.setVerticalGroup(
                        jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                        .add(jPanel1Layout.createSequentialGroup()
                                .addContainerGap()
                                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                                        .add(jLabel3)
                                        .add(jTextField2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                                .add(18, 18, 18)
                                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                                        .add(jButton4)
                                        .add(jCheckBox4)
                                        .add(jButton5)
                                        .add(jCheckBox5))
                                .addContainerGap(280, Short.MAX_VALUE))
                );

                jTabbedPane1.addTab("Export/Import", jPanel1);

                jPanel3.addComponentListener(new java.awt.event.ComponentAdapter() {
                        public void componentShown(java.awt.event.ComponentEvent evt) {
                                jPanel3ComponentShown(evt);
                        }
                });

                jButton10.setText("Gc");
                jButton10.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                jButton10ActionPerformed(evt);
                        }
                });

                jProgressBar1.setStringPainted(true);

                org.jdesktop.layout.GroupLayout jPanel3Layout = new org.jdesktop.layout.GroupLayout(jPanel3);
                jPanel3.setLayout(jPanel3Layout);
                jPanel3Layout.setHorizontalGroup(
                        jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                        .add(jPanel3Layout.createSequentialGroup()
                                .addContainerGap()
                                .add(jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                                        .add(jProgressBar1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 542, Short.MAX_VALUE)
                                        .add(jButton10))
                                .addContainerGap())
                );
                jPanel3Layout.setVerticalGroup(
                        jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                        .add(jPanel3Layout.createSequentialGroup()
                                .addContainerGap()
                                .add(jProgressBar1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                .add(18, 18, 18)
                                .add(jButton10)
                                .addContainerGap(284, Short.MAX_VALUE))
                );

                jTabbedPane1.addTab("Memory", jPanel3);

                jButton11.setText("Shutdown");
                jButton11.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                jButton11ActionPerformed(evt);
                        }
                });

                jCheckBox2.setText("Confirm");

                org.jdesktop.layout.GroupLayout jPanel4Layout = new org.jdesktop.layout.GroupLayout(jPanel4);
                jPanel4.setLayout(jPanel4Layout);
                jPanel4Layout.setHorizontalGroup(
                        jPanel4Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                        .add(jPanel4Layout.createSequentialGroup()
                                .addContainerGap()
                                .add(jButton11)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(jCheckBox2)
                                .addContainerGap(399, Short.MAX_VALUE))
                );
                jPanel4Layout.setVerticalGroup(
                        jPanel4Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                        .add(jPanel4Layout.createSequentialGroup()
                                .addContainerGap()
                                .add(jPanel4Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                                        .add(jButton11)
                                        .add(jCheckBox2))
                                .addContainerGap(325, Short.MAX_VALUE))
                );

                jTabbedPane1.addTab("Shutdown", jPanel4);
                jTabbedPane1.addTab("Console", jConsole1);

                fileMenu.setText("Connection");

                openMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_O, java.awt.event.InputEvent.CTRL_MASK));
                openMenuItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/toolbarButtonGraphics/development/Server16.gif"))); // NOI18N
                openMenuItem.setText("Open");
                openMenuItem.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                openMenuItemActionPerformed(evt);
                        }
                });
                fileMenu.add(openMenuItem);

                closeMenuItem.setText("Close");
                closeMenuItem.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                closeMenuItemActionPerformed(evt);
                        }
                });
                fileMenu.add(closeMenuItem);

                exitMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_W, java.awt.event.InputEvent.CTRL_MASK));
                exitMenuItem.setText("Exit");
                exitMenuItem.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                exitMenuItemActionPerformed(evt);
                        }
                });
                fileMenu.add(exitMenuItem);

                menuBar.add(fileMenu);

                helpMenu.setText("Help");

                aboutMenuItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/toolbarButtonGraphics/general/About16.gif"))); // NOI18N
                aboutMenuItem.setText("About");
                aboutMenuItem.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                aboutMenuItemActionPerformed(evt);
                        }
                });
                helpMenu.add(aboutMenuItem);

                menuBar.add(helpMenu);

                setJMenuBar(menuBar);

                org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(getContentPane());
                getContentPane().setLayout(layout);
                layout.setHorizontalGroup(
                        layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                        .add(jTabbedPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 574, Short.MAX_VALUE)
                );
                layout.setVerticalGroup(
                        layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                        .add(jTabbedPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 403, Short.MAX_VALUE)
                );

                pack();
        }// </editor-fold>//GEN-END:initComponents

	private void exitMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exitMenuItemActionPerformed
		System.exit(0);
	}//GEN-LAST:event_exitMenuItemActionPerformed

	private void openMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_openMenuItemActionPerformed
		setDialogLocation(jDialog1);
		jDialog1.setVisible(true);
	}//GEN-LAST:event_openMenuItemActionPerformed

	private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
		jDialog1.setVisible(false);
		open();
	}//GEN-LAST:event_jButton1ActionPerformed

	private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
		jDialog1.setVisible(false);
	}//GEN-LAST:event_jButton2ActionPerformed

	private void aboutMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_aboutMenuItemActionPerformed
		setDialogLocation(jDialog2);
		jDialog2.setVisible(true);
	}//GEN-LAST:event_aboutMenuItemActionPerformed

	private void jButton3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton3ActionPerformed
		jDialog2.setVisible(false);
	}//GEN-LAST:event_jButton3ActionPerformed

	private void closeMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_closeMenuItemActionPerformed
		close();
	}//GEN-LAST:event_closeMenuItemActionPerformed

	private void jButton4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton4ActionPerformed
		export();
	}//GEN-LAST:event_jButton4ActionPerformed

	private void jButton5ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton5ActionPerformed
		inport();
	}//GEN-LAST:event_jButton5ActionPerformed

	private void jButton6ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton6ActionPerformed
		addUser();
	}//GEN-LAST:event_jButton6ActionPerformed

	private void jButton7ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton7ActionPerformed
		changePassword();
	}//GEN-LAST:event_jButton7ActionPerformed

	private void jButton8ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton8ActionPerformed
		deleteUser();
	}//GEN-LAST:event_jButton8ActionPerformed

	private void jButton10ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton10ActionPerformed
		gc();
	}//GEN-LAST:event_jButton10ActionPerformed

	private void jButton11ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton11ActionPerformed
		shutdown();
	}//GEN-LAST:event_jButton11ActionPerformed

	private void jPanel3ComponentShown(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_jPanel3ComponentShown
		refresh();
	}//GEN-LAST:event_jPanel3ComponentShown

	private void jTable2ComponentShown(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_jTable2ComponentShown
		refreshTransactions();
	}//GEN-LAST:event_jTable2ComponentShown

	private void jMenuItem1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem1ActionPerformed
		abort();
	}//GEN-LAST:event_jMenuItem1ActionPerformed

	/**
	 * @param args the command line arguments
	 */
	public static void main(String args[]) {
		java.awt.EventQueue.invokeLater(new Runnable() {
			public void run() {
				new AdminUI().setVisible(true);
			}
		});
	}

        // Variables declaration - do not modify//GEN-BEGIN:variables
        private javax.swing.JMenuItem aboutMenuItem;
        private javax.swing.JMenuItem closeMenuItem;
        private javax.swing.JMenuItem exitMenuItem;
        private javax.swing.JMenu fileMenu;
        private javax.swing.JMenu helpMenu;
        private javax.swing.JButton jButton1;
        private javax.swing.JButton jButton10;
        private javax.swing.JButton jButton11;
        private javax.swing.JButton jButton2;
        private javax.swing.JButton jButton3;
        private javax.swing.JButton jButton4;
        private javax.swing.JButton jButton5;
        private javax.swing.JButton jButton6;
        private javax.swing.JButton jButton7;
        private javax.swing.JButton jButton8;
        private javax.swing.JCheckBox jCheckBox1;
        private javax.swing.JCheckBox jCheckBox2;
        private javax.swing.JCheckBox jCheckBox3;
        private javax.swing.JCheckBox jCheckBox4;
        private javax.swing.JCheckBox jCheckBox5;
        private javax.swing.JCheckBox jCheckBox6;
        private bsh.util.JConsole jConsole1;
        private javax.swing.JDialog jDialog1;
        private javax.swing.JDialog jDialog2;
        private javax.swing.JLabel jLabel1;
        private javax.swing.JLabel jLabel11;
        private javax.swing.JLabel jLabel2;
        private javax.swing.JLabel jLabel3;
        private javax.swing.JLabel jLabel4;
        private javax.swing.JLabel jLabel5;
        private javax.swing.JLabel jLabel6;
        private javax.swing.JMenuItem jMenuItem1;
        private javax.swing.JPanel jPanel1;
        private javax.swing.JPanel jPanel2;
        private javax.swing.JPanel jPanel3;
        private javax.swing.JPanel jPanel4;
        private javax.swing.JPasswordField jPasswordField1;
        private javax.swing.JPasswordField jPasswordField2;
        private javax.swing.JPasswordField jPasswordField3;
        private javax.swing.JPopupMenu jPopupMenu1;
        private javax.swing.JProgressBar jProgressBar1;
        private javax.swing.JScrollPane jScrollPane1;
        private javax.swing.JScrollPane jScrollPane2;
        private javax.swing.JScrollPane jScrollPane3;
        private javax.swing.JScrollPane jScrollPane4;
        private javax.swing.JSplitPane jSplitPane1;
        private javax.swing.JTabbedPane jTabbedPane1;
        private javax.swing.JTable jTable1;
        private javax.swing.JTable jTable2;
        private javax.swing.JTextArea jTextArea1;
        private javax.swing.JTextField jTextField1;
        private javax.swing.JTextField jTextField2;
        private javax.swing.JTextField jTextField3;
        private javax.swing.JTree jTree1;
        private javax.swing.JMenuBar menuBar;
        private javax.swing.JMenuItem openMenuItem;
        // End of variables declaration//GEN-END:variables
}

class TransactionTableModel extends AbstractTableModel {
	List list;

	public TransactionTableModel(List list) {
		this.list=list;
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
