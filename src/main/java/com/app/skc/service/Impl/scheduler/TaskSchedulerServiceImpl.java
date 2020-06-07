package com.app.skc.service.Impl.scheduler;

import com.app.skc.service.scheduler.TaskSchedulerService;
import org.springframework.stereotype.Service;

@Service("taskSchedulerService")
public class TaskSchedulerServiceImpl implements TaskSchedulerService {

    @Override
    public void testTask(String testTask) {
        System.out.println(testTask);
    }
}
