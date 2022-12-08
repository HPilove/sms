package org.sms.util;

import java.util.Random;

public class Util {
    public static int randInt() {
        Random generator = new Random();
        return generator.nextInt(255);
    }

    /**
     * Cut 84 prefix if any
     * Chỉ cắt 84 với các số có 11 chữ số, (tránh lỗi đầu số 84 của VNPT)
     */
    public static String formatISDN(String isdn) {
        if (isdn.startsWith("84") && isdn.length() == 11) {
            return isdn.substring(2);
        } else if (isdn.startsWith("0") && isdn.length() == 10) {
            return isdn.substring(1);
        } else
            return isdn;
    }

    /**
     * Force add 84 prefix to isdn if not
     */
    public static String fixPhoneNumber(String isdn) {
        if (isdn == null) {
            return null;
        } else {
            return "84" + formatISDN(isdn);
        }
    }

    public static String[] splitByWidth(String strContent, int iWidth) {
        String[] arrMsg;
        if (strContent == null || strContent.equals("") || iWidth == 0 || strContent.length() <= iWidth) {
            arrMsg = new String[1];
            arrMsg[0] = strContent;
        } else {
            int iPart = strContent.length() / iWidth + 1;
            arrMsg = new String[iPart];
            int iStartPos = 0;

            for (int i = 0; i < iPart - 1; i++) {
                arrMsg[i] = strContent.substring(iStartPos, ((iWidth * (i + 1))));
                iStartPos = (i + 1) * iWidth;
            }
            arrMsg[iPart - 1] = strContent.substring(iStartPos);
        }
        return arrMsg;
    }
}
