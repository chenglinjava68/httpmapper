package cn.yxffcode.httpmapper.core;

import cn.yxffcode.httpmapper.core.cfg.Configuration;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

/**
 * @author gaohang on 8/11/17.
 */
public class MappedProxyTest {

  @Test
  public void handleInvocation() throws ExecutionException, InterruptedException, IOException, NoSuchMethodException {

    //初始化框架
    final Configuration configuration = Configuration.newBuilder()
        .parse(TestServiceFacade.class)
        .build();

    //获取mapper接口的实例并调用接口上的方法
    final TestServiceFacade testServiceFacade = configuration.newMapper(TestServiceFacade.class);
    JsonResult<TestBean> result = testServiceFacade.get("name").get();
    System.out.println(result);

    result = testServiceFacade.post("name");
    System.out.println(result);

    System.out.println(testServiceFacade.getString("name"));

  }

}