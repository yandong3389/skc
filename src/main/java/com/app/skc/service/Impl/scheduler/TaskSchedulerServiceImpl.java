package com.app.skc.service.Impl.scheduler;

import com.app.skc.service.scheduler.SKWalletScheduler;
import org.springframework.stereotype.Service;

@Service("taskSchedulerService")
public class TaskSchedulerServiceImpl implements SKWalletScheduler {

    @Override
    public void testTask(String testTask) {
        System.out.println(testTask);
    }
}
