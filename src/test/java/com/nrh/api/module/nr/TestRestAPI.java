package com.nrh.api.module.nr;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.ArrayList;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.junit4.SpringRunner;
import com.nrh.api.APIApplication;
import com.nrh.api.module.nr.config.*;
import com.nrh.api.module.nr.model.*;
import com.nrh.api.module.nr.client.rest.*;

@RunWith(SpringRunner.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestRestAPI {

	private static final Logger log = LoggerFactory.getLogger(TestRestAPI.class);
	
	private APIKeyset keys;
	private ApplicationsAPI appClient; 
	private ApplicationHostsAPI appHostClient;
	
	// These values are used by the ordered test cases so they are static
	private static int appId;
	private static ApplicationConfig appConfig;
	private static MetricConfig metricConfig;
	private static int metricCount;
	
	@Before
	public void setUp() throws Exception {
		
		// Read in the config files
		APIApplication.readConfig();
		log.info("Config file used: " + APIApplication.getConfig().origin());

		// Get the name of the unitTestAccount
		String unitTestAccount = APIApplication.getConfString("newrelic-api-client.tests.unitTestAccount");
		keys = APIApplication.getAPIKeyset(unitTestAccount);
		log.info("Application API Test using keyset for account: " + keys.getAccountName());
		
		// Initialize the Applications API
		appClient = new ApplicationsAPI(keys);
		appHostClient = new ApplicationHostsAPI(keys);
		appConfig = new ApplicationConfig(ApplicationConfig.TYPE_APP_ONLY);
	}

	@Test
	public void app1List() throws IOException {
		ArrayList<AppModel> appList = appClient.list(appConfig);
		log.info("Number of applications: " + appList.size());
		
		assertNotEquals(0, appList.size());
		
		// Grab the first app that is "reporting"
		for (AppModel appModel : appList) {
			if (appModel.getReporting()) {
				appId = appModel.getId();
				String appName = appModel.getName();
				metricConfig = new MetricConfig(appId, appName);
				log.info("Setting appId to this reporting app: " + appName + " (" + appId + ")");
				break;
			}
		}
	}
	
	@Test
	public void app2Show() throws IOException {
		appConfig.setAppId(appId);
		log.info("appShow(" + appConfig.getAppId() + ")");
		AppModel appModel = appClient.show(appConfig);
		assertEquals(appConfig.getAppId(), appModel.getId());
	}

	@Test
	public void app3MetricNames() throws IOException {
		
		ArrayList<MetricNameModel> metricNameList = appClient.metricNames(metricConfig);

		// There should be more than 0 metrics
		assertNotEquals(0, metricNameList.size());

		// Grab a group of metrics
		int i = 0;
		for ( ; i < metricNameList.size(); i++) {
			// Don't get more than 5 metrics
			if (i == 5) {
				break;
			}
			
			MetricNameModel metricNameModel = metricNameList.get(i);
			String fullName = metricNameModel.getName();

			// Add metrics to the list unless they start with Instance
			// String fullName = jMetrics.getJSONObject(i).getString("name");
			if (!fullName.startsWith("Instance")) {
				metricConfig.addMetricName(fullName);
				metricCount++;
			}
		}
	}

	@Test
	public void app4MetricData() throws IOException {

		ArrayList<MetricDataModel> metricDataList = appClient.metricData(metricConfig);
		
		// Check that the correct number of metrics were found
		assertEquals(metricCount, metricDataList.size());
	}

	@Test
	public void appHost1List() throws IOException {
		appConfig.setAppId(appId);
		log.info("appHost1List(" + appConfig.getAppId() + ")");
		ArrayList<AppModel> hostList = appHostClient.list(appConfig);
		log.info("Number of hosts: " + hostList.size());
		
		assertNotEquals(0, hostList.size());
	}
}