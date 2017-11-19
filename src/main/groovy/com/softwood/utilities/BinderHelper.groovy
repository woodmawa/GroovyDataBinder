package com.softwood.utilities

class BinderHelper {

    //in lieue of java9 Process
    static long getPID() {
        String processName = java.lang.management.ManagementFactory.getRuntimeMXBean().getName();
        if (processName != null && processName.length() > 0) {
            try {
                return Long.parseLong(processName.split("@")[0])
            }
            catch (Exception e) {
                return 0
            }
        }

        return 0
    }
}
