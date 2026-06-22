package com.joy.joymall.thirdparty;

import com.aliyun.oss.OSSClient;
import com.joy.joymall.thirdparty.component.SmsComponent;
import com.joy.joymall.thirdparty.utils.HttpUtils;
import org.apache.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

@SpringBootTest
class JoymallThirdPartyApplicationTests {
    @Resource
    private OSSClient ossClient;

    @Autowired
    private SmsComponent smsComponent;

    @Test
    void contextLoads() {
    }

    @Test
    void testFile() throws FileNotFoundException {

//        // Endpoint以杭州为例，其它Region请按实际情况填写。
//        String endpoint = "http://oss-cn-qingdao.aliyuncs.com";
//        // 云账号AccessKey有所有API访问权限，建议遵循阿里云安全最佳实践，创建并使用RAM子账号进行API访问或日常运维，请登录 https://ram.console.aliyun.com 创建。
//        String accessKeyId = "your-access-key";
//        String accessKeySecret = "your-secret-key";

        // 创建OSSClient实例。
//        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);

        // 上传文件流。
        InputStream inputStream = new FileInputStream("C:\\path\\to\\test\\image.jpg");
        ossClient.putObject("your-bucket", "bird.jpg", inputStream);

        // 关闭OSSClient。
        ossClient.shutdown();

        System.out.println("上传成功~~");
    }

    @Test
    void testYZM() {
        String host = "https://gyytz.market.alicloudapi.com";
        String path = "/sms/smsSend";
        String method = "POST";
        String appcode = "your-appcode";
        Map<String, String> headers = new HashMap<String, String>();
        //最后在header中的格式(中间是英文空格)为Authorization:APPCODE 83359fd73fe94948385f570e3c139105
        headers.put("Authorization", "APPCODE " + appcode);
        Map<String, String> querys = new HashMap<String, String>();
        querys.put("mobile", "13800000000");
        querys.put("param", "**code**:5211314,**minute**:5");
        querys.put("smsSignId", "your-sms-sign-id");
        querys.put("templateId", "your-template-id");
        Map<String, String> bodys = new HashMap<String, String>();


        try {
            HttpResponse response = HttpUtils.doPost(host, path, method, headers, querys, bodys);
            System.out.println(response.toString());
            //获取response的body
            //System.out.println(EntityUtils.toString(response.getEntity()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    void testSms(){
        smsComponent.sendSmsCode("13800000000","1314521");
    }

}
