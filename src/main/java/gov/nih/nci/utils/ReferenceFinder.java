package gov.nih.nci.utils;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.protege.editor.owl.model.OWLModelManager;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLClassExpressionVisitor;
import org.semanticweb.owlapi.model.OWLDataAllValuesFrom;
import org.semanticweb.owlapi.model.OWLDataExactCardinality;
import org.semanticweb.owlapi.model.OWLDataHasValue;
import org.semanticweb.owlapi.model.OWLDataMaxCardinality;
import org.semanticweb.owlapi.model.OWLDataMinCardinality;
import org.semanticweb.owlapi.model.OWLDataSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLObjectAllValuesFrom;
import org.semanticweb.owlapi.model.OWLObjectComplementOf;
import org.semanticweb.owlapi.model.OWLObjectExactCardinality;
import org.semanticweb.owlapi.model.OWLObjectHasSelf;
import org.semanticweb.owlapi.model.OWLObjectHasValue;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.model.OWLObjectMaxCardinality;
import org.semanticweb.owlapi.model.OWLObjectMinCardinality;
import org.semanticweb.owlapi.model.OWLObjectOneOf;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLObjectUnionOf;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;

import gov.nih.nci.ui.NCIEditTab;

public class ReferenceFinder implements OWLClassExpressionVisitor {

	private OWLEntity entity;
	
	private OWLClassExpression currentExpression;

	private OWLModelManager owlModelManager;

	private OWLOntology ont;
	
	HashMap<OWLAnnotationProperty, Set<String>> fixups = new HashMap<OWLAnnotationProperty, Set<String>>();	

	public ReferenceFinder(OWLModelManager man) {
		owlModelManager = man;
	}



	public Map<OWLAnnotationProperty, Set<String>> computeAnnotations(OWLClass inst) { 
		
		fixups = new HashMap<OWLAnnotationProperty, Set<String>>();		

		entity = (OWLEntity) inst;

		ont = owlModelManager.getActiveOntology();

		Set<OWLAxiom> axioms = ont.getReferencingAxioms(inst);
		for (OWLAxiom ax : axioms) {
			if (ax instanceof OWLEquivalentClassesAxiom) {
				for (OWLClassExpression desc : ((OWLEquivalentClassesAxiom) ax).getClassExpressions()) {
					if (!desc.isAnonymous()) {
						setExpression(desc);
					} else {
						desc.accept(this);	
					}
				}
				
			} else if (ax instanceof OWLSubClassOfAxiom) {
				OWLClassExpression par = ((OWLSubClassOfAxiom) ax).getSuperClass();
				
				if (par.isAnonymous()) {
					par.accept(this);
				} else if (par.asOWLClass().equals(entity)) {
					OWLClassExpression child = ((OWLSubClassOfAxiom) ax).getSubClass();
					String val = child.asOWLClass().getIRI().getShortForm();
					fixups = addToFixups(fixups, NCIEditTab.DEP_CHILD, val);
					System.out.println("OLD_CHILD:" + val );
				}
			}
		}
		return fixups;
	}
	
	public List<OWLOntologyChange> retargetRefs(OWLClass from, OWLClass to) { 
		// TODO: finish !! same idea as retirement but modify rather than annotate

		List<OWLOntologyChange> changes = new ArrayList<OWLOntologyChange>();
		

		entity = (OWLEntity) from;

		ont = owlModelManager.getActiveOntology();

		Set<OWLAxiom> axioms = ont.getReferencingAxioms(from);
		for (OWLAxiom ax : axioms) {
			if (ax instanceof OWLEquivalentClassesAxiom) {
				for (OWLClassExpression desc : ((OWLEquivalentClassesAxiom) ax).getClassExpressions()) {
					if (!desc.isAnonymous()) {
						setExpression(desc);
					} else {
						desc.accept(this);	
					}
				}
				
			} else if (ax instanceof OWLSubClassOfAxiom) {
				OWLClassExpression par = ((OWLSubClassOfAxiom) ax).getSuperClass();
				
				if (par.isAnonymous()) {
					par.accept(this);
				} else if (par.asOWLClass().equals(entity)) {
					OWLClassExpression child = ((OWLSubClassOfAxiom) ax).getSubClass();
					String val = child.asOWLClass().getIRI().getShortForm();
					fixups = addToFixups(fixups, NCIEditTab.DEP_CHILD, val);
					System.out.println("OLD_CHILD:" + val );
				}
			}
		}

		return changes;

	}

	
	public HashMap<OWLAnnotationProperty, Set<String>> addToFixups(HashMap<OWLAnnotationProperty, Set<String>> fixups, OWLAnnotationProperty prop, String val) {
		Set<String> ss = fixups.get(prop);
		if (ss == null) {
			ss = new HashSet<String>();
			fixups.put(prop,  ss);
		}
		ss.add(val);
		return fixups;
	}
	
	private void setExpression(OWLClassExpression exp) {
		currentExpression = exp;
	}

	
	@Override
	public void visit(OWLClass ce) {		
		
	}
	
	@Override
	public void visit(OWLObjectIntersectionOf ce) {
		Set<OWLClassExpression> conjs = ce.asConjunctSet();
		for (OWLClassExpression c : conjs) {
			c.accept(this);
		}		
	}

	@Override
	public void visit(OWLObjectUnionOf ce) {
		Set<OWLClassExpression> conjs = ce.asDisjunctSet();
		for (OWLClassExpression c : conjs) {
			c.accept(this);
		}
	}

	@Override
	public void visit(OWLObjectComplementOf ce) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(OWLObjectSomeValuesFrom ce) {
		if (ce.getFiller().asOWLClass().equals(entity)) {
			System.out.println("OSVF: " + ce);
			String val = ce.getProperty().asOWLObjectProperty().getIRI().getShortForm() + "|"
					+ "some" + "|" + currentExpression.asOWLClass().getIRI().getShortForm();
			fixups = addToFixups(fixups, NCIEditTab.DEP_IN_ROLE, val);
			
			System.out.println("OLD_SOURCE_ROLE:" + val);
			
		}
		
	}

	@Override
	public void visit(OWLObjectAllValuesFrom ce) {
		if (ce.getFiller().asOWLClass().equals(entity)) {
			System.out.println("OSVF: " + ce);
			String val = ce.getProperty().asOWLObjectProperty().getIRI().getShortForm() + "|"
					+ "all" + "|" + currentExpression.asOWLClass().getIRI().getShortForm();
			fixups = addToFixups(fixups, NCIEditTab.DEP_IN_ROLE, val);
			
			System.out.println("OLD_SOURCE_ROLE:" + val);
			
		}
	}



	@Override
	public void visit(OWLObjectHasValue ce) {
		// TODO Auto-generated method stub
		
	}



	@Override
	public void visit(OWLObjectMinCardinality ce) {
		// TODO Auto-generated method stub
		
	}



	@Override
	public void visit(OWLObjectExactCardinality ce) {
		// TODO Auto-generated method stub
		
	}



	@Override
	public void visit(OWLObjectMaxCardinality ce) {
		// TODO Auto-generated method stub
		
	}



	@Override
	public void visit(OWLObjectHasSelf ce) {
		// TODO Auto-generated method stub
		
	}



	@Override
	public void visit(OWLObjectOneOf ce) {
		// TODO Auto-generated method stub
		
	}



	@Override
	public void visit(OWLDataSomeValuesFrom ce) {
		// TODO Auto-generated method stub
		
	}



	@Override
	public void visit(OWLDataAllValuesFrom ce) {
		// TODO Auto-generated method stub
		
	}



	@Override
	public void visit(OWLDataHasValue ce) {
		// TODO Auto-generated method stub
		
	}



	@Override
	public void visit(OWLDataMinCardinality ce) {
		// TODO Auto-generated method stub
		
	}



	@Override
	public void visit(OWLDataExactCardinality ce) {
		// TODO Auto-generated method stub
		
	}



	@Override
	public void visit(OWLDataMaxCardinality ce) {
		// TODO Auto-generated method stub
		
	}



	
}

