/**
 * Copyright (C) 2011 Akiban Technologies Inc.
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses.
 */

package com.akiban.server.service.servicemanager;

import com.akiban.server.service.servicemanager.configuration.ServiceBinding;
import com.akiban.util.ArgumentValidation;
import com.akiban.util.Exceptions;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.ProvisionException;
import com.google.inject.Scopes;
import com.google.inject.TypeLiteral;
import com.google.inject.matcher.Matchers;
import com.google.inject.spi.InjectionListener;
import com.google.inject.spi.Message;
import com.google.inject.spi.TypeEncounter;
import com.google.inject.spi.TypeListener;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

public final class Guicer {
    // Guicer interface

    public Collection<Class<?>> directlyRequiredClasses() {
        return directlyRequiredClasses;
    }

    public void stopAllServices(ServiceLifecycleActions<?> withActions) {
        try {
            stopServices(withActions, null);
        } catch (Exception e) {
            throw new RuntimeException("while stopping services", e);
        }
    }

    public <T> T get(Class<T> serviceClass, ServiceLifecycleActions<?> withActions) {
        final T instance;
        try {
            instance = _injector.getInstance(serviceClass);
        } catch (ProvisionException e) {
            for (Message message: e.getErrorMessages()) {
                if (message.getMessage().contains("circular dependency")) {
                    throw new CircularDependencyException(e);
                }
            }
            throw e;
        }
        return startService(instance, withActions);
    }

    public boolean serviceIsStarted(Class<?> serviceClass) {
        return services.contains(serviceClass);
    }

    public boolean isRequired(Class<?> interfaceClass) {
        return directlyRequiredClasses.contains(interfaceClass);
    }

    // public class methods

    public static Guicer forServices(Collection<ServiceBinding> serviceBindings, InjectionHandler<?> injectionHandler)
    throws ClassNotFoundException
    {
        ArgumentValidation.notNull("bindings", serviceBindings);
        ArgumentValidation.notNull("injection handler", injectionHandler);
        return new Guicer(serviceBindings, injectionHandler);
    }

    // private methods

    private Guicer(Collection<ServiceBinding> serviceBindings, InjectionHandler<?> injectionHandler)
    throws ClassNotFoundException
    {
        List<Class<?>> localDirectlyRequiredClasses = new ArrayList<Class<?>>();
        List<ResolvedServiceBinding> resolvedServiceBindings = new ArrayList<ResolvedServiceBinding>();

        for (ServiceBinding serviceBinding : serviceBindings) {
            ResolvedServiceBinding resolvedServiceBinding = new ResolvedServiceBinding(serviceBinding);
            resolvedServiceBindings.add(resolvedServiceBinding);
            if (serviceBinding.isDirectlyRequired()) {
                localDirectlyRequiredClasses.add(resolvedServiceBinding.serviceInterfaceClass());
            }
        }
        Collections.sort(localDirectlyRequiredClasses, BY_CLASS_NAME);
        directlyRequiredClasses = Collections.unmodifiableCollection(localDirectlyRequiredClasses);

        this.services = Collections.synchronizedSet(new LinkedHashSet<Object>());

        AbstractModule module = new ServiceBindingsModule(
                resolvedServiceBindings,
                new DelegatingInjectionHandler(services, injectionHandler)
        );
        _injector = Guice.createInjector(module);
    }

    private static class DelegatingInjectionHandler extends InjectionHandler<Object> {

        @Override
        protected void handle(Object instance) {
            services.add(instance);
            delegate.afterInjection(instance);
        }

        DelegatingInjectionHandler(Set<Object> services, InjectionHandler<?> delegate)
        {
            super(Object.class);
            this.services = services;
            this.delegate = delegate;
        }

        private final Set<Object> services;
        private final InjectionHandler<?> delegate;
    }

    private <T,S> T startService(T instance, ServiceLifecycleActions<S> withActions) {
        synchronized (services) {
            if (services.contains(instance)) {
                return instance;
            }
            if (withActions == null) {
                services.add(instance);
                return instance;
            }

            S service = withActions.castIfActionable(instance);
            if (service != null) {
                try {
                    withActions.onStart(service);
                    services.add(service);
                } catch (Exception e) {
                    try {
                        stopServices(withActions, e);
                    } catch (Exception e1) {
                        e = e1;
                    }
                    throw new ProvisionException("While starting service " + instance.getClass(), e);
                }
            }
        }
        return instance;
    }

    private void stopServices(ServiceLifecycleActions<?> withActions, Exception initialCause) throws Exception {
        List<Throwable> exceptions = tryStopServices(withActions, initialCause);
        if (!exceptions.isEmpty()) {
            if (exceptions.size() == 1) {
                throw Exceptions.throwAlways(exceptions.get(0));
            }
            for (Throwable t : exceptions) {
                t.printStackTrace();
            }
            throw new Exception("Failure(s) while shutting down services: " + exceptions, exceptions.get(0));
        }
    }

    private <S> List<Throwable> tryStopServices(ServiceLifecycleActions<S> withActions, Exception initialCause) {
        ListIterator<?> reverseIter;
        synchronized (services) {
            reverseIter = new ArrayList<Object>(services).listIterator(services.size());
        }
        List<Throwable> exceptions = new ArrayList<Throwable>();
        if (initialCause != null) {
            exceptions.add(initialCause);
        }
        while (reverseIter.hasPrevious()) {
            try {
                Object serviceObject = reverseIter.previous();
                synchronized (services) {
                    services.remove(serviceObject);
                }
                if (withActions != null) {
                    S service = withActions.castIfActionable(serviceObject);
                    if (service != null) {
                        withActions.onShutdown(service);
                    }
                }
            } catch (Throwable t) {
                exceptions.add(t);
            }
        }
        // TODO because our dependency graph is created via Service.start() invocations, if service A uses service B
        // in stop() but not start(), and service B has already been shut down, service B will be resurrected. Yuck.
        // I don't know of a good way around this, other than by formalizing our dependency graph via constructor
        // params (and thus removing ServiceManagerImpl.get() ). Until this is resolved, simplest is to just shrug
        // our shoulders and not check
//        synchronized (lock) {
//            assert services.isEmpty() : services;
//        }
        return exceptions;
    }

    // object state

    private final Collection<Class<?>> directlyRequiredClasses;
    private final Set<Object> services;
    private final Injector _injector;

    // consts

    private static final Comparator<? super Class<?>> BY_CLASS_NAME = new Comparator<Class<?>>() {
        @Override
        public int compare(Class<?> o1, Class<?> o2) {
            return o1.getName().compareTo(o2.getName());
        }
    };

    // nested classes

    private static final class ResolvedServiceBinding {

        // ResolvedServiceBinding interface

        public Class<?> serviceInterfaceClass() {
            return serviceInterfaceClass;
        }

        public Class<?> serviceImplementationClass() {
            return serviceImplementationClass;
        }

        public ResolvedServiceBinding(ServiceBinding serviceBinding) throws ClassNotFoundException {
            this.serviceInterfaceClass = Class.forName(serviceBinding.getInterfaceName());
            this.serviceImplementationClass = Class.forName(serviceBinding.getImplementingClassName());
            if (!this.serviceInterfaceClass.isAssignableFrom(this.serviceImplementationClass)) {
                throw new IllegalArgumentException(this.serviceInterfaceClass + " is not assignable from "
                        + this.serviceImplementationClass);
            }
        }

        // object state
        private final Class<?> serviceInterfaceClass;
        private final Class<?> serviceImplementationClass;
    }

    private static final class ServiceBindingsModule extends AbstractModule {
        @Override
        // we use unchecked, raw Class, relying on the invariant established by ResolvedServiceBinding's ctor
        @SuppressWarnings("unchecked")
        protected void configure() {
            for (ResolvedServiceBinding binding : bindings) {
                Class unchecked = binding.serviceInterfaceClass();
                bind(unchecked).to(binding.serviceImplementationClass()).in(Scopes.SINGLETON);
            }
            binder().bindListener(Matchers.any(), new MyTypeListener(injectionHandler));
            binder().disableCircularProxies();
        }

        // ServiceBindingsModule interface

        private ServiceBindingsModule(Collection<ResolvedServiceBinding> bindings, InjectionHandler<?> injectionHandler)
        {
            this.bindings = bindings;
            this.injectionHandler = injectionHandler;
        }

        // object state

        private final Collection<ResolvedServiceBinding> bindings;
        private final InjectionHandler<?> injectionHandler;
    }

    private static class MyTypeListener implements TypeListener {
        @Override
        public <I> void hear(TypeLiteral<I> type, TypeEncounter<I> encounter) {
            if (injectionHandler.classIsInteresting(type.getRawType())) {
                encounter.register(injectionHandler);
            }
        }

        MyTypeListener(InjectionHandler<?> injectionHandler) {
            this.injectionHandler = injectionHandler;
        }

        private final InjectionHandler<?> injectionHandler;
    }

    abstract static class InjectionHandler<T> implements InjectionListener<Object> {

        @Override
        final public void afterInjection(Object injectee) {
            if (classIsInteresting(injectee.getClass())) {
                handle(targetClass.cast(injectee));
            }
        }

        final public boolean classIsInteresting(Class<?> type) {
            return targetClass.isAssignableFrom(type) && ! (type.getPackage().getName().startsWith("com.google."));
        }

        protected abstract void handle(T instance);

        protected InjectionHandler(Class<T> targetClass) {
            this.targetClass = targetClass;
        }

        private final Class<T> targetClass;
    }

    static interface ServiceLifecycleActions<T> {
        void onStart(T service) throws Exception;
        void onShutdown(T service) throws Exception;

        /**
         * Cast the given object to the actionable type if possible, or return {@code null} otherwise.
         * @param object the object which may or may not be actionable
         * @return the object reference, correctly casted; or null
         */
        T castIfActionable(Object object);
    }

}
