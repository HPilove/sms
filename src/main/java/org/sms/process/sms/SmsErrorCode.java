package org.sms.process.sms;

import java.util.HashMap;

public class SmsErrorCode {
    private static final HashMap<Integer, String> hmErrorCode = new HashMap<Integer, String>();

    static {
        hmErrorCode.put(0, "ESME_ROK	(Ok - Message Acceptable)");
        hmErrorCode.put(1, "ESME_RINVMSGLEN	(Invalid Message Length)");
        hmErrorCode.put(2, "ESME_RINVCMDLEN	(Invalid Command Length)");
        hmErrorCode.put(3, "ESME_RINVCMDID	(Invalid Command ID)");
        hmErrorCode.put(4, "ESME_RINVBNDSTS	(Invalid bind status)");
        hmErrorCode.put(5, "ESME_RALYBND	(Bind attempted when already bound)");
        hmErrorCode.put(6, "ESME_RINVPRTFLG	(Invalid priority flag)");
        hmErrorCode.put(7, "ESME_RINVREGDLVFLG	(Invalid registered-delivery flag)");
        hmErrorCode.put(8, "ESME_RSYSERR	(SMSC system error)");
        hmErrorCode.put(10, "ESME_RINVSRCADR	(Invalid source address)");
        hmErrorCode.put(11, "ESME_RINVDSTADR	(Invalid destination address)");
        hmErrorCode.put(12, "ESME_RINVMSGID	(Invalid message-id)");
        hmErrorCode.put(13, "ESME_RBINDFAIL	(Generic bind failure)");
        hmErrorCode.put(14, "ESME_RINVPASWD	(Invalid password)");
        hmErrorCode.put(15, "ESME_RINVSYSID	(Invalid System-ID)");
        hmErrorCode.put(17, "ESME_RCANCELFAIL	(Cancel failure)");
        hmErrorCode.put(19, "ESME_RREPLACEFAIL	(Replace failure)");
        hmErrorCode.put(21, "ESME_RMSGQFUL	(Too many messages in queue, at present)");
        hmErrorCode.put(22, "ESME_RINVSERTYP	(Invalid services type)");
        hmErrorCode.put(51, "ESME_RINVNUMDESTS	(Invalid number of destination addresses)");
        hmErrorCode.put(52, "ESME_RINVDLNAME	(Invalid name)");
        hmErrorCode.put(64, "ESME_RINVDESTFLAG	(Invalid Destination Flag Option)");
        hmErrorCode.put(66, "ESME_RINVSUBREP	(Invalid value for submit with replace option)");
        hmErrorCode.put(67, "ESME_RINVESMCLASS	(Invalid value for esm_class field)");
        hmErrorCode.put(68, "ESME_RCNTSUBDL	(Cannot submit to a distribution list)");
        hmErrorCode.put(69, "ESME_RSUBMITFAIL	(Generic submission failure)");
        hmErrorCode.put(72, "ESME_RINVSRCTON	(Invalid type of number for source)");
        hmErrorCode.put(73, "ESME_RINVSRCNPI	(Invalid numbering plan indicator for source)");
        hmErrorCode.put(74, "ESME_RINVDSTTON	(Invalid type of number for destination)");
        hmErrorCode.put(75, "ESME_RINVDSTNPI	(Invalid numbering plan indicator for destination)");
        hmErrorCode.put(77, "ESME_RINVSYSTYP	(Invalid esm type)");
        hmErrorCode.put(78, "ESME_RINVREPFLAG	(Invalid submit with replace flag option)");
        hmErrorCode.put(85, "ESME_RINVNUMMSGS	(Invalid number of messages specified for query_last_msgs primitive)");
        hmErrorCode.put(88, "ESME_RTHROTTLED	(SMSC is throttling inbound messages)");
        hmErrorCode.put(97, "ESME_RINVSCHED");
        hmErrorCode.put(98, "ESME_RINVEXPIRY	(Invalid Validity Date)");
        hmErrorCode.put(99, "ESME_RINVDFTMSGID");
        hmErrorCode.put(100, "ESME_RX_T_APPN");
        hmErrorCode.put(101, "ESME_RX_P_APPN");
        hmErrorCode.put(102, "ESME_RX_R_APPN");
        hmErrorCode.put(103, "ESME_RQUERYFAIL	(Query failure)");
        hmErrorCode.put(192, "ESME_RINVOPTPARSTREAM");
        hmErrorCode.put(193, "ESME_ROPTPARNOTALLWD");
        hmErrorCode.put(194, "ESME_RINVPARLEN	(Invalid optional parameter length)");
        hmErrorCode.put(195, "ESME_RMISSINGOPTPARAM	(Missing optional parameter)");
        hmErrorCode.put(196, "ESME_RINVOPTPARAMVAL	(Invalid optional parameter value)");
        hmErrorCode.put(254, "ESME_RDELIVERYFAILURE	(Generic delivery failure)");
        hmErrorCode.put(255, "ESME_RUNKNOWNERR	(Unknown Error)");
    }

    public static String getCommandStatus(int iCmdStatus) {
        return hmErrorCode.get(iCmdStatus);
    }
}
