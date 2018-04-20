package com.gaohx.demo.test;

import com.gaohx.demo.spring.annotation.MyService;

@MyService
public class MyTestServiceImpl implements MyTestService {
    @Override
    public void call() {
        System.out.println("MyTestServiceImpl.call");
    }
}
