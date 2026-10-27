package com.webmodel.agent.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ToolExecutor {

    private static final Logger log = LoggerFactory.getLogger(ToolExecutor.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    public record ToolResult(boolean success, String output, String toolName, boolean needsAuth, String requestedPath) {
        public ToolResult(boolean success, String output, String toolName) {
            this(success, output, toolName, false, null);
        }
        public ToolResult(boolean success, String output, String toolName, boolean needsAuth, String requestedPath) {
            this.success = success;
            this.output = output;
            this.toolName = toolName;
            this.needsAuth = needsAuth;
            this.requestedPath = requestedPath;
        }
    }

    public List<ToolResult> executeToolsInReply(String reply, String workingDir) {
        return executeToolsInReply(reply, workingDir, Collections.emptyList());
    }

    public List<ToolResult> executeToolsInReply(String reply, String workingDir, List<String> authorizedPaths) {
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
            ToolResult tr = executeStructuredCall(json, dir, authorizedPaths);
            if (tr != null) results.add(tr);
        }

        if (foundStructured) {
            return results;
        }

        Pattern bashPattern = Pattern.compile("```bash\\s*\\n(.*?)```", Pattern.DOTALL);
        Matcher bashMatcher = bashPattern.matcher(reply);
        while (bashMatcher.find()) {
            results.add(executeBash(bashMatcher.group(1).trim(), dir, authorizedPaths));
        }

        Pattern fileReadPattern = Pattern.compile("```file:read\\s+(.*?)```", Pattern.DOTALL);
        Matcher fileReadMatcher = fileReadPattern.matcher(reply);
        while (fileReadMatcher.find()) {
            results.add(readFile(fileReadMatcher.group(1).trim(), dir, authorizedPaths));
        }

        Pattern fileWritePattern = Pattern.compile("```file:write\\s+(\\S+)\\s*\\n(.*?)```", Pattern.DOTALL);
        Matcher fileWriteMatcher = fileWritePattern.matcher(reply);
        while (fileWriteMatcher.find()) {
            results.add(writeFile(fileWriteMatcher.group(1).trim(), fileWriteMatcher.group(2).trim(), dir, authorizedPaths));
        }

        return results;
    }

    public boolean hasToolCalls(String reply) {
        return reply.contains("<tool_call>")
                || reply.contains("```bash")
                || reply.contains("```file:read")
                || reply.contains("```file:write");
    }

    private ToolResult executeStructuredCall(String json, String workingDir, List<String> authorizedPaths) {
        try {
            JsonNode node = objectMapper.readTree(json);
            String tool = node.has("tool") ? node.get("tool").asText() : "";
            JsonNode args = node.has("args") ? node.get("args") : objectMapper.createObjectNode();

            return switch (tool) {
                case "bash" -> executeBash(args.has("command") ? args.get("command").asText() : "", workingDir, authorizedPaths);
                case "file.read", "file:read" -> readFile(args.has("path") ? args.get("path").asText() : "", workingDir, authorizedPaths);
                case "file.write", "file:write" -> writeFile(
                        args.has("path") ? args.get("path").asText() : "",
                        args.has("content") ? args.get("content").asText() : "",
                        workingDir, authorizedPaths);
                case "file.list", "file:list" -> executeBash(
                        "ls -la" + (args.has("path") ? " " + args.get("path").asText() : ""), workingDir, authorizedPaths);
                case "file.search", "file:search" -> executeBash(
                        "grep -rn \"" + (args.has("pattern") ? args.get("pattern").asText() : "") + "\""
                                + (args.has("path") ? " " + args.get("path").asText() : " ."), workingDir, authorizedPaths);
                default -> new ToolResult(false, "未知工具: " + tool, tool);
            };
        } catch (Exception e) {
            return new ToolResult(false, "解析工具调用失败: " + e.getMessage(), "parse_error");
        }
    }

    private ToolResult executeBash(String script, String workingDir, List<String> authorizedPaths) {
        try {
            Path wdPath = Paths.get(workingDir).toAbsolutePath().normalize();
            log.info("[bash] 执行命令, workingDir={}, script:\n{}", workingDir, script);
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
            log.info("[bash] 执行完成, exitCode={}, output:\n{}", exitCode, result);
            if (exitCode == 0) {
                return new ToolResult(true, result.isEmpty() ? "(执行成功，无输出)" : result, "bash");
            } else {
                return new ToolResult(false, "退出码: " + exitCode + "\n" + result, "bash");
            }
        } catch (Exception e) {
            return new ToolResult(false, "执行异常: " + e.getMessage(), "bash");
        }
    }

    private ToolResult readFile(String filePath, String workingDir, List<String> authorizedPaths) {
        try {
            Path path = resolvePath(filePath, workingDir);
            Path wdPath = Paths.get(workingDir).toAbsolutePath().normalize();
            if (!isPathAuthorized(path, wdPath, authorizedPaths)) {
                log.warn("[file.read] 路径超出授权范围: {} (workingDir={})", path, wdPath);
                return new ToolResult(false,
                    "[需要授权] 路径 " + path + " 超出工作目录 " + wdPath + "，需要用户授权后才能读取",
                    "file.read", true, path.toString());
            }
            if (!Files.exists(path)) {
                return new ToolResult(false, "文件不存在: " + path, "file.read");
            }
            log.info("[file.read] 读取文件: {}", path);
            String content = Files.readString(path);
            log.info("[file.read] 读取完成, 长度={}", content.length());
            return new ToolResult(true, content, "file.read");
        } catch (Exception e) {
            return new ToolResult(false, "读取异常: " + e.getMessage(), "file.read");
        }
    }

    private ToolResult writeFile(String filePath, String content, String workingDir, List<String> authorizedPaths) {
        try {
            Path path = resolvePath(filePath, workingDir);
            Path wdPath = Paths.get(workingDir).toAbsolutePath().normalize();
            if (!isPathAuthorized(path, wdPath, authorizedPaths)) {
                log.warn("[file.write] 路径超出授权范围: {} (workingDir={})", path, wdPath);
                return new ToolResult(false,
                    "[需要授权] 路径 " + path + " 超出工作目录 " + wdPath + "，需要用户授权后才能写入",
                    "file.write", true, path.toString());
            }
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
            log.info("[file.write] 写入文件: {}, 内容长度={}", path, content.length());
            Files.writeString(path, content);
            log.info("[file.write] 写入完成: {}", path);
            return new ToolResult(true, "已写入: " + path, "file.write");
        } catch (Exception e) {
            return new ToolResult(false, "写入异常: " + e.getMessage(), "file.write");
        }
    }

    private Path resolvePath(String filePath, String workingDir) {
        Path path = Paths.get(filePath);
        if (path.isAbsolute()) {
            return path.toAbsolutePath().normalize();
        }
        return Paths.get(workingDir).toAbsolutePath().resolve(path).normalize();
    }

    private boolean isPathAuthorized(Path targetPath, Path workingDir, List<String> authorizedPaths) {
        Path normalizedTarget = targetPath.toAbsolutePath().normalize();
        Path normalizedWD = workingDir.toAbsolutePath().normalize();
        if (normalizedTarget.startsWith(normalizedWD)) {
            return true;
        }
        if (authorizedPaths != null) {
            for (String authPath : authorizedPaths) {
                if (authPath == null || authPath.isBlank()) continue;
                Path normalizedAuth = Paths.get(authPath).toAbsolutePath().normalize();
                if (normalizedTarget.startsWith(normalizedAuth)) {
                    return true;
                }
            }
        }
        return false;
    }
}
