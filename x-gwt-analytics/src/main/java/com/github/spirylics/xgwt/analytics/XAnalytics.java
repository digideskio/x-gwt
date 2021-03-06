package com.github.spirylics.xgwt.analytics;

import com.arcbees.analytics.shared.Analytics;
import com.arcbees.analytics.shared.AnalyticsPlugin;
import com.arcbees.analytics.shared.Storage;
import com.arcbees.analytics.shared.Task;
import com.arcbees.analytics.shared.options.EventsOptions;
import com.arcbees.analytics.shared.options.TimingOptions;
import com.github.spirylics.xgwt.cordova.XCordova;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.gwt.query.client.Function;
import com.google.gwt.query.client.GQuery;
import com.google.gwt.query.client.Promise;
import com.google.gwt.query.client.plugins.deferred.Deferred;
import com.googlecode.gwtphonegap.client.device.Device;
import com.gwtplatform.mvp.shared.proxy.PlaceRequest;
import com.gwtplatform.mvp.shared.proxy.TokenFormatter;

import javax.annotation.Nullable;

public class XAnalytics {
    final Analytics analytics;
    final TokenFormatter tokenFormatter;
    final Joiner labelJoiner = Joiner.on("-").skipNulls();
    final String appName;
    final String appVersion;
    final protected Promise.Deferred readyDeferred = new Deferred();
    com.google.common.base.Function<PlaceRequest, Void> sendRequestFn;

    public XAnalytics(final Analytics analytics,
                      final XCordova cordova,
                      final TokenFormatter tokenFormatter,
                      final String appName,
                      final String appVersion) {
        this.analytics = analytics;
        this.tokenFormatter = tokenFormatter;
        this.appName = appName;
        this.appVersion = appVersion;
        cordova.getDevicePromise().done(new Function() {
            @Override
            public Object f(Object... args) {
                if (cordova.isNative()) {
                    Device device = (Device) args[0];
                    analytics.create().clientId(device.getUuid()).storage(Storage.NONE).go();
                    analytics.setGlobalSettings().generalOptions().disableTask(Task.CHECK_PROTOCOL).go();
                    analytics.enablePlugin(AnalyticsPlugin.DISPLAY);
                    sendRequestFn = new com.google.common.base.Function<PlaceRequest, Void>() {
                        @Override
                        public Void apply(@Nullable PlaceRequest placeRequest) {
                            analytics.sendScreenView()
                                    .screenName(getPlaceToken(placeRequest))
                                    .appTrackingOptions()
                                    .applicationName(appName)
                                    .applicationVersion(appVersion)
                                    .go();
                            ;
                            return null;
                        }
                    };
                    readyDeferred.resolve();
                    return null;
                } else {
                    analytics.create().go();
                    analytics.enablePlugin(AnalyticsPlugin.DISPLAY);
                    sendRequestFn = new com.google.common.base.Function<PlaceRequest, Void>() {
                        @Override
                        public Void apply(@Nullable PlaceRequest placeRequest) {
                            analytics.sendPageView().documentPath(getPlaceToken(placeRequest)).go();
                            return null;
                        }
                    };
                    readyDeferred.resolve();
                }
                return null;
            }
        });
    }

    protected String getPlaceToken(PlaceRequest placeRequest) {
        return tokenFormatter.toPlaceToken(placeRequest);
    }

    public final XAnalytics startTiming(final String category, final String variable) {
        analytics.startTimingEvent(category, variable);
        return this;
    }

    public final XAnalytics endTiming(final String category, final String variable, final String... labels) {
        final TimingOptions timingOptions = analytics.endTimingEvent(category, variable);
        return whenReady(new Function() {
            @Override
            public void f() {
                String label = labelJoiner.join(labels);
                if (!Strings.isNullOrEmpty(label)) {
                    timingOptions.userTimingLabel(label);
                }
                timingOptions.go();
            }
        });
    }

    public final XAnalytics sendPlaceRequest(final PlaceRequest placeRequest) {
        return whenReady(new Function() {
            @Override
            public void f() {
                sendRequestFn.apply(placeRequest);
            }
        });
    }

    public final XAnalytics sendException(final Throwable throwable, final boolean fatal) {
        return whenReady(new Function() {
            @Override
            public void f() {
                analytics
                        .sendException()
                        .exceptionDescription(throwable.getMessage())
                        .isExceptionFatal(fatal)
                        .go();
            }
        });
    }

    public final XAnalytics sendEvent(final String category, final String action, final String label, final Integer value) {
        return whenReady(new Function() {
            @Override
            public void f() {
                EventsOptions eventsOptions = analytics.sendEvent(category, action);
                if (!Strings.isNullOrEmpty(label)) {
                    eventsOptions.eventLabel(label);
                }
                if (value != null) {
                    eventsOptions.eventValue(value);
                }
                eventsOptions.go();
            }
        });
    }

    final XAnalytics whenReady(Function fn) {
        GQuery.when(readyDeferred.promise()).done(fn);
        return this;
    }
}
