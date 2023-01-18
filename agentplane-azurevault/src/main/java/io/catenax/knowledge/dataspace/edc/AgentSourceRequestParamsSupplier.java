//
// EDC Data Plane Agent Extension
// See copyright notice in the top folder
// See authors file in the top folder
// See license file in the top folder
//
package io.catenax.knowledge.dataspace.edc;

import io.catenax.knowledge.dataspace.edc.http.HttpUtils;
import org.eclipse.dataspaceconnector.dataplane.http.pipeline.HttpSourceRequestParamsSupplier;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.security.Vault;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.HttpDataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataFlowRequest;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.eclipse.dataspaceconnector.dataplane.spi.schema.DataFlowRequestSchema.QUERY_PARAMS;

/**
 * request params supplier which correctly double encodes
 * valid url symbols in the parameter section, extracts
 * original headers like accept from special parameters and caters for
 * translating url-encoded form bodies into their
 * "normal" param+body form
 */
public class AgentSourceRequestParamsSupplier extends HttpSourceRequestParamsSupplier {

    /**
     * the edc config section
     */
    final AgentConfig config;
    /**
     * logging subsystem
     */
    final Monitor monitor;

    /**
     * creates a supplier
     * @param vault secret host
     * @param config edc config section
     * @param monitor logging reference
     */
    public AgentSourceRequestParamsSupplier(Vault vault, AgentConfig config, Monitor monitor) {
        super(vault);
        this.config=config;
        this.monitor=monitor;
    }

    /**
     * regex to extract the special ACCEPT header parameter embedding
     */
    public static Pattern CX_ACCEPT_PARAM= Pattern.compile("cx_accept=?(?<Accept>[^&]*)&?");

    /**
     * extracts the address from a data flow request.
     * Since this is used both for sink-source transmission as
     * for the final source-backend transmission, we have a case
     * distinction to make. In case of transfer, all we have to
     * do is maybe adjust the base url to the target data plane
     * (such that this code will run on both "publicUrl" conventions
     *  which switched between CX EDC 0.1.1 and 0.1.2).
     * In case of final transmission, we do not call the backend directly
     * but rather the internal agent which will then be allowed to
     * delegate to the backend.
     * @param request ids data flow request
     * @return address of the target
     */
    @Override
    protected @NotNull DataAddress selectAddress(DataFlowRequest request) {
        DataAddress superAddress = super.selectAddress(request);
        // is it an ordinary transfer?
        if(AgentSourceFactory.isTransferRequest(request)) {
            // yes, cater for the api/public jetty/jakarta slash drama
            if(!superAddress.getProperty("baseUrl").endsWith("/")) {
                superAddress=HttpDataAddress.Builder.newInstance().copyFrom(superAddress).baseUrl(superAddress.getProperty("baseUrl")+"/").build();
            }
            return superAddress;
        }
        // no, manipulate the endpoint address with additional
        // headers based on given parameters and source address properties
        // for example ONTOP does not like getting no Host header
        // for example RDF4J returns csv if no Accept header is given
        try {
            monitor.debug(String.format("Rewriting params/headers of request %s before hitting backend.",request));
            boolean fixedHeader=superAddress.getProperty(HttpDataAddress.ADDITIONAL_HEADER+"Accept")!=null;
            boolean fixedHost=superAddress.getProperty(HttpDataAddress.ADDITIONAL_HEADER+"Host")!=null;
            String oldQueryParams=request.getProperties().getOrDefault(QUERY_PARAMS,"");

            String accept="*/*";
            StringBuilder newQueryParams=new StringBuilder();
            int lastStart=0;
            Matcher acceptMatcher = CX_ACCEPT_PARAM.matcher(oldQueryParams);
            while (acceptMatcher.find()) {
                int yetString=Math.max(acceptMatcher.start() - 1,lastStart);
                newQueryParams.append(oldQueryParams.substring(lastStart, yetString));
                String newAccept= acceptMatcher.group("Accept");
                if(newAccept!=null && newAccept.length()>0) {
                    accept=newAccept.replace("q=[0-9]+(\\.[0-9]+)?, ","").replace("%2F","/");
                }
                lastStart = acceptMatcher.end();
            }
            newQueryParams.append(oldQueryParams.substring(lastStart));
            String targetQueryParams=HttpUtils.encodeParameter(newQueryParams.toString());

            var addressBuilder =
                HttpDataAddress.Builder.newInstance().copyFrom(superAddress);

            addressBuilder.property(QUERY_PARAMS,targetQueryParams);

            if(!fixedHeader) {
                addressBuilder.addAdditionalHeader("Accept", accept);
            }
            if(!fixedHost) {
                URL controlPlane=new URL(config.getControlPlaneManagementUrl());
                String hostPart=controlPlane.getHost();
                int portPart = controlPlane.getPort();
                String host=String.format("%s:%d",hostPart,portPart);
                addressBuilder.addAdditionalHeader("Host", host);
            }
            return addressBuilder.build();
        } catch (Throwable e) {
            monitor.severe("Cannot encode query params",e);
            throw new EdcException("Cannot encode query params.",e);
        }
    }

    /**
     * regex to extract url-encoded form parts
     */
    public static Pattern WWW_FORM_PART = Pattern.compile("(?<param>[^=&]+)=(?<value>[^&]*)");

    /**
     * parse the body as a url-encoded form into a map
     * @param body url-encoded form body
     * @return a map of parts
     */
    public static Map<String,String> parseFormBody(String body) {
        Map<String,String> parts=new HashMap<>();
        if(body!=null) {
            Matcher matcher=WWW_FORM_PART.matcher(body);
            while(matcher.find()) {
                parts.put(matcher.group("param"),matcher.group("value"));
            }
        }
        return parts;
    }

    /**
     * extract the content type
     * @param address target address
     * @param request data flow request
     * @return the content type (which would be derived from the query language part in case the original content type is a url-encoded form)
     */
    @Override
    protected @Nullable String extractContentType(HttpDataAddress address, DataFlowRequest request) {
        String contentType = super.extractContentType(address, request);
        if (contentType != null && contentType.contains("application/x-www-form-urlencoded")) {
            Map<String,String> parts=parseFormBody(super.extractBody(address,request));
            if("SPARQL".equals(parts.get("queryLn"))) {
                contentType="application/sparql-query";
            } else {
                // other languages, we have no idea of, currently
                contentType=null;
            }
        }
        return contentType;
    }

    /**
     * extracts the query param section of the url
     * @param address target address
     * @param request data flow request
     * @return query params (extended with non-query parts of the body if the content type is a url-encoded form)
     */
    @Override
    protected @Nullable String extractQueryParams(HttpDataAddress address, DataFlowRequest request) {
        String result;
        if(AgentSourceFactory.isTransferRequest(request))
            result=super.extractQueryParams(address,request);
        else
            result=address.getProperty(QUERY_PARAMS);
        String contentType=super.extractContentType(address, request);
        if (contentType!=null && contentType.contains("application/x-www-form-urlencoded")) {
            Map<String,String> parts=parseFormBody(super.extractBody(address,request));
            parts.remove("query");
            String newParams=parts.entrySet().stream().map( entry -> entry.getKey()+"="+entry.getValue()).collect(Collectors.joining("&"));
            if(!newParams.isEmpty()) {
                if (result != null && !result.isEmpty()) {
                    newParams = result + "&" + newParams;
                }
                return newParams;
            }
        }

        return result;
    }

    /**
     * extracts the body
     * @param address target address
     * @param request data flow request
     * @return the string-based body (or the query part of the body if the original content type is url-encoded form)
     */
    @Override
    protected @Nullable String extractBody(HttpDataAddress address, DataFlowRequest request) {
        String body = super.extractBody(address, request);
        String contentType=super.extractContentType(address, request);
        if (contentType!=null && contentType.contains("application/x-www-form-urlencoded")) {
            Map<String, String> parts = parseFormBody(super.extractBody(address, request));
            body = parts.get("query");
            if(body!=null) {
                body = URLDecoder.decode(body, StandardCharsets.UTF_8);
            }
        }
        return body;
    }
}
