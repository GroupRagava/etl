package com.linkedpipes.plugin.transformer.tabular;

import com.linkedpipes.etl.component.api.Component;
import com.linkedpipes.etl.component.api.service.ExceptionFactory;
import com.linkedpipes.etl.component.api.service.ProgressReport;
import com.linkedpipes.etl.dataunit.sesame.api.rdf.WritableChunkedStatements;
import com.linkedpipes.etl.dataunit.system.api.files.FilesDataUnit;
import com.linkedpipes.etl.dataunit.system.api.files.FilesDataUnit.Entry;
import com.linkedpipes.etl.executor.api.v1.exception.LpException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Chunked version of tabular.
 */
public class TabularChunked implements Component.Sequential {

    private static final Logger LOG
            = LoggerFactory.getLogger(TabularChunked.class);

    @Component.InputPort(id = "InputFiles")
    public FilesDataUnit inputFilesDataUnit;

    @Component.OutputPort(id = "OutputRdf")
    public WritableChunkedStatements outputRdfDataUnit;

    @Component.Configuration
    public TabularConfiguration configuration;

    @Component.Inject
    public ExceptionFactory exceptionFactory;

    @Component.Inject
    public ProgressReport progressReport;

    @Override
    public void execute() throws LpException {
        final RdfOutput output = new RdfOutput(outputRdfDataUnit,
                configuration.getChunkSize());
        LOG.info("CHUNK SIZE: {}", configuration.getChunkSize());
        final Parser parser = new Parser(configuration, exceptionFactory);
        final Mapper mapper = new Mapper(output, configuration,
                ColumnFactory.createColumnList(configuration, exceptionFactory),
                exceptionFactory);
        mapper.initialize(null);
        progressReport.start(inputFilesDataUnit.size());
        for (Entry entry : inputFilesDataUnit) {
            output.onFileStart();
            final String table;
            switch (configuration.getEncodeType()) {
                case "emptyHost":
                    table = "file:///" + entry.getFileName();
                    break;
                default:
                    table = "file://" + entry.getFileName();
                    break;
            }
            mapper.onTableStart(table, null);
            try {
                parser.parse(entry, mapper);
            } catch (IOException | ColumnAbstract.MissingColumnValue ex) {
                throw exceptionFactory.failure("Can't process file: {}",
                        entry.getFileName(), ex);
            }
            mapper.onTableEnd();
            output.onFileEnd();
            progressReport.entryProcessed();
        }
        progressReport.done();
    }

}
