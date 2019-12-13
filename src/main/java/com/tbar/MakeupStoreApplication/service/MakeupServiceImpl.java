package com.tbar.MakeupStoreApplication.service;

import com.tbar.MakeupStoreApplication.service.consumer.MultiAPIConsumer;
import com.tbar.MakeupStoreApplication.service.consumer.SoloAPIConsumer;
import com.tbar.MakeupStoreApplication.service.consumer.model.Item;
import com.tbar.MakeupStoreApplication.utility.exceptions.APIConnectionException;
import com.tbar.MakeupStoreApplication.utility.exceptions.ProductNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
public class MakeupServiceImpl implements MakeupService {

    // === fields ===
    private URI multiBaseUri;
    private URI soloBaseUri;
    @Value("${application.makeup.api.solo.uri.suffix}")
    private String soloUriSuffix;
    private Set<String> validParameters;
    private MultiAPIConsumer multiAPIConsumer;
    private SoloAPIConsumer soloAPIConsumer;

    // === constructors ===
    @Autowired
    public MakeupServiceImpl(MultiAPIConsumer multiAPIConsumer, SoloAPIConsumer soloAPIConsumer) {
        this.multiAPIConsumer = multiAPIConsumer;
        this.soloAPIConsumer = soloAPIConsumer;
    }

    // === setters ===
    @Value("${application.makeup.api.multi.base.uri}")
    private void setMultiBaseUri(String multiBaseUri) {
        this.multiBaseUri = URI.create(multiBaseUri);
    }

    @Value("${application.makeup.api.solo.base.uri}")
    private void setSoloBaseUri(String soloBaseUri) {
        this.soloBaseUri = URI.create(soloBaseUri);
    }

    @Value("${application.makeup.api.valid.parameters}")
    private void setValidParameters(String[] validParameters) {
        this.validParameters = new HashSet<>(Set.of(validParameters));
    }

    // === public methods ===
    @Override
    public List<Item> getProducts(@Nullable Map<String, String> parameters) throws ProductNotFoundException, APIConnectionException {
        // get response
        URI requestUri = buildUri(parameters);
        ResponseEntity<List<Item>> response = multiAPIConsumer.requestData(requestUri);
        log.debug("getProducts method. URI = {}, ResponseEntity = {}", requestUri, response);
        // check if response is ok and not null then return body
        if (response.getStatusCode() == HttpStatus.OK) {
            if (!response.getBody().isEmpty()) {
                return response.getBody();
            } else {
                throw new ProductNotFoundException(requestUri.toString());
            }
        } else {
            throw new APIConnectionException(response.getStatusCodeValue(), requestUri.toString());
        }
    }

    @Override
    public Item getProduct(@NonNull Long id) throws ProductNotFoundException, APIConnectionException {
        // get response
        URI requestUri = buildUri(id);
        ResponseEntity<Item> response = soloAPIConsumer.requestData(requestUri);
//        log.info("Response = {}, class = {}", response, response.getBody().getClass());
        // check if response is ok then return body
        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            return response.getBody();
        } else if (response.getStatusCode() == HttpStatus.NOT_FOUND) {
            throw new ProductNotFoundException(requestUri.toString());
        } else {
            throw new APIConnectionException(response.getStatusCodeValue(), requestUri.toString());
        }
    }

    // === private methods ===
    /**
     * It extends <i>multiSearchBaseUri</i> field with query parameters from {@code Map} argument.<br>
     * Method checks if parameters provided in argument {@code Map} match with
     * the valid ones from <i>validParameters</i> field.<br>
     * If parameters {@code Map} argument is {@code null} then method returns <i>multiSearchBaseUri</i>.<br>
     *
     * @param parameters {@code Map} of query parameters to be added. Can be {@code null}.
     * @return {@code URI} build with valid (or all) query parameters.
     */
    private URI buildUri(@Nullable Map<String, String> parameters) {
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUri(multiBaseUri);
        if (parameters != null) {
            // loop through arguments parameters to find and add valid ones
            for (Map.Entry<String, String> entry : parameters.entrySet()) {
                if (validParameters.contains(entry.getKey())) {
                    uriBuilder.queryParam(entry.getKey(), entry.getValue());
                }
            }
        }
        log.debug("Entered buildUri method with map of parameters = {}; Build URI = {}", parameters, uriBuilder.build());
        return uriBuilder.build().toUri();
    }

    /**
     * It extends <i>soloSearchBaseUri</i> field with path element containing <i>id</i> argument.<br>
     *
     * @param id unique product id added to {@code URI} path
     * @return {@code URI} build with id in path
     */
    private URI buildUri(@NonNull Long id) {
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUri(soloBaseUri);

        uriBuilder.path(id + soloUriSuffix);

        log.debug("Entered buildUri method with id = {}; Build URI = {}", id, uriBuilder.build());
        return uriBuilder.build().toUri();
    }
}
