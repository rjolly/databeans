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
import java.util.Map;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.event.TreeWillExpandListener;
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
	ObjectTableModel userModel;
	ObjectTableModel classModel;
	Interpreter interpreter;

	/** Creates new form AdminUI */
	public AdminUI() {
		initComponents();
		jDialog1.pack();
		jDialog2.pack();
		jDialog3.pack();
		jDialog4.pack();
		jDialog5.pack();
		jDialog6.pack();
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
						if(n>-1) jTable2.setRowSelectionInterval(n,n);
					}
					transactionPopupMenu.show(e.getComponent(),
					e.getX(), e.getY());
				}
			}
		});
		jTable3.addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				maybeShowPopup(e);
			}

			public void mouseReleased(MouseEvent e) {
				maybeShowPopup(e);
			}

			private void maybeShowPopup(MouseEvent e) {
				if (e.isPopupTrigger()) {
					int n=jTable3.rowAtPoint(e.getPoint());
					jTable3.setRowSelectionInterval(n,n);
					userPopupMenu.show(e.getComponent(),
					e.getX(), e.getY());
				}
			}
		});
		jTable4.addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				maybeShowPopup(e);
			}

			public void mouseReleased(MouseEvent e) {
				maybeShowPopup(e);
			}

			private void maybeShowPopup(MouseEvent e) {
				if (e.isPopupTrigger()) {
					int n=jTable4.rowAtPoint(e.getPoint());
					jTable4.setRowSelectionInterval(n,n);
					classPopupMenu.show(e.getComponent(),
					e.getX(), e.getY());
				}
			}
		});
		interpreter = new Interpreter( jConsole1 );
		new Thread( interpreter ).start(); // start a thread to call the run() method
		enableTabs(new boolean[] {false,false,false,false,false,false,false,true});
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
			setRoot(system.root());
			if(admin) {
				setTransactions(system.getTransactions());
				setUsers(system.getUsers());
				setClasses(system.getClasses());
			}
		} catch (Exception e) {
			error(e);
		}
		try {
			interpreter.set("conn",conn);
		} catch (bsh.EvalError e) {
			error(e);
		}
		enableTabs(new boolean[] {true,admin,admin,admin,admin,admin,admin,true});
		jDialog1.setVisible(false);
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
		enableTabs(new boolean[] {false,false,false,false,false,false,false,true});
	}

	void setRoot(Object root) {
		model.setRoot(ObjectTreeNode.node(root,"root"));
		model.reload();
		jTree1.setSelectionRow(0);
	}

	void setTransactions(List list) {
		transactionModel=new TransactionTableModel(list);
		jTable2.setModel(transactionModel);
	}

	void refreshTransactions() {
		transactionModel.reload();
	}

	void abortTransaction() {
		AdminConnection conn=(AdminConnection)this.conn;
		int row[]=jTable2.getSelectedRows();
		try {
			for(int i=0;i<row.length;i++) {
				int n=row[i];
				if(n>-1) conn.abortTransaction((Transaction)jTable2.getValueAt(n,0));
			}
			refreshTransactions();
		} catch (Exception e) {
			error(e);
		}
	}

	void setUsers(Map map) {
		userModel=ObjectTableModel.model(map);
		jTable3.setModel(userModel);
	}

	void refreshUsers() {
		userModel.reload();
	}

	String user() {
		int n=jTable3.getSelectedRow();
		return (String)jTable3.getValueAt(n, 0);
	}

	void setClasses(Map map) {
		classModel=ObjectTableModel.model(map);
		jTable4.setModel(classModel);
	}

	void refreshClasses() {
		classModel.reload();
	}

	String fields() {
		int n=jTable4.getSelectedRow();
		return jTable4.getValueAt(n, 1).toString();
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
		String name=jTextField5.getText();
		char pwd[]=jPasswordField6.getPassword();
		char conf[]=jPasswordField7.getPassword();
		jPasswordField6.setText("");
		jPasswordField7.setText("");
		try {
			if(!Arrays.equals(pwd, conf)) throw new Exception("passwords don't match");
			conn.addUser(name, String.valueOf(pwd));
			refreshUsers();
			jDialog3.setVisible(false);
		} catch (Exception e) {
			error(e);
		}
	}

	void deleteUser() {
		AdminConnection conn=(AdminConnection)this.conn;
		String name=jTextField6.getText();
		try {
			conn.deleteUser(name);
			refreshUsers();
			jDialog5.setVisible(false);
		} catch (Exception e) {
			error(e);
		}
	}

	void changePassword() {
		AdminConnection conn=(AdminConnection)this.conn;
		String name=jTextField4.getText();
		char pwd[]=jPasswordField3.getPassword();
		char conf[]=jPasswordField4.getPassword();
		char old[]=jPasswordField5.getPassword();
		jPasswordField3.setText("");
		jPasswordField4.setText("");
		jPasswordField5.setText("");
		boolean enable=jCheckBox3.isSelected();
		try {
			if(!Arrays.equals(pwd, conf)) throw new Exception("passwords don't match");
			if(enable) conn.changePassword(name, String.valueOf(old), String.valueOf(pwd));
			else conn.changePassword(name, String.valueOf(pwd));
			refreshUsers();
			jDialog4.setVisible(false);
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
                transactionPopupMenu = new javax.swing.JPopupMenu();
                abortTransactionMenuItem = new javax.swing.JMenuItem();
                jDialog3 = new javax.swing.JDialog();
                jPanel6 = new javax.swing.JPanel();
                jLabel11 = new javax.swing.JLabel();
                jTextField5 = new javax.swing.JTextField();
                jLabel12 = new javax.swing.JLabel();
                jLabel13 = new javax.swing.JLabel();
                jButton12 = new javax.swing.JButton();
                jPasswordField6 = new javax.swing.JPasswordField();
                jPasswordField7 = new javax.swing.JPasswordField();
                jButton13 = new javax.swing.JButton();
                userPopupMenu = new javax.swing.JPopupMenu();
                addUserMenuItem = new javax.swing.JMenuItem();
                deleteUserMenuItem = new javax.swing.JMenuItem();
                changePasswordMenuItem = new javax.swing.JMenuItem();
                jDialog4 = new javax.swing.JDialog();
                jPanel5 = new javax.swing.JPanel();
                jLabel7 = new javax.swing.JLabel();
                jTextField4 = new javax.swing.JTextField();
                jLabel8 = new javax.swing.JLabel();
                jLabel9 = new javax.swing.JLabel();
                jButton8 = new javax.swing.JButton();
                jPasswordField3 = new javax.swing.JPasswordField();
                jPasswordField4 = new javax.swing.JPasswordField();
                jButton9 = new javax.swing.JButton();
                jLabel10 = new javax.swing.JLabel();
                jCheckBox3 = new javax.swing.JCheckBox();
                jPasswordField5 = new javax.swing.JPasswordField();
                jDialog5 = new javax.swing.JDialog();
                jLabel14 = new javax.swing.JLabel();
                jTextField6 = new javax.swing.JTextField();
                jButton14 = new javax.swing.JButton();
                jButton15 = new javax.swing.JButton();
                classPopupMenu = new javax.swing.JPopupMenu();
                displayClassMenuItem = new javax.swing.JMenuItem();
                jDialog6 = new javax.swing.JDialog();
                jScrollPane7 = new javax.swing.JScrollPane();
                jTextArea2 = new javax.swing.JTextArea();
                jTabbedPane1 = new javax.swing.JTabbedPane();
                jSplitPane1 = new javax.swing.JSplitPane();
                jScrollPane1 = new javax.swing.JScrollPane();
                jTree1 = new javax.swing.JTree();
                jScrollPane2 = new javax.swing.JScrollPane();
                jTextArea1 = new javax.swing.JTextArea();
                jScrollPane4 = new javax.swing.JScrollPane();
                jTable2 = new javax.swing.JTable();
                jScrollPane6 = new javax.swing.JScrollPane();
                jTable4 = new javax.swing.JTable();
                jScrollPane5 = new javax.swing.JScrollPane();
                jTable3 = new javax.swing.JTable();
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

                jLabel2.setText("<html>databeans : a new, fully object oriented persistence framework for java<br/>Copyright (C) 2007-2008 Databeans<br/><br/>Version 2.0rc17</html>");

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
                                .add(jDialog2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                                        .add(jLabel2)
                                        .add(jButton3))
                                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
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

                abortTransactionMenuItem.setText("Abort");
                abortTransactionMenuItem.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                abortTransactionMenuItemActionPerformed(evt);
                        }
                });
                transactionPopupMenu.add(abortTransactionMenuItem);

                jLabel11.setText("Username:");

                jLabel12.setText("Password:");

                jLabel13.setText("Confirm password:");

                jButton12.setText("Add");
                jButton12.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                jButton12ActionPerformed(evt);
                        }
                });

                jButton13.setText("Cancel");
                jButton13.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                jButton13ActionPerformed(evt);
                        }
                });

                org.jdesktop.layout.GroupLayout jPanel6Layout = new org.jdesktop.layout.GroupLayout(jPanel6);
                jPanel6.setLayout(jPanel6Layout);
                jPanel6Layout.setHorizontalGroup(
                        jPanel6Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                        .add(jPanel6Layout.createSequentialGroup()
                                .addContainerGap()
                                .add(jPanel6Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                                        .add(jPanel6Layout.createSequentialGroup()
                                                .add(jPanel6Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                                                        .add(jLabel13)
                                                        .add(jLabel12)
                                                        .add(jLabel11))
                                                .add(12, 12, 12)
                                                .add(jPanel6Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                                                        .add(jPasswordField6, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 213, Short.MAX_VALUE)
                                                        .add(jPasswordField7, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 213, Short.MAX_VALUE)
                                                        .add(jTextField5, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 213, Short.MAX_VALUE)))
                                        .add(jPanel6Layout.createSequentialGroup()
                                                .add(jButton12)
                                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                                .add(jButton13)))
                                .addContainerGap())
                );
                jPanel6Layout.setVerticalGroup(
                        jPanel6Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                        .add(jPanel6Layout.createSequentialGroup()
                                .addContainerGap()
                                .add(jPanel6Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                                        .add(jLabel11)
                                        .add(jTextField5, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(jPanel6Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                                        .add(jLabel12)
                                        .add(jPasswordField6, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                                .add(8, 8, 8)
                                .add(jPanel6Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                                        .add(jLabel13)
                                        .add(jPasswordField7, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                                .add(18, 18, 18)
                                .add(jPanel6Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                                        .add(jButton12)
                                        .add(jButton13))
                                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                );

                org.jdesktop.layout.GroupLayout jDialog3Layout = new org.jdesktop.layout.GroupLayout(jDialog3.getContentPane());
                jDialog3.getContentPane().setLayout(jDialog3Layout);
                jDialog3Layout.setHorizontalGroup(
                        jDialog3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                        .add(0, 367, Short.MAX_VALUE)
                        .add(0, 367, Short.MAX_VALUE)
                        .add(0, 367, Short.MAX_VALUE)
                        .add(0, 367, Short.MAX_VALUE)
                        .add(jDialog3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                                .add(jDialog3Layout.createSequentialGroup()
                                        .add(0, 0, Short.MAX_VALUE)
                                        .add(jPanel6, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                        .add(0, 0, Short.MAX_VALUE)))
                );
                jDialog3Layout.setVerticalGroup(
                        jDialog3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                        .add(0, 166, Short.MAX_VALUE)
                        .add(0, 166, Short.MAX_VALUE)
                        .add(0, 166, Short.MAX_VALUE)
                        .add(0, 166, Short.MAX_VALUE)
                        .add(jDialog3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                                .add(jDialog3Layout.createSequentialGroup()
                                        .add(0, 0, Short.MAX_VALUE)
                                        .add(jPanel6, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                        .add(0, 0, Short.MAX_VALUE)))
                );

                addUserMenuItem.setText("Add");
                addUserMenuItem.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                addUserMenuItemActionPerformed(evt);
                        }
                });
                userPopupMenu.add(addUserMenuItem);

                deleteUserMenuItem.setText("Delete");
                deleteUserMenuItem.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                deleteUserMenuItemActionPerformed(evt);
                        }
                });
                userPopupMenu.add(deleteUserMenuItem);

                changePasswordMenuItem.setText("Change password");
                changePasswordMenuItem.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                changePasswordMenuItemActionPerformed(evt);
                        }
                });
                userPopupMenu.add(changePasswordMenuItem);

                jLabel7.setText("Username:");

                jTextField4.setEditable(false);

                jLabel8.setText("Password:");

                jLabel9.setText("Confirm password:");

                jButton8.setText("Change password");
                jButton8.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                jButton8ActionPerformed(evt);
                        }
                });

                jButton9.setText("Cancel");
                jButton9.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                jButton9ActionPerformed(evt);
                        }
                });

                jLabel10.setText("Old password:");

                jCheckBox3.setText("Enable");

                org.jdesktop.layout.GroupLayout jPanel5Layout = new org.jdesktop.layout.GroupLayout(jPanel5);
                jPanel5.setLayout(jPanel5Layout);
                jPanel5Layout.setHorizontalGroup(
                        jPanel5Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                        .add(jPanel5Layout.createSequentialGroup()
                                .addContainerGap()
                                .add(jPanel5Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                                        .add(jPanel5Layout.createSequentialGroup()
                                                .add(jPanel5Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                                                        .add(jLabel9)
                                                        .add(jLabel8)
                                                        .add(jLabel7)
                                                        .add(jLabel10))
                                                .add(12, 12, 12)
                                                .add(jPanel5Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING, false)
                                                        .add(jTextField4)
                                                        .add(jPasswordField3)
                                                        .add(jPasswordField4)
                                                        .add(jPasswordField5, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 175, Short.MAX_VALUE))
                                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                                .add(jCheckBox3))
                                        .add(jPanel5Layout.createSequentialGroup()
                                                .add(jButton8)
                                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                                .add(jButton9)))
                                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                );
                jPanel5Layout.setVerticalGroup(
                        jPanel5Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                        .add(jPanel5Layout.createSequentialGroup()
                                .addContainerGap()
                                .add(jPanel5Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                                        .add(jLabel7)
                                        .add(jTextField4, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(jPanel5Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                                        .add(jLabel8)
                                        .add(jPasswordField3, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                                .add(8, 8, 8)
                                .add(jPanel5Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                                        .add(jLabel9)
                                        .add(jPasswordField4, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(jPanel5Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                                        .add(jLabel10)
                                        .add(jCheckBox3)
                                        .add(jPasswordField5, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                                .add(18, 18, 18)
                                .add(jPanel5Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                                        .add(jButton8)
                                        .add(jButton9))
                                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                );

                org.jdesktop.layout.GroupLayout jDialog4Layout = new org.jdesktop.layout.GroupLayout(jDialog4.getContentPane());
                jDialog4.getContentPane().setLayout(jDialog4Layout);
                jDialog4Layout.setHorizontalGroup(
                        jDialog4Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                        .add(0, 402, Short.MAX_VALUE)
                        .add(0, 402, Short.MAX_VALUE)
                        .add(0, 402, Short.MAX_VALUE)
                        .add(jDialog4Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                                .add(jDialog4Layout.createSequentialGroup()
                                        .add(0, 0, Short.MAX_VALUE)
                                        .add(jPanel5, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                        .add(0, 0, Short.MAX_VALUE)))
                );
                jDialog4Layout.setVerticalGroup(
                        jDialog4Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                        .add(0, 199, Short.MAX_VALUE)
                        .add(0, 199, Short.MAX_VALUE)
                        .add(0, 199, Short.MAX_VALUE)
                        .add(jDialog4Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                                .add(jDialog4Layout.createSequentialGroup()
                                        .add(0, 0, Short.MAX_VALUE)
                                        .add(jPanel5, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                        .add(0, 0, Short.MAX_VALUE)))
                );

                jLabel14.setText("Username:");

                jTextField6.setEditable(false);

                jButton14.setText("Delete");
                jButton14.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                jButton14ActionPerformed(evt);
                        }
                });

                jButton15.setText("Cancel");
                jButton15.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                jButton15ActionPerformed(evt);
                        }
                });

                org.jdesktop.layout.GroupLayout jDialog5Layout = new org.jdesktop.layout.GroupLayout(jDialog5.getContentPane());
                jDialog5.getContentPane().setLayout(jDialog5Layout);
                jDialog5Layout.setHorizontalGroup(
                        jDialog5Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                        .add(jDialog5Layout.createSequentialGroup()
                                .addContainerGap()
                                .add(jDialog5Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                                        .add(jDialog5Layout.createSequentialGroup()
                                                .add(jLabel14)
                                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                                .add(jTextField6, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 260, Short.MAX_VALUE))
                                        .add(jDialog5Layout.createSequentialGroup()
                                                .add(jButton14)
                                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                                .add(jButton15)))
                                .addContainerGap())
                );
                jDialog5Layout.setVerticalGroup(
                        jDialog5Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                        .add(jDialog5Layout.createSequentialGroup()
                                .addContainerGap()
                                .add(jDialog5Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                                        .add(jLabel14)
                                        .add(jTextField6, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                                .add(18, 18, 18)
                                .add(jDialog5Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                                        .add(jButton14)
                                        .add(jButton15))
                                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                );

                displayClassMenuItem.setText("Display");
                displayClassMenuItem.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                displayClassMenuItemActionPerformed(evt);
                        }
                });
                classPopupMenu.add(displayClassMenuItem);

                jTextArea2.setColumns(20);
                jTextArea2.setEditable(false);
                jTextArea2.setLineWrap(true);
                jTextArea2.setRows(5);
                jScrollPane7.setViewportView(jTextArea2);

                org.jdesktop.layout.GroupLayout jDialog6Layout = new org.jdesktop.layout.GroupLayout(jDialog6.getContentPane());
                jDialog6.getContentPane().setLayout(jDialog6Layout);
                jDialog6Layout.setHorizontalGroup(
                        jDialog6Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                        .add(jScrollPane7, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 363, Short.MAX_VALUE)
                );
                jDialog6Layout.setVerticalGroup(
                        jDialog6Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                        .add(jScrollPane7, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 214, Short.MAX_VALUE)
                );

                setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

                jScrollPane1.setViewportView(jTree1);

                jSplitPane1.setLeftComponent(jScrollPane1);

                jTextArea1.setColumns(20);
                jTextArea1.setEditable(false);
                jTextArea1.setLineWrap(true);
                jTextArea1.setRows(5);
                jScrollPane2.setViewportView(jTextArea1);

                jSplitPane1.setRightComponent(jScrollPane2);

                jTabbedPane1.addTab("Root", jSplitPane1);

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

                jTable4.setModel(new javax.swing.table.DefaultTableModel(
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
                jTable4.addComponentListener(new java.awt.event.ComponentAdapter() {
                        public void componentShown(java.awt.event.ComponentEvent evt) {
                                jTable4ComponentShown(evt);
                        }
                });
                jScrollPane6.setViewportView(jTable4);

                jTabbedPane1.addTab("Classes", jScrollPane6);

                jTable3.setModel(new javax.swing.table.DefaultTableModel(
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
                jTable3.addComponentListener(new java.awt.event.ComponentAdapter() {
                        public void componentShown(java.awt.event.ComponentEvent evt) {
                                jTable3ComponentShown(evt);
                        }
                });
                jScrollPane5.setViewportView(jTable3);

                jTabbedPane1.addTab("Users", jScrollPane5);

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
                                        .add(jTextField2, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 524, Short.MAX_VALUE))
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
                                        .add(jProgressBar1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 598, Short.MAX_VALUE)
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
                                .addContainerGap(455, Short.MAX_VALUE))
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
                        .add(jTabbedPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 630, Short.MAX_VALUE)
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

	private void abortTransactionMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_abortTransactionMenuItemActionPerformed
		abortTransaction();
}//GEN-LAST:event_abortTransactionMenuItemActionPerformed

	private void addUserMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addUserMenuItemActionPerformed
		setDialogLocation(jDialog3);
		jDialog3.setVisible(true);
	}//GEN-LAST:event_addUserMenuItemActionPerformed

	private void jButton8ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton8ActionPerformed
		changePassword();
	}//GEN-LAST:event_jButton8ActionPerformed

	private void jButton12ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton12ActionPerformed
		addUser();
	}//GEN-LAST:event_jButton12ActionPerformed

	private void jButton14ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton14ActionPerformed
		deleteUser();
	}//GEN-LAST:event_jButton14ActionPerformed

	private void jButton13ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton13ActionPerformed
		jDialog3.setVisible(false);
	}//GEN-LAST:event_jButton13ActionPerformed

	private void changePasswordMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_changePasswordMenuItemActionPerformed
		setDialogLocation(jDialog4);
		jDialog4.setVisible(true);
		jTextField4.setText(user());
	}//GEN-LAST:event_changePasswordMenuItemActionPerformed

	private void jButton9ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton9ActionPerformed
		jDialog4.setVisible(false);
	}//GEN-LAST:event_jButton9ActionPerformed

	private void deleteUserMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_deleteUserMenuItemActionPerformed
		setDialogLocation(jDialog5);
		jDialog5.setVisible(true);
		jTextField6.setText(user());
	}//GEN-LAST:event_deleteUserMenuItemActionPerformed

	private void jButton15ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton15ActionPerformed
		jDialog5.setVisible(false);
	}//GEN-LAST:event_jButton15ActionPerformed

	private void jTable3ComponentShown(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_jTable3ComponentShown
		refreshUsers();
	}//GEN-LAST:event_jTable3ComponentShown

	private void jTable4ComponentShown(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_jTable4ComponentShown
		refreshClasses();
	}//GEN-LAST:event_jTable4ComponentShown

	private void displayClassMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_displayClassMenuItemActionPerformed
		setDialogLocation(jDialog6);
		jDialog6.setVisible(true);
		jTextArea2.setText(fields());
	}//GEN-LAST:event_displayClassMenuItemActionPerformed

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
        private javax.swing.JMenuItem abortTransactionMenuItem;
        private javax.swing.JMenuItem aboutMenuItem;
        private javax.swing.JMenuItem addUserMenuItem;
        private javax.swing.JMenuItem changePasswordMenuItem;
        private javax.swing.JPopupMenu classPopupMenu;
        private javax.swing.JMenuItem closeMenuItem;
        private javax.swing.JMenuItem deleteUserMenuItem;
        private javax.swing.JMenuItem displayClassMenuItem;
        private javax.swing.JMenuItem exitMenuItem;
        private javax.swing.JMenu fileMenu;
        private javax.swing.JMenu helpMenu;
        private javax.swing.JButton jButton1;
        private javax.swing.JButton jButton10;
        private javax.swing.JButton jButton11;
        private javax.swing.JButton jButton12;
        private javax.swing.JButton jButton13;
        private javax.swing.JButton jButton14;
        private javax.swing.JButton jButton15;
        private javax.swing.JButton jButton2;
        private javax.swing.JButton jButton3;
        private javax.swing.JButton jButton4;
        private javax.swing.JButton jButton5;
        private javax.swing.JButton jButton8;
        private javax.swing.JButton jButton9;
        private javax.swing.JCheckBox jCheckBox1;
        private javax.swing.JCheckBox jCheckBox2;
        private javax.swing.JCheckBox jCheckBox3;
        private javax.swing.JCheckBox jCheckBox4;
        private javax.swing.JCheckBox jCheckBox5;
        private bsh.util.JConsole jConsole1;
        private javax.swing.JDialog jDialog1;
        private javax.swing.JDialog jDialog2;
        private javax.swing.JDialog jDialog3;
        private javax.swing.JDialog jDialog4;
        private javax.swing.JDialog jDialog5;
        private javax.swing.JDialog jDialog6;
        private javax.swing.JLabel jLabel1;
        private javax.swing.JLabel jLabel10;
        private javax.swing.JLabel jLabel11;
        private javax.swing.JLabel jLabel12;
        private javax.swing.JLabel jLabel13;
        private javax.swing.JLabel jLabel14;
        private javax.swing.JLabel jLabel2;
        private javax.swing.JLabel jLabel3;
        private javax.swing.JLabel jLabel7;
        private javax.swing.JLabel jLabel8;
        private javax.swing.JLabel jLabel9;
        private javax.swing.JPanel jPanel1;
        private javax.swing.JPanel jPanel3;
        private javax.swing.JPanel jPanel4;
        private javax.swing.JPanel jPanel5;
        private javax.swing.JPanel jPanel6;
        private javax.swing.JPasswordField jPasswordField3;
        private javax.swing.JPasswordField jPasswordField4;
        private javax.swing.JPasswordField jPasswordField5;
        private javax.swing.JPasswordField jPasswordField6;
        private javax.swing.JPasswordField jPasswordField7;
        private javax.swing.JProgressBar jProgressBar1;
        private javax.swing.JScrollPane jScrollPane1;
        private javax.swing.JScrollPane jScrollPane2;
        private javax.swing.JScrollPane jScrollPane3;
        private javax.swing.JScrollPane jScrollPane4;
        private javax.swing.JScrollPane jScrollPane5;
        private javax.swing.JScrollPane jScrollPane6;
        private javax.swing.JScrollPane jScrollPane7;
        private javax.swing.JSplitPane jSplitPane1;
        private javax.swing.JTabbedPane jTabbedPane1;
        private javax.swing.JTable jTable1;
        private javax.swing.JTable jTable2;
        private javax.swing.JTable jTable3;
        private javax.swing.JTable jTable4;
        private javax.swing.JTextArea jTextArea1;
        private javax.swing.JTextArea jTextArea2;
        private javax.swing.JTextField jTextField1;
        private javax.swing.JTextField jTextField2;
        private javax.swing.JTextField jTextField4;
        private javax.swing.JTextField jTextField5;
        private javax.swing.JTextField jTextField6;
        private javax.swing.JTree jTree1;
        private javax.swing.JMenuBar menuBar;
        private javax.swing.JMenuItem openMenuItem;
        private javax.swing.JPopupMenu transactionPopupMenu;
        private javax.swing.JPopupMenu userPopupMenu;
        // End of variables declaration//GEN-END:variables
}
