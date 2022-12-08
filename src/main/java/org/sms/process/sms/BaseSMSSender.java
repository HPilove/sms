package org.sms.process.sms;

import lombok.Getter;
import lombok.Setter;
import org.smpp.Data;
import org.smpp.pdu.Address;
import org.smpp.pdu.SubmitSM;
import org.smpp.pdu.SubmitSMResp;
import org.smpp.pdu.WrongLengthOfStringException;
import org.smpp.util.ByteBuffer;
import org.sms.util.DBUtil;
import org.sms.util.Util;
import smartlib.database.Database;
import smartlib.thread.GroupParameter;
import smartlib.thread.ParameterType;
import smartlib.thread.ThreadConstant;
import smartlib.thread.ThreadParameter;
import smartlib.util.AppException;
import smartlib.util.StringUtil;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.MessageFormat;
import java.util.*;

public class BaseSMSSender extends SMSThreadSender {
    private final Address mSrcAddress = new Address();
    private final Address mDestAddress = new Address();
    private PreparedStatement stmtDelete = null, stmtUpdate = null, qQueue = null, insMTHis = null;
    @Setter
    private int totalThread, threadNumber;
    private long nextQuery = System.currentTimeMillis();
    private String sql_condition;
    @Getter
    @Setter
    private String priority = " AND NVL(priority,1) != 0 ";
    private int batchSize = 1000;

    @SuppressWarnings({"rawtypes"})
    public Vector getParameterDefinition() {
        Vector<ThreadParameter> vtReturn = new Vector<>();
        vtReturn.add(createParameter("sql_condition", "", ParameterType.PARAM_TEXTBOX_MAX, "10000", "Bổ sung điều kiện lọc của câu lệnh."));
        vtReturn.addAll(getAddressParameterDefinition("src-address-range"));
        vtReturn.addAll(getAddressParameterDefinition("dest-address-range"));

        vtReturn.addAll(super.getParameterDefinition());
        removeParameterDefinition(vtReturn, "bind-mode");
        return vtReturn;
    }

    ////////////////////////////////////////////////////////
    public void fillParameter() throws AppException {
        sql_condition = StringUtil.nvl(getParameter("sql_condition"), "").trim();
        setAddressParameter("src-address-range", mSrcAddress);
        setAddressParameter("dest-address-range", mDestAddress);
        super.fillParameter();
        bindMode = "t";
    }

    ////////////////////////////////////////////////////////
    // language=Oracle
    public void beforeSession() throws Exception {
        super.beforeSession();
        String sql = " SELECT id,isdn,content,shortcode,to_char(create_time,'dd/mm/yyyy hh24:mi:ss') create_time,priority,retry,program_id" +
                     " FROM mt_queue WHERE MOD(id,?) = ? AND status ='0' AND retry > 0 " +
                     getPriority() + ("".equals(sql_condition) ? sql_condition : "") +
                     " ORDER BY priority,create_time,id";
        qQueue = mcnMain.prepareStatement(sql);

        sql = "DELETE mt_queue WHERE id = ?";
        stmtDelete = mcnMain.prepareStatement(sql);

        sql = "UPDATE mt_queue SET status = '0', sent_time = sysdate, retry = NVL (retry, 3) - 1 WHERE id = ?";
        stmtUpdate = mcnMain.prepareStatement(sql);

        sql = " INSERT INTO mt_history (id,isdn,content,shortcode,create_time,priority,retry,program_id,sent_time)" +
              " VALUES (?,?,?,?,to_date(?,'dd/mm/yyyy hh24:mi:ss'),?,?,?,SYSDATE)";
        insMTHis = mcnMain.prepareStatement(sql);
    }

    ////////////////////////////////////////////////////////
    public void afterSession() throws Exception {
        DBUtil.closeObject(stmtDelete, stmtUpdate, qQueue, insMTHis);
        super.afterSession();
    }

    ////////////////////////////////////////////////////////
    public void processSession() throws AppException {
        try {
            logMonitor("Going to submit function");
            submit();
        } catch (Exception e) {
            e.printStackTrace();
            throw new AppException(e, "processSession");
        }
    }

    ////////////////////////////////////////////////////////
    private void submit() throws Exception {
        while (miThreadCommand != ThreadConstant.THREAD_STOP) {
            ResultSet rs = null;
            String id, isdn, content, shortcode, create_time, priority, retry, program_id;
            long count = 0, rejected_count = 0;
            try {
                fillLogFile();
                //send enquire link
                if (System.currentTimeMillis() >= nextEnquireLink) {
                    enquireLink();
                }
                if (System.currentTimeMillis() >= nextQuery) {
                    rs = DBUtil.query(qQueue, totalThread, threadNumber);
                    mcnMain.setAutoCommit(false);
                    while (rs.next() && miThreadCommand != ThreadConstant.THREAD_STOP) {
                        try {
                            count++;
                            id = rs.getString("Id");
                            isdn = rs.getString("isdn");
                            isdn = Util.fixPhoneNumber(isdn);
                            shortcode = rs.getString("shortcode");
                            content = rs.getString("content");
                            create_time = rs.getString("create_time");
                            priority = rs.getString("priority");
                            retry = rs.getString("retry");
                            program_id = rs.getString("program_id");
                            try {
                                sendTextMessage(isdn, shortcode, content, id);
                                DBUtil.crud(stmtDelete, id);
                                insertMTHistory(id, isdn, content, shortcode, create_time, priority, retry, program_id);
                            } catch (Exception e) {
                                logMonitor(e.getMessage());
                                DBUtil.crud(stmtUpdate, id);
                            }
                        } catch (Exception ex) {
                            logMonitor(ex.getMessage());
                            throw ex;
                        }
                        if (count % batchSize == 0) {
                            mcnMain.commit();
                            logDebugMonitor("Commit {0} records", batchSize);
                        }
                    }
                    logMonitor("Total: " + count + " records:\r\n" +
                               "\tSent: " + (count - rejected_count) + " record(s)");
                    nextQuery = System.currentTimeMillis() + miDelayTime * 1000L;
                }
                for (int iIndex = 0; iIndex < this.miDelayTime && this.miThreadCommand != ThreadConstant.THREAD_STOP && this.mthrMain != null; ++iIndex) {
                    Thread.sleep(1000L);
                }
            } catch (Exception ex) {
                throw new AppException(ex, "submit");
            } finally {
                mcnMain.commit();
                mcnMain.setAutoCommit(true);
                Database.closeObject(rs);
            }
        }
    }

    private void insertMTHistory(String id, String isdn, String content, String shortcode, String create_time, String priority, String retry, String program_id) {
        try {
            DBUtil.crud(insMTHis, id, isdn, content, shortcode, create_time, priority, retry, program_id);
        } catch (Exception e) {
            e.printStackTrace();
            logMonitor("INSERT MT_HISTORY ERROR (Id = " + id + "): " + e.getMessage());
        }
    }

    private void sendTextMessage(String called, String calling, String content, String id) throws Exception {
        SubmitSM submitSM;
        ByteBuffer bbMSG;
        String[] arrMSG = Util.splitByWidth(content, 67);
        int iTotalSegment = arrMSG.length;
        int iRand = Util.randInt();
        for (int i = 0; i < iTotalSegment; i++) {
            submitSM = new SubmitSM();
            bbMSG = new ByteBuffer();
            bbMSG.appendByte((byte) 0x05);
            bbMSG.appendByte((byte) 0x00);
            bbMSG.appendByte((byte) 0x03);
            bbMSG.appendByte((byte) iRand);
            bbMSG.appendByte((byte) iTotalSegment);
            bbMSG.appendByte((byte) (i + 1));
            bbMSG.appendString(arrMSG[i], Data.ENC_UTF16_BE);
            submitSM.setDataCoding((byte) (0x08));
            submitSM.setShortMessageData(bbMSG);
            submitSM.getSourceAddr().setAddress(calling);
            submitSM.getDestAddr().setAddress(called);
            submitSM.setEsmClass((byte) (Data.SM_UDH_GSM));
            if (calling != null && !Objects.equals(calling, ""))
                mSrcAddress.setAddress(calling);
            else setAddressParameter("src-address-range", mSrcAddress);
            mDestAddress.setAddress(called);
            submitSM.setSourceAddr(mSrcAddress);
            submitSM.setDestAddr(mDestAddress);
            submitSM.setReplaceIfPresentFlag((byte) 0);
            submitSM.setRegisteredDelivery((byte) 0);
            logMonitor("Submit data: " + debugString(submitSM, arrMSG[i]));

            if (asynchronous) {
                synchronized (session) {
                    try {
                        session.submit(submitSM);
                        logMonitor("Submit data is Ok.");
                    } catch (Exception ex) {
                        logMonitor("Submit data is NOk: " + id);
                        throw ex;
                    }
                }
            } else {
                SubmitSMResp submitResponse;
                synchronized (session) {
                    try {
                        submitResponse = session.submit(submitSM);
                        logMonitor("Submit data is Ok :" + submitResponse.debugString());
                    } catch (Exception ex) {
                        logMonitor("Submit data is NOk: " + id);
                        throw ex;
                    }
                }
            }
            nextEnquireLink = System.currentTimeMillis() + enquireInterval * 1000L;
            Thread.sleep(20);
        }
    }

    public String debugString(SubmitSM sb, String content) {
        return "(submit: " + sb.getSourceAddr().debugString() + " " + sb.getDestAddr().debugString() + " " + content + ") ";
    }

    ////////////////////////////////////////////////////////
    private void setAddressParameter(String descr, Address address) throws AppException {
        try {
            GroupParameter gp = new GroupParameter(this, descr);
            byte ton = (byte) gp.loadUnsignedInteger("addr-ton");
            byte npi = (byte) gp.loadUnsignedInteger("addr-npi");
            String addr = gp.loadString("address-range");
            address.setTon(ton);
            address.setNpi(npi);
            address.setAddress(addr);
        } catch (WrongLengthOfStringException e) {
            logMonitor("The length of " + descr + " parameter is wrong.");
        }
    }

    private void logDebugMonitor(String log) {
        if (StringUtil.nvl(getParameter("Debug"), "N").equals("Y")) {
            logMonitor(log);
        }
    }

    private void logDebugMonitor(String log, Object... arguments) {
        if (StringUtil.nvl(getParameter("Debug"), "N").equals("Y")) {
            logMonitor(MessageFormat.format(log, arguments));
        }
    }
}
