package org.camunda.bpm.extension.dropwizard;

import java.beans.FeatureDescriptor;
import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.camunda.bpm.engine.impl.javax.el.ELContext;
import org.camunda.bpm.engine.impl.javax.el.ELResolver;

import com.google.inject.Injector;

/**
 * Currently unused.
 */
public class GuiceElResolver extends ELResolver {

    protected static Injector injector;

    public GuiceElResolver(Injector injector) {
        GuiceElResolver.injector = injector;
    }

    //We only need to see the currently processed Objects in our Thread, that
    //prevents multithread issues without synchronization
    private static ThreadLocal currentlyProcessedThreadLocal = new ThreadLocal()
    {
        @Override
        protected Object initialValue()
        {
            return new LinkedList();
        }
    };

    //Im not sure if the synchronized lists seriously slow down the whole EL
    //resolving process
    private static List<WeakReference> alreadyInjectedObjects = Collections.synchronizedList( new LinkedList() );

        @Override
    public Object getValue( ELContext context, Object base, Object property )
    {

        //if the list of currently processed property objects doesnt exist for this
        //thread, create it
        List<Object> currentlyProcessedPropertyObjects = (List<Object>) currentlyProcessedThreadLocal.get();

        //Handle only root inquiries, we wont handle property resolving
        if ( base != null )
        {
            return null;
        }

        //checking if this property is currently processed, if so ignore it -> prevent
        //endless loop
        if ( checkIfObjectIsContained( property,
                currentlyProcessedPropertyObjects ) )
        {
            return null;
        }

        //add the to-be-resolved object to the currently processed list
        currentlyProcessedPropertyObjects.add( property );

        //now we can savely invoke the getValue() Method of the composite EL
        //resolver, we wont process it again
        Object resolvedObj = context.getELResolver().getValue( context, base,
                property );

        //ok, we got our result, remove the object from the currently processed list
        removeObject( property, currentlyProcessedPropertyObjects );

        if ( resolvedObj == null )
        {
            return null;
        }

        //ok we got an object
        context.setPropertyResolved( true );

        //check if the object was already injected
        if ( !checkIfObjectIsContainedWeak( resolvedObj, alreadyInjectedObjects ) )
        {
            injector.injectMembers( resolvedObj );
            //prevent a second injection by adding it as weakreference to our list
            alreadyInjectedObjects.add( new WeakReference( resolvedObj ) );
        }

        return resolvedObj;
    }

    /**
     * This method will search for an object in a Weak List. If there are any
     * WeakReferences on the way that were removed by the garbage collection
     * we will remove them from this list
     * @param object
     * @param list
     * @return
     */
    private boolean checkIfObjectIsContainedWeak( Object object,
                                                  List<WeakReference> list )
    {
        for ( int i = 0; i < list.size(); i++ )
        {
            WeakReference curReference = list.get( i );
            Object curObject = curReference.get();
            if ( curObject == null )
            {
                //ok, there is are slight chance that could go wrong, if you
                //have to prevent a double injection by all means, you might
                //want to add a synchronized block here
                list.remove( i );
                i--;
            }
            else
            {
                if ( curObject == object )
                {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * checks if an object is contained in a collection (really the same object '==' not equals)
     * @param object
     * @param collection
     * @return
     */
    private boolean checkIfObjectIsContained( Object object,
                                              Collection collection )
    {
        for ( Object curObject : collection )
        {
            if ( object == curObject )
            {
                return true;
            }
        }
        return false;
    }

    /**
     * removes an object from a list. really removes the given instance, not an other
     * object that fits equals
     * @param object
     * @param list
     */
    private void removeObject( Object object, List list )
    {
        for ( int i = 0; i < list.size(); i++ )
        {
            if ( list.get( i ) == object )
            {
                list.remove( i );
            }
        }
    }

    @Override
    public Class<?> getType( ELContext context, Object base, Object property )
    {
        return null;
    }

    @Override
    public void setValue( ELContext context, Object base, Object property,
                          Object value )
    {
    }

    @Override
    public boolean isReadOnly( ELContext context, Object base, Object property )
    {
        return false;
    }

    @Override
    public Iterator<FeatureDescriptor> getFeatureDescriptors( ELContext context,
                                                              Object base )
    {
        return null;
    }

    @Override
    public Class<?> getCommonPropertyType( ELContext context, Object base )
    {
        return null;
    }
}
