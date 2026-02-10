package com.jimmy.monetization.monetizationbackend.payment.mpesa.dto;

import java.util.List;

public class MpesaCallbackDTO {
    private Body Body;

    public Body getBody() { return Body; }
    public void setBody(Body body) { Body = body; }

    public static class Body {
        private StkCallback stkCallback;

        public StkCallback getStkCallback() { return stkCallback; }
        public void setStkCallback(StkCallback stkCallback) { this.stkCallback = stkCallback; }
    }

    public static class StkCallback {
        private String MerchantRequestID;
        private String CheckoutRequestID;
        private int ResultCode;
        private String ResultDesc;
        private CallbackMetadata CallbackMetadata;

        public String getMerchantRequestID() { return MerchantRequestID; }
        public void setMerchantRequestID(String merchantRequestID) { MerchantRequestID = merchantRequestID; }

        public String getCheckoutRequestID() { return CheckoutRequestID; }
        public void setCheckoutRequestID(String checkoutRequestID) { CheckoutRequestID = checkoutRequestID; }

        public int getResultCode() { return ResultCode; }
        public void setResultCode(int resultCode) { ResultCode = resultCode; }

        public String getResultDesc() { return ResultDesc; }
        public void setResultDesc(String resultDesc) { ResultDesc = resultDesc; }

        public CallbackMetadata getCallbackMetadata() { return CallbackMetadata; }
        public void setCallbackMetadata(CallbackMetadata callbackMetadata) { CallbackMetadata = callbackMetadata; }
    }

    public static class CallbackMetadata {
        private List<Item> Item;

        public List<Item> getItem() { return Item; }
        public void setItem(List<Item> item) { Item = item; }
    }

    public static class Item {
        private String Name;
        private Object Value;

        public String getName() { return Name; }
        public void setName(String name) { Name = name; }

        public Object getValue() { return Value; }
        public void setValue(Object value) { Value = value; }
    }
}
