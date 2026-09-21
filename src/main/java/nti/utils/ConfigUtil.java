package nti.utils;

public class ConfigUtil {
    private static String appBaseUrl = "http://localhost:8081/NTI_JavaEE_ECommerce";

    public static void setAppBaseUrl(String url) {
        if (url != null && !url.isEmpty()) appBaseUrl = url;
    }

    public static String getAppBaseUrl() {
        return appBaseUrl;
    }
}