package com.git.hui.springai.app.vectorstore;

import com.hankcs.hanlp.HanLP;
import com.hankcs.hanlp.seg.Segment;
import com.hankcs.hanlp.seg.common.Term;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ChineseTextProcessorTest {

    @Test
    public void testHanLPChineseSegmentation() {
        // 测试HanLP分词效果
        String chineseText = "人工智能是计算机科学的一个重要分支，致力于让机器具备智能行为。";
        
        Segment segment = HanLP.newSegment();
        List<Term> termList = segment.seg(chineseText);
        
        System.out.println("HanLP分词结果:");
        for (Term term : termList) {
            System.out.println(term.word + " / " + term.nature);
        }
        
        assertTrue(!termList.isEmpty(), "HanLP应该能正确分词中文文本");
    }
    
    @Test
    public void testChineseTextProcessing() {
        // 测试中文文档处理
        String chineseText = "人工智能是计算机科学的一个重要分支，致力于让机器具备智能行为。" +
                            "机器学习是人工智能的一个子领域，专注于算法和统计模型的研究。" +
                            "自然语言处理是另一个重要领域，专注于计算机与人类语言的交互。";
        
        Document document = new Document("test_doc", chineseText, Map.of());
        
        // 测试文档量化
        float[] vector = DocumentQuantizer.quantizeDocument(document);
        
        // 向量应该不为空
        assertNotNull(vector);
        assertTrue(vector.length > 0);
        
        System.out.println("中文文档量化向量长度: " + vector.length);
        System.out.println("前5个向量值: ");
        for (int i = 0; i < Math.min(5, vector.length); i++) {
            System.out.print(vector[i] + " ");
        }
        System.out.println();
    }
    
    @Test
    public void testChineseQuerySimilarity() {
        String text1 = "人工智能技术在医疗领域的应用";
        String text2 = "AI技术在医院中的使用";
        String text3 = "汽车制造工艺流程";
        
        float[] vec1 = DocumentQuantizer.quantizeText(text1);
        float[] vec2 = DocumentQuantizer.quantizeText(text2);
        float[] vec3 = DocumentQuantizer.quantizeText(text3);
        
        // 计算相似度
        double similarity12 = DocumentQuantizer.calculateCosineSimilarity(vec1, vec2);
        double similarity13 = DocumentQuantizer.calculateCosineSimilarity(vec1, vec3);
        
        System.out.println("文本1和文本2的相似度: " + similarity12);
        System.out.println("文本1和文本3的相似度: " + similarity13);
        
        // 应该是文本1和文本2更相似
        assertTrue(similarity12 >= similarity13, 
                  "语义相关的中文文本应该具有更高的相似度");
    }
    
    @Test
    public void testDocumentChunkingWithChinese() {
        String longChineseText = "第一章：人工智能概述。人工智能是计算机科学的重要分支，致力于模拟、延伸和扩展人的智能理论、方法和技术。" +
                                "第二章：机器学习基础。机器学习是人工智能的核心，它使计算机能够从数据中学习并做出决策。" +
                                "第三章：深度学习进展。深度学习通过模拟人脑神经网络结构，在图像识别、语音识别等领域取得了突破性进展。" +
                                "第四章：自然语言处理。自然语言处理使计算机能够理解和生成人类语言，是实现人机交互的关键技术。" +
                                "第五章：未来发展趋势。人工智能未来发展将更加注重与人类协作，实现更高级别的智能应用。";
        
        Document document = new Document("chinese_doc", longChineseText, Map.of());

        // 测试文档分块
        DocumentChunker chunker = new DocumentChunker(100, 20); // 较小的块大小用于测试
        java.util.List<Document> chunks = chunker.chunkDocument(document);
        
        // 验证分块结果
        assertTrue(chunks.size() > 1, "长中文文档应该被分成多个块");
        System.out.println("中文文档分块数量: " + chunks.size());
        
        for (int i = 0; i < chunks.size(); i++) {
            Document chunk = chunks.get(i);
            System.out.println("块 " + i + " 长度: " + chunk.getText().length());
            System.out.println("内容: " + chunk.getText().substring(0, Math.min(50, chunk.getText().length())) + "...");
        }
    }
    
    @Test
    public void testEnglishChineseMixedText() {
        String mixedText = "Machine Learning 机器学习是AI的核心技术。Deep Learning 深度学习在图像识别中有广泛应用。";
        
        Document document = new Document("mixed_doc", mixedText, Map.of());
        
        float[] vector = DocumentQuantizer.quantizeDocument(document);
        
        assertNotNull(vector);
        assertTrue(vector.length > 0);
        
        System.out.println("中英文混合文档量化向量长度: " + vector.length);
    }
    
    @Test
    public void testHanLPAdvancedFeatures() {
        String text = "南京市长江大桥非常壮观";
        
        // 使用HanLP不同分词模式进行对比
        Segment segment = HanLP.newSegment();
        List<Term> termList = segment.seg(text);
        
        System.out.println("默认分词模式结果:");
        for (Term term : termList) {
            System.out.print(term.word + "/" + term.nature + " ");
        }
        System.out.println();
        
        // 测试其他分词模式
        Segment nShortSegment = HanLP.newSegment().enableIndexMode(true);
        List<Term> nShortTerms = nShortSegment.seg(text);
        
        System.out.println("最短路径分词模式结果:");
        for (Term term : nShortTerms) {
            System.out.print(term.word + "/" + term.nature + " ");
        }
        System.out.println();
        
        assertTrue(!termList.isEmpty(), "HanLP应该能正确分词中文文本");
    }
}