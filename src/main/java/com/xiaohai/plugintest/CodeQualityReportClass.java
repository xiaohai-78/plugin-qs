package com.xiaohai.plugintest;

import cn.hutool.json.JSONObject;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ChangeListManager;
import com.intellij.openapi.vcs.changes.ContentRevision;
import difflib.Delta;
import difflib.DiffUtils;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.jetbrains.annotations.NotNull;

import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

public class CodeQualityReportClass extends AnAction {

    private static final String requestUrl = "";
    private static final String apiKey = "your-api-key";


    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) return;

        // 获取选中的改动文件
        Collection<Change> allChanges = ChangeListManager.getInstance(project).getAllChanges();

        for (Change change : allChanges) {
            // 获取改动前后的内容
            ContentRevision beforeRevision = change.getBeforeRevision();
            ContentRevision afterRevision = change.getAfterRevision();

            if (afterRevision != null) {
                String filePath = afterRevision.getFile().getPath();
                String newContent = null;
                String oldContent = null;

                try {
                    newContent = afterRevision.getContent();
                    if (beforeRevision != null) {
                        oldContent = beforeRevision.getContent();
                    }
                } catch (VcsException ex) {
                    System.out.println("Error getting content for file: " + filePath);
                    continue; // 继续处理下一个改动
                }

                System.out.println("File: " + filePath);

                if (newContent != null) {
                    // 计算改动的内容
                    String changes = calculateDifferences(oldContent, newContent);

                    System.out.println("Changes:\n" + changes);
                    // 发送 POST 请求
                    String answ = sendLLMRequest("abc", filePath, changes);
                }
            }
        }
    }

    // 计算两个版本内容的差异
    private String calculateDifferences(String oldContent, String newContent) {
        // 如果没有旧内容，直接返回新内容作为差异
        if (oldContent == null || oldContent.isEmpty()) {
            return newContent;
        }

        // 将内容按行分割
        List<String> oldLines = Arrays.asList(oldContent.split("\n"));
        List<String> newLines = Arrays.asList(newContent.split("\n"));

        // 计算差异
        List<Delta<String>> deltas = DiffUtils.diff(oldLines, newLines).getDeltas();

        // 格式化差异输出
        StringBuilder diffOutput = new StringBuilder();
        for (Delta<String> delta : deltas) {
            diffOutput.append(delta.toString()).append("\n");
        }

        return diffOutput.toString();
    }

    private String sendLLMRequest(String user, String filePath, String changes) {
        JSONObject jsonObject = new JSONObject();

        String userName = user;
        // 添加inputs键，其值为一个空的JSONObject
        jsonObject.put("inputs", new JSONObject());

        // 添加query键
        jsonObject.put("query", "文件名称：" + filePath + "\n" + "改动内容：" + changes);

        // 添加response_mode键
        jsonObject.put("response_mode", "blocking");

        // 添加conversation_id键，其值为一个空字符串
        jsonObject.put("conversation_id", "");

        // 添加user键
        if (user == null) {
            userName = "abc-123";
        }
        jsonObject.put("user", userName);

        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer " + apiKey);
        headers.put("Content-Type", "application/json");

        RequestBody body = RequestBody.create(
                okhttp3.MediaType.parse("application/json; charset=utf-8"),
                jsonObject.toString());

        OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder();
        // 设置连接超时时间为60秒
        clientBuilder.connectTimeout(60, TimeUnit.SECONDS);
        // 设置读取超时时间为60秒
        clientBuilder.readTimeout(60, TimeUnit.SECONDS);
        // 设置写入超时时间为60秒
        clientBuilder.writeTimeout(60, TimeUnit.SECONDS);

        // 使用Builder创建OkHttpClient实例
        OkHttpClient client = clientBuilder.build();

        Request request = new Request.Builder()
                .url(requestUrl)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .post(body)
                .build();
        try {
            Response response = client.newCall(request).execute();
            ResponseBody responseBody = response.body();

            if (responseBody != null) {
                String responseBodyString = responseBody.string();
                System.out.println(responseBodyString);
                return responseBodyString;
            } else {
                System.out.println("Response body is null");
                return "Response body is null";
            }
        } catch (IOException e) {
            e.printStackTrace();
            return "Response body is null";
        }
    }




    public static void sendEmail(String toEmail, String subject, String body) {
        // 发件人的电子邮件地址和密码
        final String fromEmail = "your-email@example.com"; // 替换为您的电子邮件地址
        final String password = "your-email-password"; // 替换为您的电子邮件密码

        // 设置邮件服务器的属性
        Properties props = new Properties();
        props.put("mail.smtp.host", "smtp.example.com"); // SMTP 服务器地址，例如 smtp.gmail.com
        props.put("mail.smtp.port", "587"); // SMTP 服务器端口
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true"); // 启用 STARTTLS

        // 创建一个会话对象并传递身份验证信息
        Session session = Session.getInstance(props, new javax.mail.Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(fromEmail, password);
            }
        });

        try {
            // 创建邮件消息对象
            Message message = new MimeMessage(session);

            // 设置发件人
            message.setFrom(new InternetAddress(fromEmail));

            // 设置收件人
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));

            // 设置邮件主题
            message.setSubject(subject);

            // 设置邮件内容
            message.setText(body);

            // 发送邮件
            Transport.send(message);

            System.out.println("Email sent successfully.");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

