package gov.nih.nci.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Label;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.AbstractAction;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;

import org.protege.editor.owl.OWLEditorKit;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLClass;

import gov.nih.nci.ui.dialog.PropertyEditingDialog;

public class PropertyTablePanel extends JPanel implements ActionListener {

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private OWLEditorKit owlEditorKit;

    private JPopupMenu popupMenu;
    
    private PropertyTableModel tableModel;
    
    private JTable propertyTable;
    
    private OWLAnnotationProperty complexProp;
    
    public OWLAnnotationProperty getComplexProp() {
    	return complexProp;
    }
    
    private String tableName;
    
    private JScrollPane sp;
    
    private JLabel tableNameLabel;
    
    private JPanel tableHeaderPanel;
    
    private JButton addButton;
    
    private JButton editButton;
    
    private JButton deleteButton;

    //private ActionListener actionListener;
    
    public PropertyTablePanel(OWLEditorKit editorKit) {
        this.owlEditorKit = editorKit;
        initialiseOWLView();
        createPopupMenu();
    }
    
    public PropertyTablePanel(OWLEditorKit editorKit, OWLAnnotationProperty complexProperty, String tableName) {
        this.owlEditorKit = editorKit;
        this.complexProp = complexProperty;
        this.tableName = tableName;
        initialiseOWLView();
        createPopupMenu();
    }


    private void createPopupMenu() {
        popupMenu = new JPopupMenu();
        popupMenu.add(new AbstractAction("Show axioms") {
            public void actionPerformed(ActionEvent e) {
                

            }
        });
    }





    protected void initialiseOWLView() {
        tableModel = new PropertyTableModel(owlEditorKit, complexProp);
        createButtons(this);
        createUI();
        
    }


    private void createUI() {
        
    	setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    	
        propertyTable = new JTable(tableModel);
        
        propertyTable.setGridColor(Color.LIGHT_GRAY);
        propertyTable.setRowHeight(propertyTable.getRowHeight() + 4);
        propertyTable.setShowGrid(true);       
        propertyTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        propertyTable.getTableHeader().setReorderingAllowed(true);
        propertyTable.setFillsViewportHeight(true);       

        sp = new JScrollPane(propertyTable);
        createLabelHeader(tableName, addButton, editButton, deleteButton);
        add(tableHeaderPanel);
        tableHeaderPanel.setVisible(false);        
        sp.setVisible(false);
        add(sp);
    }

    public void setSelectedCls(OWLClass cls) {
    	tableModel.setSelection(cls);

    	if (tableModel.hasAnnotation()) {
    		tableHeaderPanel.setVisible(true);
    		sp.setVisible(true);
    	} else {
    		tableHeaderPanel.setVisible(false);
    		sp.setVisible(false);
    	}
    	repaint();
    }

    private void createLabelHeader(String labeltext, JButton b1, JButton b2, JButton b3){
    	
    	tableHeaderPanel = new JPanel();
        
    	tableHeaderPanel.setLayout(new BorderLayout());
        
        tableNameLabel = new JLabel(labeltext);
        tableNameLabel.setPreferredSize(new Dimension(100, 25));
        
        
        JPanel panel2 = new JPanel();
        
        panel2.setPreferredSize(new Dimension(80,25));
        panel2.add(b1);
        panel2.add(b2);
        panel2.add(b3);
        
        tableHeaderPanel.add(tableNameLabel, BorderLayout.WEST);
        tableHeaderPanel.add(panel2, BorderLayout.EAST);
        
    }
    
    private void createButtons(ActionListener actionListener) {
    	addButton = new IconButton(NCIEditTabConstants.ADD, "ButtonAddIcon.png", NCIEditTabConstants.ADD, actionListener);
    	editButton = new IconButton(NCIEditTabConstants.EDIT, "ButtonEditIcon.png", NCIEditTabConstants.EDIT, actionListener);
    	deleteButton = new IconButton(NCIEditTabConstants.DELETE, "ButtonDeleteIcon.png", NCIEditTabConstants.DELETE, actionListener);
    }

	@Override
	public void actionPerformed(ActionEvent e) {
		if(e.getSource() instanceof IconButton){
			IconButton button = (IconButton)e.getSource();

			if(button.getType() == NCIEditTabConstants.ADD){
				PropertyEditingDialog addedit = new	PropertyEditingDialog(NCIEditTabConstants.ADD, tableModel.getSelectedPropertyType(), null, tableModel.getSelectedPropertyOptions());
				HashMap<String, String> data = 	addedit.showDialog(owlEditorKit, "Adding Properties");
				if (data != null) {
					NCIEditTab.currentTab().complexPropOp(NCIEditTabConstants.ADD, tableModel.getSelection(),
							tableModel.getComplexProp(), null, data);
					tableModel.setSelection(tableModel.getSelection());
				}
				System.out.println("The data: " + data);
				//upade view
			}		
			else if(button.getType() == NCIEditTabConstants.EDIT){
				int row = propertyTable.getSelectedRow();
				PropertyEditingDialog addedit = new	PropertyEditingDialog(NCIEditTabConstants.EDIT, tableModel.getSelectedPropertyType(), tableModel.getSelectedPropertyValue(row), tableModel.getSelectedPropertyOptions());
				HashMap<String, String> data = 	addedit.showDialog(owlEditorKit, "Editing Properties");
				if (data != null) {
					NCIEditTab.currentTab().complexPropOp(NCIEditTabConstants.EDIT, tableModel.getSelection(),
							tableModel.getComplexProp(), tableModel.getAssertion(row), data);
					tableModel.setSelection(tableModel.getSelection());
					
				}
				System.out.println("The data: " + data);

				//update view
			}
			else if(button.getType() == NCIEditTabConstants.DELETE){
				int row = propertyTable.getSelectedRow();
				if (row >= 0) {
					int ret = JOptionPane.showConfirmDialog(this, "Please confirm if you want to delete the selected property!", "Delete Confirmation", JOptionPane.OK_CANCEL_OPTION);
					if (ret == JOptionPane.OK_OPTION) {
						NCIEditTab.currentTab().complexPropOp(NCIEditTabConstants.DELETE, tableModel.getSelection(), 
								tableModel.getComplexProp(), tableModel.getAssertion(row), null);
						
						
						tableModel.setSelection(tableModel.getSelection());
					}
				}
				//todo - delete seleted table row from table, update view

			}
		}
	}
	
	public void createNewProp() {
		
		PropertyEditingDialog addedit = new	PropertyEditingDialog(NCIEditTabConstants.ADD, tableModel.getSelectedPropertyType(), null, tableModel.getSelectedPropertyOptions());
		HashMap<String, String> data = 	addedit.showDialog(owlEditorKit, "Adding Properties");
		if (data != null) {
			NCIEditTab.currentTab().complexPropOp(NCIEditTabConstants.ADD, tableModel.getSelection(),
					tableModel.getComplexProp(), null, data);
			tableModel.setSelection(tableModel.getSelection());
		}
		System.out.println("The data: " + data);
		//upade view
		
	}

    

   
}
