package com.git.hui.springai.agentscope;

import io.agentscope.core.message.TextBlock;
import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.core.permission.PermissionContextState;
import io.agentscope.core.permission.PermissionDecision;
import io.agentscope.core.tool.ToolBase;
import io.agentscope.core.tool.ToolCallParam;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 网页抓取工具 —— 传入 HTTP 链接，提取网页正文内容
 * <p>
 * 继承 ToolBase，实现异步抓取能力，可在 AgentScope 的 ReActAgent 中使用。
 * 支持自动提取网页标题和正文，过滤掉导航、脚本、样式等干扰内容。
 *
 * @author YiHui
 * @date 2026/7/23
 */
public class WebSearchTool extends ToolBase {

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private static final int MAX_CONTENT_LENGTH = 8000;

    /** 匹配非正文标签 */
    private static final Pattern REMOVE_TAGS = Pattern.compile(
            "</?(?:script|style|nav|footer|header|aside|noscript|iframe|svg|form|button|input|select|textarea)[^>]*>.*?</(?:script|style|nav|footer|header|aside|noscript|iframe|svg|form|button|input|select|textarea)>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    /** 匹配 HTML 注释 */
    private static final Pattern COMMENT = Pattern.compile("<!--.*?-->", Pattern.DOTALL);

    /** 匹配 HTML 标签 */
    private static final Pattern HTML_TAG = Pattern.compile("<[^>]*>");

    /** 匹配多余空白行 */
    private static final Pattern MULTI_NEWLINE = Pattern.compile("\\n{3,}");

    public WebSearchTool() {
        super(
                ToolBase.builder()
                        .name("WebFetch")
                        .description("Fetch and extract the main text content from a given web page URL. " +
                                "Use this to read the full content of a web page. " +
                                "Returns the page title and cleaned body text (up to 8000 characters).")
                        .inputSchema(Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "url", Map.of(
                                                "type", "string",
                                                "description", "The full HTTP/HTTPS URL of the web page to fetch.")),
                                "required", List.of("url")))
                        .readOnly(true)
                        .concurrencySafe(true));
    }

    @Override
    public Mono<PermissionDecision> checkPermissions(Map<String, Object> toolInput, PermissionContextState context) {
        String url = (String) toolInput.get("url");
        if (url == null || (!url.startsWith("http://") && !url.startsWith("https://"))) {
            return Mono.just(PermissionDecision.deny("Only HTTP/HTTPS URLs are allowed."));
        }
        return Mono.just(PermissionDecision.allow("Web fetch is read-only."));
    }

    @Override
    public Mono<ToolResultBlock> callAsync(ToolCallParam param) {
        String url = (String) param.getInput().get("url");
        System.out.println("[WebFetch] 抓取页面: " + url);
        return doFetchAsync(url)
                .map(text ->
                        ToolResultBlock.builder()
                                .id(param.getToolUseBlock().getId())
                                .name(getName())
                                .output(List.of(TextBlock.builder().text(text).build()))
                                .build());
    }

    /**
     * 异步抓取网页
     */
    private Mono<String> doFetchAsync(String url) {
        return Mono.fromCallable(() -> doFetch(url));
    }

    /**
     * 同步抓取实现
     */
    private String doFetch(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                            "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
                    .timeout(Duration.ofSeconds(60))
                    .GET()
                    .build();

            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                return "页面请求失败，HTTP 状态码: " + response.statusCode();
            }

            String html = response.body();
            String title = extractTitle(html);
            String body = extractBody(html);

            if (body.isEmpty()) {
                return "未能从 " + url + " 提取到有效内容。";
            }

            // 截断过长内容
            if (body.length() > MAX_CONTENT_LENGTH) {
                body = body.substring(0, MAX_CONTENT_LENGTH) + "\n\n... [内容已截断，原文共 " +
                        body.length() + " 字符]";
            }

            StringBuilder result = new StringBuilder();
            if (!title.isEmpty()) {
                result.append("标题: ").append(title).append("\n");
            }
            result.append("来源: ").append(url).append("\n\n");
            result.append(body);

            return result.toString().trim();
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            return "页面抓取异常: " + e.getMessage();
        } catch (Exception e) {
            return "抓取失败: " + e.getMessage();
        }
    }

    /**
     * 提取页面标题
     */
    private String extractTitle(String html) {
        return extractBetween(html, "<title", "</title>");
    }

    /**
     * 提取页面正文
     */
    private String extractBody(String html) {
        // 1. 先尝试从 <body> 中提取
        String body = extractBetween(html, "<body", "</body>");
        if (body.isEmpty()) {
            body = html;
        }

        // 2. 去除注释
        body = COMMENT.matcher(body).replaceAll("");

        // 3. 去除 script / style / nav / footer 等非正文标签
        body = REMOVE_TAGS.matcher(body).replaceAll("");

        // 4. 去除所有剩余 HTML 标签
        body = HTML_TAG.matcher(body).replaceAll("");

        // 5. 解码 HTML 实体
        body = decodeHtmlEntities(body);

        // 6. 压缩多余空白行
        body = body.lines()
                .map(String::strip)
                .filter(line -> !line.isEmpty())
                .reduce((a, b) -> a + "\n" + b)
                .orElse("");

        // 7. 合并 3 个以上连续换行
        body = MULTI_NEWLINE.matcher(body).replaceAll("\n\n");

        return body;
    }

    /**
     * 在两个标记之间提取文本
     */
    private String extractBetween(String html, String startMarker, String endMarker) {
        int start = html.indexOf(startMarker);
        if (start == -1) return "";
        start = html.indexOf(">", start) + 1;
        if (start == 0) return "";
        int end = html.indexOf(endMarker, start);
        if (end == -1) return html.substring(start);
        return html.substring(start, end);
    }

    /**
     * 解码常见 HTML 实体
     */
    private String decodeHtmlEntities(String text) {
        if (text == null || text.isEmpty()) return "";
        return text.replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replace("&nbsp;", " ")
                .replace("&#160;", " ")
                .replace("&mdash;", "—")
                .replace("&ndash;", "–")
                .replace("&#x27;", "'");
    }
}
