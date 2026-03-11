package com.microboxlabs.dashboards.datasource.model;

import org.alfresco.service.namespace.QName;

public class DataSourceModel {

    private DataSourceModel() {
        // utility class
    }

    public static final String NAMESPACE = "http://www.mintral.com/model/datasource/1.0";
    public static final String PREFIX = "dsdl";
    public static final String PREFIX_TYPE_DATA_SOURCE_CONFIG = "dsdl:dataSourceConfig";

    public static final QName TYPE_DATA_SOURCE_CONFIG =
        QName.createQName(NAMESPACE, "dataSourceConfig");

    public static final QName PROP_DS_NAME =             QName.createQName(NAMESPACE, "dsName");
    public static final QName PROP_DS_TYPE =             QName.createQName(NAMESPACE, "dsType");
    public static final QName PROP_DS_DESCRIPTION =      QName.createQName(NAMESPACE, "dsDescription");
    public static final QName PROP_DS_URL =              QName.createQName(NAMESPACE, "dsUrl");
    public static final QName PROP_DS_CONFIG_JSON =      QName.createQName(NAMESPACE, "dsConfigJson");
    public static final QName PROP_DS_IS_ACTIVE =        QName.createQName(NAMESPACE, "dsIsActive");
    public static final QName PROP_DS_LAST_TESTED_AT =   QName.createQName(NAMESPACE, "dsLastTestedAt");
    public static final QName PROP_DS_LAST_TEST_RESULT = QName.createQName(NAMESPACE, "dsLastTestResult");
}
