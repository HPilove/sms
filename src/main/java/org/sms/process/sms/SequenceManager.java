package org.sms.process.sms;

import smartlib.util.FileUtil;

import java.io.File;

public class SequenceManager {
    private static String sequencePath = null;

    public static void setSequencePath(String path) throws Exception {
        sequencePath = path;
        File flList = new File(path);
        if (!flList.exists())
            FileUtil.forceFolderExist(sequencePath);
    }

    public static synchronized long getSequence(String sequenceName) throws Exception {
        if (sequencePath == null)
            FileUtil.forceFolderExist(sequencePath);
        File flList = new File(sequencePath);
        if (!flList.exists())
            throw new Exception("Sequence path does not exists");
        String[] fileList = flList.list();
        long lngSequenceVal = -1;
        for (int i = 0; i < fileList.length; i++) {
            String fileName = fileList[i];
            String[] flElements = fileName.split("-");
            if (flElements.length > 1) {
                sequenceName = sequenceName.toUpperCase();
                if (flElements[0].toUpperCase().equals(sequenceName)) {
                    try {
                        lngSequenceVal = Long.parseLong(flElements[1]);
                        String sourceFile = sequencePath + "/" + fileName;
                        lngSequenceVal++;
                        String newFileName = sequenceName.toUpperCase() + "-" + lngSequenceVal;
                        String destFile = sequencePath + "/" + newFileName;
                        if (!FileUtil.renameFile(sourceFile, destFile))
                            throw new Exception("Can not get sequence value");
                    } catch (Exception e) {
                        throw new Exception("Sequence with name " + sequenceName + " is invalid");
                    }
                }
            } else {
                FileUtil.deleteFile(sequencePath + "/" + fileName);
            }
        }
        if (lngSequenceVal == -1)
            throw new Exception("No sequence with name " + sequenceName + " found");
        else
            return lngSequenceVal;
    }
}
