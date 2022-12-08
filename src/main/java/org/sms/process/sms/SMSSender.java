package org.sms.process.sms;

import smartlib.thread.ParameterType;
import smartlib.util.AppException;

import java.util.Vector;

public class SMSSender extends BaseSMSSender {
    @Override
    public Vector getParameterDefinition() {
        Vector vtTemp = new Vector();
        vtTemp.add(createParameter("ThreadCount", "", ParameterType.PARAM_TEXTBOX_MASK, "0",
                "Tổng số thread."));
        vtTemp.add(createParameter("ThreadNumber", "", ParameterType.PARAM_TEXTBOX_MASK, "0",
                "Số thứ tự thread, bắt đầu từ 0."));
        vtTemp.addAll(super.getParameterDefinition());
        return vtTemp;
    }

    @Override
    public void fillParameter() throws AppException {
        int temp = loadUnsignedInteger("ThreadCount");
        setTotalThread(temp);
        temp = loadUnsignedInteger("ThreadNumber");
        setThreadNumber(temp);
        super.fillParameter();
    }
}
