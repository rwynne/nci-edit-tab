package gov.nih.nci.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.swing.table.AbstractTableModel;

import org.protege.editor.owl.OWLEditorKit;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAnnotationValue;
import org.semanticweb.owlapi.model.OWLAnnotationValueVisitor;
import org.semanticweb.owlapi.model.OWLAnonymousIndividual;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.search.EntitySearcher;

import gov.nih.nci.ui.dialog.NCIClassCreationDialog;


public class PropertyTableModel extends AbstractTableModel {

	private static final long serialVersionUID = -702166512096011158L;

	OWLOntology ont;

	private OWLClass selection = null;
	
	public OWLClass getSelection() {return selection;}
	
	//private List<OWLAnnotationProperty> complexProperties;
	private OWLAnnotationProperty complexProp;
	
	public OWLAnnotationProperty getComplexProp() {
		return complexProp;
	}
	private Set<OWLAnnotationProperty> requiredAnnotations;
	private List<OWLAnnotationProperty> requiredAnnotationsList;
	private List<OWLAnnotation> annotations = new ArrayList<>();
	
	
	
	private List<OWLAnnotationAssertionAxiom> assertions = new ArrayList<OWLAnnotationAssertionAxiom>();
	
	public OWLAnnotationAssertionAxiom getAssertion(int idx) {
		return assertions.get(idx);
	}

	public PropertyTableModel(OWLEditorKit k, OWLAnnotationProperty complexProperty) {
		ont = k.getOWLModelManager().getActiveOntology();
		complexProp = complexProperty;
		requiredAnnotations = NCIEditTab.currentTab().getRequiredAnnotationsForAnnotation(complexProp);
		requiredAnnotationsList = new ArrayList<OWLAnnotationProperty>(requiredAnnotations);
	}


	public int getRowCount() {
		if (annotations.size() > 0) {
			return annotations.size() / getColumnCount();
		}
		return 0;
		
	}


	public int getColumnCount() {
		if (this.requiredAnnotations != null) {
			return this.requiredAnnotations.size() + 1;
		} else {
			return 0;
		}
	}

	public Object getValueAt(int rowIndex, int columnIndex) {

		int index = rowIndex * getColumnCount() + columnIndex; 
		LiteralExtractor literalExtractor = new LiteralExtractor();

		if ( index < annotations.size() ) {
			OWLAnnotation annot = annotations.get(index);
			if (annot == null) {
				return null;
			} else {
				if (columnIndex == 0) {
					return literalExtractor.getLiteral(annot.getValue());
				}
				for (OWLAnnotationProperty aprop : requiredAnnotations) {

					if ( annot.getProperty().equals(aprop)) {
						return literalExtractor.getLiteral(annot.getValue());
					}

				}
			}
		}
		return null;
	}

	public HashMap<String, String> getSelectedPropertyType() {
		HashMap<String, String> propertyTypes = new HashMap<String, String>();
		propertyTypes.put("Value", "TextArea");
		int columnCount = getColumnCount();
		IRI dataType;
		String columnName;
		for (int i = 1; i < columnCount; i++) {
			dataType = NCIEditTab.currentTab().getDataType(requiredAnnotationsList.get(i -1));
			columnName = getColumnName(i);


			if (isDataTypeTextArea(dataType)) {
				propertyTypes.put(columnName, "TextArea");
			} else if (isDataTypeTextField(dataType)) {
				propertyTypes.put(columnName, "TextField");
			} else if (isDataTypeCombobox(dataType)) {
				propertyTypes.put(columnName, "ComboBox");
			}
		}
		return propertyTypes;
	}

	public HashMap<String, String> getSelectedPropertyValue(int row) {
		HashMap<String, String> propertyValues = new HashMap<String, String>();
		if ( row < 0 ) {
			return propertyValues;
		}
		int columnCount = getColumnCount();
		int startIndex = row * columnCount;
		LiteralExtractor literalExtractor = new LiteralExtractor();

		for (int i=0; i<columnCount; i++) {
			OWLAnnotation annot = annotations.get(startIndex + i);
			if (annot != null) {
				propertyValues.put(getColumnName(i), literalExtractor.getLiteral(annot.getValue()));
			}
		}

		return propertyValues;
	}
	
	public HashMap<String, String> getDefaultPropertyValues() {
		HashMap<String, String> propertyValues = new HashMap<String, String>();
		
		for (OWLAnnotationProperty aprop : requiredAnnotationsList) {

			propertyValues.put(NCIEditTab.currentTab().getRDFSLabel(aprop).get(),
					NCIEditTab.currentTab().getDefaultValue(NCIEditTab.currentTab().getDataType(aprop)));
		}
		return propertyValues;
	}

	public HashMap<String, ArrayList<String>> getSelectedPropertyOptions() {
		HashMap<String, ArrayList<String>> propertyOptions = new HashMap<String, ArrayList<String>>();
		for (OWLAnnotationProperty aprop : requiredAnnotationsList) {

			if (isDataTypeCombobox(NCIEditTab.currentTab().getDataType(aprop))) {
				ArrayList<String> optionList = new ArrayList<String>();
				optionList.addAll(NCIEditTab.currentTab().getEnumValues(NCIEditTab.currentTab().getDataType(aprop)));
				propertyOptions.put(NCIEditTab.currentTab().getRDFSLabel(aprop).get(), optionList);
			}
		}
		return propertyOptions;
	}


	private boolean isDataTypeTextArea( IRI dataType ) {
		boolean result = false;
		if (dataType.toString().contains("textarea")) {
			result = true;
		}
		return result;
	}

	private boolean isDataTypeTextField( IRI dataType ) {
		boolean result = false;
		if (dataType.toString().contains("string")) {
			result = true;
		}
		if (dataType.toString().endsWith("system")) {
			return true;
		}
	
		return result;
	}

	private boolean isDataTypeCombobox( IRI dataType ) {
		boolean result = false;
		//if (!isDataTypeTextArea(dataType) && !isDataTypeTextField(dataType)) {
		if (dataType.toString().endsWith("enum")) {
			result = true;
		}
		return result;
	}

	class LiteralExtractor implements OWLAnnotationValueVisitor {

		private String label;

		public String getLiteral(OWLAnnotationValue value){
			label = null;
			value.accept(this);
			return label;
		}

		public void visit(IRI iri) {
			// do nothing
		}


		public void visit(OWLAnonymousIndividual owlAnonymousIndividual) {
			// do nothing
		}


		public void visit(OWLLiteral literal) {
			label = literal.getLiteral();
		}
	}

	public String getColumnName(int column) {

		if (column == 0) {
			return "Value";
		}
		return NCIEditTab.currentTab().getRDFSLabel(requiredAnnotationsList.get(column-1)).get();
	}

	public void setSelection(OWLClass cls) {

		this.selection = cls;
		loadAnnotations();
	}

	private void loadAnnotations() {

		if (selection != null) {
			annotations.clear();
			assertions.clear();
			for (OWLAnnotationAssertionAxiom ax : EntitySearcher.getAnnotationAssertionAxioms(selection, ont)) {
				OWLAnnotation annot = ax.getAnnotation();
				if (annot.getProperty().equals(this.complexProp)) {
					assertions.add(ax);
					//annotations.add(annot);
					if (annot.getValue() != null) {

						annotations.add(annot);
					}
					Set<OWLAnnotation> annotSet = ax.getAnnotations();
					for (OWLAnnotationProperty req_a : requiredAnnotations) {
						OWLAnnotation found = null;
						for (OWLAnnotation owl_a : annotSet) {
							if (req_a.equals(owl_a.getProperty())) {
								found = owl_a;
							}
						}
						annotations.add(found);
					}
				}

			}  
			
		}

	}

	public boolean hasAnnotation() {
		return !annotations.isEmpty();
	}
	
}
