package eu.deltasw.common.util;

import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

public class RequestContext {
    private static final String USER_ID_ATTRIBUTE = "userId";

    public static String getCurrentUserId() {
        if (RequestContextHolder.getRequestAttributes() == null) {
            return null;
        }
        return (String) RequestContextHolder.currentRequestAttributes()
                .getAttribute(USER_ID_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);
    }

    public static void setCurrentUserId(String userId) {
        if (RequestContextHolder.getRequestAttributes() != null) {
            RequestContextHolder.currentRequestAttributes()
                    .setAttribute(USER_ID_ATTRIBUTE, userId, RequestAttributes.SCOPE_REQUEST);
        }
    }
}