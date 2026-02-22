package cn.zbx1425.nquestmod.data;

public class SyncConfig {

    public String backendUrl = "";
    public String apiKey = "";
    public int pollIntervalSeconds = 60;
    public boolean enabled = false;

    public boolean isValid() {
        return enabled 
            && backendUrl != null && !backendUrl.isEmpty()
            && apiKey != null && !apiKey.isEmpty()
            && pollIntervalSeconds > 0
            ;
    }
}
