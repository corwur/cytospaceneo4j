package nl.corwur.cytoscape.neo4j.internal.tasks;

import nl.corwur.cytoscape.neo4j.internal.Services;
import nl.corwur.cytoscape.neo4j.internal.graph.Graph;
import nl.corwur.cytoscape.neo4j.internal.graph.commands.Command;
import nl.corwur.cytoscape.neo4j.internal.graph.implementation.GraphImplementation;
import nl.corwur.cytoscape.neo4j.internal.neo4j.CypherQuery;
import nl.corwur.cytoscape.neo4j.internal.neo4j.CypherQueryReader;
import nl.corwur.cytoscape.neo4j.internal.neo4j.Neo4jClientException;
import nl.corwur.cytoscape.neo4j.internal.neo4j.Neo4jGraphImplementation;
import nl.corwur.cytoscape.neo4j.internal.tasks.exportneo4j.ExportDifference;
import nl.corwur.cytoscape.neo4j.internal.tasks.exportneo4j.ExportNetworkConfiguration;
import nl.corwur.cytoscape.neo4j.internal.tasks.exportneo4j.ExportNew;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;

import java.io.StringReader;
import java.util.Optional;

/**
 * This class exports a cytoscape graph to neo4j.
 */
public class ExportNetworkToNeo4jTask extends AbstractTask {

    private final Services services;
    private final ExportNetworkConfiguration exportNetworkConfiguration;

    public ExportNetworkToNeo4jTask(Services services, ExportNetworkConfiguration exportNetworkConfiguration) {
        this.services = services;
        this.exportNetworkConfiguration = exportNetworkConfiguration;
    }

    @Override
    public void run(TaskMonitor taskMonitor) {
        try {
            taskMonitor.setTitle("Export network to Neo4j");
            taskMonitor.setProgress(0);
            taskMonitor.setStatusMessage("Exporting network to Neo4j");
            CyNetwork cyNetwork = services.getCyApplicationManager().getCurrentNetwork();
            if (cyNetwork == null) {
                taskMonitor.showMessage(TaskMonitor.Level.WARN, "No network selected");
            } else {
                GraphImplementation graphImplementation = Neo4jGraphImplementation.create(
                        services.getNeo4jClient(),
                        TaskConstants.NEO4J_PROPERTY_CYTOSCAPE_NETWORK,
                        exportNetworkConfiguration.getNodeLabel().getLabel()
                );
                Command command = getCypherQuery(cyNetwork).map(cypherQuery -> {
                    try {
                        Graph grapInDb = services.getNeo4jClient().getGraph(cypherQuery);
                        return ExportDifference.create(grapInDb, cyNetwork, graphImplementation).compute();
                    } catch (Neo4jClientException e) {
                        throw new IllegalStateException(e);
                    }
                }).orElseGet(() -> ExportNew.create(cyNetwork, graphImplementation).compute());

                taskMonitor.setStatusMessage("Updating graph");
                // @TODO proper export: Label names from shared names and correct edge names and properties
                command.execute();
                taskMonitor.showMessage(TaskMonitor.Level.INFO, "Export completed");
            }

        } catch (Exception e) {
            taskMonitor.showMessage(TaskMonitor.Level.ERROR, e.getMessage());
        }
    }

    private Optional<CypherQuery> getCypherQuery(CyNetwork cyNetwork) {
        if (cyNetwork.getRow(cyNetwork).isSet("cypher_query")) {
            String serializedCypherQuery = cyNetwork.getRow(cyNetwork).get("cypher_query", String.class);
            StringReader stringReader = new StringReader(serializedCypherQuery);
            CypherQueryReader reader = new CypherQueryReader(stringReader);
            return Optional.ofNullable(reader.read());
        } else {
            return Optional.empty();
        }
    }
}
