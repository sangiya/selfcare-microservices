package com.selfcare.reports.domain;

/**
 * One entry per legacy controller family being consolidated here (see README-TODO.md for the
 * full 19-controller list). Extend this enum as each report type's generation logic is ported.
 */
public enum ReportType {
    ACTIVITY_REPORT,      // ActivityReportsController.php (3,668 LOC)
    CHARGING_HISTORY,     // ChargingHistoryController.php (2,531 LOC)
    CDR_SEARCH,           // CdrController.php / cdr/CdrSearchController.php
    USAGE_HISTORY,        // webapp/UsageHistoryController.php
    SIM_PURCHASE_REPORT,  // SendSimPurchaseReportController.php
    SMS_DELIVERY_REPORT,  // cdr/SmsDeliveryReportController.php
    MMS_DELIVERY_REPORT   // cdr/MmsDeliveryReportController.php
}
