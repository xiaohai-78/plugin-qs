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
import org.jetbrains.annotations.NotNull;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

public class CodeQualityReportClass extends AnAction {

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

    private void sendLLMRequest(String user, String filePath, String changes) {
        try {
            // 设置请求的 URL
            URL url = new URL("http://dify.xxx.com.cn/v1/chat-messages");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Authorization", "Bearer {api_key}"); // 替换 {api_key} 为实际的 API 密钥
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            // 构建 JSON 请求体
            String jsonInputString = """
        {
            "inputs": {},
            "query": "changes filePath: %s . changes code: [ %s ]",
            "response_mode": "blocking",
            "conversation_id": "",
            "user": "%s"
        }
        """.formatted(filePath, changes, user);

            // 发送请求体
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonInputString.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            // 读取响应
            try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"))) {
                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }

                // 解析响应 JSON，提取 answer 字段
                JSONObject jsonResponse = new JSONObject(response.toString());
                String answer = jsonResponse.getStr("answer", "No answer provided");

                System.out.println("Response from server: " + answer);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
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

