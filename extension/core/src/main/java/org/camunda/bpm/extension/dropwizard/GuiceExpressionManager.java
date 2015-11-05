package org.camunda.bpm.extension.dropwizard;

import org.camunda.bpm.engine.impl.el.ExpressionManager;
import org.camunda.bpm.engine.impl.el.ProcessApplicationBeanElResolverDelegate;
import org.camunda.bpm.engine.impl.el.ProcessApplicationElResolverDelegate;
import org.camunda.bpm.engine.impl.el.ReadOnlyMapELResolver;
import org.camunda.bpm.engine.impl.el.VariableScopeElResolver;
import org.camunda.bpm.engine.impl.javax.el.ArrayELResolver;
import org.camunda.bpm.engine.impl.javax.el.CompositeELResolver;
import org.camunda.bpm.engine.impl.javax.el.ELResolver;
import org.camunda.bpm.engine.impl.javax.el.ListELResolver;
import org.camunda.bpm.engine.impl.javax.el.MapELResolver;

import com.google.inject.Injector;

/**
 * Currently unused.
 */
public class GuiceExpressionManager extends ExpressionManager {

    protected Injector injector;

    public GuiceExpressionManager(Injector injector) {
        super();
        this.injector = injector;
    }

    @Override
    protected ELResolver createElResolver() {
        CompositeELResolver elResolver = new CompositeELResolver();
        elResolver.add(new VariableScopeElResolver());

        if (beans != null) {
            elResolver.add(new ReadOnlyMapELResolver(beans));
        }

        elResolver.add(new ProcessApplicationElResolverDelegate());

        elResolver.add(new ArrayELResolver());
        elResolver.add(new ListELResolver());
        elResolver.add(new MapELResolver());
        elResolver.add(new ProcessApplicationBeanElResolverDelegate());

        elResolver.add(new GuiceElResolver(injector));
        System.out.println("resolver injector: "+injector);
        System.out.println("added guice resolver");

        return elResolver;
    }

}
