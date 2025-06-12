package com.example.PaystackIntegrationApp.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class PaystackWebhookRequest {
    public String event;
    public Data data; // This 'data' object holds the transaction details

    public static class Data {
        public Long id;
        public String domain;
        public String status;
        public String reference;
        @SerializedName("receipt_number")
        public String receiptNumber;
        public Long amount;
        public String message;
        @SerializedName("gateway_response")
        public String gatewayResponse;
        @SerializedName("paid_at")
        public String paidAt;
        @SerializedName("created_at")
        public String createdAt;
        public String channel;
        public String currency;
        @SerializedName("ip_address")
        public String ipAddress;
        public Object metadata; // <-- CHANGE THIS LINE from Map<String, Object> to Object
        public Log log;
        public Long fees;
        @SerializedName("fees_split")
        public Object feesSplit; // Can be null or complex object
        public Authorization authorization;
        public Customer customer;
        public Object plan; // Can be null or object
        public Object split; // Can be empty object or complex object
        @SerializedName("order_id")
        public Object orderId; // Can be null or String/Long
        @SerializedName("paidAt")
        public String paidAtCamel; // Paystack often sends both snake_case and camelCase
        @SerializedName("createdAt")
        public String createdAtCamel;
        @SerializedName("requested_amount")
        public Long requestedAmount;
        @SerializedName("pos_transaction_data")
        public Object posTransactionData;
        public Object source;
        @SerializedName("fees_breakdown")
        public Object feesBreakdown; // Can be null or array
        public Object connect;
        @SerializedName("transaction_date")
        public String transactionDate;
        @SerializedName("plan_object")
        public Object planObject;
        public Object subaccount;
    }

    public static class Log {
        @SerializedName("start_time")
        public Long startTime;
        @SerializedName("time_spent")
        public Long timeSpent;
        public Long attempts;
        public Long errors;
        public Boolean success;
        public Boolean mobile;
        public List<String> input;
        public List<History> history;
        public String authentication;
    }

    public static class History {
        public String type;
        public String message;
        public Long time;
    }

    public static class Authorization {
        @SerializedName("authorization_code")
        public String authorizationCode;
        public String bin;
        public String last4;
        @SerializedName("exp_month")
        public String expMonth;
        @SerializedName("exp_year")
        public String expYear;
        public String channel;
        @SerializedName("card_type")
        public String cardType;
        public String bank;
        @SerializedName("country_code")
        public String countryCode;
        public String brand;
        public Boolean reusable;
        public String signature;
        @SerializedName("account_name")
        public String accountName;
    }

    public static class Customer {
        public Long id;
        @SerializedName("first_name")
        public String firstName;
        @SerializedName("last_name")
        public String lastName;
        public String email;
        @SerializedName("customer_code")
        public String customerCode;
        public String phone;
        public Object metadata; // Can be null or object
        @SerializedName("risk_action")
        public String riskAction;
        @SerializedName("international_format_phone")
        public String internationalFormatPhone;
    }
}
