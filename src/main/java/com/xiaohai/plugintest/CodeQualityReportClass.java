package com.xiaohai.plugintest;

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

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

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


}

