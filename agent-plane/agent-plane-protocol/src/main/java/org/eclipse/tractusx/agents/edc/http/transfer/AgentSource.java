//
// EDC Data Plane Agent Extension
// See copyright notice in the top folder
// See authors file in the top folder
// See license file in the top folder
//
package org.eclipse.tractusx.agents.edc.http.transfer;

import org.eclipse.edc.connector.dataplane.spi.pipeline.StreamResult;
import org.eclipse.tractusx.agents.edc.AgentExtension;
import org.eclipse.tractusx.agents.edc.SkillStore;
import org.eclipse.tractusx.agents.edc.sparql.SparqlQueryProcessor;
import okhttp3.Response;
import org.eclipse.edc.connector.dataplane.http.params.HttpRequestFactory;
import org.eclipse.edc.connector.dataplane.http.spi.HttpRequestParams;
import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSource;
import org.eclipse.edc.spi.http.EdcHttpClient;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowRequest;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.stream.Stream;

import static java.lang.String.format;

/**
 * An agent-enabled http-transfer source
 * there are two modes of operation: Routing transfer and
 * calling a single (in the future: several) graph endpoint
 * (in which case we rather need to
 * invoke the surrounding plane sparql engine to
 * perform some joins/pre and postprocessing and event
 * further delegations) and that engine will in turn
 * delegate to the final endpoint (for which we
 * replace any "GRAPH" occurences with "SERVICE" references
 * in the processor)
 * TODO generalize to shield several endpoints/assets
 */
public class AgentSource implements DataSource {
    protected String name;
    protected HttpRequestParams params;
    protected String requestId;
    protected HttpRequestFactory requestFactory;
    protected EdcHttpClient httpClient;
    protected boolean isTransfer;
    protected SparqlQueryProcessor processor;
    protected SkillStore skillStore;

    protected DataFlowRequest request;

    public static String AGENT_BOUNDARY="--";

    /**
     * creates new agent source
     */
    public AgentSource() {
    }

    @Override
    public StreamResult<Stream<Part>> openPartStream() {
        // check whether this is a cross-plane call or a final agent call
        if(!isTransfer) {
            return openTransfer();
        } else {
            return openMatchmaking();
        }
    }

    /**
     * delegates/tunnels a KA-TRANSFER call
     * @return multipart body containing result and warnings
     */
    @NotNull
    protected StreamResult<Stream<Part>> openTransfer() {
        // Agent call, we translate from KA-MATCH to KA-TRANSFER
        String skill=null;
        String graph=null;
        String asset= request.getSourceDataAddress().getProperties().get("asset:prop:id");
        if(asset!=null && asset.length() > 0) {
            Matcher graphMatcher= AgentExtension.GRAPH_PATTERN.matcher(asset);
            if(graphMatcher.matches()) {
                graph=asset;
            }
            Matcher skillMatcher=SkillStore.SKILL_PATTERN.matcher(asset);
            if(skillMatcher.matches()) {
                skill=asset;
            }
        }
        String authKey=request.getSourceDataAddress().getProperties().getOrDefault("authKey",null);
        String authCode=request.getSourceDataAddress().getProperties().getOrDefault("authCode",null);
        try (Response response = processor.execute(this.requestFactory.toRequest(params),skill,graph,authKey,authCode)) {
            if(!response.isSuccessful()) {
                return StreamResult.error(format("Received code transferring HTTP data for request %s: %s - %s.", requestId, response.code(), response.message()));
            }
            List<Part> results=new ArrayList<>();
            if(response.body()!=null) {
                results.add(new AgentPart(response.body().contentType().toString(),response.body().bytes()));
            }
            if(response.header("cx_warnings")!=null) {
                results.add(new AgentPart("application/cx-warnings+json",response.header("cx_warnings").getBytes()));
            }
            return StreamResult.success(results.stream());
        } catch (IOException e) {
            return StreamResult.error(e.getMessage());
        }
    }

    /**
     * executes a KA-MATCHMAKING call and pipes the results into KA-TRANSFER
     * @return multipart body containing result and warnings
     */
    @NotNull
    protected StreamResult<Stream<Part>> openMatchmaking() {
        try (var response = httpClient.execute(this.requestFactory.toRequest(params))) {
            if(!response.isSuccessful()) {
                return StreamResult.error(format("Received code transferring HTTP data for request %s: %s - %s.", requestId, response.code(), response.message()));
            }
            List<Part> results=new ArrayList<>();
            if(response.body()!=null) {
                try(BufferedInputStream bis = new BufferedInputStream(response.body().byteStream())) {
                    bis.mark(AGENT_BOUNDARY.length());
                    byte[] boundary = new byte[AGENT_BOUNDARY.length()];
                    int all = bis.read(boundary);
                    bis.reset();
                    if (all==boundary.length && AGENT_BOUNDARY.equals(new String(boundary))) {
                        StringBuilder nextPart = null;
                        String embeddedContentType = null;
                        try (BufferedReader reader = new BufferedReader(new InputStreamReader(bis))) {
                            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                                if (AGENT_BOUNDARY.equals(line)) {
                                    if (nextPart != null && embeddedContentType != null) {
                                        results.add(new AgentPart(embeddedContentType, nextPart.toString().getBytes()));
                                    }
                                    nextPart = new StringBuilder();
                                    String contentLine = reader.readLine();
                                    if (contentLine != null && contentLine.startsWith("Content-Type: ")) {
                                        embeddedContentType = contentLine.substring(14);
                                    } else {
                                        embeddedContentType = null;
                                    }
                                } else if(nextPart!=null) {
                                    nextPart.append(line);
                                    nextPart.append("\n");
                                }
                            }
                        }
                        if (nextPart != null && embeddedContentType != null) {
                            results.add(new AgentPart(embeddedContentType, nextPart.toString().getBytes()));
                        }
                    } else {
                        results.add(new AgentPart(name, response.body().bytes()));
                    }
                }
            }
            return StreamResult.success(results.stream());
        } catch (IOException e) {
            return StreamResult.error(e.getMessage());
        }
    }

    @Override
    public String toString() {
        return String.format("AgentSource(%s,%s)",requestId,name);
    }

    /**
     * the agent source builder
     */
    public static class Builder {
        protected final AgentSource dataSource;

        public static AgentSource.Builder newInstance() {
            return new AgentSource.Builder();
        }

        public AgentSource.Builder params(HttpRequestParams params) {
            dataSource.params = params;
            return this;
        }

        public AgentSource.Builder name(String name) {
            dataSource.name = name;
            return this;
        }

        public AgentSource.Builder requestId(String requestId) {
            dataSource.requestId = requestId;
            return this;
        }

        public AgentSource.Builder requestFactory(HttpRequestFactory requestFactory) {
            dataSource.requestFactory = requestFactory;
            return this;
        }

        public AgentSource.Builder httpClient(EdcHttpClient httpClient) {
            dataSource.httpClient = httpClient;
            return this;
        }

        public AgentSource.Builder isTransfer(boolean isTransfer) {
            dataSource.isTransfer=isTransfer;
            return this;
        }

        public AgentSource.Builder processor(SparqlQueryProcessor processor) {
            dataSource.processor=processor;
            return this;
        }

        public AgentSource.Builder skillStore(SkillStore skillStore) {
            dataSource.skillStore=skillStore;
            return this;
        }

        public AgentSource.Builder request(DataFlowRequest request) {
            dataSource.request=request;
            return this;
        }

        public AgentSource build() {
            Objects.requireNonNull(dataSource.requestId, "requestId");
            Objects.requireNonNull(dataSource.httpClient, "httpClient");
            Objects.requireNonNull(dataSource.requestFactory, "requestFactory");
            return dataSource;
        }

        public Builder() {
            dataSource = new AgentSource();
        }
    }

    private static class AgentPart implements Part {
        private final String name;
        private final byte[] content;

        AgentPart(String name, byte[] content) {
            this.name = name;
            if(this.name!=null) {
                StringBuilder newContent=new StringBuilder();
                newContent.append(AGENT_BOUNDARY);
                newContent.append("\n");
                newContent.append("Content-Type: ");
                newContent.append(name);
                newContent.append("\n");
                newContent.append(new String(content));
                this.content=newContent.toString().getBytes();
            } else {
                this.content = content;
            }
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public long size() {
            return content.length;
        }

        @Override
        public InputStream openStream() {
            return new ByteArrayInputStream(content);
        }

    }

}
