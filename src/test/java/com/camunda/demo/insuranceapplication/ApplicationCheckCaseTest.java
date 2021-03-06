package com.camunda.demo.insuranceapplication;

import static org.camunda.bpm.engine.test.assertions.ProcessEngineAssertions.assertThat;
import static org.camunda.bpm.engine.test.assertions.ProcessEngineAssertions.init;
import static org.camunda.bpm.engine.test.assertions.ProcessEngineAssertions.processEngine;
import static org.camunda.bpm.engine.test.assertions.ProcessEngineTests.*;
import static org.junit.Assert.*;

import java.util.List;

import org.apache.ibatis.logging.LogFactory;
import org.camunda.bpm.engine.CaseService;
import org.camunda.bpm.engine.history.HistoricCaseActivityInstance;
import org.camunda.bpm.engine.history.HistoricCaseActivityInstanceQuery;
import org.camunda.bpm.engine.impl.util.LogUtil;
import org.camunda.bpm.engine.runtime.CaseExecution;
import org.camunda.bpm.engine.runtime.CaseInstance;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.Task;
import org.camunda.bpm.engine.test.Deployment;
import org.camunda.bpm.engine.test.ProcessEngineRule;
import org.camunda.bpm.engine.variable.VariableMap;
import org.camunda.bpm.engine.variable.Variables;
import org.camunda.bpm.engine.variable.Variables.SerializationDataFormats;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.camunda.demo.insuranceapplication.ProcessConstants;
import com.camunda.demo.insuranceapplication.model.Application;

public class ApplicationCheckCaseTest {

  @Rule
  public ProcessEngineRule rule = new ProcessEngineRule();
  
  @Before
  public void setup() {
    init(rule.getProcessEngine());
  }
  
  /**
   * Just tests if the process definition is deployable.
   */
  
  @Test
  @Deployment(resources = {"ApplicationCheck.cmmn"})
  public void testParsingAndDeployment() {
    // nothing is done here, as we just want to check for exceptions during deployment
  }

  @Test
  @Deployment(resources = {"ApplicationCheck.cmmn"})
  public void testCaseApplicationCheckAcceptedWithoutApproval() {
    Application application = DemoData.createNewApplication(20, "Porsche", "911");
    application.setPriceIndicationInCents(30000);
    
    VariableMap variables = Variables.putValue( //
        ProcessConstants.VAR_NAME_application,
        Variables.objectValue(application).serializationDataFormat(SerializationDataFormats.JSON).create());
    
	  CaseInstance caseInstance = processEngine().getCaseService().createCaseInstanceByKey( //
			  ProcessConstants.CASE_KEY_applicationCheck, variables);
	  
	  assertThat(caseInstance).stage("Stage_ApplicationDecision").isActive();
	  assertThat(caseInstance).stage("Stage_ApplicationDecision").humanTask("HumanTask_DecideOnApplication").isActive();
	  
	  processEngine().getCaseService().setVariable(caseInstance.getId(), "approved", Boolean.TRUE);
	  complete(caseExecution("HumanTask_DecideOnApplication", caseInstance), Variables.createVariables().putValue( //
			  ProcessConstants.VAR_NAME_approved, Boolean.TRUE));
	  
	  // printCaseStatusAndTasklist();

	  assertThat(caseInstance).isCompleted();
  }
  
  @Test
  @Deployment(resources = {"ApplicationCheck.cmmn"})
  public void testCaseApplicationCheckAcceptedWithApproval(){
	  Application application = DemoData.createNewApplication(20, "Porsche", "911");
	  application.setPriceIndicationInCents(30001);
	  
	  VariableMap variables = Variables.putValue( //
			  ProcessConstants.VAR_NAME_application, //
			  Variables.objectValue(application).serializationDataFormat(SerializationDataFormats.JSON).create());
	  
	  CaseInstance caseInstance = processEngine().getCaseService().createCaseInstanceByKey( //
			  ProcessConstants.CASE_KEY_applicationCheck, variables);
	  
	  assertThat(caseInstance).stage("Stage_ApplicationDecision").isActive();
	  assertThat(caseInstance).stage("Stage_ApplicationDecision").humanTask("HumanTask_DecideOnApplication").isActive();
	  
	  complete(caseExecution("HumanTask_DecideOnApplication", caseInstance), Variables.createVariables().putValue( //
			  ProcessConstants.VAR_NAME_approved, Boolean.TRUE));
	  
	  complete(caseExecution("HumanTask_ApproveDecision", caseInstance));
	  
	  assertThat(caseInstance).isCompleted();
	  
	  
	  
  }
  
  @Test
  @Deployment(resources = {"ApplicationCheck.cmmn"})
  public void testCaseApplicationCheckRejectedWithoutApproval() {
    Application application = DemoData.createNewApplication(20, "Porsche", "911");
    application.setPriceIndicationInCents(30000);
    
    VariableMap variables = Variables.putValue( //
        ProcessConstants.VAR_NAME_application,
        Variables.objectValue(application).serializationDataFormat(SerializationDataFormats.JSON).create());
    
	  CaseInstance caseInstance = processEngine().getCaseService().createCaseInstanceByKey( //
			  ProcessConstants.CASE_KEY_applicationCheck, variables);
	  
	  assertThat(caseInstance).stage("Stage_ApplicationDecision").isActive();
	  assertThat(caseInstance).stage("Stage_ApplicationDecision").humanTask("HumanTask_DecideOnApplication").isActive();
	  
	  processEngine().getCaseService().setVariable(caseInstance.getId(), "approved", Boolean.TRUE);
	  complete(caseExecution("HumanTask_DecideOnApplication", caseInstance), Variables.createVariables().putValue( //
			  ProcessConstants.VAR_NAME_approved, Boolean.FALSE));
	  
	  // printCaseStatusAndTasklist();

	  assertThat(caseInstance).isCompleted();
  }
  
  private void printCaseStatusAndTasklist() {
	     System.out.println("Case Status:");
	     CaseService caseService = processEngine().getCaseService();
	     List<CaseExecution> caseExecutions = caseService.createCaseExecutionQuery().list();
	     for (CaseExecution caseExecution : caseExecutions) {
	       if (caseExecution.isActive()) {
	         System.out.println("  active:  " +
	caseExecution.getActivityName() + " (" + caseExecution.getActivityType() 
	+ ")");
	       }
	       else if (caseExecution.isEnabled()) {
	         System.out.println("  enabled: " +
	caseExecution.getActivityName() + " (" + caseExecution.getActivityType() 
	+ ")");
	       }
	       else if (caseExecution.isTerminated()) {
		         System.out.println("  terminated: " +
		caseExecution.getActivityName() + " (" + caseExecution.getActivityType() 
		+ ")");
		       }
	     }


	     HistoricCaseActivityInstanceQuery historicCaseActivityInstanceQuery = processEngine().getHistoryService()
	       .createHistoricCaseActivityInstanceQuery()
	       .completed();

	     System.out.println("Case History (" +
	historicCaseActivityInstanceQuery.count() + "):");
	     List<HistoricCaseActivityInstance> completedCaseActivities = historicCaseActivityInstanceQuery
	       .list();
	     for (HistoricCaseActivityInstance completedCaseActivity : 
	completedCaseActivities) {
	       System.out.println("  completed:  " +
	completedCaseActivity.getCaseActivityName() + " (" +
	completedCaseActivity.getCaseActivityType() + ")");
	     }

	     System.out.println("Task List (" + taskQuery().count() + "):");
	     List<Task> tasklist = taskQuery().list();
	     for (Task task : tasklist) {
	       System.out.println("  " + (task.getAssignee() == null ? 
	"unassigned" : task.getAssignee()) + ": " + task.getName());
	     }
	     System.out.println();
	   }


  
}
