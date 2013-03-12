package pt.uminho.haslab.echo.transform;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.ocl.examples.pivot.OCLExpression;
import org.eclipse.ocl.examples.pivot.VariableDeclaration;
import org.eclipse.qvtd.pivot.qvtbase.Domain;
import org.eclipse.qvtd.pivot.qvtbase.Predicate;
import org.eclipse.qvtd.pivot.qvtbase.TypedModel;
import org.eclipse.qvtd.pivot.qvtrelation.DomainPattern;
import org.eclipse.qvtd.pivot.qvtrelation.Relation;
import org.eclipse.qvtd.pivot.qvtrelation.RelationDomain;
import org.eclipse.qvtd.pivot.qvttemplate.ObjectTemplateExp;
import org.eclipse.qvtd.pivot.qvttemplate.TemplateExp;

import pt.uminho.haslab.echo.ErrorAlloy;
import pt.uminho.haslab.echo.ErrorTransform;
import pt.uminho.haslab.echo.ErrorUnsupported;

import edu.mit.csail.sdg.alloy4.Err;
import edu.mit.csail.sdg.alloy4compiler.ast.Decl;
import edu.mit.csail.sdg.alloy4compiler.ast.Expr;
import edu.mit.csail.sdg.alloy4compiler.ast.Func;
import edu.mit.csail.sdg.alloy4compiler.ast.Sig;
import edu.mit.csail.sdg.alloy4compiler.ast.Sig.PrimSig;

public class QVTRelation2Alloy {

	// variables occuring in the when constraint
	private Set<Decl> alloywhenvars = new HashSet<Decl>();
	// variables occuring in source domains but not in the when constraint
	private Set<Decl> alloysourcevars = new HashSet<Decl>();
	// variables occuring in target domain and where constraint, but not in the when constraint or source domains
	private Set<Decl> alloytargetvars = new HashSet<Decl>();
	// Declarations of quantified variables; needed for respective variable occurrences (union of the above)
	private List<Decl> alloyrootvars = new ArrayList<Decl>();
	// Declarations of quantified variables; needed for respective variable occurrences (union of the above)
	private Set<Decl> decls = new HashSet<Decl>();
	
	private List<VariableDeclaration> rootvariables = new ArrayList<VariableDeclaration>();

	
	// the alloy signatures of each metamodel
	private Map<String,PrimSig> statesigs = new HashMap<String,PrimSig>();
	private Map<String,List<Sig>> modelsigs = new HashMap<String,List<Sig>>();
	
	// separated target and source domains
	private RelationDomain targetdomain;
	private List<RelationDomain> sourcedomains = new ArrayList<RelationDomain>();
	// the QVT relation being transformed
	private Relation rel;
	// tue target metamodel
	private TypedModel direction;

	// the Alloy expression rising from this relations
	final Expr fact;
	final Func func;

	public QVTRelation2Alloy (TypedModel direction, Relation rel, Map<String,PrimSig> statesigs, Map<String,List<Sig>> modelsigs) throws ErrorTransform, ErrorAlloy, ErrorUnsupported {
		this.modelsigs = modelsigs;
		this.statesigs = statesigs;
		this.rel = rel;
		this.direction = direction;
		
		initDomains ();
		initVariableDeclarationLists(true);

		func = null;
		fact = calculateFact();
		//try {func = new Func(null,"aa",alloyrootvars,null,fact); }
		//catch (Err a) {throw new ErrorAlloy (a.getMessage(),"QVTRelation2Alloy",fact);}
	}
	
	// this one takes a list of declarations as an extra argument: used with relation calls, since some variables are already quantified
	public QVTRelation2Alloy (TypedModel direction, Relation rel, Map<String,PrimSig> statesigs, Map<String,List<Sig>> modelsigs, Set<Decl> prevdecls) throws ErrorTransform, ErrorAlloy, ErrorUnsupported {
		this.modelsigs = modelsigs;
		this.statesigs = statesigs;
		this.rel = rel;
		this.direction = direction;
		
		initDomains ();
		//decls.addAll(prevdecls);
		initVariableDeclarationLists(false);
		
		fact = calculateFact();
		try {func = new Func(null,rel.getName(),alloyrootvars,null,fact); }
		catch (Err a) {throw new ErrorAlloy (a.getMessage(),"QVTRelation2Alloy",fact);}
	}
	
	// separating target domain from the rest
	private void initDomains () throws ErrorTransform {
		for (Domain dom : rel.getDomain()) {
			// "Domains" of QVT Relations must be "RelationDomains"
			if (!(dom instanceof RelationDomain)) 
				throw new ErrorTransform("Not a domain relation.","QVTRelation2Alloy",dom);
			else {
				rootvariables.add(((RelationDomain) dom).getRootVariable());
				if (dom.getTypedModel().equals(direction)) targetdomain = (RelationDomain) dom;
				else sourcedomains.add((RelationDomain) dom);
			}
		}
	}
	// calculates the final fact
	private Expr calculateFact() throws ErrorAlloy, ErrorTransform, ErrorUnsupported {

		Expr fact,sourceexpr = Sig.NONE.no(),targetexpr = Sig.NONE.no(),whereexpr = Sig.NONE.no(), whenexpr = Sig.NONE.no();

		// calculates the target expression
		try {
			if (rel.getWhere() != null){
				OCL2Alloy ocltrans = new OCL2Alloy(direction,statesigs,modelsigs,decls);
				for (Predicate predicate : rel.getWhere().getPredicate()) {
					OCLExpression oclwhere = predicate.getConditionExpression();
					whereexpr = AlloyUtil.cleanAnd(whereexpr,ocltrans.oclExprToAlloy(oclwhere));
				}
			}
			targetexpr = AlloyUtil.cleanAnd(patternToExpr(targetdomain),whereexpr);
			for (Decl d : alloytargetvars)
				targetexpr = targetexpr.forSome(d);
			
			// calculates the source expression
			for (RelationDomain dom : sourcedomains) 
				sourceexpr = AlloyUtil.cleanAnd(sourceexpr,patternToExpr(dom));
			
			// calculates the source => target expression
			fact = (sourceexpr.implies(targetexpr));
			for (Decl d : alloysourcevars)
				fact = fact.forAll(d);	
			
			// calculates the when expression
			if (rel.getWhen() != null){
				OCL2Alloy ocltrans = new OCL2Alloy(direction,statesigs,modelsigs,decls);
				for (Predicate predicate : rel.getWhen().getPredicate()) {
					OCLExpression oclwhen = predicate.getConditionExpression();
					whenexpr = AlloyUtil.cleanAnd(whenexpr,ocltrans.oclExprToAlloy(oclwhen));
				}
	
				// calculates the when => source => target expression
				fact = (whenexpr.implies(fact));	
				for (Decl d : alloywhenvars)
					fact = fact.forAll(d);
			}
			
		} catch (Err a) {throw new ErrorAlloy (a.getMessage(),"QVTRelation2Alloy",sourceexpr);}
		
		return fact;
	}
	
	// separates source from target variables (eventually also when and where variables)
	private void initVariableDeclarationLists(boolean top) throws ErrorTransform, ErrorAlloy, ErrorUnsupported {
		TemplateExp temp;
		Set<VariableDeclaration> whenvariables = new HashSet<VariableDeclaration>();
		Set<VariableDeclaration> sourcevariables = new HashSet<VariableDeclaration>();
		Set<VariableDeclaration> targetvariables = new HashSet<VariableDeclaration>();
		
		if (rel.getWhen() != null)
			for (Predicate predicate : rel.getWhen().getPredicate()) {
				OCLExpression oclwhen = predicate.getConditionExpression();
				whenvariables.addAll(OCLUtil.variablesOCLExpression(oclwhen));
			}
		
		for (RelationDomain dom : sourcedomains) {
			temp = dom.getPattern().getTemplateExpression();
			sourcevariables.addAll(OCLUtil.variablesOCLExpression(temp));
		}
		sourcevariables.removeAll(whenvariables);
		
		temp = targetdomain.getPattern().getTemplateExpression();
		// No support for CollectionTemplateExp
		targetvariables.addAll(OCLUtil.variablesOCLExpression(temp));
		if (rel.getWhere() != null)
			for (Predicate predicate : rel.getWhere().getPredicate()) {
				OCLExpression oclwhere = predicate.getConditionExpression();
				targetvariables.addAll(OCLUtil.variablesOCLExpression(oclwhere));
			}
		targetvariables.removeAll(sourcevariables);
		targetvariables.removeAll(whenvariables);

		if (!top) {
			whenvariables.removeAll(rootvariables);
			targetvariables.removeAll(rootvariables);
			sourcevariables.removeAll(rootvariables);
		}
		
		// calculates the source variables declarations (quantifications)
		alloysourcevars = (Set<Decl>) OCL2Alloy.variableListToExpr(sourcevariables,modelsigs,true);
		decls.addAll(alloysourcevars);
		// calculates the when variables declarations (quantifications)
		alloywhenvars =  (Set<Decl>) OCL2Alloy.variableListToExpr(whenvariables,modelsigs,true);
		decls.addAll(alloywhenvars);
		// calculates the target variables declarations (quantifications)
		alloytargetvars = (Set<Decl>) OCL2Alloy.variableListToExpr(targetvariables,modelsigs,true);
		decls.addAll(alloytargetvars);
		// calculates the target variables declarations (quantifications)
		alloyrootvars = (List<Decl>) OCL2Alloy.variableListToExpr(rootvariables,modelsigs,false);
	    if (!top) decls.addAll(alloyrootvars);

		/*System.out.println(rel.getName()+" Target variables: "+targetvariables);
		System.out.println(rel.getName()+" Source variables: "+sourcevariables);
		System.out.println(rel.getName()+" When variables: "+whenvariables);
		System.out.println(rel.getName()+" Root variables: "+rootvariables);*/

		
	}

	// calls OCL2Alloy on the domain pattern
	private Expr patternToExpr (RelationDomain domain) throws ErrorTransform, ErrorAlloy, ErrorUnsupported {
		OCL2Alloy ocltrans = new OCL2Alloy(domain.getTypedModel(),statesigs,modelsigs,decls);

		DomainPattern pattern = domain.getPattern();
		ObjectTemplateExp temp = (ObjectTemplateExp) pattern.getTemplateExpression(); 
		
		return ocltrans.oclExprToAlloy(temp);
	}
	
	public Expr getFact() {
		return fact;
	}
	public Func getFunc() {
		return func;
	}
}
