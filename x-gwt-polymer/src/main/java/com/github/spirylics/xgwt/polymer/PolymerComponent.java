package com.github.spirylics.xgwt.polymer;

import com.google.gwt.query.client.Function;
import com.google.gwt.query.client.GQuery;
import com.google.gwt.query.client.Promise;
import com.google.gwt.query.client.js.JsUtils;
import com.google.gwt.user.client.ui.HTMLPanel;

import static com.google.gwt.query.client.GQuery.$;

public class PolymerComponent extends HTMLPanel {
    public final Lifecycle lifecycle;
    public final GQuery gQuery;

    public PolymerComponent(String tag, String html) {
        super(tag, html);
        this.lifecycle = XPolymer.lifecycle(getElement());
        this.gQuery = $(this);
    }

    public <T> T invoke(String method, Object... args) {
        return JsUtils.jsni(getElement(), method, args);
    }

    public Promise invoke(Lifecycle.State state, final String method, final Object... args) {
        return lifecycle.promise(state).then(new Function() {
            @Override
            public Object f(Object... arguments) {
                return invoke(method, args);
            }
        });
    }

    public void off(String event, Function function) {
        gQuery.off(event, function);
    }

    public void on(String event, Function function) {
        gQuery.on(event, function);
    }

    public void once(final String event, final Function function) {
        on(event, new Function() {
            @Override
            public void f() {
                function.f(getEvent(), getArguments());
                off(event, this);
            }
        });
    }

    public void offPropertyChange(String key, Function function) {
        off(key + "-changed", function);
    }

    public void onPropertyChange(String key, Function function) {
        on(key + "-changed", function);
    }

    public void oncePropertyChange(String key, Function function) {
        once(key + "-changed", function);
    }

    public Promise whenEvent(final String event) {
        final Promise.Deferred deferred = GQuery.Deferred();
        once(event, new Function() {
            @Override
            public void f() {
                deferred.resolve();
            }
        });
        return deferred.promise();
    }

    public boolean isPropertyEquals(final String key, final Object value) {
        Object property = gQuery.prop(key);
        return property == value || (property != null && property.equals(value));
    }

    public Promise whenProperty(final String key, final Object value) {
        final Promise.Deferred deferred = GQuery.Deferred();
        if (isPropertyEquals(key, value)) {
            deferred.resolve(value);
        } else {
            onPropertyChange(key, new Function() {
                @Override
                public void f() {
                    if (isPropertyEquals(key, value)) {
                        deferred.resolve(value);
                        offPropertyChange(key, this);
                    }
                }
            });
        }
        return deferred.promise();
    }
}
