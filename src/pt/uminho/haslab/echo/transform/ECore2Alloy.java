package pt.uminho.haslab.echo.transform;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.emf.ecore.EAttribute;
//import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EEnum;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EEnumLiteral;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;

import pt.uminho.haslab.echo.ErrorAlloy;
import pt.uminho.haslab.echo.ErrorTransform;
import pt.uminho.haslab.echo.ErrorUnsupported;

import edu.mit.csail.sdg.alloy4.Err;
import edu.mit.csail.sdg.alloy4compiler.ast.Attr;
import edu.mit.csail.sdg.alloy4compiler.ast.Decl;
import edu.mit.csail.sdg.alloy4compiler.ast.Expr;
import edu.mit.csail.sdg.alloy4compiler.ast.ExprConstant;
import edu.mit.csail.sdg.alloy4compiler.ast.Sig;
import edu.mit.csail.sdg.alloy4compiler.ast.Sig.PrimSig;

public class ECore2Alloy {

	private HashMap<EClassifier,PrimSig> mapClassSig = new HashMap<EClassifier,PrimSig>();
	private HashMap<EEnumLiteral,PrimSig> mapLitSig = new HashMap<EEnumLiteral,PrimSig>();
	private HashMap<EStructuralFeature,Expr> mapSfField = new HashMap<EStructuralFeature,Expr>();
	private HashMap<PrimSig,Expr> mapSigState = new HashMap<PrimSig,Expr>();
	private final EPackage pack;
	private final PrimSig state;
	private List<PrimSig> sigList;
	
	public ECore2Alloy(EPackage p, PrimSig statesig) throws ErrorUnsupported, ErrorAlloy, ErrorTransform{
		state = statesig;
		pack = p;
		sigList = makeSigList();
	}
	
	public HashMap<PrimSig,Expr> getMapSigState()
	{
		//return new HashMap<PrimSig,Expr>(mapSigState);
		return mapSigState;
	}
	
	public HashMap<EEnumLiteral,PrimSig> getMapLitSig()
	{
		return mapLitSig;
	}
	
	public PrimSig getState()
	{
		return state;
	}
	
	public HashMap<EStructuralFeature,Expr> getMapSfField()
	{
		//return new HashMap<EStructuralFeature,Expr>(mapSfField);
		return mapSfField;
	}
	
	public HashMap<EClassifier,PrimSig> getMapClassSig()
	{
		//return new HashMap<EClassifier,PrimSig>(mapClassSig);
		return mapClassSig;
	}
	
	public List<PrimSig> getSigList()
	{
		return new ArrayList<PrimSig>(sigList);
	}
	/*
	private void processAttribute(EAttribute attr,PrimSig ec) throws Err
	{
		EClassifier type = attr.getEType();
		PrimSig sigType = mapClassSig.get(type);
		if(sigType == null)
		{
			sigType =  new PrimSig(prefix + type.getName());
			mapClassSig.put(type, sigType);
			sigList.add(sigType);
			mapSigState.put(sigType,sigType.addField(prefix+type.getName().toLowerCase(), state.setOf()));
		}
		Expr field = ec.addField(prefix + attr.getName(),sigType.product(state));
		mapSfField.put(attr,field);
		Expr fact= field.join(state.decl.get());
		Expr bound = mapSigState.get(ec).join(state.decl.get());
		bound = bound.any_arrow_one(mapSigState.get(sigType).join(state.decl.get()));
		fact = fact.in(bound);
		fact = fact.forAll(state.decl);
		ec.addFact(fact);
	}
	*/
	private void processAttributes(List<EAttribute> attrList,PrimSig ec) throws ErrorUnsupported, ErrorAlloy
	{
		Expr field=null, fact=null;
		for(EAttribute attr : attrList)
			try{
				if (attr.getEType() instanceof EEnum)
				{
					PrimSig sigType = mapClassSig.get(attr.getEType());
					field = ec.addField(AlloyUtil.pckPrefix(pack.getName(),attr.getName()),sigType.product(state));
					mapSfField.put(attr,field);
					fact = field.join(state.decl.get());
					Expr bound = mapSigState.get(ec).join(state.decl.get()).any_arrow_one(sigType);
					fact = fact.in(bound);
					fact = fact.forAll(state.decl);
					ec.addFact(fact);				
				}else if(attr.getEType().getName().equals("EBoolean"))
				{
					field = ec.addField(AlloyUtil.pckPrefix(pack.getName(),attr.getName()),state.setOf());
					mapSfField.put(attr,field);
					
				}else if(attr.getEType().getName().equals("EString"))
				{
					field = ec.addField(AlloyUtil.pckPrefix(pack.getName(),attr.getName()),Sig.STRING.product(state));
					mapSfField.put(attr,field);
					fact = field.join(state.decl.get());
					Expr bound = mapSigState.get(ec).join(state.decl.get()).any_arrow_one(Sig.STRING);
					fact = fact.in(bound);
					fact = fact.forAll(state.decl);
					ec.addFact(fact);
				}else if(attr.getEType().getName().equals("EInt"))
				{
					field = ec.addField(AlloyUtil.pckPrefix(pack.getName(),attr.getName()),Sig.SIGINT.product(state));
					mapSfField.put(attr,field);
					fact = field.join(state.decl.get());
					Expr bound = mapSigState.get(ec).join(state.decl.get()).any_arrow_one(Sig.SIGINT);
					fact = fact.in(bound);
					fact = fact.forAll(state.decl);
					ec.addFact(fact);
				}else
					throw new ErrorUnsupported("Primitive type for attribute not supported.","ECore2Alloy",attr.getEType());
			} catch (Err a) {throw new ErrorAlloy (a.getMessage(),"ECore2Alloy",attr);}			
			

	}
	
	
	private PrimSig makeSig(EClass ec) throws ErrorUnsupported, ErrorAlloy, ErrorTransform
	{
		PrimSig res = mapClassSig.get(ec);
		if(res == null) {
			try {
				PrimSig parent = null;
				List<EClass> superTypes = null;
			
				superTypes = ec.getESuperTypes();
				if(superTypes.size() > 1) throw new ErrorTransform("Multiple inheritance not allowed.","ECore2Alloy",ec);
				if(!superTypes.isEmpty())
				{
					parent = mapClassSig.get(superTypes.get(0));
					if(parent == null) throw new ErrorTransform("Parent class not found.","ECore2Alloy",superTypes);	
				}
				if(ec.isAbstract())
					res = new PrimSig(AlloyUtil.pckPrefix(pack.getName(),ec.getName()),parent,Attr.ABSTRACT);
				else res = new PrimSig(AlloyUtil.pckPrefix(pack.getName(),ec.getName()),parent);
				mapSigState.put(res,res.addField(AlloyUtil.pckPrefix(pack.getName(),ec.getName()).toLowerCase(),state.setOf()));
				mapClassSig.put(ec, res);
				processAttributes(ec.getEAllAttributes(),res);
				sigList.add(res);
			} catch (Err a) {throw new ErrorAlloy (a.getMessage(),"ECore2Alloy",ec);}	
		}
		return res;
	}
	
	private void processReferences(List<EReference> eAllReferences, PrimSig parent) throws ErrorAlloy, ErrorTransform {
		for(EReference r : eAllReferences)
			processReference(r,parent);
	}

	private void processReference(EReference r, PrimSig srcsig) throws ErrorAlloy, ErrorTransform {
		EClass type = r.getEReferenceType();
		PrimSig trgsig = mapClassSig.get(type);
		Expr field;
		try{field = srcsig.addField(AlloyUtil.pckPrefix(pack.getName(),r.getName()),trgsig.product(state));}
		catch (Err a) {throw new ErrorAlloy (a.getMessage(),"ECore2Alloy",r);}
		mapSfField.put(r, field);
		// processing opposite references
		Expr opField = null;
		EReference op = r.getEOpposite();
		Decl s = state.decl;
		if(op!=null) {
			opField = mapSfField.get(op);
			if(opField != null)
				try{srcsig.addFact(field.join(s.get()).equal(opField.join(s.get()).transpose()).forAll(state.decl));}
				catch (Err a) {throw new ErrorAlloy (a.getMessage(),"ECore2Alloy",r);}
		}
		// processing multiplicities
		Expr fact;
		try{
			if(r.getLowerBound() > 0) {
				Decl d = AlloyUtil.localStateSig(srcsig,s.get()).oneOf("x");
				fact = (d.get()).join(field.join(s.get())).cardinality().gte(ExprConstant.makeNUMBER(r.getLowerBound())).forAll(s,d);
				srcsig.addFact(fact);
			}
			if(r.getUpperBound() != -1){
				Decl d = AlloyUtil.localStateSig(srcsig,s.get()).oneOf("x");
				fact = (d.get()).join(field.join(s.get())).cardinality().lte(ExprConstant.makeNUMBER(r.getUpperBound())).forAll(s,d);
				srcsig.addFact(fact);
				System.out.println("DEBUG2: "+fact+" , "+d.expr+ " , "+s.expr);
			}
			if(r.isContainment()){
				Decl d = AlloyUtil.localStateSig(trgsig,s.get()).oneOf("x");
				fact = ((field.join(s.get())).join(d.get())).one().forAll(s,d);
				trgsig.addFact(fact);
			}
			Expr parState = mapSigState.get(srcsig);
			Expr sTypeState = mapSigState.get(trgsig);		
			srcsig.addFact(field.join(s.get()).in(parState.join(s.get()).product(sTypeState.join(s.get()))).forAll(state.decl));
		} catch (Err a) {throw new ErrorAlloy (a.getMessage(),"ECore2Alloy",r);}
	}


	private List<PrimSig> makeSigList () throws ErrorUnsupported, ErrorAlloy, ErrorTransform
	{
		List<EClassifier> list = pack.getEClassifiers();
		List<EClass> classList = new LinkedList<EClass>();
		List<EDataType> dataList = new ArrayList<EDataType>();
		List<EEnum> enumList = new ArrayList<EEnum>();
		sigList = new ArrayList<PrimSig>();
		
		for(EClassifier e: list)
		{
			if (e instanceof EClass)
				classList.add((EClass)e);
			else if (e instanceof EEnum)
				enumList.add((EEnum) e);
			else if (e instanceof EDataType)
				dataList.add((EDataType) e);
		}
		
		processEEnum(enumList);
		processClass(classList);
		
		return sigList;
	}
	
	
	private void processEEnumLiterals(List<EEnumLiteral> el,PrimSig parent) throws ErrorAlloy 
	{
		PrimSig litSig = null;
		for(EEnumLiteral lit: el)
		{
			try { litSig = new PrimSig(AlloyUtil.pckPrefix(pack.getName(),lit.getLiteral()),parent,Attr.ONE); }
			catch (Err a) {throw new ErrorAlloy(a.getMessage(),"ECore2Alloy",lit);}
			mapLitSig.put(lit, litSig);
			sigList.add(litSig);
		}
	} 
	
	private List<PrimSig> processEEnum(List<EEnum> list) throws ErrorAlloy 
	{
		PrimSig enumSig = null;
		for(EEnum en: list)
		{
			try{ enumSig = new PrimSig(AlloyUtil.pckPrefix(pack.getName(),en.getName()),Attr.ABSTRACT);}
			catch (Err a) {throw new ErrorAlloy(a.getMessage(),"ECore2Alloy",en);}
			sigList.add(enumSig);
			mapClassSig.put(en, enumSig);
			//mapSigState.put(enumSig, enumSig.addField(prefix + en.getName().toLowerCase(),state.setOf()));
			processEEnumLiterals(en.getELiterals(),enumSig);
		}
		
		return sigList;
	}
	
	private List<PrimSig> processClass(List<EClass> classList) throws ErrorUnsupported, ErrorAlloy, ErrorTransform
	{
		LinkedList<EClass> list = new LinkedList<EClass>(classList);
		EClass ec = list.poll();
		Sig itSig = null;
		while(ec !=null)
		{
			itSig = makeSig(ec);
			if(itSig == null)
				list.offer(ec);
			ec = list.poll();			
		}
		
		for(EClass e: classList)
			processReferences(e.getEAllReferences(),mapClassSig.get(e));
		
		return sigList;
	}
	
	public Expr getDeltaExpr(PrimSig m, PrimSig n) throws ErrorAlloy{
		Expr result = ExprConstant.makeNUMBER(0);
		for (Expr e : mapSigState.values()) {
			Expr aux = (((e.join(m)).minus(e.join(n))).plus((e.join(n)).minus(e.join(m)))).cardinality();
			result = result.iplus(aux);
		}
		for (Expr e : mapSfField.values()) {
			Expr aux = (((e.join(m)).minus(e.join(n))).plus((e.join(n)).minus(e.join(m)))).cardinality();
			result = result.iplus(aux);
		}
		return result;
	}
	
	
}
