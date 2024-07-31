package com.xiaohai.plugintest;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.ide.util.PropertiesComponent;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class ReportConfigurable implements Configurable {
    private JPanel myPanel;
    private JTextField emailField;

    private static final String EMAIL_KEY = "report.email";

    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return "Report Settings";
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        myPanel = new JPanel();
        emailField = new JTextField(20);

        // 从配置中加载已保存的邮箱
        PropertiesComponent properties = PropertiesComponent.getInstance();
        String savedEmail = properties.getValue(EMAIL_KEY, "");
        emailField.setText(savedEmail);

        myPanel.add(new JLabel("Email:"));
        myPanel.add(emailField);
        return myPanel;
    }

    @Override
    public boolean isModified() {
        PropertiesComponent properties = PropertiesComponent.getInstance();
        String savedEmail = properties.getValue(EMAIL_KEY, "");
        String currentEmail = emailField.getText();
        return !savedEmail.equals(currentEmail);
    }

    @Override
    public void apply() throws ConfigurationException {
        PropertiesComponent properties = PropertiesComponent.getInstance();
        String currentEmail = emailField.getText();

        // 保存当前的邮箱到配置中
        properties.setValue(EMAIL_KEY, currentEmail);
    }

    @Override
    public void reset() {
        PropertiesComponent properties = PropertiesComponent.getInstance();
        String savedEmail = properties.getValue(EMAIL_KEY, "");
        emailField.setText(savedEmail);
    }
}
