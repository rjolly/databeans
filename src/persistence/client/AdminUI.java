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
import java.io.File;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import persistence.AdminConnection;
import persistence.Connection;
import persistence.Connections;
import persistence.Password;
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
	Interpreter interpreter;
	Transaction transaction;
	String user;
	Timer timer;

	/** Creates new form AdminUI */
	public AdminUI() {
		initComponents();
		jDialog1.pack();
		jDialog2.pack();
		jDialog3.pack();
		jDialog4.pack();
		jTree1.setModel(model);
		jTree1.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		interpreter = new Interpreter( jConsole1 );
		new Thread( interpreter ).start(); // start a thread to call the run() method
		enableTabs(new boolean[] {false,false,false,false,true});
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

	void refreshLevel() {
		boolean admin=jCheckBox1.isSelected();
		jComboBox1.setEnabled(!admin);
		jComboBox1.setSelectedIndex(admin?0:1);
	}

	void open1() throws Exception {
		boolean admin=jCheckBox1.isSelected();
		String location=jTextField1.getText();
		int level=jComboBox1.getSelectedIndex();
		conn=admin?Connections.getAdminConnection(location):Connections.getConnection(location,level);
	}

	void open2() {
		boolean admin=jCheckBox1.isSelected();
		try {
			setSystem(conn.system());
		} catch (Exception e) {
			error(e);
		}
		enableTabs(new boolean[] {true,admin,admin,admin,true});
		try {
			interpreter.set("conn",conn);
		} catch (bsh.EvalError e) {
			error(e);
		}
	}

	void close() {
		try {
			interpreter.set("conn",null);
		} catch (bsh.EvalError e) {
			error(e);
		}
		enableTabs(new boolean[] {false,false,false,false,true});
		try {
			conn.close();
		} catch (Exception e) {
			error(e);
		}
	}

	void select() {
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

	void reload(TreePath path) {
		model.reload((TreeNode)path.getLastPathComponent());
		if(path.getPathCount()==1) jTree1.setSelectionRow(0);
	}

	void showPopup(Point point, Component component, int x, int y) {
		if(!(conn instanceof AdminConnection)) return;
		int n=jTable1.rowAtPoint(point);
		if(n<0) return;
		jTable1.setRowSelectionInterval(n,n);
		Object obj=jTable1.getValueAt(n, 1);
		if(obj instanceof Transaction) jPopupMenu1.show(component,x,y);
		else if(obj instanceof Password) jPopupMenu2.show(component,x,y);
	}

	void setSystem(PersistentSystem system) {
		model.setRoot(ObjectTreeNode.node(system,"system"));
		model.reload();
		jTree1.setSelectionRow(0);
	}

	void refreshTable() {
		((ObjectTableModel)jTable1.getModel()).reload();
	}

	void setTransaction() {
		int n=jTable1.getSelectedRow();
		transaction=(Transaction)jTable1.getValueAt(n, 1);
	}

	void willAbortTransaction() {
		setTransaction();
		jTextField4.setText(transaction.toString());
	}

	void abortTransaction() {
		try {
			((AdminConnection)conn).abortTransaction(transaction);
			refreshTable();
			jDialog4.setVisible(false);
		} catch (Exception e) {
			error(e);
		}
	}

	void setUser() {
		int n=jTable1.getSelectedRow();
		user=(String)jTable1.getValueAt(n, 0);
	}

	void willAddUser() {
		jTextField3.setEnabled(true);
		jPasswordField1.setEnabled(true);
		jPasswordField2.setEnabled(true);
		jPasswordField3.setEnabled(false);
		jCheckBox3.setEnabled(false);
		jButton6.setActionCommand("Add user");
		jDialog3.validate();
	}

	void willDeleteUser() {
		setUser();
		jTextField3.setText(user);
		jTextField3.setEnabled(false);
		jPasswordField1.setEnabled(false);
		jPasswordField2.setEnabled(false);
		jPasswordField3.setEnabled(false);
		jCheckBox3.setEnabled(false);
		jButton6.setActionCommand("Delete user");
		jDialog3.validate();
	}

	void willChangePassword() {
		setUser();
		jTextField3.setText(user);
		jTextField3.setEnabled(false);
		jPasswordField1.setEnabled(true);
		jPasswordField2.setEnabled(true);
		jCheckBox3.setEnabled(true);
		refreshOldPassword();
		jButton6.setActionCommand("Change password");
		jDialog3.validate();
	}

	void refreshOldPassword() {
		jPasswordField3.setEnabled(jCheckBox3.isSelected());
	}

	void userAction(String action) {
		if(action.equals("Add user")) addUser();
		else if(action.equals("Delete user")) deleteUser();
		else if(action.equals("Change password")) changePassword();
		
	}

	void addUser() {
		String name=jTextField3.getText();
		char pwd[]=jPasswordField1.getPassword();
		char conf[]=jPasswordField2.getPassword();
		jPasswordField1.setText("");
		jPasswordField2.setText("");
		try {
			if(!Arrays.equals(pwd, conf)) throw new Exception("passwords don't match");
			((AdminConnection)conn).addUser(name, String.valueOf(pwd));
			refreshTable();
			jDialog3.setVisible(false);
		} catch (Exception e) {
			error(e);
		}
	}

	void deleteUser() {
		String name=jTextField3.getText();
		try {
			((AdminConnection)conn).deleteUser(name);
			refreshTable();
			jDialog3.setVisible(false);
		} catch (Exception e) {
			error(e);
		}
	}

	void changePassword() {
		String name=jTextField3.getText();
		char pwd[]=jPasswordField1.getPassword();
		char conf[]=jPasswordField2.getPassword();
		char old[]=jPasswordField3.getPassword();
		jPasswordField1.setText("");
		jPasswordField2.setText("");
		jPasswordField3.setText("");
		boolean enable=jCheckBox3.isSelected();
		try {
			if(!Arrays.equals(pwd, conf)) throw new Exception("passwords don't match");
			if(enable) ((AdminConnection)conn).changePassword(name, String.valueOf(old), String.valueOf(pwd));
			else ((AdminConnection)conn).changePassword(name, String.valueOf(pwd));
			refreshTable();
			jDialog3.setVisible(false);
		} catch (Exception e) {
			error(e);
		}
	}

	void refreshExport() {
		boolean overwrite=jCheckBox4.isSelected();
		String name=jTextField2.getText();
		jButton4.setEnabled(overwrite || !new File(name).exists());
	}

	void export() {
		String name=jTextField2.getText();
		try {
			((AdminConnection)conn).export(name);
			jCheckBox4.setSelected(false);
			refreshExport();
		} catch (Exception e) {
			error(e);
		}
	}

	void refreshImport() {
		boolean overwrite=jCheckBox5.isSelected();
		jButton5.setEnabled(overwrite || conn.root()==null);
	}

	void inport() {
		String name=jTextField2.getText();
		try {
			((AdminConnection)conn).inport(name);
			model.reload();
			jTree1.setSelectionRow(0);
			jCheckBox5.setSelected(false);
			refreshImport();
		} catch (Exception e) {
			error(e);
		}
	}

	void refresh() {
		try {
			long max=((AdminConnection)conn).maxSpace();
			long value=((AdminConnection)conn).allocatedSpace();
			jProgressBar1.setMaximum((int)max);
			jProgressBar1.setValue((int)value);
			jProgressBar1.setString(String.valueOf(value)+"/"+String.valueOf(max));
		} catch (Exception e) {
			error(e);
		}
	}

	void refreshTimer() {
		boolean refresh=jCheckBox6.isSelected();
		if(refresh) {
			try {
				int period=Integer.valueOf(jTextField5.getText()).intValue();
				jTextField5.setEnabled(false);
				timer=new Timer(true);
				timer.schedule(new TimerTask() {
					public void run() {
						try {
							refresh0();
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}, 0, period*1000);
			} catch (Exception e) {
				error(e);
				jCheckBox6.setSelected(false);
			}
		} else {
			timer.cancel();
			jTextField5.setEnabled(true);
		}
	}

	void refresh0() throws Exception {
		SwingUtilities.invokeAndWait(new Runnable() {
			public void run() {
				refresh();
			}
		});
	}

	void closeTimer() {
		boolean refresh=jCheckBox6.isSelected();
		if(refresh) {
			timer.cancel();
			jTextField5.setEnabled(true);
			jCheckBox6.setSelected(false);
		}
	}

	void gc() {
		try {
			((AdminConnection)conn).gc();
			refresh();
		} catch (Exception e) {
			error(e);
		}
	}

	void refreshShutdown() {
		jButton11.setEnabled(jCheckBox2.isSelected());
	}

	void shutdown() {
		try {
			((AdminConnection)conn).shutdown();
			jCheckBox2.setSelected(false);
			refreshShutdown();
			close();
		} catch (Exception e) {
			error(e);
		}
	}

	void error(Exception e) {
		JOptionPane.showMessageDialog(this, e.toString(), "Error", JOptionPane.ERROR_MESSAGE);
		e.printStackTrace();
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
                jComboBox1 = new javax.swing.JComboBox();
                jDialog2 = new javax.swing.JDialog();
                jLabel2 = new javax.swing.JLabel();
                jButton3 = new javax.swing.JButton();
                jDialog3 = new javax.swing.JDialog();
                jLabel4 = new javax.swing.JLabel();
                jTextField3 = new javax.swing.JTextField();
                jLabel5 = new javax.swing.JLabel();
                jPasswordField1 = new javax.swing.JPasswordField();
                jLabel6 = new javax.swing.JLabel();
                jPasswordField2 = new javax.swing.JPasswordField();
                jLabel7 = new javax.swing.JLabel();
                jPasswordField3 = new javax.swing.JPasswordField();
                jCheckBox3 = new javax.swing.JCheckBox();
                jButton6 = new javax.swing.JButton();
                jButton7 = new javax.swing.JButton();
                jPopupMenu1 = new javax.swing.JPopupMenu();
                jMenuItem1 = new javax.swing.JMenuItem();
                jPopupMenu2 = new javax.swing.JPopupMenu();
                jMenuItem2 = new javax.swing.JMenuItem();
                jMenuItem3 = new javax.swing.JMenuItem();
                jMenuItem4 = new javax.swing.JMenuItem();
                jDialog4 = new javax.swing.JDialog();
                jLabel8 = new javax.swing.JLabel();
                jTextField4 = new javax.swing.JTextField();
                jButton8 = new javax.swing.JButton();
                jButton9 = new javax.swing.JButton();
                jTabbedPane1 = new javax.swing.JTabbedPane();
                jSplitPane1 = new javax.swing.JSplitPane();
                jScrollPane1 = new javax.swing.JScrollPane();
                jTree1 = new javax.swing.JTree();
                jScrollPane2 = new javax.swing.JScrollPane();
                jTextArea1 = new javax.swing.JTextArea();
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
                jTextField5 = new javax.swing.JTextField();
                jCheckBox6 = new javax.swing.JCheckBox();
                jLabel10 = new javax.swing.JLabel();
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
                jTable1.addMouseListener(new java.awt.event.MouseAdapter() {
                        public void mousePressed(java.awt.event.MouseEvent evt) {
                                jTable1MousePressed(evt);
                        }
                        public void mouseReleased(java.awt.event.MouseEvent evt) {
                                jTable1MouseReleased(evt);
                        }
                });
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
                jCheckBox1.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                jCheckBox1ActionPerformed(evt);
                        }
                });

                jComboBox1.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "TRANSACTION_NONE", "TRANSACTION_READ_UNCOMMITTED", "TRANSACTION_READ_COMMITTED", "TRANSACTION_REPEATABLE_READ", "TRANSACTION_SERIALIZABLE" }));
                jComboBox1.setEnabled(false);

                org.jdesktop.layout.GroupLayout jDialog1Layout = new org.jdesktop.layout.GroupLayout(jDialog1.getContentPane());
                jDialog1.getContentPane().setLayout(jDialog1Layout);
                jDialog1Layout.setHorizontalGroup(
                        jDialog1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                        .add(jDialog1Layout.createSequentialGroup()
                                .addContainerGap()
                                .add(jLabel1)
                                .add(3, 3, 3)
                                .add(jDialog1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                                        .add(jDialog1Layout.createSequentialGroup()
                                                .add(jButton1)
                                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                                .add(jButton2))
                                        .add(jTextField1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 311, Short.MAX_VALUE)
                                        .add(jCheckBox1)
                                        .add(jComboBox1, 0, 311, Short.MAX_VALUE))
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
                                .add(jComboBox1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                                .add(jCheckBox1)
                                .add(18, 18, 18)
                                .add(jDialog1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                                        .add(jButton1)
                                        .add(jButton2))
                                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
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

                jLabel4.setText("Username:");

                jLabel5.setText("Password:");

                jLabel6.setText("Confirm password:");

                jLabel7.setText("Old password:");

                jCheckBox3.setText("Enable old password");
                jCheckBox3.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                jCheckBox3ActionPerformed(evt);
                        }
                });

                jButton6.setActionCommand("");
                jButton6.setLabel("Ok");
                jButton6.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                jButton6ActionPerformed(evt);
                        }
                });

                jButton7.setText("Cancel");
                jButton7.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                jButton7ActionPerformed(evt);
                        }
                });

                org.jdesktop.layout.GroupLayout jDialog3Layout = new org.jdesktop.layout.GroupLayout(jDialog3.getContentPane());
                jDialog3.getContentPane().setLayout(jDialog3Layout);
                jDialog3Layout.setHorizontalGroup(
                        jDialog3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                        .add(jDialog3Layout.createSequentialGroup()
                                .addContainerGap()
                                .add(jDialog3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                                        .add(jLabel6)
                                        .add(jLabel4)
                                        .add(jLabel5)
                                        .add(jLabel7))
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(jDialog3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                                        .add(jDialog3Layout.createSequentialGroup()
                                                .add(jButton6)
                                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                                .add(jButton7))
                                        .add(jDialog3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING, false)
                                                .add(org.jdesktop.layout.GroupLayout.LEADING, jPasswordField3)
                                                .add(org.jdesktop.layout.GroupLayout.LEADING, jPasswordField2)
                                                .add(org.jdesktop.layout.GroupLayout.LEADING, jPasswordField1)
                                                .add(org.jdesktop.layout.GroupLayout.LEADING, jTextField3, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 226, Short.MAX_VALUE)
                                                .add(org.jdesktop.layout.GroupLayout.LEADING, jCheckBox3)))
                                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                );
                jDialog3Layout.setVerticalGroup(
                        jDialog3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                        .add(jDialog3Layout.createSequentialGroup()
                                .addContainerGap()
                                .add(jDialog3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                                        .add(jLabel4)
                                        .add(jTextField3, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(jDialog3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                                        .add(jLabel5)
                                        .add(jPasswordField1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(jDialog3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                                        .add(jLabel6)
                                        .add(jPasswordField2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(jDialog3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                                        .add(jLabel7)
                                        .add(jPasswordField3, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(jCheckBox3)
                                .add(18, 18, 18)
                                .add(jDialog3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                                        .add(jButton6)
                                        .add(jButton7))
                                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                );

                jMenuItem1.setText("Abort");
                jMenuItem1.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                jMenuItem1ActionPerformed(evt);
                        }
                });
                jPopupMenu1.add(jMenuItem1);

                jMenuItem2.setText("Add");
                jMenuItem2.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                jMenuItem2ActionPerformed(evt);
                        }
                });
                jPopupMenu2.add(jMenuItem2);

                jMenuItem3.setText("Delete");
                jMenuItem3.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                jMenuItem3ActionPerformed(evt);
                        }
                });
                jPopupMenu2.add(jMenuItem3);

                jMenuItem4.setText("Change password");
                jMenuItem4.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                jMenuItem4ActionPerformed(evt);
                        }
                });
                jPopupMenu2.add(jMenuItem4);

                jLabel8.setText("Transaction:");

                jTextField4.setEnabled(false);

                jButton8.setText("Ok");
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

                org.jdesktop.layout.GroupLayout jDialog4Layout = new org.jdesktop.layout.GroupLayout(jDialog4.getContentPane());
                jDialog4.getContentPane().setLayout(jDialog4Layout);
                jDialog4Layout.setHorizontalGroup(
                        jDialog4Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                        .add(jDialog4Layout.createSequentialGroup()
                                .addContainerGap()
                                .add(jLabel8)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(jDialog4Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                                        .add(jDialog4Layout.createSequentialGroup()
                                                .add(jButton8)
                                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                                .add(jButton9))
                                        .add(jTextField4, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 286, Short.MAX_VALUE))
                                .addContainerGap())
                );
                jDialog4Layout.setVerticalGroup(
                        jDialog4Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                        .add(jDialog4Layout.createSequentialGroup()
                                .addContainerGap()
                                .add(jDialog4Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                                        .add(jLabel8)
                                        .add(jTextField4, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                                .add(18, 18, 18)
                                .add(jDialog4Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                                        .add(jButton8)
                                        .add(jButton9))
                                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                );

                setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

                jTree1.addTreeWillExpandListener(new javax.swing.event.TreeWillExpandListener() {
                        public void treeWillCollapse(javax.swing.event.TreeExpansionEvent evt)throws javax.swing.tree.ExpandVetoException {
                        }
                        public void treeWillExpand(javax.swing.event.TreeExpansionEvent evt)throws javax.swing.tree.ExpandVetoException {
                                jTree1TreeWillExpand(evt);
                        }
                });
                jTree1.addTreeSelectionListener(new javax.swing.event.TreeSelectionListener() {
                        public void valueChanged(javax.swing.event.TreeSelectionEvent evt) {
                                jTree1ValueChanged(evt);
                        }
                });
                jScrollPane1.setViewportView(jTree1);

                jSplitPane1.setLeftComponent(jScrollPane1);

                jTextArea1.setColumns(20);
                jTextArea1.setEditable(false);
                jTextArea1.setLineWrap(true);
                jTextArea1.setRows(5);
                jScrollPane2.setViewportView(jTextArea1);

                jSplitPane1.setRightComponent(jScrollPane2);

                jTabbedPane1.addTab("System", jSplitPane1);

                jPanel1.addComponentListener(new java.awt.event.ComponentAdapter() {
                        public void componentShown(java.awt.event.ComponentEvent evt) {
                                jPanel1ComponentShown(evt);
                        }
                });

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
                jCheckBox4.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                jCheckBox4ActionPerformed(evt);
                        }
                });

                jCheckBox5.setText("Overwrite");
                jCheckBox5.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                jCheckBox5ActionPerformed(evt);
                        }
                });

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

                jTextField5.setText("10");

                jCheckBox6.setText("Refresh every:");
                jCheckBox6.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                jCheckBox6ActionPerformed(evt);
                        }
                });

                jLabel10.setText("seconds");

                org.jdesktop.layout.GroupLayout jPanel3Layout = new org.jdesktop.layout.GroupLayout(jPanel3);
                jPanel3.setLayout(jPanel3Layout);
                jPanel3Layout.setHorizontalGroup(
                        jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                        .add(jPanel3Layout.createSequentialGroup()
                                .addContainerGap()
                                .add(jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                                        .add(jPanel3Layout.createSequentialGroup()
                                                .add(jCheckBox6)
                                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                                .add(jTextField5, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 42, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                                .add(jLabel10))
                                        .add(org.jdesktop.layout.GroupLayout.TRAILING, jProgressBar1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 598, Short.MAX_VALUE)
                                        .add(jButton10))
                                .addContainerGap())
                );
                jPanel3Layout.setVerticalGroup(
                        jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                        .add(jPanel3Layout.createSequentialGroup()
                                .addContainerGap()
                                .add(jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                                        .add(jCheckBox6)
                                        .add(jLabel10)
                                        .add(jTextField5, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                                .add(jProgressBar1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                .add(18, 18, 18)
                                .add(jButton10)
                                .addContainerGap(245, Short.MAX_VALUE))
                );

                jTabbedPane1.addTab("Memory", jPanel3);

                jButton11.setText("Shutdown");
                jButton11.setEnabled(false);
                jButton11.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                jButton11ActionPerformed(evt);
                        }
                });

                jCheckBox2.setText("Confirm");
                jCheckBox2.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                jCheckBox2ActionPerformed(evt);
                        }
                });

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
		jDialog1.setVisible(false);
		closeTimer();
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
		closeTimer();
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
		closeTimer();
		shutdown();
	}//GEN-LAST:event_jButton11ActionPerformed

	private void jPanel3ComponentShown(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_jPanel3ComponentShown
		refresh();
	}//GEN-LAST:event_jPanel3ComponentShown

	private void jTree1ValueChanged(javax.swing.event.TreeSelectionEvent evt) {//GEN-FIRST:event_jTree1ValueChanged
		select();
	}//GEN-LAST:event_jTree1ValueChanged

	private void jTree1TreeWillExpand(javax.swing.event.TreeExpansionEvent evt)throws javax.swing.tree.ExpandVetoException {//GEN-FIRST:event_jTree1TreeWillExpand
		reload(evt.getPath());
	}//GEN-LAST:event_jTree1TreeWillExpand

	private void jTable1MousePressed(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jTable1MousePressed
		if (evt.isPopupTrigger()) showPopup(evt.getPoint(),evt.getComponent(),evt.getX(),evt.getY());
	}//GEN-LAST:event_jTable1MousePressed

	private void jTable1MouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jTable1MouseReleased
		if (evt.isPopupTrigger()) showPopup(evt.getPoint(),evt.getComponent(),evt.getX(),evt.getY());
	}//GEN-LAST:event_jTable1MouseReleased

	private void jCheckBox3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBox3ActionPerformed
		refreshOldPassword();
	}//GEN-LAST:event_jCheckBox3ActionPerformed

	private void jButton6ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton6ActionPerformed
		userAction(evt.getActionCommand());
	}//GEN-LAST:event_jButton6ActionPerformed

	private void jMenuItem1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem1ActionPerformed
		willAbortTransaction();
		setDialogLocation(jDialog4);
		jDialog4.setVisible(true);
	}//GEN-LAST:event_jMenuItem1ActionPerformed

	private void jMenuItem2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem2ActionPerformed
		willAddUser();
		setDialogLocation(jDialog3);
		jDialog3.setVisible(true);
	}//GEN-LAST:event_jMenuItem2ActionPerformed

	private void jMenuItem3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem3ActionPerformed
		willDeleteUser();
		setDialogLocation(jDialog3);
		jDialog3.setVisible(true);
	}//GEN-LAST:event_jMenuItem3ActionPerformed

	private void jMenuItem4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem4ActionPerformed
		willChangePassword();
		setDialogLocation(jDialog3);
		jDialog3.setVisible(true);
	}//GEN-LAST:event_jMenuItem4ActionPerformed

	private void jButton7ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton7ActionPerformed
		jDialog3.setVisible(false);
	}//GEN-LAST:event_jButton7ActionPerformed

	private void jButton8ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton8ActionPerformed
		abortTransaction();
	}//GEN-LAST:event_jButton8ActionPerformed

	private void jButton9ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton9ActionPerformed
		jDialog4.setVisible(false);
	}//GEN-LAST:event_jButton9ActionPerformed

	private void jCheckBox1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBox1ActionPerformed
		refreshLevel();
	}//GEN-LAST:event_jCheckBox1ActionPerformed

	private void jCheckBox4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBox4ActionPerformed
		refreshExport();
	}//GEN-LAST:event_jCheckBox4ActionPerformed

	private void jCheckBox5ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBox5ActionPerformed
		refreshImport();
	}//GEN-LAST:event_jCheckBox5ActionPerformed

	private void jPanel1ComponentShown(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_jPanel1ComponentShown
		refreshExport();
		refreshImport();
	}//GEN-LAST:event_jPanel1ComponentShown

	private void jCheckBox2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBox2ActionPerformed
		refreshShutdown();
	}//GEN-LAST:event_jCheckBox2ActionPerformed

	private void jCheckBox6ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBox6ActionPerformed
		refreshTimer();
	}//GEN-LAST:event_jCheckBox6ActionPerformed

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
        private javax.swing.JButton jButton9;
        private javax.swing.JCheckBox jCheckBox1;
        private javax.swing.JCheckBox jCheckBox2;
        private javax.swing.JCheckBox jCheckBox3;
        private javax.swing.JCheckBox jCheckBox4;
        private javax.swing.JCheckBox jCheckBox5;
        private javax.swing.JCheckBox jCheckBox6;
        private javax.swing.JComboBox jComboBox1;
        private bsh.util.JConsole jConsole1;
        private javax.swing.JDialog jDialog1;
        private javax.swing.JDialog jDialog2;
        private javax.swing.JDialog jDialog3;
        private javax.swing.JDialog jDialog4;
        private javax.swing.JLabel jLabel1;
        private javax.swing.JLabel jLabel10;
        private javax.swing.JLabel jLabel2;
        private javax.swing.JLabel jLabel3;
        private javax.swing.JLabel jLabel4;
        private javax.swing.JLabel jLabel5;
        private javax.swing.JLabel jLabel6;
        private javax.swing.JLabel jLabel7;
        private javax.swing.JLabel jLabel8;
        private javax.swing.JMenuItem jMenuItem1;
        private javax.swing.JMenuItem jMenuItem2;
        private javax.swing.JMenuItem jMenuItem3;
        private javax.swing.JMenuItem jMenuItem4;
        private javax.swing.JPanel jPanel1;
        private javax.swing.JPanel jPanel3;
        private javax.swing.JPanel jPanel4;
        private javax.swing.JPasswordField jPasswordField1;
        private javax.swing.JPasswordField jPasswordField2;
        private javax.swing.JPasswordField jPasswordField3;
        private javax.swing.JPopupMenu jPopupMenu1;
        private javax.swing.JPopupMenu jPopupMenu2;
        private javax.swing.JProgressBar jProgressBar1;
        private javax.swing.JScrollPane jScrollPane1;
        private javax.swing.JScrollPane jScrollPane2;
        private javax.swing.JScrollPane jScrollPane3;
        private javax.swing.JSplitPane jSplitPane1;
        private javax.swing.JTabbedPane jTabbedPane1;
        private javax.swing.JTable jTable1;
        private javax.swing.JTextArea jTextArea1;
        private javax.swing.JTextField jTextField1;
        private javax.swing.JTextField jTextField2;
        private javax.swing.JTextField jTextField3;
        private javax.swing.JTextField jTextField4;
        private javax.swing.JTextField jTextField5;
        private javax.swing.JTree jTree1;
        private javax.swing.JMenuBar menuBar;
        private javax.swing.JMenuItem openMenuItem;
        // End of variables declaration//GEN-END:variables
}
