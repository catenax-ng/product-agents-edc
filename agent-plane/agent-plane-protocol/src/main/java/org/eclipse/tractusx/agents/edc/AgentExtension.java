//
// EDC Data Plane Agent Extension 
// See copyright notice in the top folder
// See authors file in the top folder
// See license file in the top folder
//
package org.eclipse.tractusx.agents.edc;

import org.eclipse.tractusx.agents.edc.http.AgentController;
import org.eclipse.tractusx.agents.edc.http.DelegationService;
import org.eclipse.tractusx.agents.edc.http.HttpClientFactory;
import org.eclipse.tractusx.agents.edc.http.transfer.AgentSourceFactory;
import org.eclipse.tractusx.agents.edc.http.transfer.AgentSourceRequestParamsSupplier;
import org.eclipse.tractusx.agents.edc.rdf.RDFStore;
import org.eclipse.tractusx.agents.edc.service.DataspaceSynchronizer;
import org.eclipse.tractusx.agents.edc.sparql.DataspaceServiceExecutor;
import org.eclipse.tractusx.agents.edc.sparql.SparqlQueryProcessor;
import org.eclipse.tractusx.agents.edc.sparql.SparqlQuerySerializerFactory;
import org.eclipse.tractusx.agents.edc.validation.SwitchingDataPlaneTokenValidatorController;
import org.apache.jena.query.Syntax;
import org.apache.jena.sparql.serializer.*;
import org.apache.jena.sparql.service.ServiceExecutorRegistry;
import org.eclipse.edc.connector.dataplane.http.params.HttpRequestFactory;
import org.eclipse.edc.spi.http.EdcHttpClient;
import org.eclipse.edc.web.spi.WebService;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import okhttp3.OkHttpClient;
import org.eclipse.tractusx.agents.edc.service.DataManagement;

import org.eclipse.edc.connector.dataplane.spi.pipeline.PipelineService;
import org.eclipse.edc.spi.types.TypeManager;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.regex.Pattern;

/**
 * EDC extension that initializes the Agent subsystem (Agent Sources, Agent Endpoint and Federation Callbacks
 */
public class AgentExtension implements ServiceExtension {

    /**
     * static constants
     */
    protected static final String DEFAULT_CONTEXT_ALIAS = "default";
    protected static final String CALLBACK_CONTEXT_ALIAS = "callback";
    public static Pattern GRAPH_PATTERN=Pattern.compile("((?<url>[^#]+)#)?(?<graph>.*Graph(Asset)?.*)");
    public static Pattern SKILL_PATTERN=Pattern.compile("((?<url>[^#]+)#)?(?<skill>.*Skill(Asset)?.*)");


    /**
     * dependency injection part
     */
    @Inject
    protected WebService webService;


    @Inject
    protected PipelineService pipelineService;

    @Inject
    protected Vault vault;

    @Inject
    protected EdcHttpClient edcHttpClient;
    @Inject
    protected TypeManager typeManager;

    /**
     * refers a scheduler
     * TODO maybe reuse an injected scheduler
     */
    protected ScheduledExecutorService executorService;

    /**
     * data synchronization service
     */
    protected DataspaceSynchronizer synchronizer;

    /**
     * we use our own http client
     */
    protected OkHttpClient httpClient;


    /**
     * @return name of the extension
     */
    @Override
    public String name() {
        return "Knowledge Agents Extension";
    }

    /**
     * runs on extension initialization
     * @param context EDC bootstrap context
     */
    @Override
    public void initialize(ServiceExtensionContext context) {
        Monitor monitor = context.getMonitor();
        
        monitor.debug(String.format("Initializing %s",name()));

        AgentConfig config = new AgentConfig(monitor,context.getConfig());
        httpClient= HttpClientFactory.create(config);

        DataManagement catalogService=new DataManagement(monitor,typeManager,httpClient,config);

        AgreementController agreementController=new AgreementController(monitor,config,catalogService);
        monitor.debug(String.format("Registering agreement controller %s",agreementController));
        webService.registerResource(CALLBACK_CONTEXT_ALIAS, agreementController);

        RDFStore rdfStore=new RDFStore(config,monitor);

        executorService= Executors.newScheduledThreadPool(config.getThreadPoolSize());
        synchronizer=new DataspaceSynchronizer(executorService,config,catalogService,rdfStore,monitor);

        SwitchingDataPlaneTokenValidatorController validatorController=new SwitchingDataPlaneTokenValidatorController(httpClient,config,monitor);
        if(validatorController.isEnabled()) {
            monitor.debug(String.format("Registering switching validator controller %s",validatorController));
            webService.registerResource(DEFAULT_CONTEXT_ALIAS,validatorController);
        }

        // EDC Remoting Support
        ServiceExecutorRegistry reg = new ServiceExecutorRegistry();
        reg.addBulkLink(new DataspaceServiceExecutor(monitor,agreementController,config,httpClient,executorService,typeManager,config));
        //reg.add(new DataspaceServiceExecutor(monitor,agreementController,config,httpClient));

        // Ontop and other deep nesting-afraid providers/optimizers
        // should be supported by not relying on the Fuseki syntax graph
        SparqlQuerySerializerFactory arqQuerySerializerFactory = new SparqlQuerySerializerFactory();
        SerializerRegistry.get().addQuerySerializer(Syntax.syntaxARQ, arqQuerySerializerFactory);
        SerializerRegistry.get().addQuerySerializer(Syntax.syntaxSPARQL_10, arqQuerySerializerFactory);
        SerializerRegistry.get().addQuerySerializer(Syntax.syntaxSPARQL_11, arqQuerySerializerFactory);

        // the actual sparql engine inside the EDC
        SparqlQueryProcessor processor=new SparqlQueryProcessor(reg,monitor,config,rdfStore, typeManager);

        // stored procedure store and transport endpoint
        SkillStore skillStore=new SkillStore();
        DelegationService delegationService=new DelegationService(agreementController,monitor,httpClient,typeManager);
        AgentController agentController=new AgentController(monitor,agreementController,config,processor,skillStore,delegationService);
        monitor.debug(String.format("Registering agent controller %s",agentController));
        webService.registerResource(DEFAULT_CONTEXT_ALIAS, agentController);

        monitor.debug(String.format("Initialized %s",name()));

        HttpRequestFactory httpRequestFactory = new HttpRequestFactory();
        AgentSourceFactory sourceFactory = new AgentSourceFactory(edcHttpClient, new AgentSourceRequestParamsSupplier(vault,typeManager,config,monitor),monitor,httpRequestFactory, processor, skillStore);
        pipelineService.registerFactory(sourceFactory);
    }

    /**
     * start scheduled services
     */
    @Override
    public void start() {
        synchronizer.start();
    }

    /**
     * Signals the extension to release resources and shutdown.
     * stop any schedules services
     */
    @Override
    public void shutdown() {
        synchronizer.shutdown();
    }
}