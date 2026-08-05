package com.academy.trafficviolationsystem.jobscheduler;

public interface JobCommand {

    String getJobName();

    void execute();
}
