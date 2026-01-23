package com.git.hui.springai.app.vectorstore;

import com.hankcs.hanlp.HanLP;
import com.hankcs.hanlp.seg.Segment;
import com.hankcs.hanlp.seg.common.Term;
import org.springframework.ai.document.Document;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 文档量化处理工具类
 * 将文档转换为数值表示，用于文本相似度计算
 */
public class DocumentQuantizer {

    /**
     * HanLP分词器实例
     */
    private static final Segment SEGMENT = HanLP.newSegment();

    /**
     * 将文本转换为数值向量表示（简化版）
     * 使用TF-IDF的基本思想，但简化为词频统计
     *
     * @param text 输入文本
     * @return 数值向量
     */
    public static float[] quantizeText(String text) {
        if (text == null || text.trim().isEmpty()) {
            return new float[0];
        }

        // 简单的文本预处理
        String[] words = preprocessText(text);

        // 统计词频
        Map<String, Integer> wordFreq = countWordFrequency(words);

        // 生成固定长度的向量表示（这里使用前128个高频词）
        return generateFixedLengthVector(wordFreq, 128);
    }

    /**
     * 将文档转换为数值向量
     *
     * @param document 输入文档
     * @return 数值向量
     */
    public static float[] quantizeDocument(Document document) {
        return quantizeText(document.getText());
    }

    /**
     * 改进的文本预处理方法，使用HanLP进行中文分词
     *
     * @param text 输入文本
     * @return 处理后的词语数组
     */
    private static String[] preprocessText(String text) {
        // 使用HanLP进行中文分词
        List<Term> termList = SEGMENT.seg(text);
        return termList.stream()
                .filter(term -> !isStopWord(term.word)) // 过滤停用词
                .filter(term -> !term.nature.toString().startsWith("w")) // 过滤标点符号
                .map(term -> term.word.toLowerCase()) // 转换为小写
                .toArray(String[]::new);
    }

    /**
     * 判断是否为停用词
     *
     * @param word 待判断的词
     * @return 是否为停用词
     */
    private static boolean isStopWord(String word) {
        // 定义一些常见的中文和英文停用词
        String lowerWord = word.toLowerCase();
        return lowerWord.equals("的") || lowerWord.equals("了") || lowerWord.equals("在") ||
               lowerWord.equals("是") || lowerWord.equals("我") || lowerWord.equals("有") ||
               lowerWord.equals("和") || lowerWord.equals("就") || lowerWord.equals("不") ||
               lowerWord.equals("人") || lowerWord.equals("都") || lowerWord.equals("一") ||
               lowerWord.equals("一个") || lowerWord.equals("上") || lowerWord.equals("也") ||
               lowerWord.equals("很") || lowerWord.equals("到") || lowerWord.equals("说") ||
               lowerWord.equals("要") || lowerWord.equals("去") || lowerWord.equals("你") ||
               lowerWord.equals("会") || lowerWord.equals("着") || lowerWord.equals("没有") ||
               lowerWord.equals("看") || lowerWord.equals("好") || lowerWord.equals("自己") ||
               lowerWord.equals("这") || lowerWord.equals("那") || lowerWord.equals("他") ||
               lowerWord.equals("她") || lowerWord.equals("它") || lowerWord.equals("他们") ||
               lowerWord.equals("我们") || lowerWord.equals("你们") ||
               lowerWord.equals("这个") || lowerWord.equals("那个") || lowerWord.equals("什么") ||
               lowerWord.equals("怎么") || lowerWord.equals("如何") || lowerWord.equals("这样") ||
               lowerWord.equals("那样") || lowerWord.equals("时候") || lowerWord.equals("时候") ||
               lowerWord.equals("因为") || lowerWord.equals("所以") || lowerWord.equals("但是") ||
               lowerWord.equals("然后") || lowerWord.equals("如果") || lowerWord.equals("就是") ||
               lowerWord.equals("还是") || lowerWord.equals("还是") || lowerWord.equals("只是") ||
               // 英文停用词
               lowerWord.equals("the") || lowerWord.equals("a") || lowerWord.equals("an") ||
               lowerWord.equals("and") || lowerWord.equals("or") || lowerWord.equals("but") ||
               lowerWord.equals("in") || lowerWord.equals("on") || lowerWord.equals("at") ||
               lowerWord.equals("to") || lowerWord.equals("for") || lowerWord.equals("of") ||
               lowerWord.equals("with") || lowerWord.equals("by") || lowerWord.equals("about") ||
               lowerWord.equals("as") || lowerWord.equals("if") || lowerWord.equals("when") ||
               lowerWord.equals("than") || lowerWord.equals("so") || lowerWord.equals("such") ||
               lowerWord.equals("can") || lowerWord.equals("will") || lowerWord.equals("would") ||
               lowerWord.equals("should") || lowerWord.equals("could") || lowerWord.equals("may") ||
               lowerWord.equals("might") || lowerWord.equals("must") || lowerWord.equals("shall") ||
               lowerWord.equals("this") || lowerWord.equals("that") || lowerWord.equals("these") ||
               lowerWord.equals("those") || lowerWord.equals("i") || lowerWord.equals("you") ||
               lowerWord.equals("he") || lowerWord.equals("she") || lowerWord.equals("it") ||
               lowerWord.equals("we") || lowerWord.equals("they") || lowerWord.equals("me") ||
               lowerWord.equals("him") || lowerWord.equals("her") || lowerWord.equals("us") ||
               lowerWord.equals("them") || lowerWord.equals("my") || lowerWord.equals("your") ||
               lowerWord.equals("his") || lowerWord.equals("its") || lowerWord.equals("our") ||
               lowerWord.equals("their") || lowerWord.equals("mine") || lowerWord.equals("yours") ||
               lowerWord.equals("his") || lowerWord.equals("hers") || lowerWord.equals("ours") ||
               lowerWord.equals("theirs") || lowerWord.equals("who") || lowerWord.equals("which") ||
               lowerWord.equals("what") || lowerWord.equals("where") || lowerWord.equals("when") ||
               lowerWord.equals("why") || lowerWord.equals("how") || lowerWord.equals("whose") ||
               lowerWord.equals("whom") || lowerWord.equals("been") || lowerWord.equals("being") ||
               lowerWord.equals("have") || lowerWord.equals("has") || lowerWord.equals("had") ||
               lowerWord.equals("do") || lowerWord.equals("does") || lowerWord.equals("did") ||
               lowerWord.equals("done") || lowerWord.equals("doing") || lowerWord.equals("am") ||
               lowerWord.equals("is") || lowerWord.equals("are") || lowerWord.equals("was") ||
               lowerWord.equals("were") || lowerWord.equals("be") || lowerWord.equals("been") ||
               lowerWord.equals("being") || lowerWord.equals("was") || lowerWord.equals("were") ||
               // 特殊符号
               lowerWord.matches("^[\\s\\p{Punct}]+$") ||
               // 单字符（除了有意义的单字如"一"等，这里可以根据需要调整）
               lowerWord.length() == 1 && !lowerWord.matches("[一二三四五六七八九十]") ||
               // 空字符串
               lowerWord.trim().isEmpty();
    }

    /**
     * 统计词频
     *
     * @param words 词语数组
     * @return 词频映射
     */
    private static Map<String, Integer> countWordFrequency(String[] words) {
        Map<String, Integer> wordFreq = new HashMap<>();
        for (String word : words) {
            if (word != null && !word.trim().isEmpty()) {
                wordFreq.put(word, wordFreq.getOrDefault(word, 0) + 1);
            }
        }
        return wordFreq;
    }

    /**
     * 生成固定长度的向量表示
     *
     * @param wordFreq 词频映射
     * @param length   向量长度
     * @return 固定长度的向量
     */
    private static float[] generateFixedLengthVector(Map<String, Integer> wordFreq, int length) {
        float[] vector = new float[length];

        // 获取频率最高的词汇
        List<Map.Entry<String, Integer>> sortedEntries = wordFreq.entrySet()
                .stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(length)
                .collect(Collectors.toList());

        // 将词频填入向量
        for (int i = 0; i < Math.min(sortedEntries.size(), length); i++) {
            vector[i] = sortedEntries.get(i).getValue();
        }

        return vector;
    }

    /**
     * 计算两个向量之间的余弦相似度
     *
     * @param vectorA 第一个向量
     * @param vectorB 第二个向量
     * @return 相似度值 [0, 1]
     */
    public static double calculateCosineSimilarity(float[] vectorA, float[] vectorB) {
        if (vectorA == null || vectorB == null || vectorA.length == 0 || vectorB.length == 0) {
            return 0.0;
        }

        // 确保向量长度一致
        int minLength = Math.min(vectorA.length, vectorB.length);
        float[] adjustedA = Arrays.copyOf(vectorA, minLength);
        float[] adjustedB = Arrays.copyOf(vectorB, minLength);

        // 计算点积
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < minLength; i++) {
            dotProduct += adjustedA[i] * adjustedB[i];
            normA += Math.pow(adjustedA[i], 2);
            normB += Math.pow(adjustedB[i], 2);
        }

        normA = Math.sqrt(normA);
        normB = Math.sqrt(normB);

        if (normA == 0 || normB == 0) {
            return 0.0;
        }

        return dotProduct / (normA * normB);
    }

    /**
     * 量化查询文本
     *
     * @param query 查询文本
     * @return 量化向量
     */
    public static float[] quantizeQuery(String query) {
        return quantizeText(query);
    }
}