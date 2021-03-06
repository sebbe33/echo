package pt.uminho.haslab.echo.engine.kodkod;

import kodkod.ast.Decl;
import kodkod.util.nodes.PrettyPrinter;
import pt.uminho.haslab.echo.engine.ast.IDecl;
import pt.uminho.haslab.echo.engine.ast.IExpression;

/**
 * Created by tmg on 2/10/14.
 *
 */
class KodkodDecl implements IDecl{

    public final Decl decl;

    KodkodDecl(Decl decl) {
        this.decl = decl;
    }


    @Override
    public IExpression variable() {
        return new KodkodExpression(decl.variable());
    }

    @Override
    public String name() {
        return decl.variable().name();
    }



    @Override
    public String toString (){
        return PrettyPrinter.print(decl, 3);
    }
}

