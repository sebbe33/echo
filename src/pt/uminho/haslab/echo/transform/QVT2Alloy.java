package pt.uminho.haslab.echo.transform;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.qvtd.pivot.qvtbase.Rule;
import org.eclipse.qvtd.pivot.qvtbase.Transformation;
import org.eclipse.qvtd.pivot.qvtbase.TypedModel;
import org.eclipse.qvtd.pivot.qvtrelation.Relation;

import pt.uminho.haslab.echo.ErrorAlloy;
import pt.uminho.haslab.echo.ErrorTransform;
import pt.uminho.haslab.echo.ErrorUnsupported;

import edu.mit.csail.sdg.alloy4compiler.ast.Expr;
import edu.mit.csail.sdg.alloy4compiler.ast.Sig;

public class QVT2Alloy {

	public final Map<String,Expr> fact = new HashMap<String,Expr>();
	
	public QVT2Alloy (Map<String,Expr> statesigs, Map<String,List<Sig>> modelsigs, Transformation qvt) throws ErrorTransform, ErrorAlloy, ErrorUnsupported {
		
		for (Rule rel : qvt.getRule())
			if (!(rel instanceof Relation)) throw new ErrorTransform ("Rule not a relation.","QVT2Alloy",rel);
			else {
				if (((Relation) rel).isIsTopLevel()) {
					for (TypedModel mdl : qvt.getModelParameter()) {
						QVTRelation2Alloy trans = new QVTRelation2Alloy((Relation) rel,mdl,true,statesigs,modelsigs);
						fact.put(rel.getName()+"_"+mdl.getName(),trans.getFact());
					}
				}
			}
	}
	

	public Map<String,Expr> getFact() {
		return fact;
	}

}
