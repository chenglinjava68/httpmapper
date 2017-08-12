package cn.yxffcode.httpmapper.core.cfg;

import cn.yxffcode.httpmapper.core.DELETE;
import cn.yxffcode.httpmapper.core.GET;
import cn.yxffcode.httpmapper.core.HttpMethod;
import cn.yxffcode.httpmapper.core.MappedProxy;
import cn.yxffcode.httpmapper.core.MappedRequest;
import cn.yxffcode.httpmapper.core.POST;
import cn.yxffcode.httpmapper.core.PUT;
import cn.yxffcode.httpmapper.core.PostProcessors;
import cn.yxffcode.httpmapper.core.Request;
import cn.yxffcode.httpmapper.core.RequestPostProcessor;
import cn.yxffcode.httpmapper.core.Response;
import cn.yxffcode.httpmapper.core.ResponseHandler;
import cn.yxffcode.httpmapper.core.ToStringResponseHandler;
import cn.yxffcode.httpmapper.core.http.DefaultHttpClientFactory;
import cn.yxffcode.httpmapper.core.http.DefaultHttpExecutor;
import cn.yxffcode.httpmapper.core.http.HttpClientFactory;
import cn.yxffcode.httpmapper.core.http.HttpExecutor;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;

import java.lang.reflect.Method;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author gaohang on 8/6/17.
 */
public class Configuration {

  public static ConfigurationBuilder newBuilder() {
    return new ConfigurationBuilder();
  }

  private final Map<String, MappedRequest> mappedRequests;
  private final Multimap<String, RequestPostProcessor> requestPostProcessors;
  private final Map<String, ResponseHandler> responseHandlers;
  private final HttpExecutor httpExecutor;
  private final ResponseHandler defaultResponseHandler;


  private Configuration(Map<String, MappedRequest> mappedRequests,
                        Multimap<String, RequestPostProcessor> requestPostProcessors,
                        Map<String, ResponseHandler> responseHandlers,
                        ResponseHandler defaultResponseHandler,
                        HttpClientFactory httpClientFactory) {
    this.mappedRequests = mappedRequests;
    this.requestPostProcessors = requestPostProcessors;
    this.responseHandlers = responseHandlers;
    this.defaultResponseHandler = defaultResponseHandler;
    this.httpExecutor = new DefaultHttpExecutor(httpClientFactory, this);
  }

  public HttpExecutor getHttpExecutor() {
    return httpExecutor;
  }

  public ResponseHandler getResponseHandler(String mrId) {
    final ResponseHandler responseHandler = responseHandlers.get(mrId);
    return responseHandler != null ? responseHandler : defaultResponseHandler;
  }

  public ResponseHandler getDefaultResponseHandler() {
    return defaultResponseHandler;
  }

  public <T> T newMapper(Class<T> mapperClass) {
    return MappedProxy.newProxy(this, mapperClass);
  }

  public Iterable<RequestPostProcessor> getPostProcessors(String mrId) {
    return requestPostProcessors.get(mrId);
  }

  public MappedRequest getMappedRequest(String mrId) {
    return mappedRequests.get(mrId);
  }

  public static final class ConfigurationBuilder {
    private final Map<String, MappedRequest> mappedRequests = Maps.newHashMap();
    private final Map<Class<?>, RequestPostProcessor> postProcessorInstances = Maps.newHashMap();
    private final Map<Class<?>, ResponseHandler> typeToresponseHandlers = Maps.newHashMap();
    private final Map<String, ResponseHandler> responseHandlers = Maps.newHashMap();
    private final Multimap<String, RequestPostProcessor> requestPostProcessors = HashMultimap.create();
    private ResponseHandler defaultResponseHandler;
    private HttpClientFactory httpClientFactory;

    private ConfigurationBuilder() {
    }

    public ConfigurationBuilder setHttpClientFactory(HttpClientFactory httpClientFactory) {
      this.httpClientFactory = httpClientFactory;
      return this;
    }

    public ConfigurationBuilder addRequestPostProcessor(String mrId, Class<? extends RequestPostProcessor>[] types) {
      checkNotNull(mrId);
      checkNotNull(types);

      try {
        for (Class<? extends RequestPostProcessor> type : types) {
          final RequestPostProcessor existsProcessor = postProcessorInstances.get(type);
          if (existsProcessor != null) {
            requestPostProcessors.put(mrId, existsProcessor);
            continue;
          }
          final RequestPostProcessor requestPostProcessor = type.newInstance();
          postProcessorInstances.put(type, requestPostProcessor);
          requestPostProcessors.put(mrId, requestPostProcessor);
        }
        return this;
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }

    public ConfigurationBuilder setDefaultResponseHandler(ResponseHandler responseHandler) {
      this.defaultResponseHandler = responseHandler;
      return this;
    }

    public ConfigurationBuilder addMappedRequest(MappedRequest request) {
      checkNotNull(request);
      mappedRequests.put(request.getId(), request);
      return this;
    }

    public ConfigurationBuilder addResponseHandler(String mrId, Class<? extends ResponseHandler> responseHandlerType) {
      checkNotNull(responseHandlerType);
      if (!typeToresponseHandlers.containsKey(responseHandlerType)) {
        try {
          typeToresponseHandlers.put(responseHandlerType, responseHandlerType.newInstance());
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      }
      responseHandlers.put(mrId, typeToresponseHandlers.get(responseHandlerType));
      return this;
    }

    public ConfigurationBuilder parse(Class<?> mapperClass) {
      checkNotNull(mapperClass);

      final Method[] methods = mapperClass.getMethods();
      if (methods == null || methods.length == 0) {
        return this;
      }
      final PostProcessors typeProcessors = mapperClass.getDeclaredAnnotation(PostProcessors.class);
      for (Method method : methods) {
        final Request request = method.getDeclaredAnnotation(Request.class);
        if (request == null) {
          //不需要mapper
          continue;
        }
        final MappedRequest.MappedRequestBuilder mappedRequestBuilder =
            MappedRequest.newBuilder(method.getGenericReturnType());
        final String rawUrl = request.value();
        final int idx = rawUrl.lastIndexOf('#');
        if (idx < 0) {
          mappedRequestBuilder.setUrl(rawUrl);
        } else if (idx < rawUrl.length() - 1 && rawUrl.charAt(idx + 1) != '{') {
          mappedRequestBuilder.setUrl(rawUrl.substring(0, idx));
          if (idx != rawUrl.length() - 1) {
            mappedRequestBuilder.setAttach(rawUrl.substring(idx + 1));
          }
        } else {
          mappedRequestBuilder.setUrl(rawUrl);
        }

        final String mrId = MappedRequestUtils.buildMappedRequestId(mapperClass, method);
        mappedRequestBuilder.setId(mrId);

        //detect request method
        final GET get = method.getDeclaredAnnotation(GET.class);
        if (get != null) {
          mappedRequestBuilder.setHttpMethod(HttpMethod.GET);
        }
        final POST post = method.getDeclaredAnnotation(POST.class);
        if (post != null) {
          mappedRequestBuilder.setHttpMethod(HttpMethod.POST);
          mappedRequestBuilder.setEntityType(post.entity());
        }
        final PUT put = method.getDeclaredAnnotation(PUT.class);
        if (put != null) {
          mappedRequestBuilder.setHttpMethod(HttpMethod.PUT);
        }
        final DELETE delete = method.getDeclaredAnnotation(DELETE.class);
        if (delete != null) {
          mappedRequestBuilder.setHttpMethod(HttpMethod.DELETE);
        }

        Response response = method.getDeclaredAnnotation(Response.class);
        if (response == null) {
          response = mapperClass.getDeclaredAnnotation(Response.class);
        }
        if (response != null) {
          addResponseHandler(mrId, response.value());
        }

        addMappedRequest(mappedRequestBuilder.build());

        if (typeProcessors != null) {
          addRequestPostProcessor(mrId, typeProcessors.value());
        }
        final PostProcessors postProcessors = method.getDeclaredAnnotation(PostProcessors.class);
        if (postProcessors != null) {
          addRequestPostProcessor(mrId, postProcessors.value());
        }
      }
      return this;
    }

    public Configuration build() {
      if (defaultResponseHandler == null) {
        defaultResponseHandler = new ToStringResponseHandler();
      }
      if (httpClientFactory == null) {
        this.httpClientFactory = new DefaultHttpClientFactory();
      }
      return new Configuration(mappedRequests, requestPostProcessors,
          responseHandlers, defaultResponseHandler, httpClientFactory);
    }
  }

}
