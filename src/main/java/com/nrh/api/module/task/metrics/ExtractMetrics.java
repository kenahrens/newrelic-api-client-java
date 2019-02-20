package com.nrh.api.module.task.metrics;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.newrelic.api.agent.Trace;
import com.nrh.api.module.nr.client.rest.*;
import com.nrh.api.module.nr.config.*;
import com.nrh.api.module.nr.model.*;

public class ExtractMetrics {
  
  private static final Logger log = LoggerFactory.getLogger(ExtractMetrics.class);
  
  private AppAPI apiOnlyClient;
  private AppHostAPI apiHostClient;
  private AppInstanceAPI apiInstanceClient;
  private CopierConfig copierConfig;

  public ExtractMetrics(CopierConfig copierConfig) {
    
    // Initialize the API clients
    this.copierConfig = copierConfig;
    apiOnlyClient = new AppAPI(copierConfig.getSourceKeys());
    apiHostClient = new AppHostAPI(copierConfig.getSourceKeys());
    apiInstanceClient = new AppInstanceAPI(copierConfig.getSourceKeys());
  }
  
  @Trace
  public ArrayList<MetricConfig> prepConfig() throws IOException {
    
    ArrayList<MetricConfig> resultList = new ArrayList<>();
    
    // We need the map because each appId will have a list of metrics
    Collection<MetricConfig> metricConfigList = csvToConfigList();
    log.info("Metrics will be collected from " + metricConfigList.size() + " apps.");
    for (MetricConfig metricConfig : metricConfigList) {
      resultList.addAll(cfgExpand(metricConfig));
    }
    log.info("The expanded count of app hosts/instances is " + resultList.size());
    return resultList;
  }

  private Collection<MetricConfig> csvToConfigList() {
    HashMap<Integer, MetricConfig> metricMap = new HashMap<>();
    
    // Start with the list from the config
    List<CSVMetric> csvMetricList = copierConfig.getCsvMetricList();
    for (CSVMetric csvMetric : csvMetricList) {
      
      // Create a new config if it doesn't exist for this appId
      Integer appId = csvMetric.getAppId();
      MetricConfig metricConfig = metricMap.getOrDefault(appId, new MetricConfig());
      csvToConfig(csvMetric, metricConfig);
      metricMap.put(appId, metricConfig);
    }

    return metricMap.values();
  }

  private void csvToConfig(CSVMetric csvMetric, MetricConfig metricConfig) {
    // Populate the config
    metricConfig.setAppId(csvMetric.getAppId());
    metricConfig.setAppName(csvMetric.getAppName());
    metricConfig.setConfigType(csvMetric.getConfigType());
    String fullName = csvMetric.getMetricName();
    String shortName = csvMetric.getShortName();
    metricConfig.addMetricName(fullName, shortName);
  }

  @Trace
  private ArrayList<MetricConfig> cfgExpand(MetricConfig metricConfig) throws IOException {
    
    ArrayList<MetricConfig> resultList = new ArrayList<>();
    String configType = metricConfig.getConfigType();
    
    // If it's app only there's no need to expand
    if (configType.equals(AppConfig.TYPE_APP_ONLY)) {
      resultList.add(metricConfig);
    }

    // Fetch the host ids and make a config for each one
    if (configType.equals(AppConfig.TYPE_APP_HOST)) {
      System.out.println("Fetching host list");
      ArrayList<AppHostModel> appHostList = apiHostClient.list(metricConfig);
      for (AppHostModel appHostModel : appHostList) {
        resultList.add(hostModelToConfig(appHostModel, metricConfig));
      }
    }

    // Feth the instance ids and make a config for each one
    if (configType.equals(AppConfig.TYPE_APP_INSTANCE)) {
      System.out.println("Fetching instance list");
      ArrayList<AppInstanceModel> appInstanceList = apiInstanceClient.list(metricConfig);
      for (AppInstanceModel appInstanceModel : appInstanceList) {
        resultList.add(instanceModelToConfig(appInstanceModel, metricConfig));
      }
    }

    return resultList;
  }

  private MetricConfig hostModelToConfig(AppHostModel appHostModel, MetricConfig oldConfig) {
    try {
      MetricConfig newConfig = (MetricConfig)oldConfig.clone();
      newConfig.setHost(appHostModel.getHost());
      newConfig.setHostId(appHostModel.getHostId());
      return newConfig;
    } catch (CloneNotSupportedException cnse) {
      log.error(cnse.getMessage());
    }
    return null;
  }

  private MetricConfig instanceModelToConfig(AppInstanceModel appInstanceModel, MetricConfig oldConfig) {
    try {
      MetricConfig newConfig = (MetricConfig)oldConfig.clone();
      newConfig.setHost(appInstanceModel.getHost());
      newConfig.setPort(appInstanceModel.getPort());
      newConfig.setInstanceId(appInstanceModel.getInstanceId());
      return newConfig;
    } catch (CloneNotSupportedException cnse) {
      log.error(cnse.getMessage());
    }
    return null;
  }

  @Trace
  public ArrayList<MetricDataModel> queryMetricData(ArrayList<MetricConfig> cfgList) throws IOException {
    
    ArrayList<MetricDataModel> resultList = new ArrayList<>();

    // Loop through the metric configs
    log.info("About to query metric data from: " + cfgList.size() + " apps / hosts / instances");

    for (MetricConfig metricConfig : cfgList) {
      ArrayList<MetricDataModel> metricDataList = runProperQuery(metricConfig);
      resultList.addAll(metricDataList);
    }

    log.info("Received metric data for " + resultList.size() + " metrics.");
    return resultList;
  }

  private ArrayList<MetricDataModel> runProperQuery(MetricConfig metricConfig) throws IOException {
    String configType = metricConfig.getConfigType();

    if (configType.equals(AppConfig.TYPE_APP_HOST)) {
      return apiHostClient.metricData(metricConfig);
    }
    if (configType.equals(AppConfig.TYPE_APP_INSTANCE)) {
      return apiInstanceClient.metricData(metricConfig);
    }
    
    return apiOnlyClient.metricData(metricConfig);
  }
}
