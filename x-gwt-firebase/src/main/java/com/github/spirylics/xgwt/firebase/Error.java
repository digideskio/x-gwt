package com.github.spirylics.xgwt.firebase;

import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

@JsType(isNative = true)
public interface Error {
    @JsProperty
    String getCode();

    @JsProperty
    String getMessage();
}
