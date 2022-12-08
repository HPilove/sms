package org.sms.process.sms;

import org.smpp.*;
import org.smpp.pdu.*;
import smartlib.thread.DBManageableThread;
import smartlib.thread.GroupParameter;
import smartlib.thread.ParameterType;
import smartlib.thread.ThreadParameter;
import smartlib.util.AppException;
import smartlib.util.StringUtil;

import java.util.Vector;

public abstract class SMSThreadSender extends DBManageableThread {
    public Session session = null;
    public SMPPTestPDUEventListener smppTestPDUEventListener = null;
    protected boolean bound = false;
    protected String bindMode = "t";
    protected String ipAddress = null;
    protected int port = 0;
    protected String systemType = "";
    protected String systemId = null;
    protected String password = null;
    protected AddressRange addressRange = new AddressRange();
    protected boolean asynchronous = false;
    protected long nextEnquireLink = 0;
    protected int enquireInterval = 60;

    @SuppressWarnings({"rawtypes"})
    public Vector getParameterDefinition() {
        Vector<ThreadParameter> vtReturn = new Vector<>();

        vtReturn.add(createParameter("ip-address", "", ParameterType.PARAM_TEXTBOX_MAX, "15", ""));
        vtReturn.add(createParameter("port", "", ParameterType.PARAM_TEXTBOX_MASK, "99999", ""));
        vtReturn.add(createParameter("system-type", "", ParameterType.PARAM_TEXTBOX_MAX, "30", ""));
        vtReturn.add(createParameter("bind-mode", "", ParameterType.PARAM_COMBOBOX, new String[]{"t", "r", "tr"}, ""));
        vtReturn.add(createParameter("username", "", ParameterType.PARAM_TEXTBOX_MAX, "30", ""));
        vtReturn.add(createParameter("password", "", ParameterType.PARAM_PASSWORD, "30", ""));
        vtReturn.add(createParameter("sync-mode", "", ParameterType.PARAM_COMBOBOX, new String[]{"s", "a"}, ""));
        vtReturn.add(createParameter("enquire-interval", "", ParameterType.PARAM_TEXTBOX_MASK, "999", ""));
        vtReturn.addAll(getAddressParameterDefinition("address-range"));
        vtReturn.addAll(super.getParameterDefinition());
        return vtReturn;
    }

    public void fillParameter() throws AppException {
        ipAddress = loadString("ip-address");
        port = loadUnsignedInteger("port");
        systemType = StringUtil.nvl(getParameter("system-type"), "");
        systemId = loadString("username");
        password = loadString("password");
        bindMode = StringUtil.nvl(getParameter("bind-mode"), "");
        asynchronous = !loadString("sync-mode").equalsIgnoreCase("s");
        enquireInterval = loadUnsignedInteger("enquire-interval");
        // address-range
        GroupParameter gp = new GroupParameter(this, "address-range");
        byte ton = (byte) gp.loadUnsignedInteger("addr-ton");
        byte npi = (byte) gp.loadUnsignedInteger("addr-npi");
        String addr = gp.loadString("address-range");
        addressRange.setTon(ton);
        addressRange.setNpi(npi);
        try {
            addressRange.setAddressRange(addr);
        } catch (WrongLengthOfStringException e) {
            logMonitor("The length of address-range parameter is wrong.");
        }
        super.fillParameter();
    }

    public void beforeSession() throws Exception {
        super.beforeSession();
        try {
            if (session != null) {
                if (session.isBound()) {
                    if (session.getReceiver().isReceiver()) {
                        session.unbind();
                        session.close();
                        bound = false;
                        logMonitor("It can take a while to stop the receiver..");
                        throw new Exception("It can take a while to stop the receiver..");
                    }
                } else {
                    session.unbind();
                    session.close();
                    bound = false;
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            throw ex;
        } finally {
            try {
                if (session != null) {
                    session.close();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            session = null;
            bound = false;
        }

        BindRequest request;
        BindResponse response;

        try {
            if (bindMode.compareToIgnoreCase("t") == 0) {
                request = new BindTransmitter();
            } else if (bindMode.compareToIgnoreCase("r") == 0) {
                request = new BindReceiver();
            } else if (bindMode.compareToIgnoreCase("tr") == 0) {
                request = new BindTransciever();
            } else {
                throw new Exception(
                        "Invalid bind mode, expected t, r or tr, got " + bindMode + ". Operation canceled.");
            }
            TCPIPConnection connection = new TCPIPConnection(ipAddress, port);
            connection.setReceiveTimeout(20 * 1000);
            session = new Session(connection);

            // set values
            request.setSystemId(systemId); // user
            request.setPassword(password); // password
            request.setSystemType(systemType);
            request.setAddressRange(addressRange);
            logMonitor("Bind request " + getBindRequestInfo(request));
            if (asynchronous) {
                smppTestPDUEventListener = new SMPPTestPDUEventListener(session);
                response = session.bind(request, smppTestPDUEventListener);
                logMonitor(
                        "COMMAND_STATUS: " + StringUtil.nvl(SmsErrorCode.getCommandStatus(response.getCommandStatus()),
                                String.valueOf(response.getCommandStatus())));
            } else {
                response = session.bind(request);
            }
            if (response != null) {
                logMonitor("Bind response " + response.debugString());
                if (response.getCommandStatus() == Data.ESME_ROK) {
                    bound = true;
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            throw ex;
        }
    }

    public void afterSession() throws Exception {
        try {
            if (session != null) {
                if (session.getReceiver() != null && session.getReceiver().isReceiver()) {
                    session.unbind();
                    session.close();
                    session = null;
                    bound = false;
                    logMonitor("It can take a while to stop the receiver.");
                } else {
                    session.unbind();
                    session.close();
                    session = null;
                    bound = false;
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            logMonitor("Error Unbind :" + ex.getLocalizedMessage());
            throw ex;
        } finally {
            try {
                if (session != null) {
                    session.close();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            session = null;
            smppTestPDUEventListener = null;
            bound = false;
            super.afterSession();
        }
    }

    private String getBindRequestInfo(BindRequest request) {
        String temp = "(bindReq: ";
        temp += request.getSystemId();
        temp += " ";
        temp += request.getSystemType();
        temp += " ";
        temp += Integer.toString(request.getInterfaceVersion());
        temp += " ";
        temp += request.getAddressRange().debugString();
        temp += ") ";
        return temp;
    }

    protected synchronized void enquireLink() throws Exception {
        if (System.currentTimeMillis() < nextEnquireLink) {
            return;
        }
        EnquireLink request;
        EnquireLinkResp response;
        logMonitor("Sending enquire link.....");
        try {
            request = new EnquireLink();
            logMonitor("Enquire Link request: " + request.debugString());
            if (asynchronous) {
                synchronized (session) {
                    session.enquireLink(request);
                }
            } else {
                synchronized (session) {
                    response = session.enquireLink(request);
                }
                logMonitor("Enquire Link response: " + response.debugString());
            }
            nextEnquireLink = System.currentTimeMillis() + enquireInterval * 1000L;
        } catch (Exception ex) {
            ex.printStackTrace();
            throw ex;
        }
    }

    public Vector<ThreadParameter> getAddressParameterDefinition(String strParamName) {
        Vector<ThreadParameter> vtReturn = new Vector<>(), vtAddress = new Vector<>();
        vtAddress.add(createParameter("addr-ton", "", ParameterType.PARAM_TEXTBOX_MASK, "9", "", ""));
        vtAddress.add(createParameter("addr-npi", "", ParameterType.PARAM_TEXTBOX_MASK, "9", "", ""));
        vtAddress.add(createParameter("address-range", "", ParameterType.PARAM_TEXTBOX_MAX, "15", "", ""));
        vtReturn.add(createParameter(strParamName, "", ParameterType.PARAM_GROUP, vtAddress, ""));
        return vtReturn;
    }

    protected class SMPPTestPDUEventListener extends SmppObject implements ServerPDUEventListener {
        Session session;

        public SMPPTestPDUEventListener(Session session) {
            this.session = session;
        }

        public void handleEvent(ServerPDUEvent event) {
        }
    }
}
