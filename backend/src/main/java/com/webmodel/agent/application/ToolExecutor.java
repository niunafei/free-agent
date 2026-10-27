package com.webmodel.agent.application;

import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ToolExecutor {

    public record ToolResult(boolean success, String output, String toolName) {}

    public List<ToolResult> executeToolsInReply(String reply, String workingDir) {
        List<ToolResult> results = new ArrayList<>();
        String dir = (workingDir != null && !workingDir.isBlank()) ? workingDir : System.getProperty("user.home");

        Pattern bashPattern = Pattern.compile("```bash\\s*\\n(.*?)```", Pattern.DOTALL);
        Matcher bashMatcher = bashPattern.matcher(reply);
        while (bashMatcher.find()) {
            String script = bashMatcher.group(1).trim();
            results.add(executeBash(script, dir));
        }

        Pattern fileReadPattern = Pattern.compile("```file:read\\s+(.*?)```", Pattern.DOTALL);
        Matcher fileReadMatcher = fileReadPattern.matcher(reply);
        while (fileReadMatcher.find()) {
            String filePath = fileReadMatcher.group(1).trim();
            results.add(readFile(filePath, dir));
        }

        Pattern fileWritePattern = Pattern.compile("```file:write\\s+(\\S+)\\s*\\n(.*?)```", Pattern.DOTALL);
        Matcher fileWriteMatcher = fileWritePattern.matcher(reply);
        while (fileWriteMatcher.find()) {
            String filePath = fileWriteMatcher.group(1).trim();
            String content = fileWriteMatcher.group(2).trim();
            results.add(writeFile(filePath, content, dir));
        }

        return results;
    }

    public boolean hasToolCalls(String reply) {
        return reply.contains("```bash") || reply.contains("```file:read") || reply.contains("```file:write");
    }

    private ToolResult executeBash(String script, String workingDir) {
        try {
            ProcessBuilder pb = new ProcessBuilder("bash", "-c", script);
            pb.directory(new File(workingDir));
            pb.redirectErrorStream(true);
            Process process = pb.start();
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }
            int exitCode = process.waitFor();
            String result = output.toString().trim();
            if (exitCode == 0) {
                return new ToolResult(true, result.isEmpty() ? "(执行成功，无输出)" : result, "bash");
            } else {
                return new ToolResult(false, "退出码: " + exitCode + "\n" + result, "bash");
            }
        } catch (Exception e) {
            return new ToolResult(false, "执行异常: " + e.getMessage(), "bash");
        }
    }

    private ToolResult readFile(String filePath, String workingDir) {
        try {
            Path path = resolvePath(filePath, workingDir);
            if (!Files.exists(path)) {
                return new ToolResult(false, "文件不存在: " + path, "file:read");
            }
            String content = Files.readString(path);
            return new ToolResult(true, content, "file:read");
        } catch (Exception e) {
            return new ToolResult(false, "读取异常: " + e.getMessage(), "file:read");
        }
    }

    private ToolResult writeFile(String filePath, String content, String workingDir) {
        try {
            Path path = resolvePath(filePath, workingDir);
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
            Files.writeString(path, content);
            return new ToolResult(true, "已写入: " + path, "file:write");
        } catch (Exception e) {
            return new ToolResult(false, "写入异常: " + e.getMessage(), "file:write");
        }
    }

    private Path resolvePath(String filePath, String workingDir) {
        Path path = Paths.get(filePath);
        if (path.isAbsolute()) {
            return path;
        }
        return Paths.get(workingDir).resolve(path);
    }
}