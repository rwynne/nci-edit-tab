package gov.nih.nci.ui.dialog;

//import edu.stanford.smi.protege.model.KnowledgeBase;
//import edu.stanford.smi.protege.util.FileField;
//import edu.stanford.smi.protege.util.LabeledComponent;
//import edu.stanford.smi.protege.util.Log;
//import edu.stanford.smi.protegex.owl.model.OWLModel;
//import gov.nih.nci.protegex.dialog.TaskProgressDialog;
//import gov.nih.nci.protegex.panel.*;
//import gov.nih.nci.protegex.edit.*;
//import gov.nih.nci.protegex.util.MsgDialog;
import gov.nih.nci.ui.BatchProcessOutputPanel;
import gov.nih.nci.ui.BatchTask;
import gov.nih.nci.ui.NCIEditTab;

//import static gov.nih.nci.protegex.batch.BatchTask.TaskType.*;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Vector;
import java.util.logging.Level;

import javax.swing.*;

import org.apache.log4j.Logger;

/**
 * @Author: NGIT, Kim Ong; Iris Guo
 */

public class BatchProcessingDialog extends JDialog implements ActionListener {
	private static final long serialVersionUID = -3817605737614597419L;
	private static final Logger log = Logger.getLogger(BatchProcessingDialog.class);

	JButton fStartButton, fCancelButton, fInputButton, fOutputButton;

	JTextField fInputTf, fOutputTf;

	JComboBox batchType = null;

	NCIEditTab tab;

	BatchProcessOutputPanel be = null;

	//OWLModel owlModel;

	String infile;// = fInputTf.getText();

	String outfile;// = fOutputTf.getText();

	//OWLWrapper wrapper = null;

	public static final int BATCH_LOADER = 2;

	public static final int BATCH_EDITOR = 1;

	int type = BATCH_EDITOR;

	//FileField inputFileField;

//	FileField outputFileField;

	public BatchProcessingDialog(BatchProcessOutputPanel b, NCIEditTab tab) {
		be = b;
		this.tab = tab;
		//this.owlModel = tab.getOWLModel();
	//	this.wrapper = tab.getWrapper();

		this.infile = "";
		this.outfile = "";

		// this.type = type;
		setModal(true);

		/**
		 * if (type == BATCH_LOADER) { this.setTitle("Batch Loader"); } else {
		 * this.setTitle("Batch Editor"); }
		 */
		this.setTitle("Batch Processor");
		init();
	}

	public void setBatchProcessType(int type) {
		this.type = type;
	}
	
	private JPanel createFileField(String label, String extension, String type){
		
	  JPanel panel = new JPanel();
	  panel.setPreferredSize(new Dimension(350, 30));
	  JLabel lb = new JLabel(label);
	  JTextField field = new  JTextField();
	  
	  field.setPreferredSize(new Dimension(200, 25));
	  
	  JButton btn = new JButton();
	  if(type == "open"){
		  btn.setText("Input");
	  }
	  else{
		  
		  btn.setText("Output");
	  }
	  
	  btn.addActionListener(new ActionListener(){
		  
		  public void actionPerformed(ActionEvent e){
			  if(type == "open"){
				  JFileChooser fc = new JFileChooser();
				  //to do - add file filter
				  int select = fc.showOpenDialog(BatchProcessingDialog.this);
				  if (select == JFileChooser.APPROVE_OPTION) {
			            File file = fc.getSelectedFile();
			            infile = file.getAbsolutePath();
			            field.setText(infile);
				  }
			  }
			  else{
				  JFileChooser fc = new JFileChooser();
				  //todo - add file extension filter
				  int select = fc.showSaveDialog(BatchProcessingDialog.this);
				  
				  if (select == JFileChooser.APPROVE_OPTION) {
			            File file = fc.getSelectedFile();
			            outfile = file.getAbsolutePath();
			            field.setText(outfile);
				  }
			  }
		  }
	  });
	  panel.add(lb);
	  panel.add(field);
	  panel.add(btn);
	  
	  return panel;
	}

	public void init() {
		try {
			Container container = this.getContentPane();
			container.setLayout(new BorderLayout());

			this.setLocation(450, 300);

			JPanel filePanel = new JPanel();
			filePanel.setLayout(new BorderLayout());

			String label = "Input File";
			String path = "";
			String extension = "dat";
			String description = "";
			//inputFileField = new FileField(label, path, extension, description);

			//filePanel.add(inputFileField, BorderLayout.NORTH);\
			filePanel.add(createFileField("Input File", "dat","open"), BorderLayout.NORTH);
			label = "Log File";
			path = "";
			extension = "out";
			description = "";
			//outputFileField = new FileField(label, path, extension, description);
			//filePanel.add(outputFileField, BorderLayout.CENTER);
			filePanel.add(createFileField("Output File", "out","save"), BorderLayout.CENTER);

			container.add(filePanel, BorderLayout.NORTH);

			String[] types = new String[] { "Edit", "Load" };
			batchType = new JComboBox(types);
			batchType.setSelectedIndex(0);
			batchType.addActionListener(this);

			//LabeledComponent lc = new LabeledComponent("Processing Type",
				//	batchType);

			//container.add(lc, BorderLayout.CENTER);
			
			JPanel labelcombopanel = new JPanel();
			labelcombopanel.setPreferredSize(new Dimension(350, 50));
			labelcombopanel.add(new JLabel("Batch Type"));
			labelcombopanel.add(batchType, BorderLayout.CENTER);
			container.add(labelcombopanel, BorderLayout.CENTER);
			
			fStartButton = new JButton("Start");
			fStartButton.addActionListener(this);

			fCancelButton = new JButton("Cancel");
			fCancelButton.addActionListener(this);

			JPanel btnPanel = new JPanel();
			// btnPanel.setLayout(new BorderLayout());
			btnPanel.add(fStartButton);
			btnPanel.add(fCancelButton);

			container.add(btnPanel, BorderLayout.SOUTH);

			pack();
			this.setVisible(true);

		} catch (Exception ex) {
			log.warn("Exception caught", ex);

		}
	}

	public String getInfile() {

	//	if (inputFileField.getFilePath() == null)
		//	return null;
		//
		//return inputFileField.getFilePath().getPath();
		return  infile;
	}

	public String getOutfile() {
		//if (outputFileField.getFilePath() == null)
		//	return null;
		//return outputFileField.getFilePath().getPath();
		return outfile;
	}

	public String getToday() {
		Calendar cal = Calendar.getInstance();
		SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy");
		String today = sdf.format(cal.getTime());
		return today;
	}

	public void outputErrors(String outputfile, Vector v) {
		if (outputfile == null || v == null)
			return;
		PrintWriter writer = null;
		try {
			writer = new PrintWriter(new BufferedWriter(new FileWriter(
					outputfile)));
			String msg = getToday() + "\n";
			writer.println(msg);

			be.getTextArea().append(msg);
			be.getTextArea().append("\n");

			for (int i = 0; i < v.size(); i++) {
				msg = (String) v.elementAt(i);

				be.getTextArea().append(msg);
				be.getTextArea().append("\n");
				writer.println(msg);
			}
			writer.close();
		} catch (Exception e) {
			log.warn("Exception caught", e);
			try {
				writer.close();
			} catch (Exception ex) {
				log.warn("Exception caught", ex);
			}
		}
	}

	public void actionPerformed(ActionEvent event) {
		Object action = event.getSource();

		if (action == batchType) {
			// TODO: Bob, this is disgusting
			type = batchType.getSelectedIndex() + 1;
		} else if (action == fCancelButton) {
			dispose();
		} else if (action == fStartButton) {
			infile = getInfile();
			outfile = getOutfile();

			if (infile == null || infile.compareTo("") == 0) {
				//MsgDialog.error((JFrame) tab.getTopLevelAncestor(),
					//	"Please specify an input file.");
				return;
			}

			else if (outfile == null || outfile.compareTo("") == 0) {
				//MsgDialog.error((JFrame) tab.getTopLevelAncestor(),
				//		"Please specify an output file.");
				return;
			}

			else if (infile.equalsIgnoreCase(outfile)) {
				//MsgDialog.error((JFrame) tab.getTopLevelAncestor(),
				//		"Invalid inputs.");
			}

			else {
				setVisible(false);
				TaskProgressDialog tpd = null;

				BatchTask task = null;
				if (type == BATCH_LOADER) {
					//if (tab.byCode()) {
						//task = new BatchLoadByCodeTask(tab, infile, outfile);
					//} else {
						//task = new BatchLoadByNameTask(tab, infile, outfile);
				///	}
					//task.setType(LOAD);
					//tpd = new TaskProgressDialog((JFrame) tab
					//		.getTopLevelAncestor(),
					//		"Batch Load Progress Status", task);
				} else if (type == BATCH_EDITOR) {
					//task = new BatchEditTask(tab, infile, outfile);
					//task.setType(EDIT);
				//	tpd = new TaskProgressDialog((JFrame) tab
				//			.getTopLevelAncestor(),
				//			"Batch Edit Progress Status", task);
				}

				task.openPrintWriter(outfile);

				// task.validateData();
				if (!task.canProceed()) {
					//MsgDialog
					//		.error(
					//				(JFrame) tab.getTopLevelAncestor(),
					//				"Severe input data issues detected, cannot proceed. "
					//						+ "Please correct all errors and try again.");
					return;
				}

				if (tpd != null) {
					tpd.run();
				}

				dispose();

				if (tpd != null) {
					//MsgDialog.ok((JFrame) tab.getTopLevelAncestor(),
						//	getTitle(), "Completed actions: "
						//			+ tpd.getNumCompleted());
				}
				try {
					task.closePrintWriter();
				} catch (Exception e) {

				}

				if (task.isCancelled()) {
					//be.enableButton("clearButton", true);
				} else {
				//	be.enableButton("inputButton", false);
				//	be.enableButton("saveButton", true);
				//	be.enableButton("clearButton", true);
				}

			}

		}

	}
}
