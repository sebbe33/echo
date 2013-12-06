package pt.uminho.haslab.echo.transform;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.qvtd.pivot.qvtrelation.RelationalTransformation;

import pt.uminho.haslab.echo.EchoError;
import pt.uminho.haslab.echo.EchoSolution;
import pt.uminho.haslab.echo.EngineFactory;

/**
 * Created with IntelliJ IDEA.
 * User: tmg
 * Date: 10/23/13
 * Time: 6:26 PM
 */
public abstract class EchoTranslator {

    private static EchoTranslator instance;

    public static EchoTranslator getInstance() {
        return instance;
    }

    public static void init(EngineFactory factory){
        instance = factory.createTranslator();
    }

    public abstract void writeAllInstances(EchoSolution solution, String metaModelUri, String modelUri) throws EchoError;

    public abstract void writeInstance(EchoSolution solution, String modelUri) throws EchoError;

    public abstract String getMetaModelFromModelPath(String path);

    public abstract void translateMetaModel(EPackage metaModel) throws EchoError;

    public abstract void remMetaModel(String metaModelUri);

    public abstract void translateModel(EObject model) throws EchoError;

    public abstract void remModel(String modelUri);

    public abstract void translateQVT(RelationalTransformation qvt) throws EchoError;

    public abstract Object getQVTFact(String qvtUri);

    public abstract void translateATL(EObject atl, EObject mdl1, EObject mdl2) throws EchoError;

    public abstract boolean remQVT(String qvtUri);

    public abstract boolean hasMetaModel(String metaModelUri);

    public abstract boolean hasModel(String modelUri);
}
