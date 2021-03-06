package pt.uminho.haslab.mde.transformation.atl;

import org.eclipse.core.runtime.Path;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.util.EcoreUtil;

import pt.uminho.haslab.echo.EchoError;
import pt.uminho.haslab.echo.EchoReporter;
import pt.uminho.haslab.echo.ErrorParser;
import pt.uminho.haslab.echo.ErrorUnsupported;
import pt.uminho.haslab.mde.MDEManager;
import pt.uminho.haslab.mde.transformation.ERelation;

import java.util.ArrayList;
import java.util.List;

/**
 * An embedding of an EMF ATL relation in Echo.
 * 
 * @author nmm
 * @version 0.4 05/03/2014
 */
public class EATLRelation implements ERelation {
	private EObject relation;
	private List<EATLModelDomain> domains = new ArrayList<>();
	private EATLTransformation transformation;
	
	public EATLRelation(EATLTransformation transformation, EObject rule) throws ErrorUnsupported, ErrorParser {
		if (rule.eClass().getName().equals("MatchedRule") || rule.eClass().getName().equals("LazyMatchedRule") )
			this.relation = rule;
		else throw new ErrorUnsupported("Bad atl");
		this.transformation = transformation;
		EStructuralFeature inmdls = relation.eClass().getEStructuralFeature("inPattern");
		EStructuralFeature outmdls = relation.eClass().getESuperTypes().get(0).getEStructuralFeature("outPattern");
		EObject obj = (EObject) relation.eGet(inmdls);
		domains.add(new EATLModelDomain(this, obj));
		obj = (EObject) relation.eGet(outmdls);
		domains.add(new EATLModelDomain(this, obj));
	}

	@Override
	public boolean isTop(){
		return !relation.eClass().getName().equals("LazyMatchedRule");
	}

	@Override
	public EATLTransformation getTransformation() throws EchoError {
		return transformation;
	}

	@Override
	public String getName() {
		EStructuralFeature name = relation.eClass().getEStructuralFeature("name");
		return (String) relation.eGet(name);
	}

	@Override
	public List<EATLModelDomain> getDomains() {
		return domains;
	}

	@Override
	public EATLPredicate getPost() {
		EStructuralFeature outmdls = relation.eClass().getESuperTypes().get(0).getEStructuralFeature("outPattern");
		EObject post = (EObject) relation.eGet(outmdls);
		EATLPredicate x = new EATLPredicate();

		EStructuralFeature elems = post.eClass().getEStructuralFeature("elements");
		EList<EObject> objs = (EList<EObject>) post.eGet(elems);
		EObject var = objs.get(0);
		EStructuralFeature bindings = var.eClass().getEStructuralFeature("bindings");
		for (EObject bd : (EList<EObject>) var.eGet(bindings))
			x.addCondition(bd);
		
		return x;

	}

	@Override
	public EATLPredicate getPre() {
		return new EATLPredicate();
	}

}
