package org.sms.process.sms;

import java.util.Vector;

public class SMSSenderPriority extends BaseSMSSender {
    @Override
    public Vector getParameterDefinition() {
        Vector vtTemp = super.getParameterDefinition();
        removeParameterDefinition(vtTemp, "ThreadNumber");
        removeParameterDefinition(vtTemp, "ThreadCount");
        return vtTemp;
    }

    @Override
    public void beforeSession() throws Exception {
        setTotalThread(1);
        setThreadNumber(0);
        setPriority(" AND NVL(priority,1) = 0 ");
        super.beforeSession();
    }
}
