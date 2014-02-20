package pt.uminho.haslab.echo.engine;

import java.util.List;

import pt.uminho.haslab.echo.EchoError;
import pt.uminho.haslab.echo.ErrorParser;
import pt.uminho.haslab.echo.ErrorUnsupported;
import pt.uminho.haslab.echo.engine.ast.IDecl;
import pt.uminho.haslab.echo.engine.ast.IExpression;
import pt.uminho.haslab.mde.model.EVariable;

/**
 * Auxiliary context for the translation to engine.
 * Mainly used for variable declaration management.
 * Variables are uniquely identified by name.
 *
 * @author nmm, tmg
 * @version 0.4 14/02/2014
 */
public interface IContext {

	/**
	 * Retrieve the expression representing a variable.
	 * @param name the name of the variable
	 * @return the matching variable
	 */
    IExpression getVar(String name);

    /**
     * Adds a new variable to the context.
     * @param decl the new variable declaration
     */
    void addVar(IDecl decl);

    /**
     * Adds a new variable to the context, with an associated model.
     * @param decl the new variable declaration
     * @param modelState the model to which the variable belongs
     */
    void addVar(IDecl decl, String modelState);

    /**
     * Removes a variable from the context.
     * @param name the variable name
     */
    void remove(String name);

    /**
     * Retrieves the owning model of a variable.
     * @param name the name of the variable
     * @return the owning model
     */
    String getVarModel(String name);
    
    /**
     * Returns all declared variables in the context.
     * @return the variables
     */
    List<String> getVars();

    /**
     * Retrieves the declaration of a variable.
     * @param var the variable
     * @param addContext if the variable should be stored in the context
     * @return the matching declaration
     * @throws EchoError
     */
    IDecl getDecl(EVariable var, boolean addContext) throws EchoError;

    /**
     * Returns the expression representing a property call.
     * Should take into consideration the current pre-state and current model.
     * @param metaModelID the owning metamodel
     * @param className the class name
     * @param propName the property name
     * @return the expression representing a property
     */
    IExpression getPropExpression(String metaModelID, String className, String propName);

    /**
     * Returns the expression representing a class call.
     * Should take into consideration the current isPre and current model.
     * @param metaModelID the owning metamodel
     * @param className the class name
     * @return the expression representing a class
     * @throws ErrorParser
     * @throws ErrorUnsupported
     */
    IExpression getClassExpression(String metaModelID, String className) throws ErrorParser, ErrorUnsupported;
    
    /**
     * The current model of the context.
     * Should be used in <code>getClassExpression</code> and <code>getPropExpression</code>
     * @param object
     */
	void setCurrentModel(String object);

    /**
     * If the context is currently in pre-state.
     * Should be used in <code>getClassExpression</code> and <code>getPropExpression</code>
     * @param object
     */
	void setCurrentPre(boolean pre);
	
}
