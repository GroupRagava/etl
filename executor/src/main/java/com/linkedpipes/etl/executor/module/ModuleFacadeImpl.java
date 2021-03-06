package com.linkedpipes.etl.executor.module;

import com.linkedpipes.etl.executor.Configuration;
import com.linkedpipes.etl.executor.api.v1.Plugin;
import com.linkedpipes.etl.executor.api.v1.RdfException;
import com.linkedpipes.etl.executor.api.v1.component.Component;
import com.linkedpipes.etl.executor.api.v1.component.ComponentFactory;
import com.linkedpipes.etl.executor.api.v1.dataunit.DataUnitFactory;
import com.linkedpipes.etl.executor.api.v1.dataunit.ManageableDataUnit;
import com.linkedpipes.etl.executor.api.v1.vocabulary.LINKEDPIPES;
import com.linkedpipes.etl.executor.pipeline.PipelineDefinition;
import org.osgi.framework.*;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Scope;
import org.springframework.context.event.ContextStartedEvent;
import org.springframework.context.event.ContextStoppedEvent;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.*;
import java.util.function.Consumer;

@Service
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
class ModuleFacadeImpl implements ModuleFacade,
        ApplicationListener<ApplicationEvent> {

    private static final Logger LOG
            = LoggerFactory.getLogger(ModuleFacadeImpl.class);

    private static final String EXPORT_PACKAGE_LIST = ""
            + "" // javax additional - FIND BUNDLE WITH THIS !
            + "javax.servlet;version=\"2.4.0\","
            + "javax.servlet.http;version=\"2.4.0\","
            + "" // slf4j
            + "org.slf4j;version=\"1.7.12\","
            + "org.slf4j.helpers;version=\"1.7.12\","
            + "org.slf4j.spi;version=\"1.7.12\","
            + "" // logback
            + "ch.qos.logback.classic;version=\"1.1.3\","
            + "ch.qos.logback.classic.joran;version=\"1.1.3\","
            + "ch.qos.logback.core;version=\"1.1.3\","
            + "ch.qos.logback.core.joran.action;version=\"1.1.3\","
            + "ch.qos.logback.core.joran.spi;version=\"1.1.3\","
            + "ch.qos.logback.core.rolling;version=\"1.1.3\","
            + "ch.qos.logback.core.util;version=\"1.1.3\","
            + "" // log4j
            + "org.apache.log4j;version=\"1.7.12\","
            + "org.apache.log4j.helpers;version=\"1.7.12\","
            + "org.apache.log4j.api;version=\"1.7.12\","
            + "org.apache.log4j.xml;version=\"1.7.12\","
            + "" // core API
            + "com.linkedpipes.etl.executor.api.v1.exception,"
            + "com.linkedpipes.etl.executor.api.v1,"
            + "com.linkedpipes.etl.executor.api.v1.component,"
            + "com.linkedpipes.etl.executor.api.v1.dataunit,"
            + "com.linkedpipes.etl.executor.api.v1.event,"
            + "com.linkedpipes.etl.executor.api.v1.rdf,"
            + "com.linkedpipes.etl.executor.api.v1.vocabulary";

    private Framework framework;

    private final List<Bundle> libraries = new LinkedList<>();

    private final Map<String, Bundle> components = new HashMap<>();

    @Autowired
    private Configuration configuration;

    @Autowired
    private AbstractApplicationContext appContext;

    protected void start() {
        // http://www.eclipse.org/gemini/blueprint/documentation/reference/1.0.2.RELEASE/html/blueprint.html
        // http://felix.apache.org/documentation/tutorials-examples-and-presentations/apache-felix-osgi-tutorial/apache-felix-tutorial-example-1.html
        final FrameworkFactory frameworkFactory
                = ServiceLoader.load(FrameworkFactory.class).iterator().next();

        final Map<String, String> config = new HashMap<>();
        config.put(Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA,
                EXPORT_PACKAGE_LIST);
        config.put(Constants.FRAMEWORK_STORAGE,
                configuration.getOsgiStorageDirectory());
        config.put(Constants.FRAMEWORK_STORAGE_CLEAN,
                Constants.FRAMEWORK_STORAGE_CLEAN_ONFIRSTINIT);

        framework = frameworkFactory.newFramework(config);
        try {
            framework.start();
        } catch (BundleException ex) {
            appContext.stop();
            throw new RuntimeException("Can't start framework!", ex);
        }

        final BundleContext context = framework.getBundleContext();

        // Load libraries.
        scanDirectory(configuration.getOsgiLibDirectory(), (file) -> {
            if (file.getPath().endsWith(".jar")) {
                LOG.debug("Installing: {}", file);
                try {
                    libraries.add(context.installBundle(
                            file.toURI().toString()));
                } catch (BundleException ex) {
                    appContext.stop();
                    throw new RuntimeException(ex);
                }
            }
        });

        // Start library bundles.
        libraries.forEach((bundle) -> {
            try {
                bundle.start();
            } catch (BundleException ex) {
                LOG.error("Can't start bundle: {}", bundle.getSymbolicName(),
                        ex);
                appContext.stop();
                throw new RuntimeException(ex);
            }
        });
    }

    protected void stop() {
        LOG.debug("Closing ...");
        if (framework != null) {
            try {
                framework.stop();
                framework.waitForStop(0);
            } catch (BundleException ex) {
                LOG.error("Can't stop OSGI framework.", ex);
            } catch (InterruptedException ex) {
                LOG.error("Interrupted when waiting for framework to close.",
                        ex);
            }
        }
        LOG.debug("Closing ... done");
    }

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        if (event instanceof ContextStartedEvent) {
            start();
        } else if (event instanceof ContextStoppedEvent) {
            // This is Felix dependant:
            // FelixDispatchQueue is thread used by felix, it's created as non
            // daemon. For this reason we need to manually stop the framework
            // and not wait on OnDestroy call.
            stop();
        }
    }

    @Override
    public Collection<Plugin.PipelineListener> getPipelineListeners()
            throws ModuleException {
        return getServices(Plugin.PipelineListener.class);
    }

    /**
     * TODO This function require storage to be up and running. We should add an option how to pre-load jar files from directory.
     *
     * @param definition
     * @param resource
     * @param context Context given to new component.
     * @return
     */
    @Override
    public Component getComponent(PipelineDefinition definition,
            String resource, Component.Context context)
            throws ModuleException {
        // We need to get path to jar file first.
        final Map<String, String> componentInfo;
        final List<Map<String, String>> resultList
                = definition.executeSelect(
                "SELECT ?path WHERE {\n"
                        + "  <" + resource + "> <"
                        + LINKEDPIPES.HAS_JAR + "> ?path .\n"
                        + "}");
        if (resultList.size() != 1) {
            throw new ModuleException("Invalid component definition : " +
                    "invalid number of JAR paths: " + resultList.size());
        }
        componentInfo = resultList.get(0);
        // Then we need to load or get the bundle.
        final String jarFileUri = componentInfo.get("path");
        // TODO Enable reload for updated component versions?
        if (!components.containsKey(jarFileUri)) {
            // We do not download directly from the IRI, instead
            // we ask storage.
            final String bundleIri;
            try {
                bundleIri = configuration.getStorageAddress() +
                        "/api/v1/jars/file?iri=" +
                        URLEncoder.encode(jarFileUri, "UTF-8");
            } catch (UnsupportedEncodingException ex) {
                throw new ModuleException("Invalid encoding!", ex);
            }
            LOG.info("Loading jar file from: {}", jarFileUri);
            final Bundle bundle;
            try {
                bundle = framework.getBundleContext().installBundle(bundleIri);
            } catch (BundleException ex) {
                throw new ModuleException(
                        "Can't load bundle!", ex);
            }
            try {
                bundle.start();
            } catch (BundleException ex) {
                throw new ModuleException("Can't start bundle!", ex);
            }
            components.put(jarFileUri, bundle);
        }
        final BundleContext componentContext
                = components.get(jarFileUri).getBundleContext();
        // Use manager to get the component representation.
        for (ComponentFactory factory : getServices(ComponentFactory.class)) {
            try {
                return factory.create(definition, resource,
                        definition.getDefinitionGraph(),
                        componentContext, context);
            } catch (RdfException ex) {
                throw new ModuleException("Invalid bundle detected!", ex);
            }
        }
        throw new ModuleException(
                "No factory can handle given type of jar file.");
    }

    @Override
    public ManageableDataUnit getDataUnit(PipelineDefinition definition,
            String subject)
            throws ModuleException {
        for (DataUnitFactory factory : getServices(DataUnitFactory.class)) {
            try {
                final ManageableDataUnit dataUnit = factory.create(definition,
                        subject, definition.getDefinitionGraph());
                if (dataUnit != null) {
                    return dataUnit;
                }
            } catch (RdfException ex) {
                LOG.error("Can't create data unit.", ex);
            }
        }
        throw new ModuleException(
                "No factory can instantiate required data unit.");
    }

    protected void scanDirectory(File root, Consumer<File> consumer) {
        if (root.listFiles() == null) {
            // No content detected.
            return;
        }
        for (File file : root.listFiles()) {
            if (file.isFile()) {
                consumer.accept(file);
            } else if (file.isDirectory()) {
                scanDirectory(file, consumer);
            }
        }
    }

    /**
     * @param <T>
     * @param clazz
     * @return Services of given interface.
     */
    public <T> Collection<T> getServices(Class<T> clazz)
            throws ModuleException {
        final BundleContext context = framework.getBundleContext();
        try {
            final Collection<ServiceReference<T>> references
                    = context.getServiceReferences(clazz, null);
            final List<T> serviceList = new ArrayList<>(references.size());
            for (ServiceReference<T> reference : references) {
                serviceList.add(context.getService(reference));
            }
            LOG.trace("Detected {} of type {}", serviceList.size(),
                    clazz.getSimpleName());
            return serviceList;
        } catch (InvalidSyntaxException ex) {
            throw new ModuleException("Can't get service list!", ex);
        }
    }

}
