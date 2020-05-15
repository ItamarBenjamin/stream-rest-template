package com.ibinyami.spring.resttemplate;

import com.google.common.collect.Lists;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.ResponseExtractor;
import org.springframework.web.client.RestTemplate;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.LinkedList;
import java.util.List;

public class StreamRestTemplate extends RestTemplate {
    private static final DeferredCloseClientHttpRequestInterceptor deferredCloseClientHttpRequestInterceptor =
            new DeferredCloseClientHttpRequestInterceptor();

    public StreamRestTemplate() {
        super.setInterceptors(Lists.newArrayList(deferredCloseClientHttpRequestInterceptor));
    }

    @Override
    public void setInterceptors(List<ClientHttpRequestInterceptor> interceptors) {
        super.setInterceptors(addInterceptorAtBeginning(interceptors));
    }

    private List<ClientHttpRequestInterceptor> addInterceptorAtBeginning(List<ClientHttpRequestInterceptor> interceptors) {
        boolean interceptorExists = interceptors.contains(deferredCloseClientHttpRequestInterceptor);
        if (interceptorExists && interceptors.get(0) == deferredCloseClientHttpRequestInterceptor) {
            return interceptors;
        }
        LinkedList<ClientHttpRequestInterceptor> newInterceptors = Lists.newLinkedList();
        newInterceptors.addAll(interceptors);
        if (interceptorExists) {
            newInterceptors.remove(deferredCloseClientHttpRequestInterceptor);
        }
        newInterceptors.addFirst(deferredCloseClientHttpRequestInterceptor);
        return newInterceptors;
    }

    @Override
    protected <T> ResponseExtractor<ResponseEntity<T>> responseEntityExtractor(Type responseType) {
        ResponseExtractor<ResponseEntity<T>> responseEntityResponseExtractor = super.responseEntityExtractor(responseType);
        boolean isStream = responseType == InputStreamResource.class;
        return new StreamResponseExtractor<>(isStream, responseEntityResponseExtractor);
    }

    private static class DeferredCloseClientHttpRequestInterceptor implements ClientHttpRequestInterceptor {
        @Override
        public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
            ClientHttpResponse response = execution.execute(request, body);
            return new DeferredCloseClientHttpResponse(response);
        }
    }

    @RequiredArgsConstructor
    private static class DeferredCloseClientHttpResponse implements ClientHttpResponse {
        private final ClientHttpResponse delegate;

        @Setter
        private boolean isStream = false;

        @Override
        public HttpStatus getStatusCode() throws IOException {
            return delegate.getStatusCode();
        }

        @Override
        public int getRawStatusCode() throws IOException {
            return delegate.getRawStatusCode();
        }

        @Override
        public String getStatusText() throws IOException {
            return delegate.getStatusText();
        }

        @Override
        public void close() {
            if (isStream) {
                //do nothing, need to call close explicitly on the response
                return;
            }
            delegate.close();
        }

        @Override
        public InputStream getBody() throws IOException {
            if (isStream) {
                return this.new DeferredCloseInputStream(delegate.getBody());
            }
            return delegate.getBody();
        }

        @Override
        public HttpHeaders getHeaders() {
            return delegate.getHeaders();
        }


        private class DeferredCloseInputStream extends FilterInputStream {
            DeferredCloseInputStream(InputStream in) {
                super(in);
            }

            @Override
            public void close() {
                delegate.close();
            }
        }
    }

    @RequiredArgsConstructor
    private static class StreamResponseExtractor<T> implements ResponseExtractor<ResponseEntity<T>> {
        private final boolean isStream;
        private final ResponseExtractor<ResponseEntity<T>> delegate;

        @Override
        public ResponseEntity<T> extractData(ClientHttpResponse response) throws IOException {
            if (!(response instanceof DeferredCloseClientHttpResponse)) {
                throw new IllegalStateException("Expected response of type DeferredCloseClientHttpResponse but got "
                        + response.getClass().getCanonicalName());
            }
            DeferredCloseClientHttpResponse deferredCloseClientHttpResponse = (DeferredCloseClientHttpResponse) response;
            deferredCloseClientHttpResponse.setStream(isStream);
            return delegate.extractData(response);
        }
    }
}
