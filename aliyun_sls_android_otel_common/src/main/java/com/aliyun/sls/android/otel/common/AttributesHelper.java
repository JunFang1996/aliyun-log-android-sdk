package com.aliyun.sls.android.otel.common;

import android.content.Context;
import com.aliyun.sls.android.otel.common.utils.DeviceUtils;
import io.opentelemetry.api.common.Attributes;

/**
 * @author yulong.gyl
 * @date 2023/9/15
 */
public final class AttributesHelper {

    private AttributesHelper() {
        //no instance
    }

    public static Attributes create(Context context, String scope) {
        Environment environment = ConfigurationManager.getInstance().getEnvironmentProvider().getEnvironment(scope);
        final String uid = null != environment ? environment.getUid() : "";
        return Attributes.builder()
            .put("net.access", DeviceUtils.getAccessName(context))
            .put("uid", uid)
            .build();
    }
}
