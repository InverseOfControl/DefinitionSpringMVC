package com.gaohx.demo.spring.servlet;

import com.gaohx.demo.spring.annotation.MyAutowired;
import com.gaohx.demo.spring.annotation.MyController;
import com.gaohx.demo.spring.annotation.MyService;
import com.gaohx.demo.test.MyTestService;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class GHXDispatcherServlet extends HttpServlet {

    private Properties properties = new Properties();

    private List<String> classNamesList = new ArrayList<>();

    private Map<String, Object> beansMap = new ConcurrentHashMap<String, Object>();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        super.doGet(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        super.doPost(req, resp);
    }

    @Override
    public void init(ServletConfig config) throws ServletException {

        // 定位
        doLocation(config.getInitParameter("contextConfigLocation"));

        // 加载
        doLoad(properties.getProperty("scan.package"));

        // 注册
        doRegistry();

        //依赖注入（）
        doAutowired();

        super.init();
    }

    private void doAutowired() {
        if(beansMap.isEmpty()){
            return;
        }

        for (Map.Entry<String,Object> entry : beansMap.entrySet()) {
            Field[] fields = entry.getValue().getClass().getDeclaredFields();
            for (Field field : fields) {
                if(!field.isAnnotationPresent(MyAutowired.class)){
                    continue;
                }else{
                    MyAutowired myAutowired = field.getAnnotation(MyAutowired.class);
                    String beanName = myAutowired.value();
                    if("".equals(beanName)){
                        beanName = field.getType().getName();
                    }
                    field.setAccessible(true);

                    try {
                        field.set(entry.getValue(),beansMap.get(beanName));
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }

            }
        }
    }

    /**
     * 注册
     */
    private void doRegistry() {
        if (classNamesList.isEmpty()) {
            return;
        }

        for (String className : classNamesList) {
            try {
                Class<?> clazz = Class.forName(className);
                // 在spring中，用多个子方法进行注册
                if (clazz.isAnnotationPresent(MyController.class)) {
                    String beanName = lowerFirstCase(clazz.getSimpleName());
                    // 在spring中，不会直接把instance放到map中，而是方的beanDefinition
                    beansMap.put(beanName, clazz.newInstance());
                } else if (clazz.isAnnotationPresent(MyService.class)) {
                    /**
                     * 默认是通过类名首字母小写注入
                     * 如果自己定义了bean得名字，那么优先使用自定义得bean名字注入
                     * 如果是一个接口，使用接口得类型去自动注入
                     */
                    MyService service = clazz.getAnnotation(MyService.class);
                    String beanName = service.value();
                    if ("".equals(beanName)) {
                        beanName = lowerFirstCase(clazz.getSimpleName());
                    }
                    Object instance = clazz.newInstance();
                    beansMap.put(beanName, instance);

                    Class<?>[] interfaces = clazz.getInterfaces();
                    for (Class i : interfaces) {
                        beansMap.put(i.getName(), instance);
                    }
                }else{
                    continue;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void doLoad(String packageName) {
        URL url = this.getClass().getClassLoader().getResource("/" + packageName.replaceAll("\\.", "/"));
        File fileDir = new File(url.getFile());
        for (File file : fileDir.listFiles()) {
            if (file.isDirectory()) {
                doLoad(packageName);
            } else {
                classNamesList.add(packageName + "." + file.getName().replace(".class", ""));
            }
        }
    }

    private void doLocation(String configLocation) {
        // 咋spring中是通过Reader去查询和定位的
        InputStream is = this.getClass().getClassLoader().getResourceAsStream(configLocation.replace("classpath:", ""));
        try {
            properties.load(is);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (null != is) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private String lowerFirstCase(String str) {
        char[] chars = str.toCharArray();
        chars[0] += 32;
        return String.valueOf(chars);
    }
}
