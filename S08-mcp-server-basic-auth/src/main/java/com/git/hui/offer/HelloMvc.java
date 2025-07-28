package com.git.hui.offer;

import com.git.hui.offer.service.DateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.ZoneId;

/**
 * @author YiHui
 * @date 2025/7/28
 */
@RestController
public class HelloMvc {
    @Autowired
    private DateService dateService;

    @GetMapping("showTime")
    public String showTime(String area) {
        return dateService.getTimeByZoneId(ZoneId.of(area));
    }
}
