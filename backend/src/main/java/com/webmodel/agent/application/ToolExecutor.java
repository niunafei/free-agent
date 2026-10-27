package com.webmodel.agent.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    private final ObjectMapper objectMapper = new ObjectMapper();

    public record ToolResult(boolean success, String output, String toolName) {}

    public List<ToolResult> executeToolsInReply(String reply, String workingDir) {
        List<ToolResult> results = new ArrayList<>();
        String dir = (workingDir != null && !workingDir.isBlank()) ? workingDir : System.getProperty("user.home");

        String openTag = "<tool_call>";
        String closeTag = "</tool_call>";
        Pattern toolCallPattern = Pattern.compile(
                openTag + "\\s*\\n(.*?)\\n\\s*" + closeTag, Pattern.DOTALL);
        Matcher toolCallMatcher = toolCallPattern.matcher(reply);
        boolean foundStructured = false;
        while (toolCallMatcher.find()) {
            foundStructured = true;
            String json = toolCallMatcher.group(1).trim();
            ToolResult tr = executeStructuredCall(json, dir);
            if (tr != null) results.add(tr);
        }

        if (foundStructured) {
            return results;
        }

        Pattern bashPattern = Pattern.compile("```bash\\s*\\n(.*?)```", Pattern.DOTALL);
        Matcher bashMatcher = bashPattern.matcher(reply);
        while (bashMatcher.find()) {
            results.add(executeBash(bashMatcher.group(1).trim(), dir));
        }

        Pattern fileReadPattern = Pattern.compile("```file:read\\s+(.*?)```", Pattern.DOTALL);
        Matcher fileReadMatcher = fileReadPattern.matcher(reply);
        while (fileReadMatcher.find()) {
            results.add(readFile(fileReadMatcher.group(1).trim(), dir));
        }

        Pattern fileWritePattern = Pattern.compile("```file:write\\s+(\\S+)\\s*\\n(.*?)```", Pattern.DOTALL);
        Matcher fileWriteMatcher = fileWritePattern.matcher(reply);
        while (fileWriteMatcher.find()) {
            results.add(writeFile(fileWriteMatcher.group(1).trim(), fileWriteMatcher.group(2).trim(), dir));
        }

        return results;
    }

    public boolean hasToolCalls(String reply) {
        return reply.contains("<tool_call>")
                || reply.contains("```bash")
                || reply.contains("```file:read")
                || reply.contains("```file:write");
    }

    private ToolResult executeStructuredCall(String json, String workingDir) {
        try {
            JsonNode node = objectMapper.readTree(json);
            String tool = node.has("tool") ? node.get("tool").asText() : "";
            JsonNode args = node.has("args") ? node.get("args") : objectMapper.createObjectNode();

            return switch (tool) {
                case "bash" -> executeBash(args.has("command") ? args.get("command").asText() : "", workingDir);
                case "file.read", "file:read" -> readFile(args.has("path") ? args.get("path").asText() : "", workingDir);
                case "file.write", "file:write" -> writeFile(
                        args.has("path") ? args.get("path").asText() : "",
                        args.has("content") ? args.get("content").asText() : "",
                        workingDir);
                case "file.list", "file:list" -> executeBash(
                        "ls -la" + (args.has("path") ? " " + args.get("path").asText() : ""), workingDir);
                case "file.search", "file:search" -> executeBash(
                        "grep -rn \"" + (args.has("pattern") ? args.get("pattern").asText() : "") + "\""
                                + (args.has("path") ? " " + args.get("path").asText() : " ."), workingDir);
                default -> new ToolResult(false, "未知工具: " + tool, tool);
            };
        } catch (Exception e) {
            return new ToolResult(false, "解析工具调用失败: " + e.getMessage(), "parse_error");
        }
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
                return new ToolResult(false, "文件不存在: " + path, "file.read");
            }
            String content = Files.readString(path);
            return new ToolResult(true, content, "file.read");
        } catch (Exception e) {
            return new ToolResult(false, "读取异常: " + e.getMessage(), "file.read");
        }
    }

    private ToolResult writeFile(String filePath, String content, String workingDir) {
        try {
            Path path = resolvePath(filePath, workingDir);
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
            Files.writeString(path, content);
            return new ToolResult(true, "已写入: " + path, "file.write");
        } catch (Exception e) {
            return new ToolResult(false, "写入异常: " + e.getMessage(), "file.write");
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
