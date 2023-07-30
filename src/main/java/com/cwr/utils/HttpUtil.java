package com.cwr.utils;

import lombok.experimental.UtilityClass;
import lombok.extern.java.Log;
import org.apache.http.Header;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicHeader;
import org.apache.http.ssl.SSLContextBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.stream.Collectors;

@UtilityClass
@Log
public class HttpUtil {

    private ClientHttpRequestFactory requestFactory;
    private RestTemplate restTemplate;

    private static final int REQUEST_TIMEOUT = 60 * 1000; // 60 sec

    private static final List<Header> DEFAULT_HEADERS = getDefaultHeaders();


    private synchronized RestTemplate getRestTemplate() {
        if (restTemplate == null) {
            try {
                restTemplate = new RestTemplate(Objects.requireNonNull(getRequestFactory()));
            } catch (Exception e) {
                log.log(Level.SEVERE, String.format("Can't initialize RestTemplate -> %s", e.getMessage()), e);
            }
        }
        return restTemplate;
    }

    private synchronized ClientHttpRequestFactory getRequestFactory() {
        if (requestFactory == null) {
            try {
                SSLConnectionSocketFactory sslFactory = createTrustAllFactory();
                CloseableHttpClient client = HttpClientBuilder.create().setDefaultHeaders(DEFAULT_HEADERS)
                        .setSSLSocketFactory(sslFactory)
                        .setConnectionManager(new PoolingHttpClientConnectionManager(createTrustAllFactoryRegistry(sslFactory))).build();
                HttpComponentsClientHttpRequestFactory defaultRequestFactory = new HttpComponentsClientHttpRequestFactory(client);
                defaultRequestFactory.setReadTimeout(REQUEST_TIMEOUT);
                defaultRequestFactory.setConnectTimeout(REQUEST_TIMEOUT);
                requestFactory = defaultRequestFactory;
                return requestFactory;
            } catch (NoSuchAlgorithmException | KeyStoreException | KeyManagementException ex) {
                log.log(Level.WARNING, "Cannot init client", ex);
            }

        }
        return null;
    }

    public <T> ResponseEntity<T> postRequestBody(String path, String json, Class<T> responseType, HttpHeaders headers) throws HttpClientErrorException, HttpServerErrorException {
        HttpEntity<String> entity = new HttpEntity<>(json, headers);
        return getRestTemplate().postForEntity(path, entity, responseType);
    }

    public <T> ResponseEntity<T> sendRequest(String path, Class<T> responseType, HttpHeaders headers, HttpMethod method) {
        HttpEntity<String> entity = new HttpEntity<>(headers);
        return getRestTemplate().exchange(path, method, entity, responseType);
    }

    public <T> ResponseEntity<T> sendRequest(URI uri, Class<T> responseType, HttpHeaders headers, HttpMethod method) {
        HttpEntity<String> entity = new HttpEntity<>(headers);
        return getRestTemplate().exchange(uri, method, entity, responseType);
    }

    public <T> ResponseEntity<T> sendRequest(String path, String json, Class<T> responseType, HttpHeaders headers, HttpMethod method) {
        HttpEntity<String> entity = new HttpEntity<>(json, headers);
        return getRestTemplate().exchange(path, method, entity, responseType);
    }

    public static Map<String, String> getDefaultHeaderValue() {
        Map<String, String> result = new HashMap<>();
        result.put(HttpHeaders.ACCEPT, "text/html,application/json,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        result.put(HttpHeaders.CACHE_CONTROL, "max-age=0");
        result.put(HttpHeaders.CONNECTION, "keep-alive");
        result.put(HttpHeaders.ACCEPT_CHARSET, "ISO-8859-1,utf-8;q=0.7,*;q=0.3");
        result.put(HttpHeaders.ACCEPT_LANGUAGE, "es-ES,es;q=0.8");
        result.put(HttpHeaders.CONTENT_TYPE, "application/json; charset=utf8");
        result.put(HttpHeaders.ACCEPT_ENCODING, "gzip, deflate");
        result.put(HttpHeaders.CONTENT_ENCODING, "gzip, deflate");
        result.put(HttpHeaders.PRAGMA, "");
        return result;
    }

    public HttpHeaders getHeaderPostRequest() {
        HttpHeaders header = new HttpHeaders();
        getDefaultHeaderValue().forEach(header::add);
        return header;
    }

    public List<Header> getDefaultHeaders() {
        return getDefaultHeaderValue().entrySet().stream().map(it -> new BasicHeader(it.getKey(), it.getValue())).collect(Collectors.toList());
    }


    private Registry<ConnectionSocketFactory> createTrustAllFactoryRegistry(SSLConnectionSocketFactory sslFactory) {
        return RegistryBuilder.<ConnectionSocketFactory>create()
                .register("http", PlainConnectionSocketFactory.INSTANCE)
                .register("https", sslFactory).build();
    }

    private SSLConnectionSocketFactory createTrustAllFactory() throws KeyStoreException, NoSuchAlgorithmException, KeyManagementException {
        return new SSLConnectionSocketFactory(SSLContextBuilder.create().loadTrustMaterial((chain, authType) -> true).build(), (host, ssl) -> true); //NOSONAR
    }
}
