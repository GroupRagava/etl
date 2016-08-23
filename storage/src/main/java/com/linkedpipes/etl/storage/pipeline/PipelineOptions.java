package com.linkedpipes.etl.storage.pipeline;

import com.linkedpipes.etl.storage.pipeline.importer.ImportFacade;
import com.linkedpipes.etl.storage.pipeline.updater.UpdaterFacade;
import com.linkedpipes.etl.storage.rdf.PojoLoader;
import org.openrdf.model.IRI;
import org.openrdf.model.Literal;
import org.openrdf.model.Value;
import org.openrdf.model.impl.SimpleValueFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Contains settings for pipeline oprations.
 *
 * @author Petr Škoda
 */
class PipelineOptions implements PojoLoader.Loadable, ImportFacade.Options,
        UpdaterFacade.Options {

    public static final IRI TYPE;

    static {
        TYPE = SimpleValueFactory.getInstance().createIRI(
                "http://linkedpipes.com/ontology/UpdateOptions");
    }

    /**
     * Labels that should be used as pipeline labels, override labels
     * from pipeline definition.
     */
    private final List<Literal> labels = new ArrayList<>(2);

    /**
     * If true pipeline is local and there is no need to update resources.
     */
    private boolean local = true;

    /**
     * Target IRI of the pipeline.
     */
    private IRI pipelineIri;

    @Override
    public List<Literal> getLabels() {
        return labels;
    }

    public boolean isLocal() {
        return local;
    }

    @Override
    public IRI getPipelineIri() {
        return pipelineIri;
    }

    public void setPipelineIri(IRI pipelineIri) {
        this.pipelineIri = pipelineIri;
    }

    @Override
    public PojoLoader.Loadable load(String predicate, Value value)
            throws PojoLoader.CantLoadException {
        switch (predicate) {
            case "http://etl.linkedpipes.com/ontology/local":
                local = ((Literal) value).booleanValue();
                break;
            case "http://www.w3.org/2004/02/skos/core#prefLabel":
                if (value instanceof Literal) {
                    labels.add((Literal)value);
                }
                break;
            case "http://etl.linkedpipes.com/ontology/pipeline":
                if (value instanceof IRI) {
                    pipelineIri = (IRI)value;
                }
        }
        return null;
    }

}
