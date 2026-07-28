package com.nova.portfolio.marketdata.client;

import com.nova.portfolio.config.OpenExchangeRatesProperties;
import com.nova.portfolio.exception.OpenExchangeRatesException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Map;

@Component
public class OpenExchangeRatesClient {

    private final RestTemplate restTemplate;
    private final OpenExchangeRatesProperties properties;

    public OpenExchangeRatesClient(RestTemplate restTemplate, OpenExchangeRatesProperties properties) {
        this.restTemplate = restTemplate;
        this.properties = properties;
    }

    /** Get latest rates. base/symbols/showAlternative are all optional (pass null to skip). */
    public Map<String, Object> getLatest(String base, String symbols, Boolean showAlternative) {
        validateAppId();
        URI uri = UriComponentsBuilder.fromHttpUrl(properties.getBaseUrl() + "/latest.json")
                .queryParam("app_id", properties.getAppId())
                .queryParamIfPresent("base", java.util.Optional.ofNullable(base))
                .queryParamIfPresent("symbols", java.util.Optional.ofNullable(symbols))
                .queryParamIfPresent("show_alternative", java.util.Optional.ofNullable(showAlternative))
                .build().toUri();
        return execute(uri);
    }

    /** Get full currency list (no app_id required). */
    public Map<String, Object> getCurrencies(Boolean showAlternative, Boolean showInactive) {
        URI uri = UriComponentsBuilder.fromHttpUrl(properties.getBaseUrl() + "/currencies.json")
                .queryParamIfPresent("show_alternative", java.util.Optional.ofNullable(showAlternative))
                .queryParamIfPresent("show_inactive", java.util.Optional.ofNullable(showInactive))
                .build().toUri();
        return execute(uri);
    }

    private void validateAppId() {
        if (properties.getAppId() == null || properties.getAppId().isBlank()) {
            throw new OpenExchangeRatesException("App ID is not configured", HttpStatus.UNAUTHORIZED.value());
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> execute(URI uri) {
        try {
            return restTemplate.getForObject(uri, Map.class);
        } catch (HttpClientErrorException e) {
            throw new OpenExchangeRatesException("OXR client error: " + e.getStatusCode(), e.getStatusCode().value());
        } catch (HttpServerErrorException e) {
            throw new OpenExchangeRatesException("OXR server error: " + e.getStatusCode(), e.getStatusCode().value());
        } catch (RestClientException e) {
            throw new OpenExchangeRatesException("OXR network error", HttpStatus.BAD_GATEWAY.value(), e);
        }
    }
}

