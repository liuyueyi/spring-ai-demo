package com.git.hui.springai.app.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.common.util.StringUtils;
import io.micrometer.core.instrument.util.IOUtils;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * 地址行政区域编码服务
 * <p>
 * - 对应的数据源信息来自于: https://github.com/modood/Administrative-divisions-of-China
 *
 * @author YiHui
 * @date 2026/1/14
 */
@Service
public class AddressAdCodeService {
    private final Logger log = org.slf4j.LoggerFactory.getLogger(AddressAdCodeService.class);
    private final static String data = "data/pca-code.json";

    private volatile Map<String, ProvinceMapper> provinceMap;

    /**
     * 从 data/pca-code.json 中加载数据，并结构化
     */
    @PostConstruct
    public void init() {
        try (InputStream stream = this.getClass().getClassLoader().getResourceAsStream(data)) {
            // 读取数据
            String content = IOUtils.toString(stream, UTF_8);
            ObjectMapper mapper = new ObjectMapper();
            // 将content反序列化为 List<Province>
            List<Province> provinces = mapper.readValue(content, mapper.getTypeFactory().constructCollectionType(List.class, Province.class));

            // 构建结构化数据，方便快速查找
            HashMap<String, ProvinceMapper> map = new HashMap<>();
            for (Province province : provinces) {
                ProvinceMapper provinceMap = new ProvinceMapper(province.code, province.name, new HashMap<>());
                map.put(province.name, provinceMap);
                for (City city : province.children) {
                    Map<String, Area> areaMap = CollectionUtils.isEmpty(city.children) ? Map.of() : city.children.stream().collect(Collectors.toMap(s -> s.name, s -> s));
                    CityMapper cityMap = new CityMapper(city.code, city.name, areaMap);
                    provinceMap.children.put(city.name, cityMap);
                }
            }
            this.provinceMap = map;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 查询地址对应的行政编码
     *
     * @param province 省
     * @param city     市
     * @param area     区
     * @return 行政编码
     */
    @Tool(description = "传入地址信息，返回对应的行政区域编码， 如输入 湖北省武汉市武昌区，返回的行政编码为 420106")
    public String queryAdCode(
            @ToolParam(description = "省，如 湖北省")
            String province,
            @ToolParam(description = "市，如 武汉市")
            String city,
            @ToolParam(description = "区，如 武昌区")
            String area) {
        log.info("queryAdCode: {}, {}, {}", province, city, area);
        ProvinceMapper provinceMap = this.provinceMap.get(province);
        if (StringUtils.isBlank(city)) {
            return provinceMap.code;
        }
        CityMapper cityMap = provinceMap.children.get(city);
        if (cityMap == null) {
            // 市未查到，返回省的行政编码
            return provinceMap.code;
        }
        if (StringUtils.isBlank(area)) {
            return cityMap.code;
        }

        Area ar = cityMap.children.get(area);
        if (ar == null) {
            // 区未查到，返回市的编码
            return cityMap.code;
        }
        return ar.code;
    }

    public record Area(String code, String name) {
    }


    public record City(String code, String name, List<Area> children) {
    }

    public record CityMapper(String code, String name, Map<String, Area> children) {
    }

    public record Province(String code, String name, List<City> children) {
    }

    public record ProvinceMapper(String code, String name, Map<String, CityMapper> children) {
    }
}
