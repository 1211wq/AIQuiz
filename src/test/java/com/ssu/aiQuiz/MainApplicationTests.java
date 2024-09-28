package com.ssu.aiQuiz;

import io.reactivex.Flowable;
import io.reactivex.schedulers.Schedulers;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.TimeUnit;

/**
 * 主类测试
 *

 */
@SpringBootTest
class MainApplicationTests {

    @Test
    void contextLoads() {
    }

    @Test
    public void rxJavaTest() throws InterruptedException {
        // 创建数据流
        Flowable<Long> flowable = Flowable.interval(1, TimeUnit.SECONDS)
                .map(i -> i + 1)
                .subscribeOn(Schedulers.io());// 指定执行的线程池

        // 订阅流
        flowable
                .observeOn(Schedulers.io())
                .doOnNext(item -> System.out.println(item))
                .subscribe();
        // 主线程睡眠
        Thread.sleep(10000);
    }

}
