package com.linkedpipes.plugin.loader.graphstoreprotocol;

/**
 *
 * @author Petr Škoda
 */
class GraphStoreProtocolVocabulary {

    private static final String PREFIX
            = "http://plugins.linkedpipes.com/ontology/l-graphStoreProtocol#";

    public static final String CONFIG_CLASS = PREFIX + "Configuration";

    public static final String HAS_SELECT = PREFIX + "endpointSelect";

    public static final String HAS_UPDATE = PREFIX + "endpointUpdate";

    public static final String HAS_CRUD = PREFIX + "endpointCRUD";

    public static final String HAS_GRAPH = PREFIX + "graph";

    public static final String HAS_TYPE = PREFIX + "repositoryType";

    public static final String HAS_AUTH = PREFIX + "authentification";

    public static final String HAS_USER = PREFIX + "user";

    public static final String HAS_PASSWORD = PREFIX + "password";

    private GraphStoreProtocolVocabulary() {
    }

}
