package org.sms.process.sms;

import java.util.*;

public class MessageQueueEx {
    @SuppressWarnings("rawtypes")
    private final HashMap level1;

    ////////////////////////////////////////////////////////
    @SuppressWarnings("rawtypes")
    public MessageQueueEx() {
        level1 = new HashMap();
    }

    ////////////////////////////////////////////////////////
    @SuppressWarnings({"rawtypes", "unchecked"})
    public synchronized int size() throws Exception {
        int iTotal = 0;
        List theList = new ArrayList(level1.entrySet());
        Iterator iter = theList.iterator();
        while (iter.hasNext()) {
            Map.Entry en = (Map.Entry) iter.next();
            HashMap entry = (HashMap) en.getValue();
            List theList1 = new ArrayList(entry.entrySet());
            Iterator iter1 = theList1.iterator();
            while (iter1.hasNext()) {
                Map.Entry en1 = (Map.Entry) iter1.next();
                Vector entry2 = (Vector) en1.getValue();
                iTotal = iTotal + entry2.size();
            }
        }

        return iTotal;
    }

    ////////////////////////////////////////////////////////
    @SuppressWarnings("rawtypes")
    public synchronized int size(String strServiceCode, String strGroupCode) throws Exception {
        int iTotal = 0;
        HashMap entry = (HashMap) level1.get(strServiceCode);
        if (entry != null) {
            Vector entry2 = (Vector) entry.get(strGroupCode);
            if (entry2 != null) {
                iTotal = entry2.size();
            }
        }

        return iTotal;
    }

    ////////////////////////////////////////////////////////
    @SuppressWarnings({"rawtypes", "unchecked"})
    public String GroupSize() throws Exception {
        String strReturn = "";
        int iTotal = 0;
        List theList = new ArrayList(level1.entrySet());
        Iterator iter = theList.iterator();
        while (iter.hasNext()) {
            Map.Entry en = (Map.Entry) iter.next();
            HashMap entry = (HashMap) en.getValue();
            List theList1 = new ArrayList(entry.entrySet());
            Iterator iter1 = theList1.iterator();
            while (iter1.hasNext()) {
                Map.Entry en1 = (Map.Entry) iter1.next();
                Vector entry2 = (Vector) en1.getValue();
                iTotal = iTotal + entry2.size();
                strReturn = strReturn + en.getKey() + ":Key:" + en1.getKey() + ":" + entry2.size() + "\n";
            }
        }
        return strReturn;
    }

    ////////////////////////////////////////////////////////
    @SuppressWarnings({"unchecked", "rawtypes"})
    public synchronized void attach(String strServiceCode, String strGroupCode, Object objValue) throws Exception {
        HashMap c1 = (HashMap) level1.get(strServiceCode);
        if (c1 == null) {
            c1 = newServiceCode(level1, strServiceCode);
        }
        Vector vtQueue = (Vector) c1.get(strGroupCode);
        if (vtQueue == null) {
            vtQueue = newGroupCode(c1, strGroupCode);
        }

        vtQueue.addElement(objValue);
    }

    ////////////////////////////////////////////////////////
    @SuppressWarnings({"unchecked", "rawtypes"})
    public Vector newGroupCode(HashMap parent, String strGroup) {
        Vector vtReturn = new Vector();
        parent.put(strGroup, vtReturn);
        return vtReturn;
    }

    ////////////////////////////////////////////////////////
    @SuppressWarnings({"rawtypes", "unchecked"})
    public HashMap newServiceCode(HashMap parent, String strSer) {
        HashMap l2 = new HashMap();
        parent.put(strSer, l2);
        return l2;
    }

    ////////////////////////////////////////////////////////
    @SuppressWarnings("rawtypes")
    public synchronized Object detach(String strServiceCode, String strGroupCode) throws Exception {
        HashMap hsQueue = (HashMap) level1.get(strServiceCode);
        if (hsQueue != null) {
            Vector vtReturn = (Vector) hsQueue.get(strGroupCode);
            if (vtReturn != null && vtReturn.size() > 0) {
                try {
                    return vtReturn.remove(0);
                } catch (ArrayIndexOutOfBoundsException e) {
                    return null;
                }
            } else {
                return null;
            }
        } else {
            return null;
        }
    }
}
