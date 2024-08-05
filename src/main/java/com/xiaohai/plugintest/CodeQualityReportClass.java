package com.xiaohai.plugintest;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ChangeListManager;
import com.intellij.openapi.vcs.changes.ContentRevision;
import difflib.Delta;
import difflib.DiffUtils;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.jetbrains.annotations.NotNull;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static com.xiaohai.plugintest.ReportConfigurable.EMAIL_KEY;

public class CodeQualityReportClass extends AnAction {

    private static final String requestUrl = "http://**.com.cn/v1/chat-messages";
    private static final String apiKey = "***";
    // 发件人的电子邮件地址和应用密码（授权码）
    private static final String appPassword = "**"; // 替换为您的应用密码或授权码
    private static final String fromEmail = "xiao**@163.com";

    // 无参数的公共构造函数
    public CodeQualityReportClass() {
        super();
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) return;

        // 检查是否配置了邮箱
        if (!checkEmailConfig()) return;

        // 获取选中的改动文件
        Collection<Change> allChanges = ChangeListManager.getInstance(project).getAllChanges();

        // 发送通知，提示用户正在处理
        Notifications.Bus.notify(new Notification(
                Notifications.SYSTEM_MESSAGES_GROUP_ID,
                "Code Quality Report",
                "正在快马加鞭帮您生成报告呢，请稍等几分钟！",
                NotificationType.INFORMATION
        ), project);

        // 获取选中的改动文件并异步处理
        CompletableFuture.runAsync(() -> processAllChangesAsync(allChanges))
                .thenRun(() -> {
                    // 处理完成后的逻辑，例如通知用户任务已完成
                    System.out.println("Processing completed successfully.");
                    Notifications.Bus.notify(new Notification(
                            Notifications.SYSTEM_MESSAGES_GROUP_ID,
                            "Code Quality Report",
                            "报告异步后台生成执行完成",
                            NotificationType.INFORMATION
                    ), project);
                })
                .exceptionally(ex -> {
                    // 处理异常情况，例如记录日志或通知用户
                    System.err.println("An error occurred while processing changes: " + ex.getMessage());
                    Notifications.Bus.notify(new Notification(
                            Notifications.SYSTEM_MESSAGES_GROUP_ID,
                            "Code Quality Report 执行失败",
                            ex.getMessage(),
                            NotificationType.WARNING
                    ), project);
                    return null;
                });
//        send163HTLMEmail("854262144@qq.com", "测试报告", "测试报告内容");
    }

    private void processAllChangesAsync(Collection<Change> allChanges) {
        StringBuilder emailContent = new StringBuilder();
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
                    String Suggest = sendLLMRequest("abc", filePath, changes);
                    emailContent.append("File: ").append(filePath).append("\n");
                    emailContent.append("Suggest:\n").append(Suggest).append("\n\n");
                }
            }
        }
        System.out.println(emailContent);
        sendEmail(generateSubject(), emailContent.toString());
    }

    public void sendEmail(String subject, String body) {
        // 从配置中获取收件人邮箱地址
        PropertiesComponent properties = PropertiesComponent.getInstance();
        String toEmail = properties.getValue(EMAIL_KEY, "");
        if (toEmail.isEmpty()) {
            Notifications.Bus.notify(new Notification(
                    Notifications.SYSTEM_MESSAGES_GROUP_ID,
                    "Code Quality Report 执行失败",
                    "您没有配置接收报告的邮箱，请在Setting -> Tools -> Code Quality Report, 中设置您的邮箱地址。",
                    NotificationType.WARNING
            ));
            return;
        }
        try {
            send163Email(toEmail, subject, body);
//            send163HTLMEmail(toEmail, subject, body);
        }catch (Exception e){
            Notifications.Bus.notify(new Notification(
                    Notifications.SYSTEM_MESSAGES_GROUP_ID,
                    "Code Quality Report 执行失败",
                    "邮件发送失败！" + e.getMessage(),
                    NotificationType.WARNING
            ));
        }
    }

    public static String generateSubject() {
        // 获取当前时间
        Date now = new Date();

        // 格式化时间为所需的字符串格式
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        // 生成主题
        String formattedDate = dateFormat.format(now);
        String subject = formattedDate + " Code Quality Report";

        return subject;
    }

    public Boolean checkEmailConfig() {
        PropertiesComponent properties = PropertiesComponent.getInstance();
        if (properties.getValue(EMAIL_KEY, "").isEmpty()) {
            Notifications.Bus.notify(new Notification(
                    Notifications.SYSTEM_MESSAGES_GROUP_ID,
                    "Code quality report 执行失败",
                    "您没有配置接收报告的邮箱，请在Setting -> Tools -> Code Quality Report, 中设置您的邮箱地址。",
                    NotificationType.WARNING
            ));
            return false;
        }
        return true;
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
                JSONObject responseJson = (JSONObject) JSONValue.parse(responseBodyString);

                // 打印整个JSON对象
                System.out.println(responseJson.toJSONString());

                // 获取特定字段的值
                String answer = (String) responseJson.get("answer");

                return answer;
            } else {
                System.out.println("LLMResponse body is null");
                return "LLMResponse body is null";
            }
        } catch (IOException e) {
            e.printStackTrace();
            return "LLMResponse body is null";
        }
    }

    public void send163Email(String toEmail, String subject, String body) {
        // 163 邮箱 SMTP 配置信息
        String host = "smtp.163.com";

        // 创建一个 Properties 对象，用于设置 SMTP 服务器信息
        Properties props = new Properties();
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.auth", "true");

        // 创建一个 Authenticator 对象，用于进行 SMTP 用户名和密码认证
        Authenticator authenticator = new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(fromEmail, appPassword);
            }
        };

        // 创建一个 Session 对象，用于与 SMTP 服务器进行通信
        Session session = Session.getDefaultInstance(props, authenticator);

        try {
            // 创建一个 MimeMessage 对象
            MimeMessage message = new MimeMessage(session);
            // 设置发件人地址
            message.setFrom(new InternetAddress(fromEmail));
            // 设置收件人地址
            message.setRecipient(Message.RecipientType.TO, new InternetAddress(toEmail));
            // 设置邮件主题
            message.setSubject(subject);
            // 设置邮件内容
            message.setText(body);

            // 发送邮件
            Transport.send(message);
            System.out.println("邮件发送成功！");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void send163HTLMEmail(String toEmail, String subject, String body) {
        // 163 邮箱 SMTP 配置信息
        String host = "smtp.163.com";
        String htmlContent = """
        <!DOCTYPE html>
        <html lang="en">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>Markdown to HTML Example</title>
            <style>
                body {
                    font-family: Arial, sans-serif;
                    line-height: 1.6;
                    padding: 20px;
                    background-color: #f4f4f4;
                }
                h1 {
                    color: #333;
                }
                a {
                    color: #1a73e8;
                    text-decoration: none;
                }
                a:hover {
                    text-decoration: underline;
                }
                ul {
                    margin: 0;
                    padding: 0;
                    list-style-type: disc;
                }
                li {
                    margin: 5px 0;
                }
            </style>
        </head>
        <body>
            """ + body + """
        </body>
        </html>
        """;
        // 创建一个 Properties 对象，用于设置 SMTP 服务器信息
        Properties props = new Properties();
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.auth", "true");

        // 创建一个 Authenticator 对象，用于进行 SMTP 用户名和密码认证
        Authenticator authenticator = new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(fromEmail, appPassword);
            }
        };

        // 创建一个 Session 对象，用于与 SMTP 服务器进行通信
        Session session = Session.getDefaultInstance(props, authenticator);

        try {
            // 创建一个 MimeMessage 对象
            MimeMessage message = new MimeMessage(session);
            // 设置发件人地址
            message.setFrom(new InternetAddress(fromEmail));
            // 设置收件人地址
            message.setRecipient(Message.RecipientType.TO, new InternetAddress(toEmail));
            // 设置邮件主题
            message.setSubject(subject);
            // 设置邮件内容
//            message.setText(body);
            message.setContent(htmlContent, "text/html; charset=UTF-8");

            // 发送邮件
            Transport.send(message);
            System.out.println("邮件发送成功！");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}

