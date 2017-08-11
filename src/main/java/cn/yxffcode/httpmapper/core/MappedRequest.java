package cn.yxffcode.httpmapper.core;

import cn.yxffcode.httpmapper.core.reflection.ObjectTraversal;
import cn.yxffcode.httpmapper.core.text.TextTemplate;
import com.google.common.base.Objects;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Map;

/**
 * @author gaohang on 8/6/17.
 */
public class MappedRequest {

  public static MappedRequestBuilder newBuilder(Type returnType) {
    return new MappedRequestBuilder(returnType);
  }

  private final String id;
  private final String url;
  private final String attach;
  private final HttpMethod httpMethod;
  private final EntityType entityType;
  private final TextTemplate textTemplate;
  private final Type returnType;

  private MappedRequest(String id,
                        String url,
                        String attach,
                        HttpMethod httpMethod,
                        EntityType entityType,
                        Type returnType) {
    this.id = id;
    this.url = url;
    this.attach = attach;
    this.httpMethod = httpMethod;
    this.entityType = entityType;
    this.textTemplate = new TextTemplate(url);
    this.returnType = returnType;
  }

  public String getId() {
    return id;
  }

  public String getUrl() {
    return url;
  }

  public HttpMethod getHttpMethod() {
    return httpMethod;
  }

  public String getAttach() {
    return attach;
  }

  public EntityType getEntityType() {
    return entityType;
  }

  public Type getReturnType() {
    return returnType;
  }

  public Map<String, Object> resolveParams(Object parameterObject) {
    if (parameterObject == null) {
      return Collections.emptyMap();
    }
    final ObjectTraversal objectTraversal = ObjectTraversal.wrap(parameterObject);

    return textTemplate.params(objectTraversal);
  }

  public String rendUrl(Map<String, Object> params) {
    return textTemplate.rend(params).toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    MappedRequest that = (MappedRequest) o;
    return Objects.equal(id, that.id) &&
        Objects.equal(url, that.url) &&
        Objects.equal(attach, that.attach) &&
        httpMethod == that.httpMethod &&
        entityType == that.entityType;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(id, url, attach, httpMethod, entityType);
  }

  @Override
  public String toString() {
    return "MappedRequest{" +
        "id='" + id + '\'' +
        ", url='" + url + '\'' +
        ", attach='" + attach + '\'' +
        ", httpMethod=" + httpMethod +
        ", entityType=" + entityType +
        '}';
  }

  public static final class MappedRequestBuilder {
    private String id;
    private String url;
    private String attach;
    private HttpMethod httpMethod;
    private EntityType entityType;
    private final Type returnType;

    public MappedRequestBuilder(Type returnType) {
      this.returnType = returnType;
    }

    public MappedRequestBuilder setId(String id) {
      this.id = id;
      return this;
    }

    public MappedRequestBuilder setUrl(String url) {
      this.url = url;
      return this;
    }

    public MappedRequestBuilder setAttach(String attach) {
      this.attach = attach;
      return this;
    }

    public MappedRequestBuilder setHttpMethod(HttpMethod httpMethod) {
      this.httpMethod = httpMethod;
      return this;
    }

    public MappedRequestBuilder setEntityType(EntityType entityType) {
      this.entityType = entityType;
      return this;
    }

    public MappedRequest build() {
      if (httpMethod == null) {
        httpMethod = HttpMethod.GET;
      }
      return new MappedRequest(id, url, attach, httpMethod, entityType, returnType);
    }
  }
}