/*
 * 猛男生存 (Mengnan Survival) —— Minecraft 26.3 / NeoForge 移植版
 *
 * 本文件是自由软件：你可以依据 GNU 通用公共许可证第 3 版（GPL-3.0）的条款
 * 重新分发和/或修改它。完整条款见项目根目录的 LICENSE 文件。
 *
 * 本文件按“原样”分发，不附带任何担保。
 */
package com.mengnan.mengnansurvival;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WrittenBookContent;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 【34】初次进入世界时赠送给玩家的成书 —— 内容就是本模组的 README。
 *
 * <p>实现要点：</p>
 * <ol>
 *   <li>README.md 随 jar 一起打包（放在 jar 根目录），运行时用类加载器读取；</li>
 *   <li>客户端书页最多只显示 <b>14 行</b>（每行 114 像素宽），
 *       因此这里按「实际像素宽度」估算换行行数，把正文切成一页页，
 *       保证每一页都能完整显示、不会在末尾被截断；</li>
 *   <li>书页无法渲染 Markdown，所以只做轻度清理（去掉强调符号与标题井号）。</li>
 * </ol>
 */
public final class WelcomeBook {

    /** 成书的标题。 */
    public static final String BOOK_TITLE = "README";

    /** 成书的署名。 */
    public static final String BOOK_AUTHOR = "DeepSeek-V4.1-Flash";

    /** 客户端书页的文字宽度（像素），与 BookViewScreen.TEXT_WIDTH 一致。 */
    private static final int TEXT_WIDTH = 114;

    /** 客户端每页最多显示的行数，与 BookViewScreen 中的 14 一致。 */
    private static final int MAX_LINES_PER_PAGE = 14;

    /** 单页字符数硬上限，防止出现异常超长页。 */
    private static final int MAX_CHARS_PER_PAGE = 200;

    /** 成书最多保留的页数，超出部分会被截断并给出提示。 */
    private static final int MAX_PAGES = 100;

    /** 资源路径：README.md 位于 jar 根目录。 */
    private static final String README_RESOURCE = "/README.md";

    private WelcomeBook() {}

    /**
     * 构造赠书。若读取不到 README 资源，则返回一个说明性的短书，避免赠送空书。
     */
    public static ItemStack create() {
        String text = readReadme();
        List<String> pages = paginate(text);

        List<Filterable<Component>> components = new ArrayList<>();
        for (String page : pages) {
            // passThrough 表示不做聊天过滤，纯文字直接展示
            components.add(Filterable.passThrough(Component.literal(page)));
        }

        WrittenBookContent content = new WrittenBookContent(
                Filterable.passThrough(BOOK_TITLE),
                BOOK_AUTHOR,
                0,
                components,
                // 页面内容已经是普通文本，标记为已解析，避免再走一次解析流程
                true);

        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
        book.set(DataComponents.WRITTEN_BOOK_CONTENT, content);
        return book;
    }

    /** 读取打包在 jar 里的 README.md。 */
    private static String readReadme() {
        try (InputStream in = WelcomeBook.class.getResourceAsStream(README_RESOURCE)) {
            if (in == null) {
                return "猛男生存 (Mengnan Survival)\n\n未能读取到 README 资源。";
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "猛男生存 (Mengnan Survival)\n\n读取 README 时出错：" + e.getMessage();
        }
    }

    /**
     * 把 README 正文切分成书页。
     *
     * <p>按逻辑行逐行累加，用像素宽度估算每行会换行成几行；
     * 当累计行数会超过每页上限时就翻页。</p>
     */
    private static List<String> paginate(String raw) {
        List<String> cleaned = new ArrayList<>();
        for (String line : raw.replace("\r\n", "\n").replace('\r', '\n').split("\n", -1)) {
            cleaned.addAll(splitLongLine(cleanMarkdown(line)));
        }

        List<String> pages = new ArrayList<>();
        StringBuilder page = new StringBuilder();
        int linesUsed = 0;

        for (String line : cleaned) {
            int need = wrappedLineCount(line);
            if (linesUsed > 0 && linesUsed + need > MAX_LINES_PER_PAGE) {
                pages.add(page.toString());
                page.setLength(0);
                linesUsed = 0;
            }
            if (page.length() > 0) {
                page.append('\n');
            }
            page.append(line);
            linesUsed += need;

            if (pages.size() >= MAX_PAGES) {
                break;
            }
        }
        if (page.length() > 0 && pages.size() < MAX_PAGES) {
            pages.add(page.toString());
        }

        if (pages.isEmpty()) {
            pages.add("猛男生存 (Mengnan Survival)");
        }
        if (pages.size() >= MAX_PAGES) {
            // 内容过长时给出明确提示，而不是静默丢失
            pages.set(MAX_PAGES - 1, pages.get(MAX_PAGES - 1) + "\n\n（内容过长，此处已截断）");
        }
        return pages;
    }

    /**
     * 轻度清理 Markdown，让书里读起来不那么像源码。
     * 只去掉强调符号与标题井号，正文内容保持不变。
     */
    private static String cleanMarkdown(String line) {
        String s = line.replace("**", "");
        // 去掉行首的 #（标题标记），保留标题文字
        int i = 0;
        while (i < s.length() && s.charAt(i) == '#') {
            i++;
        }
        if (i > 0) {
            s = s.substring(i).stripLeading();
        }
        return s;
    }

    /**
     * 把过长的单行先切成若干段，保证每一段换行后不会超过一页的行数。
     * 这样即使 README 里有很长的表格行，也不会溢出书页。
     */
    private static List<String> splitLongLine(String line) {
        List<String> out = new ArrayList<>();
        if (wrappedLineCount(line) <= MAX_LINES_PER_PAGE) {
            out.add(line);
            return out;
        }
        StringBuilder chunk = new StringBuilder();
        int lines = 0;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (lines >= MAX_LINES_PER_PAGE) {
                out.add(chunk.toString());
                chunk.setLength(0);
                lines = 0;
            }
            chunk.append(c);
            if ((i + 1) % 16 == 0) {
                lines++;
            }
        }
        if (chunk.length() > 0) {
            out.add(chunk.toString());
        }
        return out;
    }

    /** 估算一行文本在书页里会占用多少显示行。 */
    private static int wrappedLineCount(String s) {
        if (s.isEmpty()) {
            return 1;
        }
        int lines = 1;
        int width = 0;
        for (int i = 0; i < s.length(); i++) {
            int cw = charWidth(s.charAt(i));
            if (width + cw > TEXT_WIDTH) {
                lines++;
                width = cw;
            } else {
                width += cw;
            }
        }
        return lines;
    }

    /** 估算单个字符的显示宽度：中日韩等全角字符按 9 像素，其余按 6 像素。 */
    private static int charWidth(char c) {
        // 覆盖 CJK 统一表意文字、假名、全角标点等常见全角区间
        if (c >= 0x1100 && (c <= 0x115F || c == 0x2329 || c == 0x232A
                || (c >= 0x2E80 && c <= 0xA4CF)
                || (c >= 0xAC00 && c <= 0xD7A3)
                || (c >= 0xF900 && c <= 0xFAFF)
                || (c >= 0xFE30 && c <= 0xFE6F)
                || (c >= 0xFF00 && c <= 0xFF60)
                || (c >= 0xFFE0 && c <= 0xFFE6))) {
            return 9;
        }
        return 6;
    }
}
