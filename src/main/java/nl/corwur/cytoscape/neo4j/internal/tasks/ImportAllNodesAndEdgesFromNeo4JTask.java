package nl.corwur.cytoscape.neo4j.internal.tasks;

import nl.corwur.cytoscape.neo4j.internal.Services;
import nl.corwur.cytoscape.neo4j.internal.neo4j.CypherQuery;
import nl.corwur.cytoscape.neo4j.internal.tasks.importgraph.DefaultImportStrategy;

import java.text.MessageFormat;

/**
 * This class imports all nodes and edges from neo4j into cytoscape.
 */
public class ImportAllNodesAndEdgesFromNeo4JTask extends AbstractImportTask {

    public ImportAllNodesAndEdgesFromNeo4JTask(Services services, String networkName, String visualStyleTitle) {
        super(
                services,
                networkName,
                visualStyleTitle,
                new DefaultImportStrategy(),
                CypherQuery.builder().query(formatQuery(networkName)).build());
    }

    private static String formatQuery(String network) {
        return MessageFormat.format(TaskConstants.MATCH_ALL_NODES_AND_EDGES, network);
    }
}
