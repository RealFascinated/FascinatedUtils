package cc.fascinated.fascinatedutils.common;

import lombok.experimental.UtilityClass;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@UtilityClass
public class UrlUtils {

    /**
     * Builds a URL by appending non-null query parameters to the provided base URL.
     */
    public static String buildUrl(String baseUrl, Map<String, ?> queryParams) {
        if (baseUrl == null || queryParams == null || queryParams.isEmpty()) {
            return baseUrl;
        }

        StringBuilder urlBuilder = new StringBuilder(baseUrl);
        boolean wroteQuery = baseUrl.contains("?");
        boolean needsSeparator = !baseUrl.endsWith("?") && !baseUrl.endsWith("&");

        for (Map.Entry<String, ?> queryParam : queryParams.entrySet()) {
            String queryKey = queryParam.getKey();
            Object queryValue = queryParam.getValue();
            if (queryKey == null || queryKey.isBlank() || queryValue == null) {
                continue;
            }

            if (needsSeparator) {
                urlBuilder.append(wroteQuery ? '&' : '?');
            }

            urlBuilder.append(encode(queryKey))
                    .append('=')
                    .append(encode(String.valueOf(queryValue)));

            wroteQuery = true;
            needsSeparator = true;
        }

        return urlBuilder.toString();
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}