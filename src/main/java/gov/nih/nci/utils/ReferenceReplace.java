package gov.nih.nci.utils;


import static gov.nih.nci.ui.NCIEditTabConstants.DEP_ASSOC;
import static gov.nih.nci.ui.NCIEditTabConstants.DEP_IN_ASSOC;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.protege.editor.owl.model.OWLModelManager;
import org.semanticweb.owlapi.model.AddAxiom;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAnnotationPropertyRangeAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationSubject;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLClassExpressionVisitor;
import org.semanticweb.owlapi.model.OWLDataAllValuesFrom;
import org.semanticweb.owlapi.model.OWLDataExactCardinality;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataHasValue;
import org.semanticweb.owlapi.model.OWLDataMaxCardinality;
import org.semanticweb.owlapi.model.OWLDataMinCardinality;
import org.semanticweb.owlapi.model.OWLDataSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLLiteral;
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
import org.semanticweb.owlapi.model.RemoveAxiom;

import gov.nih.nci.ui.NCIEditTab;

public class ReferenceReplace implements OWLClassExpressionVisitor {

	private OWLClass source;
	
	private OWLClass target;
	
	private OWLClassExpression currentExpression;

	private OWLModelManager owlModelManager;
	
	private OWLDataFactory dataFact;

	private OWLOntology ont;
	
	HashMap<OWLAnnotationProperty, Set<String>> fixups = new HashMap<OWLAnnotationProperty, Set<String>>();	

	public ReferenceReplace(OWLModelManager man) {
		owlModelManager = man;
		dataFact = man.getOWLDataFactory();
	}
	
	public List<OWLOntologyChange> retargetRefs(OWLClass from, OWLClass to) { 
		// TODO: finish !! same idea as retirement but modify rather than annotate

		List<OWLOntologyChange> changes = new ArrayList<OWLOntologyChange>();
		

		source = from;
		target = to;

		ont = owlModelManager.getActiveOntology();
		
		

		Set<OWLAxiom> axioms = ont.getReferencingAxioms(from);
		for (OWLAxiom ax : axioms) {
			
			
			if (ax instanceof OWLEquivalentClassesAxiom) {
				changes.add(new RemoveAxiom(ont, ax));
				Set<OWLClassExpression> newExps = new HashSet<OWLClassExpression>();
				for (OWLClassExpression desc : ((OWLEquivalentClassesAxiom) ax).getClassExpressions()) {
					this.setExpression(desc);
					desc.accept(this);
					newExps.add(getNewExpression());
				}
				OWLEquivalentClassesAxiom newAx = dataFact.getOWLEquivalentClassesAxiom(newExps);
				changes.add(new AddAxiom(ont, newAx));
				
			} else if (ax instanceof OWLSubClassOfAxiom) {

				OWLClassExpression par = ((OWLSubClassOfAxiom) ax).getSuperClass();
				OWLClassExpression child = ((OWLSubClassOfAxiom) ax).getSubClass();

				OWLClassExpression newPar = null;

				if (par.isAnonymous()) {
					this.setExpression(par);
					par.accept(this);
					newPar = this.getNewExpression();

				} else if (par.asOWLClass().equals(source)) {
					newPar = target;

					String val = child.asOWLClass().getIRI().getShortForm();					
					System.out.println("OLD_CHILD:" + val );
				}
				if (newPar != null) {
					changes.add(new RemoveAxiom(ont, ax));
					OWLSubClassOfAxiom newAx = dataFact.getOWLSubClassOfAxiom(child, newPar);
					changes.add(new AddAxiom(ont, newAx));
				}
				
			}
		}
		
		for (OWLAnnotationAssertionAxiom ax : ont.getAxioms(AxiomType.ANNOTATION_ASSERTION)) {
            com.google.common.base.Optional<IRI> valueIRI = ax.getValue().asIRI();
            if (valueIRI.isPresent()) {
                if (valueIRI.get().equals(from.getIRI())) {
                	changes.add(new RemoveAxiom(ont, ax));
                	OWLAnnotationAssertionAxiom newax = dataFact.getOWLAnnotationAssertionAxiom(ax.getProperty(), ax.getSubject(),
                			to.getIRI());
                	changes.add(new AddAxiom(ont, newax));
                	
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
	
	private OWLClassExpression newExpression;
	
	private OWLClassExpression getNewExpression() {
		return newExpression;
	}

	
	@Override
	public void visit(OWLClass ce) {
		newExpression = ce;
		
	}
	
	@Override
	public void visit(OWLObjectIntersectionOf ce) {
		Set<OWLClassExpression> newConjs = new HashSet<OWLClassExpression>();
		
		Set<OWLClassExpression> conjs = ce.asConjunctSet();
		for (OWLClassExpression c : conjs) {
			setExpression(c);
			c.accept(this);
			newConjs.add(getNewExpression());
		}
		newExpression = dataFact.getOWLObjectIntersectionOf(newConjs);
	}

	@Override
	public void visit(OWLObjectUnionOf ce) {
		Set<OWLClassExpression> newDisjs = new HashSet<OWLClassExpression>();
		
		Set<OWLClassExpression> disjs = ce.asDisjunctSet();
		for (OWLClassExpression c : disjs) {
			setExpression(c);
			c.accept(this);
			newDisjs.add(getNewExpression());
		}
		newExpression = dataFact.getOWLObjectIntersectionOf(newDisjs);
		
	}

	@Override
	public void visit(OWLObjectComplementOf ce) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(OWLObjectSomeValuesFrom ce) {
		if (ce.getFiller().asOWLClass().equals(source)) {
			System.out.println("OSVF: " + ce);
			
			newExpression = dataFact.getOWLObjectSomeValuesFrom(ce.getProperty(), target);
			
			
			
		} else {
			newExpression = ce;
		}		
	}

	@Override
	public void visit(OWLObjectAllValuesFrom ce) {
		if (ce.getFiller().asOWLClass().equals(source)) {
			System.out.println("OSVF: " + ce);
			
			newExpression = dataFact.getOWLObjectSomeValuesFrom(ce.getProperty(), target);
			
			
			
			
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

