package com.hazard.tjai.tool;
import org.springframework.ai.tool.annotation.Tool;

/**
 * @author hazard
 * @version 1.0
 * @description 调用天气
 * @date 2026/1/27 13:50
 */
public class WeatherTool {
    
    /**
     * 获取指定城市的天气信息
     * @param city 城市名称
     * @return 天气信息
     */
    @Tool(name = "get_weather", description = "Get weather for a given city")
    public String getWeather(String city) {
        // 模拟返回天气信息
        return "The weather in " + city + " is sunny with a temperature of 22°C.";
    }
}
