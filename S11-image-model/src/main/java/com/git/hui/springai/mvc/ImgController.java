package com.git.hui.springai.mvc;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.ai.image.Image;
import org.springframework.ai.image.ImageModel;
import org.springframework.ai.image.ImageOptionsBuilder;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.image.ImageResponse;
import org.springframework.ai.zhipuai.ZhiPuAiImageModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;

/**
 * @author YiHui
 * @date 2025/8/4
 */
@RestController
public class ImgController {
    private final ImageModel imgModel;

    public ImgController(ZhiPuAiImageModel imgModel) {
        this.imgModel = imgModel;
    }


    /**
     * 生成图片并返回
     *
     * @param msg 提示词
     * @param res 返回图片
     * @throws IOException
     */
    @GetMapping(path = "/genImg", produces = "application/octet-stream")
    public void genImg(String msg, HttpServletResponse res) throws IOException {
        ImageResponse response = imgModel.call(new ImagePrompt(msg,
                ImageOptionsBuilder.builder()
                        .height(1024)
                        .width(1024)
                        .model("CogView-3-Flash")
                        // 返回图片类型
                        .responseFormat("png")
                        // 图像风格，如 vivid 生动风格， natural 自然风格
                        .style("natural")
                        .build())
        );
        Image img = response.getResult().getOutput();
        BufferedImage image = ImageIO.read(new URL(img.getUrl()));
        res.setContentType("image/png");
        ImageIO.write(image, "png", res.getOutputStream());
    }
}
