package com.git.hui.springai.app.mvc;

import cn.hutool.http.HttpUtil;
import com.git.hui.springai.app.entity.invoice.InvoiceInfo;
import com.git.hui.springai.app.entity.req.InvoiceExtractRequest;
import com.git.hui.springai.app.entity.req.InvoiceExtractResponse;
import com.git.hui.springai.app.entity.req.ProcessStatus;
import com.git.hui.springai.app.service.InvoiceExtractionService;
import io.micrometer.common.util.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.MimeType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * 图片识别
 *
 * @author YiHui
 * @date 2025/8/4
 */
@Controller
public class InvoiceExtractionController {
    private final InvoiceExtractionService invoiceExtractionService;

    public InvoiceExtractionController(InvoiceExtractionService invoiceExtractionService) {
        this.invoiceExtractionService = invoiceExtractionService;
    }

    /**
     * 上传发票图片并提取内容
     *
     * @param file 上传的发票图片文件
     * @param msg  识别提示信息
     * @return 识别结果
     */
    @ResponseBody
    @PostMapping(path = "/extractInvoice")
    public InvoiceInfo extractInvoice(@RequestParam("file") MultipartFile file,
                                      @RequestParam(value = "msg", required = false) String msg,
                                      @RequestParam(value = "needItems", required = false) Boolean needItems,
                                      @RequestParam(value = "small", required = false) Boolean small
    ) throws IOException {
        byte[] imageBytes = file.getBytes();
        MimeType mimeType = MimeType.valueOf(file.getContentType());

        if (Boolean.TRUE.equals(small)) {
            // 小发票
            return invoiceExtractionService.fastExtractInvoice(imageBytes, mimeType, msg);
        }

        if (Boolean.TRUE.equals(needItems)) {
            return invoiceExtractionService.extractInvoiceWitInhItems(imageBytes, mimeType, msg);
        }
        return invoiceExtractionService.extractInvoice(imageBytes, mimeType, msg);
    }


    @ResponseBody
    @PostMapping(path = "/extract")
    public InvoiceExtractResponse extract(@RequestParam(value = "file", required = false) MultipartFile file,
                                          InvoiceExtractRequest request) throws IOException {
        long start = System.currentTimeMillis();
        byte[] imageBytes;
        MimeType mimeType;
        if (file != null) {
            imageBytes = file.getBytes();
            mimeType = MimeType.valueOf(file.getContentType());
        } else if (StringUtils.isBlank(request.getImage())) {
            return new InvoiceExtractResponse().setErrorMessage("请上传发票图片");
        } else if (request.getImage().startsWith("http")) {
            imageBytes = HttpUtil.downloadBytes(request.getImage());
            mimeType = MimeType.valueOf(StringUtils.isBlank(request.getImageType()) ? "image/jpeg" : request.getImageType());
        } else if (request.getImage().startsWith("data:")) {
            // base64解压为字节数组
            String base64Data = request.getImage().split(",", 2)[1]; // 使用limit=2避免分割数据中的逗号
            imageBytes = java.util.Base64.getDecoder().decode(base64Data);
            mimeType = MimeType.valueOf(request.getImage().split(",", 2)[0].substring(5));
        } else {
            return new InvoiceExtractResponse().setErrorMessage("请上传图片格式的发票");
        }

        InvoiceInfo invoiceInfo;
        if (Boolean.TRUE.equals(request.isNeedItems())) {
            invoiceInfo = invoiceExtractionService.extractInvoiceWitInhItems(imageBytes, mimeType, request.getMsg());
        } else {
            invoiceInfo = invoiceExtractionService.extractInvoice(imageBytes, mimeType, request.getMsg());
        }
        return new InvoiceExtractResponse()
                .setInvoiceInfo(invoiceInfo)
                .setProcessTime(System.currentTimeMillis() - start)
                .setStatus(ProcessStatus.SUCCESS);
    }


    /**
     * 显示发票识别页面
     *
     * @return HTML页面
     */
    @GetMapping("/invoicePage")
    public String invoicePage(Model model) {
        return "invoice_extraction";
    }

}