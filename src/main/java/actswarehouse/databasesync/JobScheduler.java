/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package actswarehouse.databasesync;

import org.apache.log4j.Logger;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 *
 * @author server
 */
public class JobScheduler implements Job{
    //private Logger log = Logger.getLogger(JobScheduler.class);
 
   @Override
   public void execute(JobExecutionContext jExeCtx) throws JobExecutionException {
      //log.debug("TestJob run successfully...");
      home Home = new home();
      Home.startprocessingupload();
   }
}
