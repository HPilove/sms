package org.sms.process.sms;

public class SMSSenderPriority extends SMSSender {
    @Override
    public void beforeSession() throws Exception {
        setPriority(" AND NVL(priority,1) = 0 ");
        super.beforeSession();
    }
}
