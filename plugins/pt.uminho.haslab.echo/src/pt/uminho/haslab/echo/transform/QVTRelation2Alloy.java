package pt.uminho.haslab.echo.transform;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
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
import pt.uminho.haslab.echo.alloy.AlloyOptimizations;
import pt.uminho.haslab.echo.alloy.AlloyUtil;
import pt.uminho.haslab.echo.emf.OCLUtil;
import pt.uminho.haslab.echo.emf.URIUtil;
import edu.mit.csail.sdg.alloy4.Err;
import edu.mit.csail.sdg.alloy4compiler.ast.Decl;
import edu.mit.csail.sdg.alloy4compiler.ast.Expr;
import edu.mit.csail.sdg.alloy4compiler.ast.ExprHasName;
import edu.mit.csail.sdg.alloy4compiler.ast.Func;
import edu.mit.csail.sdg.alloy4compiler.ast.Sig;
import edu.mit.csail.sdg.alloy4compiler.ast.Sig.Field;

class QVTRelation2Alloy {

	/** the translator containing information about process EMF artifacts */
	private final EMF2Alloy translator;	
	
	/** the QVT Relation being transformed*/
	private Relation rel;
	/** the direction of the QVT Relation*/
	private TypedModel direction;
	/** whether the QVT Relation is being called at top level or not
	 * this is not the same as being a top relation */
	private boolean top;
	/** the parent top QVT Relation, null if top */
	private final QVTRelation2Alloy parentq;

	/** the root variables of the QVT Relation being translated*/
	private List<VariableDeclaration> rootvariables = new ArrayList<VariableDeclaration>();
	/** the target relation domain */
	private RelationDomain targetdomain;
	/** the source relation domains */
	private List<RelationDomain> sourcedomains = new ArrayList<RelationDomain>();

	/** the Alloy declarations of the variables occurring in the when constraint
	 * if non-top QVT Relation, does not contain root variables*/
	private Set<Decl> alloywhenvars = new HashSet<Decl>();
	/** the Alloy declarations of the variables occurring in the source domain but not in the when constraint
	 * if non-top QVT Relation, does not contain root variables*/
	private Set<Decl> alloysourcevars = new HashSet<Decl>();
	/** the Alloy declarations of the variables occurring in the target domain and where constraint but not in the source domains and the when constraint constraint
	 * if non-top QVT Relation, does not contain root variables*/
	private Set<Decl> alloytargetvars = new HashSet<Decl>();
	/** the Alloy declarations of the root variables
	 * null if top QVT Relation */
	private List<Decl> alloyrootvars = new ArrayList<Decl>();
	/** the Alloy declarations of all variables (union of the previous sets) */ 
	private Set<Decl> decls = new HashSet<Decl>();
	
	/** the current QVT function */
	private Func func;
	/** the variables of the current QVT function */
	private Map<String,List<ExprHasName>> argsvars = new LinkedHashMap<String,List<ExprHasName>>();
	/** the additional facts, defining the fields of internal QVT calls */
	private List<Func> fieldFacts = new ArrayList<Func>();

	
	
	QVTRelation2Alloy (QVTRelation2Alloy q2a, Relation rel, EMF2Alloy translator) throws ErrorTransform, ErrorAlloy, ErrorUnsupported {
		this (false,q2a,q2a.getDirection(),rel,translator);
	}

	QVTRelation2Alloy (TypedModel mdl, Relation rel, EMF2Alloy translator) throws ErrorTransform, ErrorAlloy, ErrorUnsupported {
		this (true,null,mdl,rel,translator);
	}

	/** 
	 * Constructs a new QVT Relation to Alloy translator.
	 * Translates a QVT Relation (top or non top) to Alloy in a given direction.
	 * @param rel the QVT Relation being translated
	 * @param direction the target direction of the transformation
	 * @param top whether the QVT Relation is top or not
	 * @throws ErrorTransform, 
	 * @throws ErrorUnsupported
	 * @throws ErrorAlloy
	 */
	QVTRelation2Alloy (Boolean top, QVTRelation2Alloy q2a, TypedModel direction, Relation rel, EMF2Alloy translator) throws ErrorTransform, ErrorAlloy, ErrorUnsupported {
		this.rel = rel;
		this.direction = direction;
		this.top = top;
		this.parentq = top?this:q2a;
		this.translator = translator;
		List<Decl> mdecls = new ArrayList<Decl>();
				
		for (TypedModel mdl : rel.getTransformation().getModelParameter()) {
			Decl d;
			String metamodeluri = URIUtil.resolveURI(mdl.getUsedPackage().get(0).getEPackage().eResource());
			try {
				d = translator.getMetamodelStateSig(metamodeluri).oneOf(mdl.getName());
			} catch (Err a) { throw new ErrorAlloy(a.getMessage()); }
			mdecls.add(d);
			if (argsvars.get(metamodeluri) == null) argsvars.put(metamodeluri, new ArrayList<ExprHasName>());
			argsvars.get(metamodeluri).add(d.get());
		}
		this.decls.addAll(mdecls);
		initDomains();
		initVariableDeclarationLists();
		Expr fact = calculateFact();
		AlloyOptimizations opt = new AlloyOptimizations(translator);
		if(translator.options.isOptimize()) {
			System.out.println("Pre-onepoint "+fact);
			fact = opt.trading(fact);
			fact = opt.onePoint(fact);
		}
		System.out.println("Pos-onepoint "+fact);
		//System.out.println(argsvars);
		try {
			if(top) {
				func = new Func(null, rel.getName()+"_"+direction.getName(), mdecls, null, fact);	
			}
			else {
				Field field = addRelationFields(fact,mdecls);
				func = new Func(null, rel.getName()+"_"+direction.getName(), mdecls, field.type().toExpr(), field);	
			}
		} catch (Err a) { throw new ErrorAlloy(a.getMessage()); }	
	}
	
	/** 
	 * Initializes the domain variables {@code this.sourcedomains}, {@code this.targetdomain} and {@code this.rootvariables}
	 * @throws ErrorTransform if some {@code Domain} is not {@code RelationDomain}
	 */
	private void initDomains () throws ErrorTransform {
		for (Domain dom : rel.getDomain())
			if (!(dom instanceof RelationDomain)) throw new ErrorTransform("Not a domain relation: "+dom);
			else {
				rootvariables.add(((RelationDomain) dom).getRootVariable());
				if (dom.getTypedModel().equals(direction)) targetdomain = (RelationDomain) dom;
				else sourcedomains.add((RelationDomain) dom);
			}
	}
	
	/** 
	 * Calculates the Alloy expression denoting the QVT Relation.
	 * Takes the shape "forall whenvars : when => (forall sourcevars : sourcedomain => (exists targetvars+wherevars : targetdomain && where))"
	 * @return the Alloy expression representing the QVT Relation
	 * @throws ErrorAlloy
	 * @throws ErrorTransform
	 * @throws ErrorUnsupported
	 */
	private Expr calculateFact() throws ErrorAlloy, ErrorTransform, ErrorUnsupported {

		Expr fact,sourceexpr = Sig.NONE.no(),targetexpr = Sig.NONE.no(),whereexpr = Sig.NONE.no(), whenexpr = Sig.NONE.no();
		Decl[] arraydecl;
		try {
			if (rel.getWhere() != null){
				OCL2Alloy ocltrans = new OCL2Alloy(parentq,translator,decls,argsvars, null);
				for (Predicate predicate : rel.getWhere().getPredicate()) {
					OCLExpression oclwhere = predicate.getConditionExpression();
					whereexpr = AlloyUtil.cleanAnd(whereexpr,ocltrans.oclExprToAlloy(oclwhere));
				}
			}
			targetexpr = AlloyUtil.cleanAnd(patternToExpr(targetdomain),whereexpr);
			if (alloytargetvars.size() == 1)
				targetexpr = targetexpr.forSome(alloytargetvars.iterator().next());	
			else if (alloytargetvars.size() > 1) {
				arraydecl = (Decl[]) alloytargetvars.toArray(new Decl[alloytargetvars.size()]);
				targetexpr = targetexpr.forSome(arraydecl[0],Arrays.copyOfRange(arraydecl, 1, arraydecl.length));	
			}

			for (RelationDomain dom : sourcedomains) 
				sourceexpr = AlloyUtil.cleanAnd(sourceexpr,patternToExpr(dom));
			fact = (sourceexpr.implies(targetexpr));
			
			if (alloysourcevars.size() == 1)
				fact = fact.forAll(alloysourcevars.iterator().next());	
			else if (alloysourcevars.size() > 1) {
				arraydecl = (Decl[]) alloysourcevars.toArray(new Decl[alloysourcevars.size()]);
				fact = fact.forAll(arraydecl[0],Arrays.copyOfRange(arraydecl, 1, arraydecl.length));	
			}
			
			if (rel.getWhen() != null){
				OCL2Alloy ocltrans = new OCL2Alloy(parentq,translator,decls,argsvars, null);
				for (Predicate predicate : rel.getWhen().getPredicate()) {
					OCLExpression oclwhen = predicate.getConditionExpression();
					whenexpr = AlloyUtil.cleanAnd(whenexpr,ocltrans.oclExprToAlloy(oclwhen));
				}
	
				fact = (whenexpr.implies(fact));	
				for (Decl d : alloywhenvars)
					fact = fact.forAll(d);
			}
			
		} catch (Err a) {throw new ErrorAlloy (a.getMessage());}
		
		return fact;
	}
	
	/** 
	 * Initializes the variable lists and generates the respective Alloy declarations.
	 * @throws ErrorTransform
	 * @throws ErrorAlloy
	 * @throws ErrorUnsupported
	 * @todo Support fom <code>CollectionTemplateExp</code>
	 */
	private void initVariableDeclarationLists() throws ErrorTransform, ErrorAlloy, ErrorUnsupported {
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
		
		OCL2Alloy ocltrans = new OCL2Alloy(parentq,translator,decls,argsvars,null);
		alloysourcevars = (Set<Decl>) ocltrans.variableListToExpr(sourcevariables,true);
		decls.addAll(alloysourcevars);
		alloywhenvars =  (Set<Decl>) ocltrans.variableListToExpr(whenvariables,true);
		decls.addAll(alloywhenvars);
		alloytargetvars = (Set<Decl>) ocltrans.variableListToExpr(targetvariables,true);
		decls.addAll(alloytargetvars);
		alloyrootvars = (List<Decl>) ocltrans.variableListToExpr(rootvariables,false);
	    if (!top) decls.addAll(alloyrootvars);
	}

	/** 
	 * Translates a {@code RelationDomain} to the correspondent Alloy expression through {@code OCL2Alloy} translator.
	 * @param domain The {@code RelationDomain} to be translated
	 * @return the Alloy expression representing {@code domain}
	 * @throws ErrorTransform
	 * @throws ErrorAlloy
	 * @throws ErrorUnsupported
	 */
	private Expr patternToExpr (RelationDomain domain) throws ErrorTransform, ErrorAlloy, ErrorUnsupported {
		String metamodeluri = URIUtil.resolveURI(domain.getTypedModel().getUsedPackage().get(0).getEPackage().eResource());
		//setting the respective statesig to first position
		if (argsvars.get(metamodeluri).size() == 2 && argsvars.get(metamodeluri).get(1).toString().equals(domain.getTypedModel().getName()))
			argsvars.get(metamodeluri).add(argsvars.get(metamodeluri).remove(0));
		//System.out.println("Translating relation domain: "+domain.getTypedModel() + argsvars);
		
		OCL2Alloy ocltrans = new OCL2Alloy(parentq,translator,decls,argsvars,null);
		DomainPattern pattern = domain.getPattern();
		ObjectTemplateExp temp = (ObjectTemplateExp) pattern.getTemplateExpression(); 		
		return ocltrans.oclExprToAlloy(temp);
	}
	
	/** 
	 * Generates the QVT Relation field.
	 * @return the Alloy field for this QVT Relation
	 * @throws ErrorAlloy
	 * @throws ErrorTransform
	 * @todo Support for n models
	 */
	private Field addRelationFields(Expr fact, List<Decl> mdecls) throws ErrorAlloy, ErrorTransform{
		Field field = null;
		try {
			//Sig type = (Sig) alloyrootvars.get(1).expr.type().toExpr();
			Sig s = (Sig) alloyrootvars.get(0).expr.type().toExpr();
			for (Field f : s.getFields()) {
				if (f.label.equals(AlloyUtil.relationFieldName(rel,direction)))
					field = f;
			}
			if (field == null) {
				field = s.addField(AlloyUtil.relationFieldName(rel,direction), /*type.setOf()*/Sig.UNIV.setOf());
				Expr e = field.equal(fact.comprehensionOver(alloyrootvars.get(0), alloyrootvars.get(1)));
				//System.out.println(alloyrootvars.get(0).expr+", "+alloyrootvars.get(1).expr);
				//System.out.println("DEBUG "+e);
				Func f = new Func(null, field.label+"def",mdecls,null,e);
				parentq.addFieldFunc(f);
			}
		} catch (Err a) {throw new ErrorAlloy (a.getMessage());}
		return field;
	}
	
	/** 
	 * Returns the Alloy function corresponding to this QVT Relation
	 * @return this.func
	 */
	Func getFunc() {
		return func;
	}
	
	/** 
	 * Adds a new Alloy functions defining a non-top QVT relation field
	 * should be used by descendants on parent
	 */
	void addFieldFunc(Func x) {
		fieldFacts.add(x);
	}
	
	/** 
	 * Returns the additional facts, defining the fields of internal non-top QVT calls
	 * @returns this.fieldFacts
	 */
	List<Func> getFieldFunc() {
		return fieldFacts;
	}

	TypedModel getDirection() {
		return direction;
	}

}